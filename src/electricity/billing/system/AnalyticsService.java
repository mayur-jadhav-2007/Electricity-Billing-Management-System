package electricity.billing.system;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

public class AnalyticsService {

    public static class FilterCriteria {
        public String month = "All Months";
        public String billStatus = "All Statuses";
        public String customerMeter = "All Customers";
        public String complaintCategory = "All Categories";
        public String complaintStatus = "All Statuses";
        public String complaintPriority = "All Priorities";

        public void reset() {
            month = "All Months";
            billStatus = "All Statuses";
            customerMeter = "All Customers";
            complaintCategory = "All Categories";
            complaintStatus = "All Statuses";
            complaintPriority = "All Priorities";
        }
    }

    public static class BillingSummary {
        public int totalBills = 0;
        public int paidBills = 0;
        public int unpaidBills = 0;
        public int commercialBills = 0;
        public int otherStatusBills = 0;
        public BigDecimal totalBilledAmount = BigDecimal.ZERO;
        public BigDecimal paidBilledAmount = BigDecimal.ZERO;
        public BigDecimal unpaidBilledAmount = BigDecimal.ZERO;
        public BigDecimal commercialBilledAmount = BigDecimal.ZERO;

        public Map<String, Integer> monthBillCounts = new LinkedHashMap<>();
        public Map<String, BigDecimal> monthBilledAmounts = new LinkedHashMap<>();
        public List<CustomerBillRow> customerRows = new ArrayList<>();
    }

    public static class CustomerBillRow {
        public String meterNo;
        public String customerName;
        public String month;
        public BigDecimal units;
        public BigDecimal totalBill;
        public String status;

        public CustomerBillRow(String meterNo, String customerName, String month, BigDecimal units, BigDecimal totalBill, String status) {
            this.meterNo = meterNo;
            this.customerName = customerName;
            this.month = month;
            this.units = units;
            this.totalBill = totalBill;
            this.status = status;
        }
    }

    public static class RevenueSummary {
        public BigDecimal totalCollectedRevenue = BigDecimal.ZERO; // Revenue from Paid bills ONLY
        public BigDecimal totalOutstandingAmount = BigDecimal.ZERO; // Unpaid bills ONLY
        public BigDecimal totalStandardBilled = BigDecimal.ZERO;    // Paid + Unpaid
        public BigDecimal commercialExcludedAmount = BigDecimal.ZERO; // Commercial records explicitly isolated
        public double collectionPercentage = 0.0; // Paid Revenue / (Paid Revenue + Unpaid Revenue) * 100
        public BigDecimal totalLateFeesCollected = BigDecimal.ZERO; // Late payment surcharges collected

        public int paidCount = 0;
        public int unpaidCount = 0;
        public int commercialCount = 0;
    }

    public static class ConsumptionSummary {
        public BigDecimal totalUnitsConsumed = BigDecimal.ZERO;
        public Map<String, BigDecimal> monthlyConsumption = new LinkedHashMap<>();
        public List<CustomerConsumptionRow> customerConsumptionList = new ArrayList<>();
        public List<CustomerConsumptionRow> topConsumers = new ArrayList<>();
        public List<CustomerConsumptionRow> lowestConsumers = new ArrayList<>();
    }

    public static class CustomerConsumptionRow {
        public String meterNo;
        public String customerName;
        public BigDecimal totalUnits;
        public BigDecimal totalBilled;
        public int billCount;

        public CustomerConsumptionRow(String meterNo, String customerName, BigDecimal totalUnits, BigDecimal totalBilled, int billCount) {
            this.meterNo = meterNo;
            this.customerName = customerName;
            this.totalUnits = totalUnits;
            this.totalBilled = totalBilled;
            this.billCount = billCount;
        }
    }

    public static class CustomerSummary {
        public int totalCustomers = 0;
        public int activeCustomers = 0;
        public int inactiveCustomers = 0;
        public List<CustomerDetailRow> customerDetails = new ArrayList<>();
    }

    public static class CustomerDetailRow {
        public String meterNo;
        public String name;
        public String city;
        public String phone;
        public String status;
        public int billCount;
        public BigDecimal totalUnits;
        public BigDecimal totalBilled;

        public CustomerDetailRow(String meterNo, String name, String city, String phone, String status, int billCount, BigDecimal totalUnits, BigDecimal totalBilled) {
            this.meterNo = meterNo;
            this.name = name;
            this.city = city;
            this.phone = phone;
            this.status = status;
            this.billCount = billCount;
            this.totalUnits = totalUnits;
            this.totalBilled = totalBilled;
        }
    }

    public static class ComplaintAnalyticsSummary {
        public int totalComplaints = 0;
        public int pendingCount = 0;
        public int inProgressCount = 0;
        public int resolvedCount = 0;
        public int rejectedCount = 0;

        public int lowPriorityCount = 0;
        public int mediumPriorityCount = 0;
        public int highPriorityCount = 0;
        public int urgentPriorityCount = 0;

        public Map<String, Integer> categoryDistribution = new LinkedHashMap<>();
        public double resolutionPercentage = 0.0; // Resolved / Total * 100
        public List<ComplaintReportRow> complaintRows = new ArrayList<>();
    }

    public static class ComplaintReportRow {
        public int complaintId;
        public String referenceNumber;
        public String meterNo;
        public String customerName;
        public String category;
        public String subject;
        public String priority;
        public String status;
        public String assignedAdmin;
        public String createdAt;

        public ComplaintReportRow(int complaintId, String referenceNumber, String meterNo, String customerName, 
                                  String category, String subject, String priority, String status, 
                                  String assignedAdmin, String createdAt) {
            this.complaintId = complaintId;
            this.referenceNumber = referenceNumber;
            this.meterNo = meterNo;
            this.customerName = customerName;
            this.category = category;
            this.subject = subject;
            this.priority = priority;
            this.status = status;
            this.assignedAdmin = assignedAdmin;
            this.createdAt = createdAt;
        }
    }

    /**
     * Calculates safe percentage avoiding divide-by-zero errors.
     */
    public static double calculateSafePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
    }

    public static double calculateSafePercentage(int numerator, int denominator) {
        if (denominator <= 0) return 0.0;
        return BigDecimal.valueOf((double) numerator / denominator * 100)
                         .setScale(2, RoundingMode.HALF_UP)
                         .doubleValue();
    }

    // =========================================================================
    // 1. BILLING ANALYTICS
    // =========================================================================

    public static BillingSummary getBillingSummary(FilterCriteria filters) {
        BillingSummary summary = new BillingSummary();
        StringBuilder sql = new StringBuilder(
            "SELECT b.meter_no, c.name AS customer_name, b.month, b.units, b.total_bill, b.status " +
            "FROM bill b " +
            "LEFT JOIN customer c ON b.meter_no = c.meter_no " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();
        applyBillFilters(sql, params, filters);
        sql.append(" ORDER BY b.meter_no, FIELD(b.month, 'January','February','March','April','May','June','July','August','September','October','November','December')");

        try (Conn conn = new Conn()) {
            if (conn.c != null) {
                try (PreparedStatement ps = conn.c.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            summary.totalBills++;
                            String meter = rs.getString("meter_no");
                            String name = rs.getString("customer_name");
                            String month = rs.getString("month");
                            String st = rs.getString("status");
                            BigDecimal billAmt = parseBigDecimal(rs.getString("total_bill"));
                            BigDecimal units = parseBigDecimal(rs.getString("units"));

                            summary.totalBilledAmount = summary.totalBilledAmount.add(billAmt);

                            if ("Paid".equalsIgnoreCase(st)) {
                                summary.paidBills++;
                                summary.paidBilledAmount = summary.paidBilledAmount.add(billAmt);
                            } else if ("Not Paid".equalsIgnoreCase(st)) {
                                summary.unpaidBills++;
                                summary.unpaidBilledAmount = summary.unpaidBilledAmount.add(billAmt);
                            } else if ("Commercial".equalsIgnoreCase(st)) {
                                summary.commercialBills++;
                                summary.commercialBilledAmount = summary.commercialBilledAmount.add(billAmt);
                            } else {
                                summary.otherStatusBills++;
                            }

                            // Monthly summaries
                            if (month != null) {
                                summary.monthBillCounts.put(month, summary.monthBillCounts.getOrDefault(month, 0) + 1);
                                summary.monthBilledAmounts.put(month, summary.monthBilledAmounts.getOrDefault(month, BigDecimal.ZERO).add(billAmt));
                            }

                            summary.customerRows.add(new CustomerBillRow(
                                meter, name != null ? name : "N/A", month, units, billAmt, st
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return summary;
    }

    // =========================================================================
    // 2. REVENUE & COLLECTION ANALYTICS
    // =========================================================================

    public static RevenueSummary getRevenueSummary(FilterCriteria filters) {
        RevenueSummary summary = new RevenueSummary();
        StringBuilder sql = new StringBuilder(
            "SELECT b.total_bill, b.status, b.late_fee FROM bill b LEFT JOIN customer c ON b.meter_no = c.meter_no WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();
        applyBillFilters(sql, params, filters);

        try (Conn conn = new Conn()) {
            if (conn.c != null) {
                try (PreparedStatement ps = conn.c.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String st = rs.getString("status");
                            BigDecimal amt = parseBigDecimal(rs.getString("total_bill"));
                            BigDecimal lateFee = parseBigDecimal(rs.getString("late_fee"));

                            if ("Paid".equalsIgnoreCase(st)) {
                                summary.paidCount++;
                                summary.totalCollectedRevenue = summary.totalCollectedRevenue.add(amt);
                                summary.totalStandardBilled = summary.totalStandardBilled.add(amt);
                                summary.totalLateFeesCollected = summary.totalLateFeesCollected.add(lateFee);
                            } else if ("Not Paid".equalsIgnoreCase(st)) {
                                summary.unpaidCount++;
                                summary.totalOutstandingAmount = summary.totalOutstandingAmount.add(amt);
                                summary.totalStandardBilled = summary.totalStandardBilled.add(amt);
                            } else if ("Commercial".equalsIgnoreCase(st)) {
                                summary.commercialCount++;
                                summary.commercialExcludedAmount = summary.commercialExcludedAmount.add(amt);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Collection Percentage calculated strictly from clearly classified Paid and Not Paid bills
        BigDecimal classifiedTotal = summary.totalCollectedRevenue.add(summary.totalOutstandingAmount);
        summary.collectionPercentage = calculateSafePercentage(summary.totalCollectedRevenue, classifiedTotal);

        return summary;
    }

    // =========================================================================
    // 3. ELECTRICITY CONSUMPTION ANALYTICS
    // =========================================================================

    public static ConsumptionSummary getConsumptionSummary(FilterCriteria filters) {
        ConsumptionSummary summary = new ConsumptionSummary();
        StringBuilder sql = new StringBuilder(
            "SELECT b.meter_no, c.name AS customer_name, b.month, b.units, b.total_bill " +
            "FROM bill b " +
            "LEFT JOIN customer c ON b.meter_no = c.meter_no " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();
        applyBillFilters(sql, params, filters);

        Map<String, CustomerConsumptionRow> custMap = new HashMap<>();

        try (Conn conn = new Conn()) {
            if (conn.c != null) {
                try (PreparedStatement ps = conn.c.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String meter = rs.getString("meter_no");
                            String name = rs.getString("customer_name");
                            String month = rs.getString("month");
                            BigDecimal units = parseBigDecimal(rs.getString("units"));
                            BigDecimal amt = parseBigDecimal(rs.getString("total_bill"));

                            summary.totalUnitsConsumed = summary.totalUnitsConsumed.add(units);

                            if (month != null) {
                                summary.monthlyConsumption.put(month, 
                                    summary.monthlyConsumption.getOrDefault(month, BigDecimal.ZERO).add(units));
                            }

                            if (meter != null) {
                                CustomerConsumptionRow row = custMap.get(meter);
                                if (row == null) {
                                    row = new CustomerConsumptionRow(meter, name != null ? name : "N/A", units, amt, 1);
                                    custMap.put(meter, row);
                                } else {
                                    row.totalUnits = row.totalUnits.add(units);
                                    row.totalBilled = row.totalBilled.add(amt);
                                    row.billCount++;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        summary.customerConsumptionList = new ArrayList<>(custMap.values());
        summary.customerConsumptionList.sort((a, b) -> b.totalUnits.compareTo(a.totalUnits));

        // Top and lowest consumers
        int topN = Math.min(5, summary.customerConsumptionList.size());
        for (int i = 0; i < topN; i++) {
            summary.topConsumers.add(summary.customerConsumptionList.get(i));
        }

        List<CustomerConsumptionRow> ascList = new ArrayList<>(summary.customerConsumptionList);
        ascList.sort(Comparator.comparing(a -> a.totalUnits));
        int lowN = Math.min(5, ascList.size());
        for (int i = 0; i < lowN; i++) {
            summary.lowestConsumers.add(ascList.get(i));
        }

        return summary;
    }

    // =========================================================================
    // 4. CUSTOMER ANALYTICS
    // =========================================================================

    public static CustomerSummary getCustomerSummary(FilterCriteria filters) {
        CustomerSummary summary = new CustomerSummary();
        StringBuilder sql = new StringBuilder(
            "SELECT c.meter_no, c.name, c.city, c.phone, c.status, " +
            "COUNT(b.month) AS bill_count, " +
            "COALESCE(SUM(CAST(b.units AS DECIMAL(10,2))), 0) AS total_units, " +
            "COALESCE(SUM(CAST(b.total_bill AS DECIMAL(10,2))), 0) AS total_billed " +
            "FROM customer c " +
            "LEFT JOIN bill b ON c.meter_no = b.meter_no " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();
        if (filters.customerMeter != null && !filters.customerMeter.equalsIgnoreCase("All Customers") && !filters.customerMeter.trim().isEmpty()) {
            sql.append(" AND (LOWER(c.meter_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?))");
            params.add("%" + filters.customerMeter.trim() + "%");
            params.add("%" + filters.customerMeter.trim() + "%");
        }

        sql.append(" GROUP BY c.meter_no, c.name, c.city, c.phone, c.status ORDER BY c.meter_no");

        try (Conn conn = new Conn()) {
            if (conn.c != null) {
                try (PreparedStatement ps = conn.c.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            summary.totalCustomers++;
                            String st = rs.getString("status");
                            if ("Active".equalsIgnoreCase(st)) {
                                summary.activeCustomers++;
                            } else if ("Inactive".equalsIgnoreCase(st)) {
                                summary.inactiveCustomers++;
                            }

                            summary.customerDetails.add(new CustomerDetailRow(
                                rs.getString("meter_no"),
                                rs.getString("name"),
                                rs.getString("city"),
                                rs.getString("phone"),
                                st,
                                rs.getInt("bill_count"),
                                parseBigDecimal(rs.getString("total_units")),
                                parseBigDecimal(rs.getString("total_billed"))
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return summary;
    }

    // =========================================================================
    // 5. COMPLAINT ANALYTICS
    // =========================================================================

    public static ComplaintAnalyticsSummary getComplaintSummary(FilterCriteria filters) {
        ComplaintAnalyticsSummary summary = new ComplaintAnalyticsSummary();
        StringBuilder sql = new StringBuilder(
            "SELECT c.complaint_id, c.reference_number, c.meter_no, cust.name AS customer_name, " +
            "c.category, c.subject, c.priority, c.status, c.assigned_admin, c.created_at " +
            "FROM complaints c " +
            "LEFT JOIN customer cust ON c.meter_no = cust.meter_no " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();
        if (filters.complaintCategory != null && !filters.complaintCategory.equalsIgnoreCase("All Categories") && !filters.complaintCategory.trim().isEmpty()) {
            sql.append(" AND c.category = ?");
            params.add(filters.complaintCategory.trim());
        }
        if (filters.complaintStatus != null && !filters.complaintStatus.equalsIgnoreCase("All Statuses") && !filters.complaintStatus.trim().isEmpty()) {
            sql.append(" AND c.status = ?");
            params.add(filters.complaintStatus.trim());
        }
        if (filters.complaintPriority != null && !filters.complaintPriority.equalsIgnoreCase("All Priorities") && !filters.complaintPriority.trim().isEmpty()) {
            sql.append(" AND c.priority = ?");
            params.add(filters.complaintPriority.trim());
        }
        if (filters.customerMeter != null && !filters.customerMeter.equalsIgnoreCase("All Customers") && !filters.customerMeter.trim().isEmpty()) {
            sql.append(" AND (LOWER(c.meter_no) LIKE LOWER(?) OR LOWER(cust.name) LIKE LOWER(?))");
            params.add("%" + filters.customerMeter.trim() + "%");
            params.add("%" + filters.customerMeter.trim() + "%");
        }

        sql.append(" ORDER BY c.complaint_id DESC");

        try (Conn conn = new Conn()) {
            if (conn.c != null) {
                try (PreparedStatement ps = conn.c.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            summary.totalComplaints++;
                            String status = rs.getString("status");
                            String priority = rs.getString("priority");
                            String category = rs.getString("category");

                            if ("Pending".equalsIgnoreCase(status)) {
                                summary.pendingCount++;
                            } else if ("In Progress".equalsIgnoreCase(status)) {
                                summary.inProgressCount++;
                            } else if ("Resolved".equalsIgnoreCase(status)) {
                                summary.resolvedCount++;
                            } else if ("Rejected".equalsIgnoreCase(status)) {
                                summary.rejectedCount++;
                            }

                            if ("Low".equalsIgnoreCase(priority)) {
                                summary.lowPriorityCount++;
                            } else if ("Medium".equalsIgnoreCase(priority)) {
                                summary.mediumPriorityCount++;
                            } else if ("High".equalsIgnoreCase(priority)) {
                                summary.highPriorityCount++;
                            } else if ("Urgent".equalsIgnoreCase(priority)) {
                                summary.urgentPriorityCount++;
                            }

                            if (category != null) {
                                summary.categoryDistribution.put(category, 
                                    summary.categoryDistribution.getOrDefault(category, 0) + 1);
                            }

                            summary.complaintRows.add(new ComplaintReportRow(
                                rs.getInt("complaint_id"),
                                rs.getString("reference_number"),
                                rs.getString("meter_no"),
                                rs.getString("customer_name"),
                                category,
                                rs.getString("subject"),
                                priority,
                                status,
                                rs.getString("assigned_admin"),
                                rs.getString("created_at")
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Clearly defined denominator: Total complaints
        summary.resolutionPercentage = calculateSafePercentage(summary.resolvedCount, summary.totalComplaints);

        return summary;
    }

    // =========================================================================
    // 6. CSV REPORT EXPORT UTILITY
    // =========================================================================

    public static boolean exportToCSV(File targetFile, String[] headers, List<String[]> rows) {
        if (targetFile == null || headers == null || rows == null) return false;
        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile))) {
            // Write Header
            writer.println(formatCSVLine(headers));

            // Write Rows
            for (String[] row : rows) {
                writer.println(formatCSVLine(row));
            }
            writer.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String formatCSVLine(String[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            String val = values[i] != null ? values[i] : "";
            // If value contains comma, quotes or newline, wrap in quotes and escape existing quotes
            if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
                val = "\"" + val.replace("\"", "\"\"") + "\"";
            }
            sb.append(val);
        }
        return sb.toString();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private static void applyBillFilters(StringBuilder sql, List<Object> params, FilterCriteria filters) {
        if (filters.month != null && !filters.month.equalsIgnoreCase("All Months") && !filters.month.trim().isEmpty()) {
            sql.append(" AND LOWER(b.month) = LOWER(?)");
            params.add(filters.month.trim());
        }
        if (filters.billStatus != null && !filters.billStatus.equalsIgnoreCase("All Statuses") && !filters.billStatus.trim().isEmpty()) {
            sql.append(" AND LOWER(b.status) = LOWER(?)");
            params.add(filters.billStatus.trim());
        }
        if (filters.customerMeter != null && !filters.customerMeter.equalsIgnoreCase("All Customers") && !filters.customerMeter.trim().isEmpty()) {
            sql.append(" AND (LOWER(b.meter_no) LIKE LOWER(?) OR LOWER(c.name) LIKE LOWER(?))");
            params.add("%" + filters.customerMeter.trim() + "%");
            params.add("%" + filters.customerMeter.trim() + "%");
        }
    }

    private static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
