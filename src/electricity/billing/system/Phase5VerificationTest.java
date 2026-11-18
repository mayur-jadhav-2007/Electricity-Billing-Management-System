package electricity.billing.system;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Date;
import java.util.concurrent.*;

public class Phase5VerificationTest {

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

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  ELECTRICITY BILLING SYSTEM - PHASE 5 VERIFICATION SUITE");
        System.out.println("===============================================================");

        int[] initialCounts = getTableCounts();
        System.out.println("\n>>> Initial Database Record Counts:");
        System.out.println("    customer   : " + initialCounts[0]);
        System.out.println("    login      : " + initialCounts[1]);
        System.out.println("    bill       : " + initialCounts[2]);
        System.out.println("    meter_info : " + initialCounts[3]);
        System.out.println("    tax        : " + initialCounts[4]);
        System.out.println("---------------------------------------------------------------");

        System.out.println("\n>>> SECTION 1: PAYMENT PROCESSING & ATOMIC TRANSACTIONS");
        testPaymentProcessingAndLifecycle();

        System.out.println("\n>>> SECTION 2: CONCURRENCY & MULTIPLE PAYMENT ATTEMPTS");
        testConcurrentPaymentAttempts();

        System.out.println("\n>>> SECTION 3: ERROR HANDLING & TRANSACTION ROLLBACK");
        testFailedPaymentAndRollback();

        System.out.println("\n>>> SECTION 4: PAY BILL UI LOGIC & SELECTION");
        testPayBillUIAndSelection();

        System.out.println("\n>>> SECTION 5: PAYMENT RECEIPT & PRINTING");
        testPaymentReceiptAndPrinting();

        System.out.println("\n>>> SECTION 6: PAYMENT HISTORY & DEPOSIT LEDGER");
        testPaymentHistoryLedgerFiltering();

        System.out.println("\n>>> SECTION 7: DATABASE INTEGRITY VERIFICATION");
        testDatabaseIntegrityVerification(initialCounts);

        System.out.println("\n===============================================================");
        System.out.println("  VERIFICATION RESULT: " + testsPassed + " PASSED, " + testsFailed + " FAILED");
        System.out.println("===============================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 1: Controlled Payment Lifecycle
    // -------------------------------------------------------------------------
    private static void testPaymentProcessingAndLifecycle() {
        String testMeter = "MET001";
        String testMonth = "October"; // October has no baseline bill

        try (Conn c = new Conn()) {
            // Clean up test month
            try (PreparedStatement ps = c.c.prepareStatement("DELETE FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                ps.executeUpdate();
            }

            // Insert controlled unpaid test bill (50 units: 50*3 + 110 = 260.00)
            try (PreparedStatement ps = c.c.prepareStatement(
                "INSERT INTO bill (meter_no, month, units, total_bill, status) VALUES (?, ?, '50', '260.00', 'Not Paid')")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                ps.executeUpdate();
            }

            // 1. Successful payment processing
            PaymentGateway.PaymentResult res = PaymentGateway.executePayment(c.c, testMeter, testMonth, "260.00", "UPI Payment");
            boolean successStatus = res.status == PaymentGateway.PaymentResult.Status.SUCCESS;
            boolean validDetails = "MET001".equals(res.meterNo) && "October".equals(res.month) && "260.00".equals(res.paidAmount);
            check("Payment Processing: Successful simulated payment returns SUCCESS with matching metadata", successStatus && validDetails);

            // 8. Correct bill status update in DB
            String updatedStatus = "";
            try (PreparedStatement ps = c.c.prepareStatement("SELECT status FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) updatedStatus = rs.getString("status");
                }
            }
            check("Status Update: Database record status atomically updated to 'Paid'", "Paid".equals(updatedStatus));

            // 2. Payment of already-paid bill is blocked
            PaymentGateway.PaymentResult resAlreadyPaid = PaymentGateway.executePayment(c.c, testMeter, testMonth, "260.00", "UPI Payment");
            check("Already-Paid Protection: Subsequent payment attempt blocked with ALREADY_PAID",
                resAlreadyPaid.status == PaymentGateway.PaymentResult.Status.ALREADY_PAID);

            // 3. Duplicate payment prevention (database unchanged)
            int paidRowCount = 0;
            try (PreparedStatement ps = c.c.prepareStatement("SELECT COUNT(*) FROM bill WHERE meter_no = ? AND month = ? AND status = 'Paid'")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) paidRowCount = rs.getInt(1);
                }
            }
            check("Duplicate Prevention: Only 1 paid record exists, no duplicates created", paidRowCount == 1);

            // Clean up test bill
            try (PreparedStatement ps = c.c.prepareStatement("DELETE FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
            check("Payment Lifecycle threw unexpected exception: " + e.getMessage(), false);
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 2: Concurrency & Multiple Payment Attempts
    // -------------------------------------------------------------------------
    private static void testConcurrentPaymentAttempts() {
        String testMeter = "MET001";
        String testMonth = "October";

        try (Conn c = new Conn()) {
            // Clean up & insert fresh unpaid test bill
            try (PreparedStatement ps = c.c.prepareStatement("DELETE FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.c.prepareStatement(
                "INSERT INTO bill (meter_no, month, units, total_bill, status) VALUES (?, ?, '60', '290.00', 'Not Paid')")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                ps.executeUpdate();
            }

            // Simulate 2 parallel threads attempting to pay the same bill simultaneously
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Callable<PaymentGateway.PaymentResult> task1 = () -> PaymentGateway.executePayment(testMeter, testMonth, "290.00", "Credit Card");
            Callable<PaymentGateway.PaymentResult> task2 = () -> PaymentGateway.executePayment(testMeter, testMonth, "290.00", "Net Banking");

            Future<PaymentGateway.PaymentResult> f1 = executor.submit(task1);
            Future<PaymentGateway.PaymentResult> f2 = executor.submit(task2);

            PaymentGateway.PaymentResult r1 = f1.get(5, TimeUnit.SECONDS);
            PaymentGateway.PaymentResult r2 = f2.get(5, TimeUnit.SECONDS);
            executor.shutdown();

            int successCount = 0;
            int blockedCount = 0;
            if (r1.status == PaymentGateway.PaymentResult.Status.SUCCESS) successCount++;
            if (r2.status == PaymentGateway.PaymentResult.Status.SUCCESS) successCount++;
            if (r1.status == PaymentGateway.PaymentResult.Status.ALREADY_PAID) blockedCount++;
            if (r2.status == PaymentGateway.PaymentResult.Status.ALREADY_PAID) blockedCount++;

            check("Concurrency Guard: Concurrent payments yield exactly 1 SUCCESS and 1 ALREADY_PAID",
                successCount == 1 && blockedCount == 1);

            // Clean up
            try (PreparedStatement ps = c.c.prepareStatement("DELETE FROM bill WHERE meter_no = ? AND month = ?")) {
                ps.setString(1, testMeter);
                ps.setString(2, testMonth);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
            check("Concurrency test threw exception: " + e.getMessage(), false);
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 3: Error Handling & Transaction Rollback
    // -------------------------------------------------------------------------
    private static void testFailedPaymentAndRollback() {
        // 4. Failed payment handling for non-existent bill
        PaymentGateway.PaymentResult resInvalid = PaymentGateway.executePayment("INVALID_METER_999", "January", "500.00", "UPI");
        check("Failed Payment: Non-existent meter rejected with BILL_NOT_FOUND",
            resInvalid.status == PaymentGateway.PaymentResult.Status.BILL_NOT_FOUND);

        // Protection for historical Commercial records
        PaymentGateway.PaymentResult resComm = PaymentGateway.executePayment("MET005", "January", "811.00", "UPI");
        check("Protection: Historical Commercial records protected with PROTECTED_RECORD",
            resComm.status == PaymentGateway.PaymentResult.Status.PROTECTED_RECORD);

        // 5. Database rollback on connection failure
        try (Conn c = new Conn()) {
            c.c.close(); // simulate closed / failing connection
            PaymentGateway.PaymentResult resDbErr = PaymentGateway.executePayment(c.c, "MET001", "January", "663.00", "UPI");
            check("Rollback & Resilience: Connection error handled cleanly with DATABASE_ERROR",
                resDbErr.status == PaymentGateway.PaymentResult.Status.DATABASE_ERROR);
        } catch (Exception e) {
            check("Connection failure handling threw unexpected exception", false);
        }

        // 14. Null connection safety
        PaymentGateway.PaymentResult resNull = PaymentGateway.executePayment((Connection) null, "MET001", "January", "663.00", "UPI");
        check("Connection Guard: Null connection handled gracefully without JVM crash",
            resNull.status == PaymentGateway.PaymentResult.Status.BILL_NOT_FOUND || resNull.status == PaymentGateway.PaymentResult.Status.DATABASE_ERROR);
    }

    // -------------------------------------------------------------------------
    // SECTION 4: Pay Bill UI Logic & Selection
    // -------------------------------------------------------------------------
    private static void testPayBillUIAndSelection() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                PayBill pb = new PayBill("MET001");

                // 6. Incorrect bill selection / month without bill (e.g. November)
                pb.loadBillDetails("November");
                boolean noBillHandled = "No Bill Generated".equals(pb.labelstatus.getText())
                                     && !pb.pay.isEnabled()
                                     && !pb.viewReceipt.isEnabled()
                                     && "₹0.00".equals(pb.labeltotalbill.getText());
                check("UI Selection: Month with no bill displays 'No Bill Generated' and disables payment", noBillHandled);

                // 7. Correct outstanding amount for unpaid bill (February MET001)
                pb.loadBillDetails("February");
                boolean unpaidLoaded = "Not Paid".equals(pb.labelstatus.getText())
                                    && pb.pay.isEnabled()
                                    && "₹607.25".equals(pb.labeltotalbill.getText());
                check("Outstanding Amount: Unpaid bill correctly loads ₹607.25 and enables 'Proceed to Pay'", unpaidLoaded);

                // 10. Payment button disabled for already-paid bill (January MET001)
                pb.loadBillDetails("January");
                boolean paidLoaded = "Paid".equals(pb.labelstatus.getText())
                                  && !pb.pay.isEnabled()
                                  && pb.viewReceipt.isEnabled()
                                  && pb.labeltotalbill.getText().contains("Settled: ₹663.00");
                check("Paid State: Already-paid bill disables pay button, shows settled amount, and enables receipt", paidLoaded);

                pb.dispose();
            });
        } catch (Exception e) {
            e.printStackTrace();
            check("PayBill UI test threw exception: " + e.getMessage(), false);
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 5: Payment Receipt & Printing
    // -------------------------------------------------------------------------
    private static void testPaymentReceiptAndPrinting() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                PaymentReceiptDialog receipt = new PaymentReceiptDialog(
                    null,
                    "Parth",
                    "MET001",
                    "February",
                    "607.25",
                    "UPI Payment",
                    new Date()
                );

                String text = receipt.getReceiptText();

                // 11. Receipt content correctness
                boolean hasCompany = text.contains("ELECTRICITY BILLING SYSTEM (EBS)");
                boolean hasCustomer = text.contains("Parth") && text.contains("MET001");
                boolean hasMonth = text.contains("February");
                boolean hasAmount = text.contains("₹607.25");
                boolean hasStatus = text.contains("PAID");
                boolean noFakeTxn = text.contains("Transaction Ref    : None (Not stored in schema - Simulated)");
                boolean isSandboxNotice = text.contains("Simulated Sandbox");

                check("Receipt Content: Formatted with company header, customer details, ₹ currency, and sandbox notice",
                    hasCompany && hasCustomer && hasMonth && hasAmount && hasStatus && isSandboxNotice);
                check("Receipt Integrity: Does not invent fake transaction ID (explicit schema notice displayed)",
                    noFakeTxn);

                // 12. Receipt printing functionality
                boolean printButtonConfigured = receipt.print != null && receipt.print.getActionListeners().length > 0;
                check("Receipt Printing: Print action listener properly configured for physical/PDF printing",
                    printButtonConfigured);

                receipt.dispose();
            });
        } catch (Exception e) {
            e.printStackTrace();
            check("Receipt test threw exception: " + e.getMessage(), false);
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 6: Payment History & Deposit Ledger
    // -------------------------------------------------------------------------
    private static void testPaymentHistoryLedgerFiltering() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                DepositDetails dd = new DepositDetails();

                // Total bills
                int allBills = dd.loadBills("All Meters", "All Months", "All Statuses", false);

                // Paid bills only (Payment History Ledger)
                int paidBills = dd.loadBills("All Meters", "All Months", "Paid Only", false);

                // Unpaid bills only
                int unpaidBills = dd.loadBills("All Meters", "All Months", "Not Paid Only", false);

                check("Payment History: Ledger filters Paid bills exclusively (found " + paidBills + " Paid bills)",
                    paidBills == 9 && allBills == 18);
                check("Payment History: Status filtering correctly partitions all 18 bills (Paid: " + paidBills + ", Unpaid: " + unpaidBills + ", Commercial: 2)",
                    paidBills == 9 && unpaidBills == 7 && (paidBills + unpaidBills + 2 == allBills));

                dd.dispose();
            });
        } catch (Exception e) {
            e.printStackTrace();
            check("Payment History filter threw exception: " + e.getMessage(), false);
        }
    }

    // -------------------------------------------------------------------------
    // SECTION 7: Database Integrity Verification
    // -------------------------------------------------------------------------
    private static void testDatabaseIntegrityVerification(int[] before) {
        int[] after = getTableCounts();
        System.out.println("    customer   : Before=" + before[0] + ", After=" + after[0]);
        System.out.println("    login      : Before=" + before[1] + ", After=" + after[1]);
        System.out.println("    bill       : Before=" + before[2] + ", After=" + after[2]);
        System.out.println("    meter_info : Before=" + before[3] + ", After=" + after[3]);
        System.out.println("    tax        : Before=" + before[4] + ", After=" + after[4]);

        check("DB Integrity: Customer count unchanged", before[0] == after[0]);
        check("DB Integrity: Login count unchanged", before[1] == after[1]);
        check("DB Integrity: Bill count unchanged", before[2] == after[2]);
        check("DB Integrity: Meter info count unchanged", before[3] == after[3]);
        check("DB Integrity: Tax count unchanged", before[4] == after[4]);

        // MET005 check
        boolean commIntact = true;
        try (Conn c = new Conn()) {
            try (PreparedStatement ps = c.c.prepareStatement("SELECT status FROM bill WHERE meter_no = 'MET005'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        if (!"Commercial".equalsIgnoreCase(rs.getString("status"))) commIntact = false;
                    }
                    if (count != 2) commIntact = false;
                }
            }
        } catch (Exception e) {
            commIntact = false;
        }
        check("13. Bill Preservation: MET005 Commercial records preserved without modification (2 records)", commIntact);
    }

    private static int[] getTableCounts() {
        int[] counts = new int[5];
        String[] tables = {"customer", "login", "bill", "meter_info", "tax"};
        try (Conn c = new Conn()) {
            if (c.c != null) {
                for (int i = 0; i < tables.length; i++) {
                    try (Statement stmt = c.c.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tables[i])) {
                        if (rs.next()) counts[i] = rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counts;
    }
}
