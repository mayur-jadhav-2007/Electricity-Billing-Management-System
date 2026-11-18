package electricity.billing.system;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;

/**
 * Phase 11 Verification Suite: Existing Bill Due-Date Immutability.
 *
 * Validates:
 * 1. Newly generated bills receive appropriate due_date (standard 15 days or demo 1 day).
 * 2. When an existing unpaid bill is recalculated, its existing due_date is strictly preserved.
 * 3. Never replaces existing due_date with current date + 15 days.
 * 4. Never modifies original_due_date.
 * 5. 1-day demonstration setting applies only to new bill generation, not recalculation.
 * 6. Historical unpaid bills with NULL due_date preserve NULL on recalculation.
 * 7. Recalculation confirmation and mandatory reason auditing are preserved.
 * 8. Real customer records and baseline bills are completely untouched.
 */
public class Phase11VerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static final String TEST_METER = "999991";
    private static final String TEST_USER = "test_user_p11";

    private static void check(String description, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + description);
            testsFailed++;
        }
    }

    private static void cleanIsolatedTestData() {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement()) {
                    stmt.executeUpdate("DELETE FROM bill WHERE meter_no = '" + TEST_METER + "'");
                    stmt.executeUpdate("DELETE FROM meter_info WHERE meter_no = '" + TEST_METER + "'");
                    stmt.executeUpdate("DELETE FROM login WHERE meter_no = '" + TEST_METER + "' OR username = '" + TEST_USER + "'");
                    stmt.executeUpdate("DELETE FROM customer WHERE meter_no = '" + TEST_METER + "'");
                    stmt.executeUpdate("DELETE FROM audit_logs WHERE actor_id = '" + TEST_USER + "' OR entity_id = '" + TEST_METER + "' OR entity_id LIKE '" + TEST_METER + ":%'");
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning isolated test data: " + e.getMessage());
        }
    }

    private static void setupIsolatedCustomer() throws SQLException {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement()) {
                    stmt.executeUpdate("INSERT INTO customer (name, meter_no, address, city, state, email, phone, status) " +
                                       "VALUES ('Phase 11 Test User', '" + TEST_METER + "', '101 Safe Testing Ln', 'Lucknow', 'UP', 'p11@test.com', '9999999991', 'Active')");
                    stmt.executeUpdate("INSERT INTO meter_info (meter_no, meter_location, meter_type, phase_code, bill_type, days) " +
                                       "VALUES ('" + TEST_METER + "', 'Outside', 'Electric Meter', '011', 'Normal', '30')");
                    stmt.executeUpdate("INSERT INTO login (meter_no, username, name, password, user) " +
                                       "VALUES ('" + TEST_METER + "', '" + TEST_USER + "', 'P11 User', 'pass123', 'Customer')");
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  PHASE 11 VERIFICATION: EXISTING BILL DUE-DATE IMMUTABILITY");
        System.out.println("===============================================================");

        cleanIsolatedTestData();
        LatePaymentService.clearSimulatedDate();
        LpscConfig.restoreDefaultGraceDays();

        try (Conn c = new Conn()) {
            // Snapshot baseline bills 1..18 and bill 46
            java.sql.Date[] baselineDueDates = new java.sql.Date[19];
            try (Statement st = c.c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT bill_id, due_date FROM bill WHERE bill_id <= 18 ORDER BY bill_id ASC")) {
                while (rs.next()) {
                    baselineDueDates[rs.getInt("bill_id")] = rs.getDate("due_date");
                }
            }
            java.sql.Date bill46DueBefore = null;
            try (Statement st = c.c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT due_date FROM bill WHERE bill_id = 46")) {
                if (rs.next()) bill46DueBefore = rs.getDate(1);
            }

            setupIsolatedCustomer();
            java.sql.Date today = LatePaymentService.getDatabaseCurrentDate(c.c);

            // =================================================================
            // TEST 1: New Bill Generation with Specific Due Date (1-Day Demo)
            // =================================================================
            System.out.println("\n>>> TEST 1: NEW BILL GENERATION WITH 1-DAY DEMO GRACE PERIOD");
            CalculateBill.BillProcessResult genResult = CalculateBill.processBill(
                c.c, TEST_METER, "October", "100", false, null, LpscConfig.DEMO_GRACE_DAYS
            );
            check("New bill inserted successfully (SUCCESS_INSERTED)",
                genResult.status == CalculateBill.BillProcessResult.Status.SUCCESS_INSERTED);

            java.sql.Date expected1DayDue = LatePaymentService.calculateDueDate(today, 1);
            check("New bill due_date is today + 1 day (" + expected1DayDue + ")",
                genResult.dueDate != null && genResult.dueDate.toString().equals(expected1DayDue.toString()));
            check("New bill original_due_date matches due_date (" + expected1DayDue + ")",
                genResult.originalDueDate != null && genResult.originalDueDate.toString().equals(expected1DayDue.toString()));

            long demoBillId = genResult.billId;

            // =================================================================
            // TEST 2: Recalculate Existing Unpaid Bill -> Due Date Must Be Preserved!
            // =================================================================
            System.out.println("\n>>> TEST 2: RECALCULATING UNPAID BILL PRESERVES EXISTING DUE DATE");

            // First: Confirmation check
            CalculateBill.BillProcessResult probe = CalculateBill.processBill(
                c.c, TEST_METER, "October", "150", false
            );
            check("Recalculation requires explicit confirmation (CONFIRMATION_REQUIRED)",
                probe.status == CalculateBill.BillProcessResult.Status.CONFIRMATION_REQUIRED);
            check("Probe result reports existing due date accurately",
                probe.dueDate != null && probe.dueDate.toString().equals(expected1DayDue.toString()));

            // Now perform confirmed recalculation with standard 15-day setting active in config
            LpscConfig.setActiveGraceDays(LpscConfig.STANDARD_GRACE_DAYS);
            String adminReason = "Phase 11 Audit: meter recalibration test";
            CalculateBill.BillProcessResult recalcResult = CalculateBill.processBill(
                c.c, TEST_METER, "October", "150", true, adminReason, LpscConfig.STANDARD_GRACE_DAYS
            );

            check("Recalculation succeeded (SUCCESS_UPDATED)",
                recalcResult.status == CalculateBill.BillProcessResult.Status.SUCCESS_UPDATED);
            check("Recalculation result returns preserved original due date in res.dueDate",
                recalcResult.dueDate != null && recalcResult.dueDate.toString().equals(expected1DayDue.toString()));
            check("Recalculation result returns unchanged originalDueDate",
                recalcResult.originalDueDate != null && recalcResult.originalDueDate.toString().equals(expected1DayDue.toString()));

            // Query MySQL directly to verify database columns
            try (PreparedStatement ps = c.c.prepareStatement(
                    "SELECT units, total_bill, due_date, original_due_date, status FROM bill WHERE bill_id = ?")) {
                ps.setLong(1, demoBillId);
                try (ResultSet rs = ps.executeQuery()) {
                    check("Authoritative bill record exists in MySQL", rs.next());
                    check("Bill units updated from 100 to 150", "150".equals(rs.getString("units")));
                    check("Bill total_bill updated from 410.00 to 560.00", "560.00".equals(rs.getString("total_bill")));

                    java.sql.Date dbDueDate = rs.getDate("due_date");
                    java.sql.Date dbOrigDueDate = rs.getDate("original_due_date");

                    check("DATABASE IMMUTABILITY: due_date remains exactly " + expected1DayDue + " (NEVER replaced by today + 15d)",
                        dbDueDate != null && dbDueDate.toString().equals(expected1DayDue.toString()));
                    check("DATABASE IMMUTABILITY: original_due_date remains exactly " + expected1DayDue,
                        dbOrigDueDate != null && dbOrigDueDate.toString().equals(expected1DayDue.toString()));

                    java.sql.Date datePlus15 = LatePaymentService.calculateDueDate(today, 15);
                    check("due_date is strictly NOT equal to today + 15 days (" + datePlus15 + ")",
                        !datePlus15.toString().equals(dbDueDate.toString()));
                }
            }

            // =================================================================
            // TEST 3: Recalculate Existing Unpaid Bill with NULL Due Date (Historical)
            // =================================================================
            System.out.println("\n>>> TEST 3: RECALCULATING UNPAID BILL WITH NULL DUE DATE PRESERVES NULL");

            // Insert a bill directly with NULL due_date (simulating historical unpaid bill)
            long nullDueBillId = -1;
            try (PreparedStatement ps = c.c.prepareStatement(
                    "INSERT INTO bill (meter_no, month, units, total_bill, status, due_date, original_due_date, late_fee) " +
                    "VALUES (?, 'November', '50', '260.00', 'Not Paid', NULL, NULL, 0.00)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, TEST_METER);
                ps.executeUpdate();
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) nullDueBillId = gk.getLong(1);
                }
            }

            CalculateBill.BillProcessResult recalcNull = CalculateBill.processBill(
                c.c, TEST_METER, "November", "75", true, "Historical unpaid correction"
            );
            check("Recalculation of NULL-due bill succeeded",
                recalcNull.status == CalculateBill.BillProcessResult.Status.SUCCESS_UPDATED);
            check("Result object preserves NULL due_date", recalcNull.dueDate == null);
            check("Result object preserves NULL original_due_date", recalcNull.originalDueDate == null);

            // Verify in MySQL
            try (PreparedStatement ps = c.c.prepareStatement("SELECT units, total_bill, due_date, original_due_date FROM bill WHERE bill_id = ?")) {
                ps.setLong(1, nullDueBillId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    check("MySQL bill units updated to 75", "75".equals(rs.getString("units")));
                    check("MySQL due_date remains strictly NULL (no silent date assignment)", rs.getDate("due_date") == null);
                    check("MySQL original_due_date remains strictly NULL", rs.getDate("original_due_date") == null);
                }
            }

            // =================================================================
            // TEST 4: Recalculation Audit Trail
            // =================================================================
            System.out.println("\n>>> TEST 4: AUDIT TRAIL VERIFICATION FOR RECALCULATION");
            boolean auditFound = false;
            try (PreparedStatement ps = c.c.prepareStatement(
                    "SELECT description FROM audit_logs WHERE action_type = 'BILL_RECALCULATED' AND entity_id LIKE ?")) {
                ps.setString(1, "%" + TEST_METER + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String desc = rs.getString("description");
                        if (desc != null && desc.contains(adminReason)) {
                            auditFound = true;
                            break;
                        }
                    }
                }
            }
            check("Audit log records BILL_RECALCULATED with administrative reason", auditFound);

            // =================================================================
            // TEST 5: Baseline Customer & Bill Safety Verification
            // =================================================================
            System.out.println("\n>>> TEST 5: BASELINE CUSTOMERS AND EXISTING BILLS INTEGRITY");
            boolean baselineIntact = true;
            try (Statement st = c.c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT bill_id, due_date FROM bill WHERE bill_id <= 18 ORDER BY bill_id ASC")) {
                while (rs.next()) {
                    int id = rs.getInt("bill_id");
                    java.sql.Date curDue = rs.getDate("due_date");
                    java.sql.Date oldDue = baselineDueDates[id];
                    if (oldDue == null && curDue != null) baselineIntact = false;
                    if (oldDue != null && !oldDue.toString().equals(curDue != null ? curDue.toString() : "")) baselineIntact = false;
                }
            }
            check("All 18 historical baseline bills maintain unchanged NULL due dates", baselineIntact);

            java.sql.Date bill46DueAfter = null;
            try (Statement st = c.c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT due_date FROM bill WHERE bill_id = 46")) {
                if (rs.next()) bill46DueAfter = rs.getDate(1);
            }
            check("Existing real customer bill #46 due date untouched (" + bill46DueBefore + ")",
                bill46DueBefore != null && bill46DueBefore.toString().equals(bill46DueAfter != null ? bill46DueAfter.toString() : ""));

        } catch (Exception e) {
            e.printStackTrace();
            check("Unexpected exception during Phase 11 test: " + e.getMessage(), false);
        } finally {
            LatePaymentService.clearSimulatedDate();
            LpscConfig.restoreDefaultGraceDays();
            cleanIsolatedTestData();
            UserSession.invalidate();
        }

        System.out.println("\n===============================================================");
        System.out.println("  PHASE 11 TEST SUMMARY");
        System.out.println("  Passed: " + testsPassed);
        System.out.println("  Failed: " + testsFailed);
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.err.println("Phase 11 verification suite FAILED.");
            System.exit(1);
        } else {
            System.out.println("Phase 11 verification suite PASSED successfully!");
        }
    }
}
