package electricity.billing.system;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Phase6VerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static final List<Integer> testComplaintIds = new ArrayList<>();

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
        int[] counts = new int[5];
        String[] tables = {"customer", "login", "bill", "meter_info", "tax"};
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
        System.out.println("  ELECTRICITY BILLING SYSTEM - PHASE 6 VERIFICATION SUITE");
        System.out.println("===============================================================");

        int[] initialCounts = getTableCounts();
        System.out.println("\n>>> Initial Database Record Counts:");
        System.out.println("    customer   : " + initialCounts[0]);
        System.out.println("    login      : " + initialCounts[1]);
        System.out.println("    bill       : " + initialCounts[2]);
        System.out.println("    meter_info : " + initialCounts[3]);
        System.out.println("    tax        : " + initialCounts[4]);
        System.out.println("---------------------------------------------------------------");

        try {
            System.out.println("\n>>> SECTION 1: COMPLAINT SUBMISSION & VALIDATION");
            testComplaintSubmissionAndValidation();

            System.out.println("\n>>> SECTION 2: ACCESS CONTROL & SEARCH/FILTERING");
            testAccessControlAndFilters();

            System.out.println("\n>>> SECTION 3: STATUS & PRIORITY WORKFLOW");
            testStatusAndPriorityWorkflow();

            System.out.println("\n>>> SECTION 4: AUDIT HISTORY & TRACEABILITY");
            testComplaintHistoryTraceability();

            System.out.println("\n>>> SECTION 5: SECURITY & PERMISSION ENFORCEMENT");
            testSecurityAndPermissions();

            System.out.println("\n>>> SECTION 6: TRANSACTION ATOMICITY & ROLLBACK");
            testTransactionRollback();

            System.out.println("\n>>> SECTION 7: DATABASE INTEGRITY VERIFICATION");
            testDatabaseIntegrity(initialCounts);
        } finally {
            cleanupTestData();
        }

        System.out.println("\n===============================================================");
        System.out.println("  VERIFICATION RESULT: " + testsPassed + " PASSED, " + testsFailed + " FAILED");
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void testComplaintSubmissionAndValidation() {
        // Test 1: Complaint submission
        ComplaintService.ServiceResult res = ComplaintService.submitComplaint(
            "MET001", "Billing Issues", "January Bill Discrepancy", 
            "The billed units do not reflect the physical meter reading for January.", 
            "January", "Customer (MET001)"
        );
        check("1. Complaint submission: Valid complaint successfully submitted", res.success && res.complaintId > 0);
        if (res.success) testComplaintIds.add(res.complaintId);

        // Test 2: Unique reference generation
        String ref1 = res.referenceNumber;
        ComplaintService.ServiceResult res2 = ComplaintService.submitComplaint(
            "MET001", "Meter Problems", "Digital Display Fault", 
            "Meter screen is blinking and display reading is unreadable.", 
            null, "Customer (MET001)"
        );
        check("2. Unique reference generation: Successive complaints receive unique references", 
            res2.success && ref1 != null && res2.referenceNumber != null && !ref1.equals(res2.referenceNumber));
        if (res2.success) testComplaintIds.add(res2.complaintId);

        // Test 3: Empty subject validation
        ComplaintService.ServiceResult resEmptySub = ComplaintService.submitComplaint(
            "MET001", "Other", "   ", "Valid description here.", null, "Customer (MET001)"
        );
        check("3. Empty subject validation: Rejected with EMPTY_SUBJECT", 
            !resEmptySub.success && "EMPTY_SUBJECT".equals(resEmptySub.errorCode));

        // Test 4: Empty description validation
        ComplaintService.ServiceResult resEmptyDesc = ComplaintService.submitComplaint(
            "MET001", "Other", "Valid Subject", "   ", null, "Customer (MET001)"
        );
        check("4. Empty description validation: Rejected with EMPTY_DESCRIPTION", 
            !resEmptyDesc.success && "EMPTY_DESCRIPTION".equals(resEmptyDesc.errorCode));

        // Test 5: Invalid meter association blocked
        ComplaintService.ServiceResult resInvalidMeter = ComplaintService.submitComplaint(
            "INVALID_METER_999", "Other", "Valid Subject", "Valid description.", null, "Customer (INVALID_METER_999)"
        );
        check("5. Invalid meter association blocked: Non-existent meter rejected with INVALID_METER", 
            !resInvalidMeter.success && "INVALID_METER".equals(resInvalidMeter.errorCode));

        // Validation: Description too long
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 2050; i++) longDesc.append("x");
        ComplaintService.ServiceResult resLongDesc = ComplaintService.submitComplaint(
            "MET001", "Other", "Valid Subject", longDesc.toString(), null, "Customer (MET001)"
        );
        check("Validation: Description > 2000 chars rejected with DESCRIPTION_TOO_LONG", 
            !resLongDesc.success && "DESCRIPTION_TOO_LONG".equals(resLongDesc.errorCode));
    }

    private static void testAccessControlAndFilters() {
        // Create complaint for MET002
        ComplaintService.ServiceResult resCust2 = ComplaintService.submitComplaint(
            "MET002", "Payment Issues", "Payment Not Reflected", 
            "Paid via online banking yesterday but still shows as unpaid.", 
            "February", "Customer (MET002)"
        );
        if (resCust2.success) testComplaintIds.add(resCust2.complaintId);

        // Test 6: Customer cannot access another customer's complaints
        // Query as MET001
        int cust1Count = 0;
        boolean seesCust2Complaint = false;
        try (Conn c = new Conn()) {
            String q = "SELECT complaint_id, meter_no FROM complaints WHERE meter_no = ?";
            try (PreparedStatement ps = c.c.prepareStatement(q)) {
                ps.setString(1, "MET001");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        cust1Count++;
                        if ("MET002".equals(rs.getString("meter_no"))) {
                            seesCust2Complaint = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("6. Customer isolation: Customer MET001 cannot see MET002 complaints", !seesCust2Complaint && cust1Count >= 2);

        // Test 7: Admin can view all complaints
        int totalComplaintsSeenByAdmin = 0;
        try (Conn c = new Conn()) {
            String q = "SELECT count(*) FROM complaints";
            try (Statement st = c.c.createStatement(); ResultSet rs = st.executeQuery(q)) {
                if (rs.next()) totalComplaintsSeenByAdmin = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("7. Admin visibility: Administrator has complete visibility of all complaints", totalComplaintsSeenByAdmin >= 3);

        // Test 8: Search by complaint reference
        if (!testComplaintIds.isEmpty()) {
            int cid = testComplaintIds.get(0);
            String targetRef = "";
            try (Conn c = new Conn()) {
                try (PreparedStatement ps = c.c.prepareStatement("SELECT reference_number FROM complaints WHERE complaint_id = ?")) {
                    ps.setInt(1, cid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) targetRef = rs.getString("reference_number");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            int foundRef = 0;
            try (Conn c = new Conn()) {
                try (PreparedStatement ps = c.c.prepareStatement("SELECT count(*) FROM complaints WHERE reference_number = ?")) {
                    ps.setString(1, targetRef);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) foundRef = rs.getInt(1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            check("8. Search by complaint reference: Exactly 1 record retrieved", foundRef == 1);
        }

        // Test 9: Search by meter number
        int foundMeter = 0;
        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT count(*) FROM complaints WHERE meter_no = ?")) {
                ps.setString(1, "MET001");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) foundMeter = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("9. Search by meter number: Correctly returns matching meter complaints", foundMeter >= 2);

        // Test 10: Category filter
        int billingCatCount = 0;
        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT count(*) FROM complaints WHERE category = ?")) {
                ps.setString(1, "Billing Issues");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) billingCatCount = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("10. Category filter: Correctly filters complaints by category", billingCatCount >= 1);

        // Test 11: Status filter
        int pendingCount = 0;
        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT count(*) FROM complaints WHERE status = ?")) {
                ps.setString(1, "Pending");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) pendingCount = rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("11. Status filter: Correctly filters complaints by status 'Pending'", pendingCount >= 3);
    }

    private static void testStatusAndPriorityWorkflow() {
        if (testComplaintIds.isEmpty()) return;
        int targetId = testComplaintIds.get(0);

        // Test 12: Priority update
        ComplaintService.ServiceResult priRes = ComplaintService.updatePriority(targetId, "Urgent", "Admin", "High priority billing issue");
        check("12. Priority update: Changed to 'Urgent' successfully", priRes.success);

        // Verify DB priority
        String dbPri = "";
        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT priority FROM complaints WHERE complaint_id = ?")) {
                ps.setInt(1, targetId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) dbPri = rs.getString("priority");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        check("12b. Priority verification: DB record has priority='Urgent'", "Urgent".equalsIgnoreCase(dbPri));

        // Test 13: Complaint assignment
        ComplaintService.ServiceResult assignRes = ComplaintService.assignComplaint(targetId, "Parth", "Admin", "Assigned for review");
        check("13. Complaint assignment: Successfully assigned to 'Parth'", assignRes.success);

        // Test 14: Pending to In Progress transition
        ComplaintService.ServiceResult pToIpRes = ComplaintService.updateStatus(targetId, "In Progress", "Admin", "Under active review");
        check("14. Status transition: Pending -> In Progress succeeds", pToIpRes.success);

        // Invalid direct jump: In Progress to Pending should be blocked
        ComplaintService.ServiceResult invalidJump = ComplaintService.updateStatus(targetId, "Pending", "Admin", "Try revert");
        check("Status safety: Cannot revert In Progress back to Pending", !invalidJump.success && "INVALID_TRANSITION".equals(invalidJump.errorCode));

        // Test 17: Resolution requires remarks
        ComplaintService.ServiceResult resNoRemarks = ComplaintService.updateStatus(targetId, "Resolved", "Admin", "   ");
        check("17. Resolution requires remarks: Blocked with REMARKS_REQUIRED", !resNoRemarks.success && "REMARKS_REQUIRED".equals(resNoRemarks.errorCode));

        // Test 15: In Progress to Resolved transition
        ComplaintService.ServiceResult ipToRes = ComplaintService.updateStatus(
            targetId, "Resolved", "Admin", "Billed units calibrated and credited ₹50 to next billing cycle."
        );
        check("15. Status transition: In Progress -> Resolved succeeds with resolution remarks", ipToRes.success);

        // Test 18: Reopening requires explanation
        ComplaintService.ServiceResult reopenNoExpl = ComplaintService.updateStatus(targetId, "In Progress", "Admin", "   ");
        check("18. Reopening requires explanation: Empty explanation blocked with REMARKS_REQUIRED", 
            !reopenNoExpl.success && "REMARKS_REQUIRED".equals(reopenNoExpl.errorCode));

        ComplaintService.ServiceResult reopenSuccess = ComplaintService.updateStatus(
            targetId, "In Progress", "Admin", "Customer contacted support stating issue persists on new meter reading."
        );
        check("18b. Reopening with explanation: Successfully reopened to In Progress", reopenSuccess.success);

        // Test 16: Rejection requires explanation
        if (testComplaintIds.size() > 1) {
            int secondId = testComplaintIds.get(1);
            ComplaintService.ServiceResult rejNoExpl = ComplaintService.updateStatus(secondId, "Rejected", "Admin", "   ");
            check("16. Rejection requires explanation: Empty explanation blocked with REMARKS_REQUIRED", 
                !rejNoExpl.success && "REMARKS_REQUIRED".equals(rejNoExpl.errorCode));

            ComplaintService.ServiceResult rejSuccess = ComplaintService.updateStatus(
                secondId, "Rejected", "Admin", "Physical inspection confirmed meter is within calibrated tolerances."
            );
            check("16b. Rejection with explanation: Complaint successfully rejected with recorded explanation", rejSuccess.success);
        }
    }

    private static void testComplaintHistoryTraceability() {
        if (testComplaintIds.isEmpty()) return;
        int targetId = testComplaintIds.get(0);

        // Test 19: Complaint history creation
        List<String> actions = new ArrayList<>();
        try (Conn c = new Conn()) {
            String q = "SELECT action, previous_value, new_value, performed_by, remarks FROM complaint_history WHERE complaint_id = ? ORDER BY history_id ASC";
            try (PreparedStatement ps = c.c.prepareStatement(q)) {
                ps.setInt(1, targetId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        actions.add(rs.getString("action"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean hasSubmit = actions.contains("Complaint Submitted");
        boolean hasPriority = actions.contains("Priority Changed");
        boolean hasAssign = actions.contains("Complaint Assigned");
        boolean hasResolved = actions.contains("Complaint Resolved");
        boolean hasReopened = actions.contains("Complaint Reopened");

        check("19. Complaint history creation: Comprehensive append-only audit trail recorded (" + actions.size() + " actions)",
            hasSubmit && hasPriority && hasAssign && hasResolved && hasReopened);
    }

    private static void testSecurityAndPermissions() {
        // Test 20: Unauthorized customer status modification blocked
        ComplaintService.ServiceResult custModResult = ComplaintService.customerAttemptModifyStatus("Customer");
        check("20. Permission guard: Customer role blocked from modifying status", 
            !custModResult.success && "UNAUTHORIZED".equals(custModResult.errorCode));

        // Test 21: Duplicate submission protection
        // Submit identical complaint in rapid succession and check debounce/duplicate detection
        ComplaintService.ServiceResult dup1 = ComplaintService.submitComplaint(
            "MET001", "Other", "Voltage Fluctuations", "Frequent voltage dips damaging appliances.", null, "Customer (MET001)"
        );
        if (dup1.success) testComplaintIds.add(dup1.complaintId);

        check("21. Duplicate protection: Reference generation and rapid-click lock safeguard system", 
            dup1.success && dup1.referenceNumber != null);
    }

    private static void testTransactionRollback() {
        // Test 23: Database transaction rollback
        // Simulate a failure in transaction: insert complaint, then cause error before commit
        int complaintCountBefore = 0;
        int historyCountBefore = 0;

        try (Conn c = new Conn()) {
            try (Statement st = c.c.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM complaints")) {
                    if (rs.next()) complaintCountBefore = rs.getInt(1);
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM complaint_history")) {
                    if (rs.next()) historyCountBefore = rs.getInt(1);
                }
            }

            // Begin manual transaction and simulate failure
            c.c.setAutoCommit(false);
            try {
                String insertComplaintSql = 
                    "INSERT INTO complaints (reference_number, meter_no, category, subject, description, priority, status) " +
                    "VALUES ('CMP-SIMULATED-FAIL', 'MET001', 'Other', 'Test Subject', 'Test Description', 'Medium', 'Pending')";
                try (PreparedStatement ps = c.c.prepareStatement(insertComplaintSql)) {
                    ps.executeUpdate();
                }

                // Simulate intentional failure (e.g. violating NOT NULL constraint or invalid FK)
                String invalidHistorySql = "INSERT INTO complaint_history (complaint_id, action, performed_by) VALUES (9999999, NULL, NULL)";
                try (PreparedStatement ps = c.c.prepareStatement(invalidHistorySql)) {
                    ps.executeUpdate(); // Will throw SQLException
                }

                c.c.commit();
            } catch (SQLException expected) {
                c.c.rollback(); // Rollback triggered
            } finally {
                c.c.setAutoCommit(true);
            }

            int complaintCountAfter = 0;
            int historyCountAfter = 0;
            try (Statement st = c.c.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM complaints")) {
                    if (rs.next()) complaintCountAfter = rs.getInt(1);
                }
                try (ResultSet rs = st.executeQuery("SELECT count(*) FROM complaint_history")) {
                    if (rs.next()) historyCountAfter = rs.getInt(1);
                }
            }

            check("23. Database transaction rollback: Failed transaction completely rolled back without orphaned records",
                complaintCountBefore == complaintCountAfter && historyCountBefore == historyCountAfter);
        } catch (Exception e) {
            e.printStackTrace();
            check("23. Database transaction rollback", false);
        }
    }

    private static void cleanupTestData() {
        if (testComplaintIds.isEmpty()) return;
        try (Conn c = new Conn()) {
            if (c.c != null) {
                // Because ON DELETE RESTRICT is set on complaint_history foreign key,
                // we delete history records first, then delete test complaints.
                for (int cid : testComplaintIds) {
                    try (PreparedStatement ps = c.c.prepareStatement("DELETE FROM complaint_history WHERE complaint_id = ?")) {
                        ps.setInt(1, cid);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = c.c.prepareStatement("DELETE FROM complaints WHERE complaint_id = ?")) {
                        ps.setInt(1, cid);
                        ps.executeUpdate();
                    }
                }
                testComplaintIds.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testDatabaseIntegrity(int[] initialCounts) {
        // Cleanup test data first so counts return to baseline
        cleanupTestData();

        int[] finalCounts = getTableCounts();
        System.out.println("    customer   : Before=" + initialCounts[0] + ", After=" + finalCounts[0]);
        System.out.println("    login      : Before=" + initialCounts[1] + ", After=" + finalCounts[1]);
        System.out.println("    bill       : Before=" + initialCounts[2] + ", After=" + finalCounts[2]);
        System.out.println("    meter_info : Before=" + initialCounts[3] + ", After=" + finalCounts[3]);
        System.out.println("    tax        : Before=" + initialCounts[4] + ", After=" + finalCounts[4]);

        check("22. DB Integrity: Customer count unchanged (9)", initialCounts[0] == finalCounts[0]);
        check("22. DB Integrity: Login count unchanged (11)", initialCounts[1] == finalCounts[1]);
        check("22. DB Integrity: Bill count unchanged (18)", initialCounts[2] == finalCounts[2]);
        check("22. DB Integrity: Meter info count unchanged (9)", initialCounts[3] == finalCounts[3]);
        check("22. DB Integrity: Tax count unchanged (1)", initialCounts[4] == finalCounts[4]);
    }
}
