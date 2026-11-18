package electricity.billing.system;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Verification & Demonstration Suite for 1-Day Due Period Support.
 *
 * Validates:
 * 1. Default grace period is 15 days.
 * 2. Administrator can switch to 1 day for demonstration.
 * 3. Newly generated bill receives due_date = generation_date + 1 day.
 * 4. Existing bills (baseline 1..18) due dates remain strictly unchanged.
 * 5. LPSC calculation formula remains unchanged.
 * 6. Controlled date evaluation without changing live database server date.
 * 7. Payment settlement with 1-day delay surcharge.
 * 8. Administrator restores 15 days default setting after demonstration.
 * 9. Safe cleanup and zero regression on database baseline.
 */
public class LpscDemonstrationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static final String DEMO_METER = "999996";
    private static final String DEMO_USER = "demo_admin_p9";

    private static void check(String description, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + description);
            testsFailed++;
        }
    }

    private static void cleanDemoArtifacts() {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement()) {
                    stmt.executeUpdate("DELETE FROM bill WHERE meter_no = '" + DEMO_METER + "'");
                    stmt.executeUpdate("DELETE FROM meter_info WHERE meter_no = '" + DEMO_METER + "'");
                    stmt.executeUpdate("DELETE FROM login WHERE meter_no = '" + DEMO_METER + "' OR username = '" + DEMO_USER + "'");
                    stmt.executeUpdate("DELETE FROM customer WHERE meter_no = '" + DEMO_METER + "'");
                    stmt.executeUpdate("DELETE FROM audit_logs WHERE actor_id = '" + DEMO_USER + "' OR entity_id = '" + DEMO_METER + "' OR entity_id LIKE '" + DEMO_METER + ":%'");
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning demo artifacts: " + e.getMessage());
        }
    }

    private static void setupDemoCustomer() throws SQLException {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement()) {
                    stmt.executeUpdate("INSERT INTO customer (name, meter_no, address, city, state, email, phone, status) " +
                                       "VALUES ('Demo Customer', '" + DEMO_METER + "', '100 Demo St', 'Lucknow', 'UP', 'demo@test.com', '9999999996', 'Active')");
                    stmt.executeUpdate("INSERT INTO meter_info (meter_no, meter_location, meter_type, phase_code, bill_type, days) " +
                                       "VALUES ('" + DEMO_METER + "', 'Outside', 'Electric Meter', '011', 'Normal', '30')");
                    stmt.executeUpdate("INSERT INTO login (meter_no, username, name, password, user) " +
                                       "VALUES ('" + DEMO_METER + "', '" + DEMO_USER + "', 'Demo User', 'pass123', 'Customer')");
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  LPSC 1-DAY DUE PERIOD DEMONSTRATION & VERIFICATION SUITE");
        System.out.println("===============================================================");

        cleanDemoArtifacts();
        LatePaymentService.clearSimulatedDate();
        LpscConfig.restoreDefaultGraceDays();

        try (Conn c = new Conn()) {
            // STEP 1: Verify Initial / Default Setting is 15 Days
            System.out.println("\n>>> STEP 1: INITIAL DEFAULT GRACE PERIOD VERIFICATION");
            check("Default grace period is exactly 15 calendar days",
                LpscConfig.DEFAULT_GRACE_DAYS == 15 &&
                LpscConfig.STANDARD_GRACE_DAYS == 15 &&
                LpscConfig.getActiveGraceDays() == 15);

            // Snapshot existing baseline bills due dates
            java.sql.Date[] baselineDueDates = new java.sql.Date[19];
            try (Statement st = c.c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT bill_id, due_date FROM bill WHERE bill_id <= 18 ORDER BY bill_id ASC")) {
                while (rs.next()) {
                    baselineDueDates[rs.getInt("bill_id")] = rs.getDate("due_date");
                }
            }

            // STEP 2: Administrator Selects 1 Day for Demonstration
            System.out.println("\n>>> STEP 2: ADMINISTRATOR SELECTS 1-DAY DUE PERIOD");
            LpscConfig.setActiveGraceDays(1);
            check("Administrator successfully selects 1-day due period (getActiveGraceDays = 1)",
                LpscConfig.getActiveGraceDays() == 1);

            // STEP 3: Setup Test Customer and Generate 1-Day Bill
            System.out.println("\n>>> STEP 3: NEW BILL GENERATION WITH 1-DAY DUE PERIOD");
            setupDemoCustomer();

            java.sql.Date today = LatePaymentService.getDatabaseCurrentDate(c.c);
            CalculateBill.BillProcessResult genResult = CalculateBill.processBill(
                c.c, DEMO_METER, "October", "100", false, null, LpscConfig.DEMO_GRACE_DAYS
            );

            check("New bill created successfully with Status.SUCCESS_INSERTED",
                genResult.status == CalculateBill.BillProcessResult.Status.SUCCESS_INSERTED);

            java.sql.Date expectedDueDate1Day = LatePaymentService.calculateDueDate(today, 1);
            check("New bill due_date is exactly 1 day after generation date (" + expectedDueDate1Day + ")",
                genResult.dueDate != null && genResult.dueDate.toString().equals(expectedDueDate1Day.toString()));

            check("original_due_date matches initial 1-day due_date",
                genResult.originalDueDate != null && genResult.originalDueDate.toString().equals(expectedDueDate1Day.toString()));

            long demoBillId = genResult.billId;

            // STEP 4: Verify Existing Bill Due Dates Never Changed
            System.out.println("\n>>> STEP 4: VERIFY EXISTING BILL DUE DATES REMAIN UNCHANGED");
            boolean existingIntact = true;
            try (Statement st = c.c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT bill_id, due_date FROM bill WHERE bill_id <= 18 ORDER BY bill_id ASC")) {
                while (rs.next()) {
                    int id = rs.getInt("bill_id");
                    java.sql.Date curDue = rs.getDate("due_date");
                    java.sql.Date oldDue = baselineDueDates[id];
                    if (oldDue == null && curDue != null) existingIntact = false;
                    if (oldDue != null && !oldDue.toString().equals(curDue != null ? curDue.toString() : "")) existingIntact = false;
                }
            }
            check("All 18 historical baseline bills maintain unchanged due dates (NULL untouched)", existingIntact);

            // STEP 5: Verify LPSC Calculation Formula Unchanged
            System.out.println("\n>>> STEP 5: VERIFY LPSC FORMULA IS UNCHANGED");
            check("Tier 1 monthly rate remains 1.25% (0.0125)",
                LpscConfig.TIER1_MONTHLY_RATE.compareTo(new BigDecimal("0.0125")) == 0);
            check("Tier 2 monthly rate remains 2.00% (0.0200)",
                LpscConfig.TIER2_MONTHLY_RATE.compareTo(new BigDecimal("0.0200")) == 0);
            check("Monthly divisor remains 30 days",
                LpscConfig.DAYS_PER_MONTH.intValue() == 30);

            // STEP 6: Safe Testing with Controlled Dates without Changing Server Clock
            System.out.println("\n>>> STEP 6: CONTROLLED DATES TESTING (WITHOUT ALTERING DB SERVER CLOCK)");
            BigDecimal billPrincipal = new BigDecimal(genResult.newTotal); // 100 units * 3 + 110 fixed = ₹410.00

            // Scenario A: Evaluated on due date (Day 1) -> 0 delay -> ₹0.00 late fee
            LatePaymentService.setSimulatedDate(expectedDueDate1Day);
            LatePaymentService.LpscResult lpscOnDue = LatePaymentService.calculateLpsc(billPrincipal, expectedDueDate1Day, expectedDueDate1Day);
            check("On Due Date: 0 delay days, ₹0.00 surcharge, total payable = ₹" + billPrincipal,
                lpscOnDue.delayDays == 0 &&
                lpscOnDue.lateFee.compareTo(BigDecimal.ZERO) == 0 &&
                lpscOnDue.totalPayable.compareTo(billPrincipal) == 0);

            // Scenario B: Evaluated 1 day after due date (Day 2) -> 1 day delay
            Calendar cal = Calendar.getInstance();
            cal.setTime(expectedDueDate1Day);
            cal.add(Calendar.DATE, 1); // 1 day delayed beyond due date
            java.sql.Date oneDayLateDate = new java.sql.Date(cal.getTimeInMillis());

            LatePaymentService.setSimulatedDate(oneDayLateDate);
            LatePaymentService.LpscResult lpsc1DayLate = LatePaymentService.calculateLpsc(billPrincipal, expectedDueDate1Day, oneDayLateDate);

            // Expected math: 410 * 1 * 0.0125 / 30 = 0.170833... -> ₹0.17
            BigDecimal expectedFee1Day = new BigDecimal("0.17");
            BigDecimal expectedTotal1Day = billPrincipal.add(expectedFee1Day);
            check("1 Day Delayed Beyond 1-Day Due Date: Delay = 1 day, Late Fee = ₹0.17, Total = ₹" + expectedTotal1Day,
                lpsc1DayLate.delayDays == 1 &&
                lpsc1DayLate.lateFee.compareTo(expectedFee1Day) == 0 &&
                lpsc1DayLate.totalPayable.compareTo(expectedTotal1Day) == 0);

            // Scenario C: Evaluated 10 days after due date (Day 11) -> 10 days delay
            cal.setTime(expectedDueDate1Day);
            cal.add(Calendar.DATE, 10);
            java.sql.Date tenDaysLateDate = new java.sql.Date(cal.getTimeInMillis());

            LatePaymentService.setSimulatedDate(tenDaysLateDate);
            LatePaymentService.LpscResult lpsc10DaysLate = LatePaymentService.calculateLpsc(billPrincipal, expectedDueDate1Day, tenDaysLateDate);

            // Expected math: 410 * 10 * 0.0125 / 30 = 1.70833... -> ₹1.71
            BigDecimal expectedFee10Days = new BigDecimal("1.71");
            BigDecimal expectedTotal10Days = billPrincipal.add(expectedFee10Days);
            check("10 Days Delayed Beyond 1-Day Due Date: Delay = 10 days, Late Fee = ₹1.71, Total = ₹" + expectedTotal10Days,
                lpsc10DaysLate.delayDays == 10 &&
                lpsc10DaysLate.lateFee.compareTo(expectedFee10Days) == 0 &&
                lpsc10DaysLate.totalPayable.compareTo(expectedTotal10Days) == 0);

            // Settle payment with simulated date
            PaymentGateway.PaymentResult payRes = PaymentGateway.executePayment(
                c.c, demoBillId, expectedTotal10Days, "Demonstration Sandbox"
            );
            check("Atomic payment settlement succeeds on 1-day demo bill with ₹1.71 surcharge",
                payRes.status == PaymentGateway.PaymentResult.Status.SUCCESS &&
                payRes.lateFee.compareTo(expectedFee10Days) == 0);

            // Verify live DB server date was never modified
            LatePaymentService.clearSimulatedDate();
            check("Simulated date cleared successfully",
                !LatePaymentService.isSimulatedDateActive());

            java.sql.Date realServerDate = LatePaymentService.getDatabaseCurrentDate(c.c);
            check("Live MySQL database server date was NEVER modified (CURDATE = " + realServerDate + ")",
                realServerDate != null && realServerDate.toString().equals(today.toString()));

            // STEP 7: Restore Default 15 Days
            System.out.println("\n>>> STEP 7: RESTORE 15-DAY DEFAULT GRACE PERIOD");
            LpscConfig.restoreDefaultGraceDays();
            check("Administrator restores default 15-day grace period (getActiveGraceDays = 15)",
                LpscConfig.getActiveGraceDays() == 15);

            // Generate another bill to prove restored 15-day setting applies to subsequent new bills
            CalculateBill.BillProcessResult restoredResult = CalculateBill.processBill(
                c.c, DEMO_METER, "November", "100", false
            );
            java.sql.Date expectedDueDate15Days = LatePaymentService.calculateDueDate(today, 15);
            check("Subsequent new bill receives restored 15-day due date (" + expectedDueDate15Days + ")",
                restoredResult.status == CalculateBill.BillProcessResult.Status.SUCCESS_INSERTED &&
                restoredResult.dueDate != null &&
                restoredResult.dueDate.toString().equals(expectedDueDate15Days.toString()));

        } catch (Exception e) {
            e.printStackTrace();
            check("Unexpected exception during demonstration: " + e.getMessage(), false);
        } finally {
            LatePaymentService.clearSimulatedDate();
            LpscConfig.restoreDefaultGraceDays();
            cleanDemoArtifacts();
            UserSession.invalidate();
        }

        System.out.println("\n===============================================================");
        System.out.println("  1-DAY DUE PERIOD DEMONSTRATION SUMMARY");
        System.out.println("  Assertions Passed: " + testsPassed);
        System.out.println("  Assertions Failed: " + testsFailed);
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.err.println("Demonstration suite FAILED.");
            System.exit(1);
        } else {
            System.out.println("All 1-day due period demonstration checks PASSED successfully!");
        }
    }
}
