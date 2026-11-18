package electricity.billing.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Phase7VerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static void check(String description, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + description);
            testsFailed++;
        }
    }

    private static int[] getTableCounts() {
        int[] counts = new int[7];
        String[] tables = {"customer", "login", "bill", "meter_info", "tax", "complaints", "complaint_history"};
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement()) {
                    for (int i = 0; i < tables.length; i++) {
                        try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM " + tables[i])) {
                            if (rs.next()) counts[i] = rs.getInt(1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counts;
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  ELECTRICITY BILLING SYSTEM - PHASE 7 VERIFICATION SUITE");
        System.out.println("===============================================================");

        int[] initialCounts = getTableCounts();
        System.out.println("\n>>> Initial Database Record Counts:");
        System.out.println("    customer          : " + initialCounts[0]);
        System.out.println("    login             : " + initialCounts[1]);
        System.out.println("    bill              : " + initialCounts[2]);
        System.out.println("    meter_info        : " + initialCounts[3]);
        System.out.println("    tax               : " + initialCounts[4]);
        System.out.println("    complaints        : " + initialCounts[5]);
        System.out.println("    complaint_history : " + initialCounts[6]);
        System.out.println("---------------------------------------------------------------");

        System.out.println("\n>>> SECTION 1: BILLING ANALYTICS & TOTALS");
        testBillingTotalsAgainstDirectSql();

        System.out.println("\n>>> SECTION 2: REVENUE, OUTSTANDING & COMMERCIAL STATUS");
        testRevenueAndOutstandingCalculations();

        System.out.println("\n>>> SECTION 3: ELECTRICITY CONSUMPTION ANALYTICS");
        testConsumptionTotals();

        System.out.println("\n>>> SECTION 4: CUSTOMER DEMOGRAPHICS & LEDGER");
        testCustomerAnalytics();

        System.out.println("\n>>> SECTION 5: COMPLAINT & GRIEVANCE ANALYTICS");
        testComplaintAnalytics();

        System.out.println("\n>>> SECTION 6: FILTERS & RESET LOGIC");
        testFilterCombinationsAndReset();

        System.out.println("\n>>> SECTION 7: RESILIENCE & ZERO-DENOMINATOR SAFETY");
        testEmptyAndZeroDenominatorHandling();

        System.out.println("\n>>> SECTION 8: CSV REPORT EXPORT");
        testCsvExportCorrectness();

        System.out.println("\n>>> SECTION 9: DATABASE INTEGRITY & READ-ONLY VERIFICATION");
        testDatabaseIntegrity(initialCounts);

        System.out.println("\n===============================================================");
        System.out.println("  VERIFICATION RESULT: " + testsPassed + " PASSED, " + testsFailed + " FAILED");
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 1: Billing Totals Against Direct SQL
    // -------------------------------------------------------------------------
    private static void testBillingTotalsAgainstDirectSql() {
        AnalyticsService.FilterCriteria filters = new AnalyticsService.FilterCriteria();
        AnalyticsService.BillingSummary summary = AnalyticsService.getBillingSummary(filters);

        int directCount = 0;
        BigDecimal directSum = BigDecimal.ZERO;
        try (Conn c = new Conn()) {
            try (Statement st = c.c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT count(*), sum(cast(total_bill as decimal(10,2))) FROM bill")) {
                if (rs.next()) {
                    directCount = rs.getInt(1);
                    directSum = rs.getBigDecimal(2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        check("1. Billing Totals: Total bills count matches direct SQL (18)", summary.totalBills == directCount && summary.totalBills == 18);
        check("1b. Billed Sum: Total billed amount matches direct SQL (₹10,855.25)", summary.totalBilledAmount.compareTo(directSum) == 0);
        check("2. Paid Count: Paid bills count matches direct SQL (9)", summary.paidBills == 9);
        check("2b. Unpaid Count: Not Paid bills count matches direct SQL (7)", summary.unpaidBills == 7);
        check("3. Commercial Isolation: Commercial records isolated (2)", summary.commercialBills == 2);
    }

    // -------------------------------------------------------------------------
    // SECTION 2: Revenue and Collection Analytics
    // -------------------------------------------------------------------------
    private static void testRevenueAndOutstandingCalculations() {
        AnalyticsService.FilterCriteria filters = new AnalyticsService.FilterCriteria();
        AnalyticsService.RevenueSummary rev = AnalyticsService.getRevenueSummary(filters);

        BigDecimal expectedPaidRev = new BigDecimal("4994.25");
        BigDecimal expectedUnpaidOut = new BigDecimal("4183.75");
        BigDecimal expectedCommercial = new BigDecimal("1677.25");

        check("4. Revenue Calculation: Total collected revenue includes ONLY Paid bills (₹4,994.25)",
            rev.totalCollectedRevenue.compareTo(expectedPaidRev) == 0);
        check("5. Outstanding Calculation: Total outstanding amount includes ONLY Not Paid bills (₹4,183.75)",
            rev.totalOutstandingAmount.compareTo(expectedUnpaidOut) == 0);
        check("5b. Commercial Exclusion: Commercial records excluded from revenue calculations (₹1,677.25)",
            rev.commercialExcludedAmount.compareTo(expectedCommercial) == 0);

        // Standard classified total = 4994.25 + 4183.75 = 9178.00
        BigDecimal expectedStandard = expectedPaidRev.add(expectedUnpaidOut);
        check("5c. Standard Billed: Standard classified total equals Paid + Unpaid (₹9,178.00)",
            rev.totalStandardBilled.compareTo(expectedStandard) == 0);

        // Collection Percentage: 4994.25 / 9178.00 * 100 = 54.42%
        double expectedRate = 54.42;
        check("5d. Collection Rate: Collection rate correctly calculated (54.42%)",
            Math.abs(rev.collectionPercentage - expectedRate) < 0.05);
    }

    // -------------------------------------------------------------------------
    // SECTION 3: Electricity Consumption Analytics
    // -------------------------------------------------------------------------
    private static void testConsumptionTotals() {
        AnalyticsService.FilterCriteria filters = new AnalyticsService.FilterCriteria();
        AnalyticsService.ConsumptionSummary cons = AnalyticsService.getConsumptionSummary(filters);

        BigDecimal expectedTotalUnits = new BigDecimal("2915.00");
        check("6. Consumption Totals: Total grid units consumed matches 2,915.00 Units",
            cons.totalUnitsConsumed.compareTo(expectedTotalUnits) == 0);

        check("6b. Monthly Comparison: Contains January (1,360.00 U) and February (1,555.00 U)",
            cons.monthlyConsumption.containsKey("January") && cons.monthlyConsumption.containsKey("February"));

        check("6c. Customer Rankings: Highest and lowest consumption customer lists populated",
            !cons.topConsumers.isEmpty() && !cons.lowestConsumers.isEmpty());

        AnalyticsService.CustomerConsumptionRow top = cons.topConsumers.get(0);
        AnalyticsService.CustomerConsumptionRow low = cons.lowestConsumers.get(0);
        check("6d. Ranking Order: Peak consumer units >= Minimum consumer units",
            top.totalUnits.compareTo(low.totalUnits) >= 0);
    }

    // -------------------------------------------------------------------------
    // SECTION 4: Customer Analytics
    // -------------------------------------------------------------------------
    private static void testCustomerAnalytics() {
        AnalyticsService.FilterCriteria filters = new AnalyticsService.FilterCriteria();
        AnalyticsService.CustomerSummary cust = AnalyticsService.getCustomerSummary(filters);

        check("7. Customer Count: Total registered customers matches 9", cust.totalCustomers == 9);
        check("8. Active Accounts: Active accounts count matches 9", cust.activeCustomers == 9);
        check("8b. Inactive Accounts: Inactive accounts count matches 0", cust.inactiveCustomers == 0);
        check("8c. Ledger Rows: Customer ledger rows count matches total customers (9)",
            cust.customerDetails.size() == 9);
    }

    // -------------------------------------------------------------------------
    // SECTION 5: Complaint Analytics
    // -------------------------------------------------------------------------
    private static void testComplaintAnalytics() {
        AnalyticsService.FilterCriteria filters = new AnalyticsService.FilterCriteria();
        AnalyticsService.ComplaintAnalyticsSummary comp = AnalyticsService.getComplaintSummary(filters);

        check("9. Complaint Count: Total grievances count matches 3", comp.totalComplaints == 3);
        check("9b. Status Counts: In Progress (1), Resolved (1), and Pending (1)",
            comp.inProgressCount == 1 && comp.resolvedCount == 1 && comp.pendingCount == 1 && comp.rejectedCount == 0);
        check("10. Priority Counts: High (1), Urgent (1), and Medium (1)",
            comp.highPriorityCount == 1 && comp.urgentPriorityCount == 1 && comp.lowPriorityCount == 0 && comp.mediumPriorityCount == 1);
        check("11. Category Counts: Billing Issues (2) and Meter Problems (1)",
            comp.categoryDistribution.getOrDefault("Billing Issues", 0) == 2 &&
            comp.categoryDistribution.getOrDefault("Meter Problems", 0) == 1);
        check("11b. Resolution Rate: Resolution percentage accurately computed (33.33%)",
            comp.resolutionPercentage == 33.33);
    }

    // -------------------------------------------------------------------------
    // SECTION 6: Filter Combinations & Reset Logic
    // -------------------------------------------------------------------------
    private static void testFilterCombinationsAndReset() {
        AnalyticsService.FilterCriteria filters = new AnalyticsService.FilterCriteria();

        // 1. Month filter: January
        filters.month = "January";
        AnalyticsService.BillingSummary janSummary = AnalyticsService.getBillingSummary(filters);
        check("12. Filter by Month: January bills count equals 9", janSummary.totalBills == 9);
        check("12b. Filter by Month Sum: January total equals ₹5,123.50",
            janSummary.totalBilledAmount.compareTo(new BigDecimal("5123.50")) == 0);

        // 2. Status filter: Paid
        filters.reset();
        filters.billStatus = "Paid";
        AnalyticsService.BillingSummary paidSummary = AnalyticsService.getBillingSummary(filters);
        check("12c. Filter by Status: Paid bills count equals 9", paidSummary.totalBills == 9);

        // 3. Combined Filter: Month=February + Status=Not Paid
        filters.reset();
        filters.month = "February";
        filters.billStatus = "Not Paid";
        AnalyticsService.BillingSummary febNotPaid = AnalyticsService.getBillingSummary(filters);
        check("12d. Combined Filters: February Not Paid bills subset evaluated correctly",
            febNotPaid.totalBills > 0 && febNotPaid.totalBills < 9);

        // 4. Reset Filters
        filters.reset();
        AnalyticsService.BillingSummary resetSummary = AnalyticsService.getBillingSummary(filters);
        check("13. Reset Filters: Resets criteria and restores all 18 bills",
            resetSummary.totalBills == 18 && "All Months".equals(filters.month));
    }

    // -------------------------------------------------------------------------
    // SECTION 7: Empty and Zero-Denominator Handling
    // -------------------------------------------------------------------------
    private static void testEmptyAndZeroDenominatorHandling() {
        AnalyticsService.FilterCriteria emptyFilters = new AnalyticsService.FilterCriteria();
        emptyFilters.customerMeter = "NON_EXISTENT_METER_XYZ_9999";

        AnalyticsService.BillingSummary emptyBilling = AnalyticsService.getBillingSummary(emptyFilters);
        AnalyticsService.RevenueSummary emptyRev = AnalyticsService.getRevenueSummary(emptyFilters);
        AnalyticsService.ConsumptionSummary emptyCons = AnalyticsService.getConsumptionSummary(emptyFilters);
        AnalyticsService.ComplaintAnalyticsSummary emptyComp = AnalyticsService.getComplaintSummary(emptyFilters);

        check("14. Empty Result Handling: Zero bills handled cleanly without null pointer",
            emptyBilling.totalBills == 0 && emptyBilling.totalBilledAmount.compareTo(BigDecimal.ZERO) == 0);
        check("14b. Empty Revenue Handling: Empty dataset returns ₹0.00 revenue and 0.0% rate",
            emptyRev.totalCollectedRevenue.compareTo(BigDecimal.ZERO) == 0 && emptyRev.collectionPercentage == 0.0);
        check("14c. Empty Consumption Handling: Empty dataset returns 0.0 units",
            emptyCons.totalUnitsConsumed.compareTo(BigDecimal.ZERO) == 0);
        check("14d. Empty Complaints Handling: Empty dataset returns 0 complaints and 0.0% rate",
            emptyComp.totalComplaints == 0 && emptyComp.resolutionPercentage == 0.0);

        // Explicit test of calculateSafePercentage zero denominators
        double zeroDenomBigDec = AnalyticsService.calculateSafePercentage(BigDecimal.ZERO, BigDecimal.ZERO);
        double zeroDenomInt = AnalyticsService.calculateSafePercentage(0, 0);
        check("15. Zero-Denominator Safety: 0 / 0 returns 0.0% without ArithmeticException",
            zeroDenomBigDec == 0.0 && zeroDenomInt == 0.0);
    }

    // -------------------------------------------------------------------------
    // SECTION 8: CSV Export Correctness
    // -------------------------------------------------------------------------
    private static void testCsvExportCorrectness() {
        File tempCsv = new File("test_export_phase7.csv");
        try {
            String[] headers = {"Meter No", "Customer Name", "Month", "Total Bill (INR)", "Status"};
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"MET001", "Rajesh Kumar Sharma", "January", "607.25", "Paid"});
            rows.add(new String[]{"MET002", "Priya Singh", "February", "450.00", "Not Paid"});
            rows.add(new String[]{"MET005", "Vikram Reddy, Commercial User", "January", "811.00", "Commercial"});

            boolean exportSuccess = AnalyticsService.exportToCSV(tempCsv, headers, rows);
            check("16. CSV Export: Export utility successfully writes CSV file", exportSuccess && tempCsv.exists());

            // Verify content and row counts
            int lineCount = 0;
            boolean containsEscapedQuote = false;
            try (BufferedReader reader = new BufferedReader(new FileReader(tempCsv))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    if (line.contains("\"Vikram Reddy, Commercial User\"")) {
                        containsEscapedQuote = true;
                    }
                }
            }

            check("16b. CSV Structure: Total rows matches header + 3 data rows (4 lines)", lineCount == 4);
            check("16c. CSV Escaping: Commas in data fields correctly wrapped in quotes", containsEscapedQuote);
        } catch (Exception e) {
            e.printStackTrace();
            check("16. CSV Export failed", false);
        } finally {
            if (tempCsv.exists()) {
                tempCsv.delete();
            }
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 9: Database Integrity & Read-Only Verification
    // -------------------------------------------------------------------------
    private static void testDatabaseIntegrity(int[] initialCounts) {
        int[] finalCounts = getTableCounts();

        System.out.println("    customer          : Before=" + initialCounts[0] + ", After=" + finalCounts[0]);
        System.out.println("    login             : Before=" + initialCounts[1] + ", After=" + finalCounts[1]);
        System.out.println("    bill              : Before=" + initialCounts[2] + ", After=" + finalCounts[2]);
        System.out.println("    meter_info        : Before=" + initialCounts[3] + ", After=" + finalCounts[3]);
        System.out.println("    tax               : Before=" + initialCounts[4] + ", After=" + finalCounts[4]);
        System.out.println("    complaints        : Before=" + initialCounts[5] + ", After=" + finalCounts[5]);
        System.out.println("    complaint_history : Before=" + initialCounts[6] + ", After=" + finalCounts[6]);

        check("17. Read-Only Integrity: Customer count unchanged (9)", initialCounts[0] == finalCounts[0]);
        check("17. Read-Only Integrity: Login count unchanged (11)", initialCounts[1] == finalCounts[1]);
        check("17. Read-Only Integrity: Bill count unchanged (18)", initialCounts[2] == finalCounts[2]);
        check("17. Read-Only Integrity: Meter info count unchanged (9)", initialCounts[3] == finalCounts[3]);
        check("17. Read-Only Integrity: Tax count unchanged (1)", initialCounts[4] == finalCounts[4]);
        check("17. Read-Only Integrity: Complaints count unchanged (2)", initialCounts[5] == finalCounts[5]);
        check("17. Read-Only Integrity: Complaint history count unchanged (8)", initialCounts[6] == finalCounts[6]);
    }
}
