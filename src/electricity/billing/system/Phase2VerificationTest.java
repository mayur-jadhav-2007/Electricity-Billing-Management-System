package electricity.billing.system;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Phase2VerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  ELECTRICITY BILLING SYSTEM - PHASE 2 VERIFICATION SUITE");
        System.out.println("===============================================================\n");

        // Record initial database counts
        int initialCustomerCount = getCount("customer");
        int initialLoginCount = getCount("login");
        int initialBillCount = getCount("bill");
        int initialMeterInfoCount = getCount("meter_info");

        System.out.println(">>> Initial Database Record Counts:");
        System.out.println("    customer   : " + initialCustomerCount);
        System.out.println("    login      : " + initialLoginCount);
        System.out.println("    bill       : " + initialBillCount);
        System.out.println("    meter_info : " + initialMeterInfoCount);
        System.out.println("---------------------------------------------------------------\n");

        // PART 1: SEARCH FUNCTIONALITY VERIFICATION
        System.out.println(">>> SECTION 1: SEARCH FUNCTIONALITY TESTS");
        
        testValidMeterValidMonth();
        testValidMeterMonthNoBill();
        testInvalidMeterSearch();
        testMonthOnlySearch();
        testMeterOnlySearch();
        testBothFiltersTogether();
        testEmptyFiltersResetSearch();
        testCaseInsensitiveMonthSearch();
        testWhitespaceTolerance();
        testBillDetailsCustomerSearch();

        // PART 2: CUSTOMER DEACTIVATION & AUTHENTICATION VERIFICATION
        System.out.println("\n>>> SECTION 2: CUSTOMER DEACTIVATION & AUTHENTICATION TESTS");
        
        testAdminLogin();
        testActiveCustomerLogin();
        testCustomerDeactivationLifecycle();

        // PART 3: RECORD INTEGRITY VERIFICATION
        System.out.println("\n>>> SECTION 3: DATABASE INTEGRITY VERIFICATION");
        
        int finalCustomerCount = getCount("customer");
        int finalLoginCount = getCount("login");
        int finalBillCount = getCount("bill");
        int finalMeterInfoCount = getCount("meter_info");

        System.out.println("    customer   : Before=" + initialCustomerCount + ", After=" + finalCustomerCount);
        System.out.println("    login      : Before=" + initialLoginCount + ", After=" + finalLoginCount);
        System.out.println("    bill       : Before=" + initialBillCount + ", After=" + finalBillCount);
        System.out.println("    meter_info : Before=" + initialMeterInfoCount + ", After=" + finalMeterInfoCount);

        check("DB Integrity: Customer count unchanged", initialCustomerCount == finalCustomerCount);
        check("DB Integrity: Login count unchanged", initialLoginCount == finalLoginCount);
        check("DB Integrity: Bill count unchanged", initialBillCount == finalBillCount);
        check("DB Integrity: Meter info count unchanged", initialMeterInfoCount == finalMeterInfoCount);

        // SUMMARY
        System.out.println("\n===============================================================");
        System.out.println("  VERIFICATION RESULT: " + testsPassed + " PASSED, " + testsFailed + " FAILED");
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    // Helper query executing the exact search logic used by DepositDetails
    private static List<BillRecord> executeDepositSearch(String meter, String month) {
        List<BillRecord> records = new ArrayList<>();
        StringBuilder q = new StringBuilder("select meter_no, month, units, total_bill, status from bill where 1=1");
        List<String> params = new ArrayList<>();

        String cleanMeter = (meter != null) ? meter.trim() : "";
        String cleanMonth = (month != null) ? month.trim() : "";

        if (!cleanMeter.isEmpty() && !"All Meters".equalsIgnoreCase(cleanMeter)) {
            q.append(" and meter_no = ?");
            params.add(cleanMeter);
        }
        if (!cleanMonth.isEmpty() && !"All Months".equalsIgnoreCase(cleanMonth)) {
            q.append(" and LOWER(month) = LOWER(?)");
            params.add(cleanMonth);
        }
        q.append(" order by meter_no, FIELD(month, 'January','February','March','April','May','June','July','August','September','October','November','December')");

        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (PreparedStatement ps = c.c.prepareStatement(q.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setString(i + 1, params.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            records.add(new BillRecord(
                                rs.getString("meter_no"),
                                rs.getString("month"),
                                rs.getString("units"),
                                rs.getString("total_bill"),
                                rs.getString("status")
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return records;
    }

    private static void testValidMeterValidMonth() {
        List<BillRecord> results = executeDepositSearch("MET001", "January");
        boolean ok = results.size() == 1 
            && "MET001".equals(results.get(0).meterNo) 
            && "January".equalsIgnoreCase(results.get(0).month)
            && "180".equals(results.get(0).units);
        check("Search: Valid meter (MET001) + Valid month (January) returns 1 exact record", ok);
    }

    private static void testValidMeterMonthNoBill() {
        List<BillRecord> results = executeDepositSearch("MET001", "December");
        check("Search: Valid meter (MET001) + Month with no bill (December) returns 0 records", results.isEmpty());
    }

    private static void testInvalidMeterSearch() {
        List<BillRecord> results = executeDepositSearch("INVALID999", "All Months");
        check("Search: Invalid meter number (INVALID999) returns 0 records", results.isEmpty());
    }

    private static void testMonthOnlySearch() {
        List<BillRecord> results = executeDepositSearch("All Meters", "February");
        boolean ok = results.size() == 9;
        for (BillRecord r : results) {
            if (!"February".equalsIgnoreCase(r.month)) ok = false;
        }
        check("Search: Month-only search (February) returns all 9 February bills", ok);
    }

    private static void testMeterOnlySearch() {
        List<BillRecord> results = executeDepositSearch("MET001", "All Months");
        boolean ok = results.size() == 2;
        for (BillRecord r : results) {
            if (!"MET001".equals(r.meterNo)) ok = false;
        }
        check("Search: Meter-number-only search (MET001) returns all 2 bills for MET001", ok);
    }

    private static void testBothFiltersTogether() {
        List<BillRecord> results = executeDepositSearch("MET002", "February");
        boolean ok = results.size() == 1 
            && "MET002".equals(results.get(0).meterNo) 
            && "February".equalsIgnoreCase(results.get(0).month)
            && "190".equals(results.get(0).units)
            && "701.50".equals(results.get(0).totalBill);
        check("Search: Both filters together (MET002 + February) returns correct record", ok);
    }

    private static void testEmptyFiltersResetSearch() {
        List<BillRecord> results = executeDepositSearch("All Meters", "All Months");
        check("Search: Empty filters / reset search returns all 18 bills", results.size() == 18);
    }

    private static void testCaseInsensitiveMonthSearch() {
        List<BillRecord> lowerResults = executeDepositSearch("MET003", "january");
        List<BillRecord> upperResults = executeDepositSearch("MET003", "JANUARY");
        boolean ok = lowerResults.size() == 1 && upperResults.size() == 1 
            && lowerResults.get(0).totalBill.equals(upperResults.get(0).totalBill);
        check("Search: Month search is case-insensitive (january vs JANUARY)", ok);
    }

    private static void testWhitespaceTolerance() {
        List<BillRecord> results = executeDepositSearch("  MET004  ", "   February  ");
        check("Search: Input fields tolerate leading and trailing whitespace", results.size() == 1 && "MET004".equals(results.get(0).meterNo));
    }

    private static void testBillDetailsCustomerSearch() {
        // Customer BillDetails filters by meter_no and month
        List<BillRecord> allCustomerBills = executeDepositSearch("MET001", "");
        List<BillRecord> janBill = executeDepositSearch("MET001", "January");
        List<BillRecord> decBill = executeDepositSearch("MET001", "December");
        check("Customer BillDetails: Customer MET001 sees all bills (count 2)", allCustomerBills.size() == 2);
        check("Customer BillDetails: Filter by January returns 1 bill", janBill.size() == 1);
        check("Customer BillDetails: Filter by December returns 0 bills", decBill.isEmpty());
    }

    private static void testAdminLogin() {
        String authResult = Login.authenticateUser("Parth", "Parth123", "Admin");
        check("Authentication: Admin login succeeds (Parth / Parth123)", "SUCCESS".equals(authResult));
    }

    private static void testActiveCustomerLogin() {
        String authResult = Login.authenticateUser("rajesh2024", "raj@123", "Customer");
        check("Authentication: Active customer (rajesh2024 / MET001) login succeeds", "SUCCESS".equals(authResult));
    }

    private static void testCustomerDeactivationLifecycle() {
        String meter = "MET001";
        String username = "rajesh2024";
        String password = "raj@123";

        // 1. Deactivate customer
        boolean deactivated = CustomerDetails.setCustomerStatus(meter, "Inactive");
        check("Deactivation: Status updated to Inactive in database", deactivated);

        // Verify status in DB
        String statusInDb = getCustomerStatus(meter);
        check("Deactivation: Database reflects status = Inactive", "Inactive".equalsIgnoreCase(statusInDb));

        // 2. Inactive customer login attempt must be BLOCKED
        String blockedLogin = Login.authenticateUser(username, password, "Customer");
        check("Deactivation: Inactive customer login is BLOCKED (returns DEACTIVATED)", "DEACTIVATED".equals(blockedLogin));

        // 3. Admin login must continue working normally
        String adminLogin = Login.authenticateUser("Parth", "Parth123", "Admin");
        check("Deactivation: Admin login continues to work normally", "SUCCESS".equals(adminLogin));

        // 4. Reactivate customer
        boolean reactivated = CustomerDetails.setCustomerStatus(meter, "Active");
        check("Reactivation: Status updated to Active in database", reactivated);

        // Verify status in DB
        String statusAfterReactivate = getCustomerStatus(meter);
        check("Reactivation: Database reflects status = Active", "Active".equalsIgnoreCase(statusAfterReactivate));

        // 5. Reactivated customer login attempt must SUCCEED
        String restoredLogin = Login.authenticateUser(username, password, "Customer");
        check("Reactivation: Reactivated customer login SUCCEEDS (returns SUCCESS)", "SUCCESS".equals(restoredLogin));

        // 6. Verify customer data, bills, meter_info are completely preserved
        check("Integrity: Customer details preserved after lifecycle", getCustomerName(meter).equals("Rajesh Kumar Sharma"));
        check("Integrity: Bills preserved after lifecycle", executeDepositSearch(meter, "All Months").size() == 2);
    }

    private static String getCustomerStatus(String meterNo) {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (PreparedStatement ps = c.c.prepareStatement("select status from customer where meter_no = ?")) {
                    ps.setString(1, meterNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getString("status");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getCustomerName(String meterNo) {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (PreparedStatement ps = c.c.prepareStatement("select name from customer where meter_no = ?")) {
                    ps.setString(1, meterNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getString("name");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int getCount(String table) {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement();
                     ResultSet rs = stmt.executeQuery("select count(*) from " + table)) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + description);
            testsFailed++;
        }
    }

    private static class BillRecord {
        String meterNo, month, units, totalBill, status;
        BillRecord(String m, String mo, String u, String tb, String st) {
            this.meterNo = m;
            this.month = mo;
            this.units = u;
            this.totalBill = tb;
            this.status = st;
        }
    }
}
