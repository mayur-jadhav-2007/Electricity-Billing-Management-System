package electricity.billing.system;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service providing authoritative Late Payment Surcharge (LPSC) calculation,
 * due date management, and transactional bill settlement in accordance with
 * UPERC FY 2025-26 Rate Schedule provisions.
 *
 * All operations on existing individual bills strictly use bill_id.
 */
public final class LatePaymentService {

    private LatePaymentService() {
        // Prevent instantiation
    }

    /**
     * Immutable calculation result containing itemized tier amounts,
     * days delayed, terminal rounded surcharge, and total payable.
     */
    public static class LpscResult {
        public final BigDecimal principal;
        public final Date dueDate;
        public final Date paymentDate;
        public final long delayDays;
        public final long tier1Days;
        public final long tier2Days;
        public final BigDecimal tier1Amount;
        public final BigDecimal tier2Amount;
        public final BigDecimal unroundedTotal;
        public final BigDecimal lateFee;
        public final BigDecimal totalPayable;
        public final boolean isEligible;

        public LpscResult(BigDecimal principal, Date dueDate, Date paymentDate,
                          long delayDays, long tier1Days, long tier2Days,
                          BigDecimal tier1Amount, BigDecimal tier2Amount,
                          BigDecimal unroundedTotal, BigDecimal lateFee,
                          BigDecimal totalPayable, boolean isEligible) {
            this.principal = principal;
            this.dueDate = dueDate;
            this.paymentDate = paymentDate;
            this.delayDays = delayDays;
            this.tier1Days = tier1Days;
            this.tier2Days = tier2Days;
            this.tier1Amount = tier1Amount;
            this.tier2Amount = tier2Amount;
            this.unroundedTotal = unroundedTotal;
            this.lateFee = lateFee;
            this.totalPayable = totalPayable;
            this.isEligible = isEligible;
        }

        @Override
        public String toString() {
            return String.format("LpscResult[P=₹%s, Due=%s, Pay=%s, Delay=%dd (T1:%dd, T2:%dd), Fee=₹%s, Total=₹%s]",
                    principal, dueDate, paymentDate, delayDays, tier1Days, tier2Days, lateFee, totalPayable);
        }
    }

    /**
     * Representation of a database row from the 'bill' table.
     */
    public static class BillRecord {
        public final long billId;
        public final String meterNo;
        public final String month;
        public final String units;
        public final String totalBill;
        public final String status;
        public final Date dueDate;
        public final Date originalDueDate;
        public final Timestamp paidAt;
        public final BigDecimal lateFee;
        public final BigDecimal paidAmount;

        public BillRecord(long billId, String meterNo, String month, String units,
                          String totalBill, String status, Date dueDate,
                          Date originalDueDate, Timestamp paidAt,
                          BigDecimal lateFee, BigDecimal paidAmount) {
            this.billId = billId;
            this.meterNo = meterNo;
            this.month = month;
            this.units = units;
            this.totalBill = totalBill;
            this.status = status;
            this.dueDate = dueDate;
            this.originalDueDate = originalDueDate;
            this.paidAt = paidAt;
            this.lateFee = (lateFee != null) ? lateFee : BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
            this.paidAmount = paidAmount;
        }

        public BigDecimal getPrincipalBigDecimal() {
            try {
                if (totalBill == null || totalBill.trim().isEmpty()) {
                    return BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
                }
                String clean = totalBill.replace("₹", "").replace("$", "").replace(",", "").trim();
                return new BigDecimal(clean).setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
            } catch (Exception e) {
                return BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
            }
        }

        public boolean isPaid() {
            return "Paid".equalsIgnoreCase(status);
        }

        public boolean isHistorical() {
            return dueDate == null;
        }
    }

    /**
     * Calculates the Late Payment Surcharge (LPSC) for a given principal, due date, and payment date.
     *
     * Rules:
     * 1. If due date is NULL (historical bill): LPSC is ₹0.00.
     * 2. If principal <= 0 or payment is on/before due date: LPSC is ₹0.00.
     * 3. Tier 1: P * min(N, 90) * 0.0125 / 30 (unrounded).
     * 4. Tier 2: P * max(N - 90, 0) * 0.0200 / 30 (unrounded).
     * 5. Unrounded Sum: Tier 1 + Tier 2.
     * 6. Terminal Rounding: unroundedSum.setScale(2, HALF_UP).
     */
    public static LpscResult calculateLpsc(BigDecimal principal, Date dueDate, Date paymentDate) {
        BigDecimal safePrincipal = (principal != null)
                ? principal.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE)
                : BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);

        // Historical bill with no recorded due date
        if (dueDate == null) {
            return new LpscResult(safePrincipal, null, paymentDate, 0, 0, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE),
                    safePrincipal, false);
        }

        if (paymentDate == null) {
            paymentDate = new Date(System.currentTimeMillis());
        }

        long delayDays = calculateDelayDays(dueDate, paymentDate);

        // On-time payment (no delay) or zero principal
        if (delayDays <= 0 || safePrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            return new LpscResult(safePrincipal, dueDate, paymentDate, Math.max(0, delayDays), 0, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE),
                    safePrincipal, true);
        }

        long tier1Days = Math.min(delayDays, LpscConfig.TIER1_MAX_DAYS);
        long tier2Days = Math.max(0, delayDays - LpscConfig.TIER1_MAX_DAYS);

        // Calculate Tier 1 without intermediate rounding: P * min(N, 90) * 0.0125 / 30
        BigDecimal t1DaysBd = BigDecimal.valueOf(tier1Days);
        BigDecimal tier1Amount = safePrincipal
                .multiply(t1DaysBd)
                .multiply(LpscConfig.TIER1_MONTHLY_RATE)
                .divide(LpscConfig.DAYS_PER_MONTH, LpscConfig.INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        // Calculate Tier 2 without intermediate rounding: P * max(N - 90, 0) * 0.0200 / 30
        BigDecimal tier2Amount = BigDecimal.ZERO;
        if (tier2Days > 0) {
            BigDecimal t2DaysBd = BigDecimal.valueOf(tier2Days);
            tier2Amount = safePrincipal
                    .multiply(t2DaysBd)
                    .multiply(LpscConfig.TIER2_MONTHLY_RATE)
                    .divide(LpscConfig.DAYS_PER_MONTH, LpscConfig.INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
        }

        // Sum unrounded components first
        BigDecimal unroundedTotal = tier1Amount.add(tier2Amount);

        // Round once at the very end using HALF_UP to 2 decimal places
        BigDecimal lateFee = unroundedTotal.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
        BigDecimal totalPayable = safePrincipal.add(lateFee);

        return new LpscResult(safePrincipal, dueDate, paymentDate, delayDays,
                tier1Days, tier2Days, tier1Amount, tier2Amount, unroundedTotal,
                lateFee, totalPayable, true);
    }

    /**
     * Overload supporting java.util.Date.
     */
    public static LpscResult calculateLpsc(BigDecimal principal, java.util.Date dueDate, java.util.Date paymentDate) {
        Date sqlDue = (dueDate != null) ? new Date(dueDate.getTime()) : null;
        Date sqlPay = (paymentDate != null) ? new Date(paymentDate.getTime()) : null;
        return calculateLpsc(principal, sqlDue, sqlPay);
    }

    /**
     * Calculates the exact calendar day delay between dueDate and paymentDate.
     */
    public static long calculateDelayDays(Date dueDate, Date paymentDate) {
        if (dueDate == null || paymentDate == null) {
            return 0L;
        }
        LocalDate dueLocal = dueDate.toLocalDate();
        LocalDate payLocal = paymentDate.toLocalDate();
        return ChronoUnit.DAYS.between(dueLocal, payLocal);
    }

    /**
     * Overload supporting java.util.Date.
     */
    public static long calculateDelayDays(java.util.Date dueDate, java.util.Date paymentDate) {
        if (dueDate == null || paymentDate == null) {
            return 0L;
        }
        Date sqlDue = (dueDate instanceof Date) ? (Date) dueDate : new Date(dueDate.getTime());
        Date sqlPay = (paymentDate instanceof Date) ? (Date) paymentDate : new Date(paymentDate.getTime());
        return calculateDelayDays(sqlDue, sqlPay);
    }

    /**
     * Calculates due date by adding grace period days to generation date.
     */
    public static Date calculateDueDate(Date fromDate, int graceDays) {
        if (fromDate == null) {
            fromDate = new Date(System.currentTimeMillis());
        }
        LocalDate local = fromDate.toLocalDate().plusDays(graceDays);
        return Date.valueOf(local);
    }

    /**
     * Overload supporting java.util.Date.
     */
    public static Date calculateDueDate(java.util.Date fromDate, int graceDays) {
        Date sqlFrom = (fromDate instanceof Date) ? (Date) fromDate : ((fromDate != null) ? new Date(fromDate.getTime()) : null);
        return calculateDueDate(sqlFrom, graceDays);
    }

    /**
     * Queries database server for current date to ensure authoritative time.
     */
    public static Date getDatabaseCurrentDate(Connection conn) {
        if (conn != null) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT CURRENT_DATE()")) {
                if (rs.next()) {
                    return rs.getDate(1);
                }
            } catch (SQLException ignored) {}
        }
        return new Date(System.currentTimeMillis());
    }

    /** Controlled evaluation date for demonstration and safe testing without altering DB server clock */
    private static volatile Date simulatedCurrentDate = null;

    /**
     * Sets a controlled simulated date for demonstration or testing.
     * When set, getEffectiveDate(conn) returns this date without changing the database server date.
     */
    public static void setSimulatedDate(Date date) {
        simulatedCurrentDate = date;
    }

    /**
     * Overload supporting java.util.Date.
     */
    public static void setSimulatedDate(java.util.Date date) {
        simulatedCurrentDate = (date != null) ? new Date(date.getTime()) : null;
    }

    /**
     * Clears any active simulated date, restoring normal live database server date querying.
     */
    public static void clearSimulatedDate() {
        simulatedCurrentDate = null;
    }

    /**
     * Checks if a controlled simulated date is currently active.
     */
    public static boolean isSimulatedDateActive() {
        return simulatedCurrentDate != null;
    }

    /**
     * Returns the simulated date if active, or queries the authoritative current date from MySQL.
     * Guarantees safe demonstration and testing without changing live server dates.
     */
    public static Date getEffectiveDate(Connection conn) {
        if (simulatedCurrentDate != null) {
            return simulatedCurrentDate;
        }
        return getDatabaseCurrentDate(conn);
    }

    /**
     * Fetches a single bill record by its primary key bill_id.
     */
    public static BillRecord getBillById(Connection conn, long billId) throws SQLException {
        String sql = "SELECT bill_id, meter_no, month, units, total_bill, status, " +
                     "       due_date, original_due_date, paid_at, late_fee, paid_amount " +
                     "FROM `bill` WHERE bill_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBillRecord(rs);
                }
            }
        }
        return null;
    }

    /**
     * Fetches all bill records matching a given meter_no and month.
     * Used for disambiguation when multiple bills match a meter and month.
     */
    public static List<BillRecord> getBillsByMeterAndMonth(Connection conn, String meterNo, String month) throws SQLException {
        List<BillRecord> list = new ArrayList<>();
        String sql = "SELECT bill_id, meter_no, month, units, total_bill, status, " +
                     "       due_date, original_due_date, paid_at, late_fee, paid_amount " +
                     "FROM `bill` WHERE meter_no = ? AND LOWER(month) = LOWER(?) ORDER BY bill_id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, meterNo);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBillRecord(rs));
                }
            }
        }
        return list;
    }

    /**
     * Fetches all unpaid bills for a given meter_no.
     */
    public static List<BillRecord> getUnpaidBillsByMeter(Connection conn, String meterNo) throws SQLException {
        List<BillRecord> list = new ArrayList<>();
        String sql = "SELECT bill_id, meter_no, month, units, total_bill, status, " +
                     "       due_date, original_due_date, paid_at, late_fee, paid_amount " +
                     "FROM `bill` WHERE meter_no = ? AND status != 'Paid' ORDER BY bill_id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, meterNo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBillRecord(rs));
                }
            }
        }
        return list;
    }

    private static BillRecord mapResultSetToBillRecord(ResultSet rs) throws SQLException {
        long id = rs.getLong("bill_id");
        String meter = rs.getString("meter_no");
        String month = rs.getString("month");
        String units = rs.getString("units");
        String total = rs.getString("total_bill");
        String status = rs.getString("status");
        Date due = rs.getDate("due_date");
        Date origDue = rs.getDate("original_due_date");
        Timestamp paidAt = rs.getTimestamp("paid_at");
        BigDecimal lateFee = rs.getBigDecimal("late_fee");
        BigDecimal paidAmount = rs.getBigDecimal("paid_amount");

        return new BillRecord(id, meter, month, units, total, status,
                due, origDue, paidAt, lateFee, paidAmount);
    }
}
