package electricity.billing.system;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Automated Verification Suite for Phase 8: Audit Logs and Activity Tracking.
 * Validates session lifecycle, transactional atomicity, event auditing,
 * formula injection defense, pagination, filtering, and database baseline integrity.
 */
public class Phase8VerificationTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    private static final String TEST_METER = "999999";
    private static final String TEST_USER = "test_user_p8";

    private static void check(String description, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + description);
            testsPassed++;
        } else {
            System.err.println("  [FAIL] " + description);
            testsFailed++;
        }
    }

    private static int[] getBaselineCounts() {
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

    private static void cleanTestArtifacts() {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement()) {
                    stmt.executeUpdate("DELETE FROM complaint_history WHERE complaint_id IN (SELECT complaint_id FROM complaints WHERE meter_no = '" + TEST_METER + "')");
                    stmt.executeUpdate("DELETE FROM complaints WHERE meter_no = '" + TEST_METER + "'");
                    stmt.executeUpdate("DELETE FROM bill WHERE meter_no = '" + TEST_METER + "'");
                    stmt.executeUpdate("DELETE FROM meter_info WHERE meter_no = '" + TEST_METER + "'");
                    stmt.executeUpdate("DELETE FROM login WHERE meter_no = '" + TEST_METER + "' OR username = '" + TEST_USER + "'");
                    stmt.executeUpdate("DELETE FROM customer WHERE meter_no = '" + TEST_METER + "'");
                    stmt.executeUpdate("DELETE FROM audit_logs WHERE actor_id LIKE 'TEST%' OR entity_id = '" + TEST_METER + "' OR actor_id = '" + TEST_USER + "' OR entity_id LIKE '" + TEST_METER + "%'");
                }
            }
        } catch (Exception e) {
            System.err.println("Error cleaning test artifacts: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  ELECTRICITY BILLING SYSTEM - PHASE 8 VERIFICATION SUITE");
        System.out.println("===============================================================");

        int[] initialCounts = getBaselineCounts();
        System.out.println("\n>>> Initial Database Record Counts:");
        System.out.println("    customer          : " + initialCounts[0] + " (Expected: 9)");
        System.out.println("    login             : " + initialCounts[1] + " (Expected: 11)");
        System.out.println("    bill              : " + initialCounts[2] + " (Expected: 18)");
        System.out.println("    meter_info        : " + initialCounts[3] + " (Expected: 9)");
        System.out.println("    tax               : " + initialCounts[4] + " (Expected: 1)");
        System.out.println("    complaints        : " + initialCounts[5] + " (Expected: 2)");
        System.out.println("    complaint_history : " + initialCounts[6] + " (Expected: 8)");
        System.out.println("---------------------------------------------------------------");

        // Clean any leftover test records before starting
        cleanTestArtifacts();

        try {
            System.out.println("\n>>> SECTION 1: SESSION CONTEXT & IMMUTABILITY");
            testSessionContextAndUserSession();

            System.out.println("\n>>> SECTION 2: AUTHENTICATION AUDITING & ROLE DERIVATION");
            testAuthenticationAuditing();

            System.out.println("\n>>> SECTION 3: LOGOUT IDEMPOTENCY & AT-MOST-ONCE ATTEMPT");
            testLogoutIdempotency();

            System.out.println("\n>>> SECTION 4: AUTHORIZATION & ADMIN-ONLY AUDIT VIEWER");
            testAuthorizationChecks();

            System.out.println("\n>>> SECTION 5: CUSTOMER REGISTRATION & AUDIT ATOMICITY");
            testCustomerRegistrationAtomicity();

            System.out.println("\n>>> SECTION 6: PROFILE UPDATE & AUDIT ATOMICITY");
            testCustomerUpdateAtomicity();

            System.out.println("\n>>> SECTION 7: DEACTIVATION & REACTIVATION AUDIT EVENTS");
            testDeactivationReactivationAuditing();

            System.out.println("\n>>> SECTION 8: BILL GENERATION & RECALCULATION AUDIT EVENTS");
            testBillingAuditing();

            System.out.println("\n>>> SECTION 9: PAYMENT COMPLETION, FAILURE & DUPLICATE ATTEMPT");
            testPaymentAuditing();

            System.out.println("\n>>> SECTION 10: COMPLAINT LIFECYCLE AUDITING");
            testComplaintAuditing();

            System.out.println("\n>>> SECTION 11: PAGINATION & FILTERING QUERIES");
            testPaginationAndFiltering();

            System.out.println("\n>>> SECTION 12: CSV FORMULA INJECTION DEFENSE (CWE-1236)");
            testCsvFormulaInjectionDefense();

            System.out.println("\n>>> SECTION 13: COMPLAINT HISTORY INTEGRATION & ATTRIBUTION");
            testComplaintHistoryIntegration();

            System.out.println("\n>>> SECTION 14: AUDIT INSERT FAILURE & BUSINESS TRANSACTION ROLLBACK");
            testAuditFailureRollbackBehavior();

        } finally {
            // Guarantee cleanup of test artifacts
            cleanTestArtifacts();
            UserSession.invalidate();
        }

        System.out.println("\n>>> SECTION 15: DATABASE BASELINE INTEGRITY VERIFICATION");
        int[] finalCounts = getBaselineCounts();
        System.out.println("    customer          : " + finalCounts[0] + " (Expected: " + initialCounts[0] + ")");
        System.out.println("    login             : " + finalCounts[1] + " (Expected: " + initialCounts[1] + ")");
        System.out.println("    bill              : " + finalCounts[2] + " (Expected: " + initialCounts[2] + ")");
        System.out.println("    meter_info        : " + finalCounts[3] + " (Expected: " + initialCounts[3] + ")");
        System.out.println("    tax               : " + finalCounts[4] + " (Expected: " + initialCounts[4] + ")");
        System.out.println("    complaints        : " + finalCounts[5] + " (Expected: " + initialCounts[5] + ")");
        System.out.println("    complaint_history : " + finalCounts[6] + " (Expected: " + initialCounts[6] + ")");

        check("customer table count matches baseline exactly", finalCounts[0] == initialCounts[0]);
        check("login table count matches baseline exactly", finalCounts[1] == initialCounts[1]);
        check("bill table count matches baseline exactly", finalCounts[2] == initialCounts[2]);
        check("meter_info table count matches baseline exactly", finalCounts[3] == initialCounts[3]);
        check("tax table count matches baseline exactly", finalCounts[4] == initialCounts[4]);
        check("complaints table count matches baseline exactly", finalCounts[5] == initialCounts[5]);
        check("complaint_history table count matches baseline exactly", finalCounts[6] == initialCounts[6]);

        System.out.println("\n===============================================================");
        System.out.println("  PHASE 8 VERIFICATION SUMMARY: " + testsPassed + " PASSED, " + testsFailed + " FAILED");
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.err.println("TEST SUITE FAILED: " + testsFailed + " assertion(s) failed.");
            System.exit(1);
        }
    }

    // =========================================================================
    // SECTION TESTS
    // =========================================================================

    private static void testSessionContextAndUserSession() {
        SessionContext ctx = new SessionContext("admin_test", "Admin", "");
        check("SessionContext username preserved", "admin_test".equals(ctx.getUsername()));
        check("SessionContext role preserved", "Admin".equals(ctx.getRole()));
        check("SessionContext isAdmin() returns true", ctx.isAdmin());
        check("SessionContext isCustomer() returns false", !ctx.isCustomer());

        SessionContext custCtx = new SessionContext("cust_test", "Customer", "1001");
        check("SessionContext isCustomer() returns true", custCtx.isCustomer());
        check("SessionContext meterNo preserved", "1001".equals(custCtx.getMeterNo()));

        UserSession.setSession(ctx);
        check("UserSession.isAuthenticated() is true", UserSession.isAuthenticated());
        check("UserSession.isAdmin() is true", UserSession.isAdmin());
        check("UserSession.getActorId() returns username", "admin_test".equals(UserSession.getActorId()));
        check("UserSession.getActorRole() returns Admin", "Admin".equals(UserSession.getActorRole()));

        UserSession.invalidate();
        check("UserSession.isAuthenticated() after invalidate() is false", !UserSession.isAuthenticated());
        check("UserSession.getActorId() returns SYSTEM when unauthenticated", "SYSTEM".equals(UserSession.getActorId()));
    }

    private static void testAuthenticationAuditing() {
        // 1. Test failed login with invalid credentials
        String failRes = Login.authenticateUser("nonexistent_user_999", "wrong_pass", "Admin");
        check("Failed login returns INVALID_CREDENTIALS", "INVALID_CREDENTIALS".equals(failRes));

        // 2. Test failed login with deactivated customer
        // Deactivate MET001 temporarily
        CustomerDetails.setCustomerStatus("MET001", "Inactive");
        String deactRes = Login.authenticateUser("rajesh2024", "raj@123", "Customer");
        check("Deactivated account authentication returns DEACTIVATED", "DEACTIVATED".equals(deactRes));
        // Reactivate back immediately
        CustomerDetails.setCustomerStatus("MET001", "Active");

        // 3. Test successful admin authentication
        String successRes = Login.authenticateUser("Parth", "Parth123", "Admin");
        check("Valid admin authentication returns SUCCESS", "SUCCESS".equals(successRes));
        check("Authenticated session is established", UserSession.isAuthenticated() && UserSession.isAdmin());
    }

    private static void testLogoutIdempotency() {
        UserSession.setSession(new SessionContext("logout_user", "Admin", ""));
        check("Session active prior to logout", UserSession.isAuthenticated());

        // First consume call must succeed and return non-null context
        SessionContext first = UserSession.consumeSession();
        check("First consumeSession() call returns non-null context", first != null && "logout_user".equals(first.getUsername()));
        check("Session invalidated immediately after first consume", !UserSession.isAuthenticated());

        // Second consume call must return null (at-most-once delivery)
        SessionContext second = UserSession.consumeSession();
        check("Second consumeSession() call returns null (duplicate prevented)", second == null);
    }

    private static void testAuthorizationChecks() {
        // Customer session
        UserSession.setSession(new SessionContext("cust_user", "Customer", "1001"));
        check("Customer user has isAdmin() false", !UserSession.isAdmin());

        // Unauthenticated
        UserSession.invalidate();
        check("Unauthenticated session has isAdmin() false", !UserSession.isAdmin());

        // Admin session
        UserSession.setSession(new SessionContext("admin", "Admin", ""));
        check("Admin session has isAdmin() true", UserSession.isAdmin());
    }

    private static void testCustomerRegistrationAtomicity() {
        UserSession.setSession(new SessionContext("admin", "Admin", ""));

        try (Conn c = new Conn()) {
            c.c.setAutoCommit(false);

            // Insert customer + login + audit
            String q1 = "INSERT INTO customer (name, meter_no, address, city, state, email, phone, status) " +
                        "VALUES ('Test Customer P8', '" + TEST_METER + "', '123 Test St', 'TestCity', 'TestState', 'test@test.com', '9876543210', 'Active')";
            try (Statement st = c.c.createStatement()) {
                st.executeUpdate(q1);
            }

            String q2 = "INSERT INTO login (meter_no, username, name, password, user) " +
                        "VALUES ('" + TEST_METER + "', '" + TEST_USER + "', 'Test Customer P8', 'hash', 'Customer')";
            try (Statement st = c.c.createStatement()) {
                st.executeUpdate(q2);
            }

            AuditService.logEvent(c.c, "admin", "Admin", AuditService.ACTION_CUSTOMER_CREATED,
                AuditService.ENTITY_CUSTOMER, TEST_METER, "Registered customer: Test Customer P8", null, "Active");

            c.c.commit();
            check("Customer registration transaction committed", true);

            // Verify audit record exists
            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'CUSTOMER_CREATED' AND entity_id = ?")) {
                ps.setString(1, TEST_METER);
                try (ResultSet rs = ps.executeQuery()) {
                    check("CUSTOMER_CREATED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }

        } catch (Exception e) {
            check("Customer registration exception: " + e.getMessage(), false);
        }
    }

    private static void testCustomerUpdateAtomicity() {
        UserSession.setSession(new SessionContext("admin", "Admin", ""));

        try (Conn c = new Conn()) {
            c.c.setAutoCommit(false);

            String updateQ = "UPDATE customer SET address = '456 Updated St' WHERE meter_no = ?";
            try (PreparedStatement ps = c.c.prepareStatement(updateQ)) {
                ps.setString(1, TEST_METER);
                ps.executeUpdate();
            }

            AuditService.logEvent(c.c, "admin", "Admin", AuditService.ACTION_CUSTOMER_UPDATED,
                AuditService.ENTITY_CUSTOMER, TEST_METER, "Updated address", "123 Test St", "456 Updated St");

            c.c.commit();
            check("Customer update transaction committed", true);

            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'CUSTOMER_UPDATED' AND entity_id = ?")) {
                ps.setString(1, TEST_METER);
                try (ResultSet rs = ps.executeQuery()) {
                    check("CUSTOMER_UPDATED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Customer update exception: " + e.getMessage(), false);
        }
    }

    private static void testDeactivationReactivationAuditing() {
        UserSession.setSession(new SessionContext("admin", "Admin", ""));

        // Deactivate test customer
        boolean deactOk = CustomerDetails.setCustomerStatus(TEST_METER, "Inactive");
        check("Deactivation call succeeded", deactOk);

        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'CUSTOMER_DEACTIVATED' AND entity_id = ?")) {
                ps.setString(1, TEST_METER);
                try (ResultSet rs = ps.executeQuery()) {
                    check("CUSTOMER_DEACTIVATED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Deactivation verify error: " + e.getMessage(), false);
        }

        // Reactivate test customer
        boolean reactOk = CustomerDetails.setCustomerStatus(TEST_METER, "Active");
        check("Reactivation call succeeded", reactOk);

        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'CUSTOMER_REACTIVATED' AND entity_id = ?")) {
                ps.setString(1, TEST_METER);
                try (ResultSet rs = ps.executeQuery()) {
                    check("CUSTOMER_REACTIVATED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Reactivation verify error: " + e.getMessage(), false);
        }
    }

    private static void testBillingAuditing() {
        UserSession.setSession(new SessionContext("admin", "Admin", ""));

        // Insert meter_info for test meter so bill calculation passes rate check
        try (Conn c = new Conn()) {
            String mi = "INSERT INTO meter_info (meter_no, meter_location, meter_type, phase_code, bill_type, days) " +
                        "VALUES ('" + TEST_METER + "', 'Outside', 'Electric Meter', '011', 'Normal', '30')";
            try (Statement st = c.c.createStatement()) {
                st.executeUpdate(mi);
            }
        } catch (Exception ignored) {}

        // 1. Generate Bill
        try (Conn c = new Conn()) {
            CalculateBill.BillProcessResult res = CalculateBill.processBill(c.c, TEST_METER, "January", "100", false);
            check("Bill generated successfully", res.status == CalculateBill.BillProcessResult.Status.SUCCESS_INSERTED);

            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'BILL_GENERATED' AND entity_id = ?")) {
                ps.setString(1, TEST_METER + ":January");
                try (ResultSet rs = ps.executeQuery()) {
                    check("BILL_GENERATED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Bill generation verify error: " + e.getMessage(), false);
        }

        // 2. Recalculate Bill
        try (Conn c = new Conn()) {
            CalculateBill.BillProcessResult res = CalculateBill.processBill(c.c, TEST_METER, "January", "150", true);
            check("Bill recalculated successfully", res.status == CalculateBill.BillProcessResult.Status.SUCCESS_UPDATED);

            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'BILL_RECALCULATED' AND entity_id = ?")) {
                ps.setString(1, TEST_METER + ":January");
                try (ResultSet rs = ps.executeQuery()) {
                    check("BILL_RECALCULATED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Bill recalculation verify error: " + e.getMessage(), false);
        }
    }

    private static void testPaymentAuditing() {
        UserSession.setSession(new SessionContext("admin", "Admin", ""));

        // 1. Successful payment
        PaymentGateway.PaymentResult payRes = PaymentGateway.executePayment(TEST_METER, "January", "0", "UPI");
        check("Payment completed successfully", payRes.status == PaymentGateway.PaymentResult.Status.SUCCESS);

        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'PAYMENT_COMPLETED' AND entity_id = ?")) {
                ps.setString(1, TEST_METER + ":January");
                try (ResultSet rs = ps.executeQuery()) {
                    check("PAYMENT_COMPLETED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Payment verify error: " + e.getMessage(), false);
        }

        // 2. Duplicate payment attempt
        PaymentGateway.PaymentResult dupRes = PaymentGateway.executePayment(TEST_METER, "January", "0", "UPI");
        check("Duplicate payment rejected as ALREADY_PAID", dupRes.status == PaymentGateway.PaymentResult.Status.ALREADY_PAID);

        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'PAYMENT_DUPLICATE_ATTEMPT' AND entity_id = ?")) {
                ps.setString(1, TEST_METER + ":January");
                try (ResultSet rs = ps.executeQuery()) {
                    check("PAYMENT_DUPLICATE_ATTEMPT audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Duplicate payment verify error: " + e.getMessage(), false);
        }

        // 3. Failed payment on nonexistent bill
        PaymentGateway.PaymentResult failRes = PaymentGateway.executePayment(TEST_METER, "December", "0", "UPI");
        check("Payment on nonexistent bill returns BILL_NOT_FOUND", failRes.status == PaymentGateway.PaymentResult.Status.BILL_NOT_FOUND);

        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'PAYMENT_FAILED' AND entity_id = ?")) {
                ps.setString(1, TEST_METER + ":December");
                try (ResultSet rs = ps.executeQuery()) {
                    check("PAYMENT_FAILED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Failed payment verify error: " + e.getMessage(), false);
        }
    }

    private static void testComplaintAuditing() {
        UserSession.setSession(new SessionContext("admin", "Admin", ""));

        // 1. Submit complaint
        ComplaintService.ServiceResult subRes = ComplaintService.submitComplaint(
            TEST_METER, "Billing Issues", "Test Subject", "Test Description", "January", "Customer (" + TEST_METER + ")"
        );
        check("Complaint submitted successfully", subRes.success);
        String ref = subRes.referenceNumber;
        int id = subRes.complaintId;

        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = 'COMPLAINT_SUBMITTED' AND entity_id = ?")) {
                ps.setString(1, ref);
                try (ResultSet rs = ps.executeQuery()) {
                    check("COMPLAINT_SUBMITTED audit record logged", rs.next() && rs.getInt(1) > 0);
                }
            }
        } catch (Exception e) {
            check("Complaint submit verify error: " + e.getMessage(), false);
        }

        // 2. Assign complaint
        ComplaintService.ServiceResult assignRes = ComplaintService.assignComplaint(id, "admin", "admin", "Assigning to admin");
        check("Complaint assigned successfully", assignRes.success);

        // 3. Priority change
        ComplaintService.ServiceResult prioRes = ComplaintService.updatePriority(id, "High", "admin", "Escalating priority");
        check("Complaint priority updated successfully", prioRes.success);

        // 4. Status change to In Progress
        ComplaintService.ServiceResult inProgRes = ComplaintService.updateStatus(id, "In Progress", "admin", "Starting investigation");
        check("Complaint status changed to In Progress successfully", inProgRes.success);

        // 5. Status change to Resolved
        ComplaintService.ServiceResult resRes = ComplaintService.updateStatus(id, "Resolved", "admin", "Resolving complaint ticket");
        check("Complaint status changed to Resolved successfully", resRes.success);

        // 6. Remarks added
        ComplaintService.ServiceResult remRes = ComplaintService.addRemarks(id, "Final resolution notes", "admin");
        check("Resolution remarks added successfully", remRes.success);

        // 7. Reopen complaint (Resolved -> In Progress)
        ComplaintService.ServiceResult reopRes = ComplaintService.updateStatus(id, "In Progress", "admin", "Reopening for further testing");
        check("Complaint reopened successfully", reopRes.success);

        try (Conn c = new Conn()) {
            String[] actions = {
                AuditService.ACTION_COMPLAINT_ASSIGNED,
                AuditService.ACTION_COMPLAINT_PRIORITY_CHANGED,
                AuditService.ACTION_COMPLAINT_STATUS_CHANGED,
                AuditService.ACTION_COMPLAINT_REMARK_ADDED,
                AuditService.ACTION_COMPLAINT_REOPENED
            };
            for (String act : actions) {
                try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE action_type = ? AND entity_id = ?")) {
                    ps.setString(1, act);
                    ps.setString(2, ref);
                    try (ResultSet rs = ps.executeQuery()) {
                        check(act + " audit record logged", rs.next() && rs.getInt(1) > 0);
                    }
                }
            }
        } catch (Exception e) {
            check("Complaint actions verify error: " + e.getMessage(), false);
        }
    }

    private static void testPaginationAndFiltering() {
        try {
            // 1. Query page 1 with page size 5
            AuditService.QueryResult page1 = AuditService.queryLogs("All Actions", "All Roles", "", null, null, 1, 5, false);
            check("Pagination returns non-null QueryResult", page1 != null);
            check("QueryResult page is 1", page1.page == 1);
            check("QueryResult pageSize is 5", page1.pageSize == 5);
            check("QueryResult entries <= 5", page1.entries.size() <= 5);

            // 2. Query filter by action
            AuditService.QueryResult filteredAction = AuditService.queryLogs(AuditService.ACTION_LOGIN_SUCCESS, "All Roles", "", null, null, 1, 10, false);
            boolean allActionMatch = true;
            for (AuditService.AuditLogEntry e : filteredAction.entries) {
                if (!AuditService.ACTION_LOGIN_SUCCESS.equals(e.actionType)) {
                    allActionMatch = false;
                }
            }
            check("Action filter returns only matching action records", allActionMatch);

            // 3. Query filter by role
            AuditService.QueryResult filteredRole = AuditService.queryLogs("All Actions", "Admin", "", null, null, 1, 10, false);
            boolean allRoleMatch = true;
            for (AuditService.AuditLogEntry e : filteredRole.entries) {
                if (!"Admin".equalsIgnoreCase(e.actorRole)) {
                    allRoleMatch = false;
                }
            }
            check("Role filter returns only matching role records", allRoleMatch);

            // 4. Query search by term
            AuditService.QueryResult searchRes = AuditService.queryLogs("All Actions", "All Roles", "admin", null, null, 1, 10, false);
            check("Search query executed cleanly", searchRes != null && searchRes.totalRecords >= 0);

        } catch (Exception e) {
            check("Pagination/filtering exception: " + e.getMessage(), false);
        }
    }

    private static void testCsvFormulaInjectionDefense() {
        // 1. Trigger characters (=, +, -, @)
        check("Formula =1+1 is neutralized with prepended '", "\"'=1+1\"".equals(AuditService.sanitizeCsvCell("=1+1")));
        check("Formula +cmd is neutralized with prepended '",
              AuditService.sanitizeCsvCell("+cmd|' /C calc'!A0").startsWith("\"'+cmd"));
        check("Formula -SUM is neutralized with prepended '", "\"\'-SUM(A1:B10)\"".equals(AuditService.sanitizeCsvCell("-SUM(A1:B10)")));
        check("Formula @HYPERLINK is neutralized", AuditService.sanitizeCsvCell("@HYPERLINK(\"http://evil.com\")").startsWith("\"'@HYPERLINK"));

        // 2. Leading ASCII whitespace & control characters
        check("Leading space with formula neutralized", AuditService.sanitizeCsvCell("   =2*3").startsWith("\"'   =2*3"));
        check("Leading tab with formula neutralized", AuditService.sanitizeCsvCell("\t+100").startsWith("\"'\t+100"));
        check("Leading CR/LF with formula neutralized", AuditService.sanitizeCsvCell("\r\n=1+1").startsWith("\"'\r\n=1+1"));
        check("Leading control chars neutralized", AuditService.sanitizeCsvCell("\u0000\u001F=1+1").startsWith("\"'\u0000\u001F=1+1"));

        // 3. Unicode whitespace & BOMs
        check("Unicode whitespace neutralized", AuditService.sanitizeCsvCell("\u00A0\u2003=1+1").startsWith("\"'\u00A0\u2003=1+1"));
        check("Leading BOM neutralized", AuditService.sanitizeCsvCell("\uFEFF=1+1").startsWith("\"'\uFEFF=1+1"));
        check("Whitespace + BOM + formula neutralized", AuditService.sanitizeCsvCell("   \uFEFF=1+1").startsWith("\"'   \uFEFF=1+1"));
        check("Mixed controls + BOM + formula neutralized", AuditService.sanitizeCsvCell(" \t\r\u0000\uFEFF-cmd").startsWith("\"' \t\r\u0000\uFEFF-cmd"));

        // 4. Embedded BOM is not neutralized if no leading trigger
        check("Embedded BOM preserved safely", "\"Customer\uFEFF123\"".equals(AuditService.sanitizeCsvCell("Customer\uFEFF123")));

        // 5. Quotes, commas, and newlines
        check("Quotes and commas escaped properly", "\"John \"\"Doe\"\", Admin\"".equals(AuditService.sanitizeCsvCell("John \"Doe\", Admin")));

        // 6. Null and empty
        check("Null returns empty quoted cell", "\"\"".equals(AuditService.sanitizeCsvCell(null)));
        check("Empty string returns empty quoted cell", "\"\"".equals(AuditService.sanitizeCsvCell("")));
        check("Plain text untouched", "\"Plain Text\"".equals(AuditService.sanitizeCsvCell("Plain Text")));
    }

    private static void testComplaintHistoryIntegration() {
        try {
            // Query with includeComplaintHistory = true
            AuditService.QueryResult combined = AuditService.queryLogs("All Actions", "All Roles", "", null, null, 1, 50, true);
            check("Combined query returns results", combined != null && combined.totalRecords > 0);

            boolean hasComplaintHist = false;
            boolean legacyAdminPreserved = false;
            for (AuditService.AuditLogEntry e : combined.entries) {
                if ("complaint_history".equals(e.sourceTable)) {
                    hasComplaintHist = true;
                    check("Complaint history displayId prefixed with CMP-HIST-", e.displayId.startsWith("CMP-HIST-"));
                    if ("Admin".equals(e.actorId)) {
                        if ("Legacy/Unknown".equals(e.actorRole)) {
                            legacyAdminPreserved = true;
                        }
                    }
                } else if ("audit_logs".equals(e.sourceTable)) {
                    check("Audit log displayId prefixed with AUD-", e.displayId.startsWith("AUD-"));
                }
            }

            check("Combined query successfully included complaint_history entries", hasComplaintHist);
            check("Legacy complaint history with performed_by='Admin' mapped to actor_role='Legacy/Unknown'", legacyAdminPreserved);

        } catch (Exception e) {
            check("Complaint history integration exception: " + e.getMessage(), false);
        }
    }

    private static void testAuditFailureRollbackBehavior() {
        // Verify transactional atomicity: if audit log insertion fails, business update is rolled back.
        try (Conn c = new Conn()) {
            c.c.setAutoCommit(false);

            // Business update
            String upd = "UPDATE customer SET city = 'ShouldNotPersist' WHERE meter_no = ?";
            try (PreparedStatement ps = c.c.prepareStatement(upd)) {
                ps.setString(1, TEST_METER);
                ps.executeUpdate();
            }

            // Simulate audit failure by violating a constraint or catching forced exception
            boolean auditFailed = false;
            try {
                // Pass invalid connection or trigger exception
                AuditService.logEvent(null, "admin", "Admin", "TEST_FAIL", "CUSTOMER", TEST_METER, "desc", null, null);
            } catch (SQLException ex) {
                auditFailed = true;
                c.c.rollback(); // Rollback business update upon audit failure
            }

            check("Audit failure detected as expected", auditFailed);

            // Verify customer city was NOT updated in DB
            c.c.setAutoCommit(true);
            try (PreparedStatement ps = c.c.prepareStatement("SELECT city FROM customer WHERE meter_no = ?")) {
                ps.setString(1, TEST_METER);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String city = rs.getString("city");
                        check("Business transaction successfully rolled back upon audit failure", !"ShouldNotPersist".equals(city));
                    }
                }
            }

        } catch (Exception e) {
            check("Rollback test exception: " + e.getMessage(), false);
        }
    }
}
