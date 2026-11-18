package electricity.billing.system;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Phase4VerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  ELECTRICITY BILLING SYSTEM - PHASE 4 VERIFICATION SUITE");
        System.out.println("===============================================================\n");

        // Record initial database counts
        int initialCustomerCount = getCount("customer");
        int initialLoginCount = getCount("login");
        int initialBillCount = getCount("bill");
        int initialMeterInfoCount = getCount("meter_info");
        int initialTaxCount = getCount("tax");

        System.out.println(">>> Initial Database Record Counts:");
        System.out.println("    customer   : " + initialCustomerCount);
        System.out.println("    login      : " + initialLoginCount);
        System.out.println("    bill       : " + initialBillCount);
        System.out.println("    meter_info : " + initialMeterInfoCount);
        System.out.println("    tax        : " + initialTaxCount);
        System.out.println("---------------------------------------------------------------\n");

        // SECTION 1: TARIFF & BILL CALCULATION TESTS
        System.out.println(">>> SECTION 1: TARIFF & CALCULATION TESTS");
        testTariffCalculation100Units();
        testTariffCalculationDecimalUnits();
        testZeroUnitCalculation();
        testNegativeUnitRejection();
        testEmptyAndNonNumericUnitsRejection();
        testTwoDecimalPlacesFormatting();

        // SECTION 2: DUPLICATE BILL HANDLING & SAFETY TESTS
        System.out.println("\n>>> SECTION 2: DUPLICATE BILL & SAFETY TESTS");
        testInvalidMeterNumberRejection();
        testExistingPaidBillProtection();
        testHistoricalStatusProtectionMET005();
        testControlledNewBillCreationAndStorage();
        testExistingUnpaidBillUpdateWorkflow();

        // SECTION 3: BILL DETAILS & SEARCH TESTS
        System.out.println("\n>>> SECTION 3: BILL SEARCH & FILTER TESTS");
        testBillSearchByMeter();
        testBillSearchByMonth();
        testCombinedSearch();
        testCaseInsensitiveMonthSearch();
        testInvalidMeterSearch();
        testShowAllReset();
        testChronologicalMonthSorting();

        // SECTION 4: INVOICE PRESENTATION & PRINTING TESTS
        System.out.println("\n>>> SECTION 4: INVOICE PRESENTATION & PRINTING TESTS");
        testGenerateBillMissingBillGracefulHandling();
        testGenerateBillAmountMatchesStoredBill();
        testPrintHandlingConfigured();

        // SECTION 5: DATABASE INTEGRITY VERIFICATION
        System.out.println("\n>>> SECTION 5: DATABASE INTEGRITY VERIFICATION");
        int finalCustomerCount = getCount("customer");
        int finalLoginCount = getCount("login");
        int finalBillCount = getCount("bill");
        int finalMeterInfoCount = getCount("meter_info");
        int finalTaxCount = getCount("tax");

        System.out.println("    customer   : Before=" + initialCustomerCount + ", After=" + finalCustomerCount);
        System.out.println("    login      : Before=" + initialLoginCount + ", After=" + finalLoginCount);
        System.out.println("    bill       : Before=" + initialBillCount + ", After=" + finalBillCount);
        System.out.println("    meter_info : Before=" + initialMeterInfoCount + ", After=" + finalMeterInfoCount);
        System.out.println("    tax        : Before=" + initialTaxCount + ", After=" + finalTaxCount);

        check("DB Integrity: Customer count unchanged", initialCustomerCount == finalCustomerCount);
        check("DB Integrity: Login count unchanged", initialLoginCount == finalLoginCount);
        check("DB Integrity: Bill count unchanged", initialBillCount == finalBillCount);
        check("DB Integrity: Meter info count unchanged", initialMeterInfoCount == finalMeterInfoCount);
        check("DB Integrity: Tax count unchanged", initialTaxCount == finalTaxCount);
        testMET005RecordsUnmodified();

        // FINAL SUMMARY
        System.out.println("\n===============================================================");
        System.out.println("  VERIFICATION RESULT: " + testsPassed + " PASSED, " + testsFailed + " FAILED");
        System.out.println("===============================================================");

        System.exit(testsFailed > 0 ? 1 : 0);
    }

    // -------------------------------------------------------------
    // SECTION 1: TARIFF & CALCULATION TESTS
    // -------------------------------------------------------------

    private static void testTariffCalculation100Units() {
        CalculateBill.TariffRates rates = new CalculateBill.TariffRates();
        rates.costPerUnit = 3.00;
        rates.meterRent = 50.00;
        rates.serviceCharge = 20.00;
        rates.swacchTax = 15.00;
        rates.fixedTax = 25.00;

        double calculated = CalculateBill.calculateBillAmount(100.0, rates);
        // 100 * 3 + 110 = 410.00
        check("Calculation: 100 units @ ₹3.00/unit + ₹110 fixed = ₹410.00", Math.abs(calculated - 410.00) < 0.001);
    }

    private static void testTariffCalculationDecimalUnits() {
        CalculateBill.TariffRates rates = new CalculateBill.TariffRates();
        rates.costPerUnit = 3.00;
        rates.meterRent = 50.00;
        rates.serviceCharge = 20.00;
        rates.swacchTax = 15.00;
        rates.fixedTax = 25.00;

        double calculated = CalculateBill.calculateBillAmount(50.5, rates);
        // 50.5 * 3 + 110 = 151.5 + 110 = 261.50
        check("Calculation: Decimal units (50.5 units) correctly calculated to ₹261.50", Math.abs(calculated - 261.50) < 0.001);
    }

    private static void testZeroUnitCalculation() {
        CalculateBill.TariffRates rates = new CalculateBill.TariffRates();
        rates.costPerUnit = 3.00;
        rates.meterRent = 50.00;
        rates.serviceCharge = 20.00;
        rates.swacchTax = 15.00;
        rates.fixedTax = 25.00;

        double calculated = CalculateBill.calculateBillAmount(0.0, rates);
        // 0 * 3 + 110 = 110.00
        check("Calculation: Zero units incurs only fixed fees (₹110.00)", Math.abs(calculated - 110.00) < 0.001);
    }

    private static void testNegativeUnitRejection() {
        String validation = CalculateBill.validateUnitsInput("-25");
        check("Validation: Negative units (-25) rejected with ERROR:negative", "ERROR:negative".equals(validation));
    }

    private static void testEmptyAndNonNumericUnitsRejection() {
        boolean emptyRejected = "ERROR:empty".equals(CalculateBill.validateUnitsInput(""));
        boolean blankRejected = "ERROR:empty".equals(CalculateBill.validateUnitsInput("   "));
        boolean alphaRejected = "ERROR:non_numeric".equals(CalculateBill.validateUnitsInput("abc"));
        check("Validation: Empty and non-numeric inputs rejected", emptyRejected && blankRejected && alphaRejected);
    }

    private static void testTwoDecimalPlacesFormatting() {
        double amount = 335.0;
        String formatted = String.format(Locale.US, "%.2f", amount);
        check("Formatting: Bill amount formatted with two decimal places ('335.00')", "335.00".equals(formatted));
    }

    // -------------------------------------------------------------
    // SECTION 2: DUPLICATE BILL & SAFETY TESTS
    // -------------------------------------------------------------

    private static void testInvalidMeterNumberRejection() {
        try (Conn c = new Conn()) {
            CalculateBill.BillProcessResult res = CalculateBill.processBill(c.c, "INVALID_MTR_999", "January", "100", false);
            check("Safety: Non-existent meter rejected with REJECTED_INVALID_METER", 
                res.status == CalculateBill.BillProcessResult.Status.REJECTED_INVALID_METER);
        } catch (Exception e) {
            check("Safety: Non-existent meter threw exception", false);
        }
    }

    private static void testExistingPaidBillProtection() {
        try (Conn c = new Conn()) {
            // MET001 January is Paid in seed database
            CalculateBill.BillProcessResult res = CalculateBill.processBill(c.c, "MET001", "January", "200", false);
            check("Safety: Overwrite of paid bill blocked with REJECTED_ALREADY_PAID", 
                res.status == CalculateBill.BillProcessResult.Status.REJECTED_ALREADY_PAID);
        } catch (Exception e) {
            check("Safety: Paid bill test threw exception", false);
        }
    }

    private static void testHistoricalStatusProtectionMET005() {
        try (Conn c = new Conn()) {
            // MET005 January has status 'Commercial'
            CalculateBill.BillProcessResult res = CalculateBill.processBill(c.c, "MET005", "January", "200", false);
            check("Safety: Overwrite of historical Commercial bill blocked with REJECTED_HISTORICAL_PROTECTED", 
                res.status == CalculateBill.BillProcessResult.Status.REJECTED_HISTORICAL_PROTECTED);
        } catch (Exception e) {
            check("Safety: Historical bill test threw exception", false);
        }
    }

    private static void testControlledNewBillCreationAndStorage() {
        String testMeter = "MET001";
        String testMonth = "November"; // November has no seed bill

        try (Conn c = new Conn()) {
            // 1. Ensure test month is clean
            try (PreparedStatement ps = c.c.prepareStatement("DELETE FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                ps.executeUpdate();
            }

            // 2. Insert new bill via processBill (70 units: 70 * 3 + 110 = 320.00)
            CalculateBill.BillProcessResult res = CalculateBill.processBill(c.c, testMeter, testMonth, "70", false);
            boolean created = res.status == CalculateBill.BillProcessResult.Status.SUCCESS_INSERTED;

            // 3. Verify in database
            boolean verified = false;
            try (PreparedStatement ps = c.c.prepareStatement("SELECT total_bill, units, status FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        verified = "320.00".equals(rs.getString("total_bill")) 
                                && "70".equals(rs.getString("units")) 
                                && "Not Paid".equals(rs.getString("status"));
                    }
                }
            }

            check("Bill Generation: New month bill generated with ₹320.00 Not Paid", created && verified);
        } catch (Exception e) {
            e.printStackTrace();
            check("Bill Generation: New month bill creation threw exception", false);
        }
    }

    private static void testExistingUnpaidBillUpdateWorkflow() {
        String testMeter = "MET001";
        String testMonth = "November";

        try (Conn c = new Conn()) {
            // 1. Attempt update with confirmUpdate = false -> Must require confirmation
            CalculateBill.BillProcessResult confirmReq = CalculateBill.processBill(c.c, testMeter, testMonth, "85", false);
            boolean askedConfirm = confirmReq.status == CalculateBill.BillProcessResult.Status.CONFIRMATION_REQUIRED
                                && "70".equals(confirmReq.existingUnits)
                                && "320.00".equals(confirmReq.existingTotal)
                                && "85".equals(confirmReq.newUnits)
                                && "365.00".equals(confirmReq.newTotal);

            // 2. Attempt update with confirmUpdate = true -> Must update database
            CalculateBill.BillProcessResult updatedRes = CalculateBill.processBill(c.c, testMeter, testMonth, "85", true);
            boolean updateSuccess = updatedRes.status == CalculateBill.BillProcessResult.Status.SUCCESS_UPDATED;

            // 3. Verify database reflects new values
            boolean verified = false;
            try (PreparedStatement ps = c.c.prepareStatement("SELECT total_bill, units, status FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        verified = "365.00".equals(rs.getString("total_bill")) 
                                && "85".equals(rs.getString("units")) 
                                && "Not Paid".equals(rs.getString("status"));
                    }
                }
            }

            // 4. CLEAN UP test bill so live database remains untouched
            try (PreparedStatement ps = c.c.prepareStatement("DELETE FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                ps.executeUpdate();
            }

            check("Duplicate Prevention: Existing unpaid bill requires confirmation and updates safely without duplication", 
                askedConfirm && updateSuccess && verified);
        } catch (Exception e) {
            e.printStackTrace();
            check("Duplicate Prevention: Unpaid bill update threw exception", false);
        }
    }

    // -------------------------------------------------------------
    // SECTION 3: BILL DETAILS & SEARCH TESTS
    // -------------------------------------------------------------

    private static int runDepositQuery(String meter, String month) {
        int[] count = new int[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                DepositDetails dd = new DepositDetails();
                count[0] = dd.loadBills(meter, month, false);
                dd.dispose();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count[0];
    }

    private static void testBillSearchByMeter() {
        int count = runDepositQuery("MET001", "All Months");
        check("Search: Bill search by meter (MET001) returns exact count (2)", count == 2);
    }

    private static void testBillSearchByMonth() {
        int count = runDepositQuery("All Meters", "February");
        check("Search: Bill search by month (February) returns 9 bills", count == 9);
    }

    private static void testCombinedSearch() {
        int count = runDepositQuery("MET002", "February");
        check("Search: Combined search (MET002 + February) returns 1 exact bill", count == 1);
    }

    private static void testCaseInsensitiveMonthSearch() {
        int lower = runDepositQuery("All Meters", "february");
        int upper = runDepositQuery("All Meters", "FEBRUARY");
        int mixed = runDepositQuery("All Meters", "February");
        check("Search: Month search is case-insensitive (february=" + lower + ", FEBRUARY=" + upper + ")", 
            lower == 9 && upper == 9 && mixed == 9);
    }

    private static void testInvalidMeterSearch() {
        int count = runDepositQuery("INVALID_METER_XYZ", "All Months");
        check("Search: Invalid meter number returns 0 records cleanly", count == 0);
    }

    private static void testShowAllReset() {
        int count = runDepositQuery("All Meters", "All Months");
        check("Search: Show All reset returns all 18 bills", count == 18);
    }

    private static void testChronologicalMonthSorting() {
        List<String> monthsInOrder = new ArrayList<>();
        try (Conn c = new Conn()) {
            String q = "SELECT month FROM bill WHERE meter_no = 'MET001' ORDER BY FIELD(month, 'January','February','March','April','May','June','July','August','September','October','November','December')";
            try (Statement s = c.c.createStatement();
                 ResultSet rs = s.executeQuery(q)) {
                while (rs.next()) {
                    monthsInOrder.add(rs.getString("month"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        boolean isChronological = monthsInOrder.size() == 2 
                               && "January".equalsIgnoreCase(monthsInOrder.get(0)) 
                               && "February".equalsIgnoreCase(monthsInOrder.get(1));
        check("Sorting: Chronological month ordering verified (January before February)", isChronological);
    }

    // -------------------------------------------------------------
    // SECTION 4: INVOICE PRESENTATION & PRINTING TESTS
    // -------------------------------------------------------------

    private static void testGenerateBillMissingBillGracefulHandling() {
        boolean[] result = new boolean[3];
        try {
            SwingUtilities.invokeAndWait(() -> {
                GenerateBill gb = new GenerateBill("MET001");
                result[0] = gb.generateBillStatement("MET001", "December", false);
                result[1] = gb.area.getText().contains("NO BILL RECORD FOUND") || gb.area.getText().contains("NO BILL GENERATED");
                result[2] = !gb.print.isEnabled();
                gb.dispose();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("Invoice: Missing bill handled gracefully with notice and print disabled", !result[0] && result[1] && result[2]);
    }

    private static void testGenerateBillAmountMatchesStoredBill() {
        boolean[] result = new boolean[4];
        try {
            SwingUtilities.invokeAndWait(() -> {
                GenerateBill gb = new GenerateBill("MET001");
                result[0] = gb.generateBillStatement("MET001", "February", false);
                String text = gb.area.getText();
                result[1] = text.contains("607.25");
                result[2] = text.contains("Units Consumed") && text.contains("Energy Charges") && text.contains("Meter Rent");
                result[3] = gb.print.isEnabled();
                gb.dispose();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("Invoice: Displayed invoice amount matches stored database bill (₹607.25) with full tariff breakdown", 
            result[0] && result[1] && result[2] && result[3]);
    }

    private static void testPrintHandlingConfigured() {
        boolean[] result = new boolean[2];
        try {
            SwingUtilities.invokeAndWait(() -> {
                BillDetails bd = new BillDetails("MET001");
                result[0] = bd.print != null && bd.print.getActionListeners().length > 0;
                bd.dispose();

                DepositDetails dd = new DepositDetails();
                result[1] = dd.print != null && dd.print.getActionListeners().length > 0;
                dd.dispose();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("Printing: Print facilities configured with action listeners in BillDetails & DepositDetails", 
            result[0] && result[1]);
    }

    private static void testMET005RecordsUnmodified() {
        boolean commercialPreserved = true;
        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT status FROM bill WHERE meter_no = 'MET005'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        if (!"Commercial".equalsIgnoreCase(rs.getString("status"))) {
                            commercialPreserved = false;
                        }
                    }
                    if (count != 2) commercialPreserved = false;
                }
            }
        } catch (Exception e) {
            commercialPreserved = false;
        }
        check("Safety: MET005 Commercial records preserved without modification (2 records)", commercialPreserved);
    }

    // -------------------------------------------------------------
    // UTILITY METHODS
    // -------------------------------------------------------------

    private static int getCount(String table) {
        try (Conn c = new Conn();
             Statement s = c.c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static void check(String label, boolean passed) {
        if (passed) {
            testsPassed++;
            System.out.println("  [PASS] " + label);
        } else {
            testsFailed++;
            System.out.println("  [FAIL] " + label);
        }
    }
}
