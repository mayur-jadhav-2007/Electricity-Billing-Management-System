package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentGateway extends JFrame implements ActionListener {

    long billId = -1;
    String meter, month, amount;
    BigDecimal principalAmount = BigDecimal.ZERO;
    BigDecimal lateFeeAmount = BigDecimal.ZERO;
    BigDecimal totalPayableAmount = BigDecimal.ZERO;
    java.sql.Date dueDate = null;
    java.sql.Date paymentDate = null;
    long delayDays = 0;

    JButton payUPI, payCard, payNetBanking, payWallet, backBtn;
    JLabel amountLabel, meterLabel, billIdLabel, dueDateLabel, delayLabel, surchargeLabel, principalLabel;
    boolean isProcessing = false;

    // Structured result class for transaction execution and headless verification
    public static class PaymentResult {
        public enum Status {
            SUCCESS,
            ALREADY_PAID,
            BILL_NOT_FOUND,
            PROTECTED_RECORD,
            DATABASE_ERROR,
            PRICE_DRIFT
        }

        public final Status status;
        public final String message;
        public final String customerName;
        public final String meterNo;
        public final String month;
        public final String billedAmount;
        public final String paidAmount;
        public final String paymentMethod;
        public final Date timestamp;
        public final long billId;
        public final BigDecimal lateFee;
        public final java.sql.Date dueDate;

        public PaymentResult(Status status, String message, String customerName, String meterNo,
                             String month, String billedAmount, String paidAmount, String paymentMethod, Date timestamp) {
            this(status, message, customerName, meterNo, month, billedAmount, paidAmount, paymentMethod, timestamp, -1, BigDecimal.ZERO, null);
        }

        public PaymentResult(Status status, String message, String customerName, String meterNo,
                             String month, String billedAmount, String paidAmount, String paymentMethod, Date timestamp,
                             long billId, BigDecimal lateFee, java.sql.Date dueDate) {
            this.status = status;
            this.message = (message != null) ? message : "";
            this.customerName = (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : "Customer";
            this.meterNo = (meterNo != null) ? meterNo.trim() : "N/A";
            this.month = (month != null) ? month.trim() : "N/A";
            this.billedAmount = (billedAmount != null) ? billedAmount.trim() : "0.00";
            this.paidAmount = (paidAmount != null) ? paidAmount.trim() : "0.00";
            this.paymentMethod = (paymentMethod != null) ? paymentMethod.trim() : "Simulated Sandbox";
            this.timestamp = (timestamp != null) ? timestamp : new Date();
            this.billId = billId;
            this.lateFee = (lateFee != null) ? lateFee : BigDecimal.ZERO;
            this.dueDate = dueDate;
        }
    }

    /**
     * Primary Phase 9 constructor: identifies the exact bill by primary key billId.
     */
    public PaymentGateway(long billId, String meter, String month, String amount) {
        this.billId = billId;
        this.meter = (meter != null) ? meter.trim() : "";
        this.month = (month != null) ? month.trim() : "";
        this.amount = (amount != null) ? amount.trim() : "0.00";

        loadAuthoritativeBillPreview();
        initWindow();
    }

    /**
     * Legacy constructor: resolves bill_id from meter and month for backward compatibility.
     */
    public PaymentGateway(String meter, String month, String amount) {
        this(-1, meter, month, amount);
    }

    private void loadAuthoritativeBillPreview() {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                // If billId not passed, resolve from meter and month
                if (this.billId <= 0 && !this.meter.isEmpty() && !this.month.isEmpty()) {
                    String findSql = "SELECT bill_id FROM bill WHERE meter_no = ? AND LOWER(month) = LOWER(?) ORDER BY bill_id ASC LIMIT 1";
                    try (PreparedStatement ps = c.c.prepareStatement(findSql)) {
                        ps.setString(1, this.meter);
                        ps.setString(2, this.month);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                this.billId = rs.getLong("bill_id");
                            }
                        }
                    }
                }

                // If billId resolved, load authoritative details
                if (this.billId > 0) {
                    LatePaymentService.BillRecord br = LatePaymentService.getBillById(c.c, this.billId);
                    if (br != null) {
                        this.meter = br.meterNo;
                        this.month = br.month;
                        this.principalAmount = br.getPrincipalBigDecimal();
                        this.dueDate = br.dueDate;
                        this.paymentDate = LatePaymentService.getEffectiveDate(c.c);

                        LatePaymentService.LpscResult lpsc = LatePaymentService.calculateLpsc(this.principalAmount, this.dueDate, this.paymentDate);
                        this.delayDays = lpsc.delayDays;
                        this.lateFeeAmount = lpsc.lateFee;
                        this.totalPayableAmount = lpsc.totalPayable;
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback calculation
        this.principalAmount = parseBigDecimal(this.amount);
        this.lateFeeAmount = BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
        this.totalPayableAmount = this.principalAmount;
    }

    private void initWindow() {
        setTitle("Payment Gateway - Electricity Bill Settlement");
        setSize(620, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        initializeUI();
        setVisible(true);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Main panel with gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                Color color1 = new Color(30, 58, 138);  // Deep royal navy
                Color color2 = new Color(79, 70, 229);  // Indigo
                GradientPaint gp = new GradientPaint(0, 0, color1, w, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new BorderLayout());

        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 40, 12, 40));

        JLabel titleLabel = new JLabel("💳 Payment Gateway", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Authoritative Billing & Surcharge Settlement (Simulated)", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(254, 240, 138)); // Soft warning yellow
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel noticeLabel = new JLabel("UPERC Compliant Surcharge: 1.25% (<=90d), 2.00% (>90d)", JLabel.CENTER);
        noticeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        noticeLabel.setForeground(new Color(226, 232, 240));
        noticeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(2));
        headerPanel.add(noticeLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Payment details panel
        JPanel detailsPanel = new JPanel();
        detailsPanel.setOpaque(false);
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 10, 40));

        // Bill details card
        JPanel billCard = createInfoCard();
        billCard.setMaximumSize(new Dimension(540, 200));
        billCard.setLayout(new BoxLayout(billCard, BoxLayout.Y_AXIS));
        billCard.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        billCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel billTitle = new JLabel("Invoice Itemization & Surcharge Breakdown", JLabel.CENTER);
        billTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        billTitle.setForeground(new Color(15, 23, 42));
        billTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        billIdLabel = new JLabel("Bill Identifier: " + (this.billId > 0 ? "Bill #" + this.billId : "Historical / Unassigned"));
        billIdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        billIdLabel.setForeground(new Color(71, 85, 105));
        billIdLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        meterLabel = new JLabel("Account Meter: " + (this.meter.isEmpty() ? "N/A" : this.meter) + "  |  Billing Month: " + this.month);
        meterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        meterLabel.setForeground(new Color(71, 85, 105));
        meterLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        principalLabel = new JLabel("Original Bill Principal: ₹" + principalAmount.toString());
        principalLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        principalLabel.setForeground(new Color(15, 23, 42));
        principalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        dueDateLabel = new JLabel("Due Date: " + (dueDate != null ? dueDate.toString() : "Not recorded (Historical - No Surcharge)"));
        dueDateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dueDateLabel.setForeground(new Color(71, 85, 105));
        dueDateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String delayText = (dueDate != null && delayDays > 0)
                ? "Delay: " + delayDays + " days overdue (Tier 1: " + Math.min(delayDays, 90) + "d, Tier 2: " + Math.max(0, delayDays - 90) + "d)"
                : (dueDate != null ? "Payment Status: On-Time (0 days delay)" : "Historical Bill (No surcharge applicable)");
        delayLabel = new JLabel(delayText);
        delayLabel.setFont(new Font("Segoe UI", (delayDays > 0 ? Font.BOLD : Font.PLAIN), 12));
        delayLabel.setForeground(delayDays > 0 ? new Color(185, 28, 28) : new Color(22, 101, 52));
        delayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        surchargeLabel = new JLabel("Late Payment Surcharge: ₹" + lateFeeAmount.toString());
        surchargeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        surchargeLabel.setForeground(delayDays > 0 ? new Color(185, 28, 28) : new Color(71, 85, 105));
        surchargeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        amountLabel = new JLabel("TOTAL AMOUNT PAYABLE: ₹" + totalPayableAmount.toString());
        amountLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        amountLabel.setForeground(new Color(225, 29, 72)); // Rose red
        amountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        billCard.add(billTitle);
        billCard.add(Box.createVerticalStrut(4));
        billCard.add(billIdLabel);
        billCard.add(meterLabel);
        billCard.add(Box.createVerticalStrut(4));
        billCard.add(principalLabel);
        billCard.add(dueDateLabel);
        billCard.add(delayLabel);
        billCard.add(surchargeLabel);
        billCard.add(Box.createVerticalStrut(6));
        billCard.add(amountLabel);

        detailsPanel.add(billCard);
        detailsPanel.add(Box.createVerticalStrut(12));

        // Payment methods title
        JLabel paymentTitle = new JLabel("Choose Payment Channel to Settle Bill", JLabel.CENTER);
        paymentTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        paymentTitle.setForeground(Color.WHITE);
        paymentTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsPanel.add(paymentTitle);
        detailsPanel.add(Box.createVerticalStrut(8));

        // Payment methods panel
        JPanel paymentsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        paymentsPanel.setOpaque(false);
        paymentsPanel.setMaximumSize(new Dimension(540, 140));

        payUPI = createPaymentButton("📱", "UPI Payment", new Color(16, 185, 129));        // Emerald
        payCard = createPaymentButton("💳", "Credit/Debit Card", new Color(14, 165, 233));   // Sky
        payNetBanking = createPaymentButton("🏦", "Net Banking", new Color(245, 158, 11));   // Amber
        payWallet = createPaymentButton("👛", "Digital Wallet", new Color(168, 85, 247));   // Purple

        paymentsPanel.add(payUPI);
        paymentsPanel.add(payCard);
        paymentsPanel.add(payNetBanking);
        paymentsPanel.add(payWallet);

        detailsPanel.add(paymentsPanel);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);

        // Footer with back button
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(4, 40, 12, 40));

        backBtn = createStyledButton("Cancel / Back", new Color(100, 116, 139), Color.WHITE);
        footerPanel.add(backBtn);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private JPanel createInfoCard() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 255, 255, 248));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2d.setColor(new Color(203, 213, 225));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            }
        };
    }

    private JButton createPaymentButton(String icon, String text, Color accentColor) {
        JButton btn = new JButton("<html><center><font size='5'>" + icon + "</font><br><b>" + text + "</b></center></html>");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(new Color(15, 23, 42));
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(new Color(248, 250, 252));
                    btn.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(accentColor, 2),
                        new EmptyBorder(7, 11, 7, 11)
                    ));
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(Color.WHITE);
                    btn.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(226, 232, 240), 1),
                        new EmptyBorder(8, 12, 8, 12)
                    ));
                }
            }
        });

        btn.addActionListener(this);
        return btn;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bg.darker(), 1),
            new EmptyBorder(7, 20, 7, 20)
        ));
        btn.addActionListener(this);
        return btn;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == backBtn) {
            dispose();
            return;
        }

        if (isProcessing) return;

        String method = "Simulated Sandbox";
        if (ae.getSource() == payUPI) {
            method = "UPI Payment";
        } else if (ae.getSource() == payCard) {
            method = "Credit/Debit Card";
        } else if (ae.getSource() == payNetBanking) {
            method = "Net Banking";
        } else if (ae.getSource() == payWallet) {
            method = "Digital Wallet";
        }

        processPaymentWithWorker(this.billId, this.totalPayableAmount, method);
    }

    private void processPaymentWithWorker(long targetBillId, BigDecimal expectedTotal, String paymentMethod) {
        isProcessing = true;
        setButtonsEnabled(false);

        JDialog processingDialog = new JDialog(this, "Processing Payment...", true);
        processingDialog.setLayout(new BorderLayout());
        processingDialog.setSize(320, 110);
        processingDialog.setLocationRelativeTo(this);
        processingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel statusMsg = new JLabel("🔒 Authorizing payment securely...", JLabel.CENTER);
        statusMsg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(15, 20, 15, 20));
        p.add(statusMsg, BorderLayout.NORTH);
        p.add(pb, BorderLayout.CENTER);
        processingDialog.add(p);

        SwingWorker<PaymentResult, Void> worker = new SwingWorker<PaymentResult, Void>() {
            @Override
            protected PaymentResult doInBackground() throws Exception {
                Thread.sleep(300); // Visual responsiveness feedback
                if (targetBillId > 0) {
                    return executePayment(targetBillId, expectedTotal, paymentMethod);
                } else {
                    return executePayment(meter, month, expectedTotal != null ? expectedTotal.toString() : amount, paymentMethod);
                }
            }

            @Override
            protected void done() {
                processingDialog.dispose();
                isProcessing = false;
                setButtonsEnabled(true);

                PaymentResult res;
                try {
                    res = get();
                } catch (Exception e) {
                    res = new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Worker error: " + e.getMessage(),
                            null, meter, month, amount, "0", paymentMethod, new Date(), targetBillId, BigDecimal.ZERO, null);
                }

                handlePaymentResult(res);
            }
        };

        worker.execute();
        processingDialog.setVisible(true);
    }

    private void handlePaymentResult(PaymentResult res) {
        if (res == null) {
            JOptionPane.showMessageDialog(this, "Unknown payment error occurred.", "Payment Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Handle Price Drift: non-blocking re-confirmation protocol
        if (res.status == PaymentResult.Status.PRICE_DRIFT) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Authoritative payment amount has been recalculated due to date/time transition:\n\n" +
                "  • Original Principal      : ₹" + res.billedAmount + "\n" +
                "  • Late Payment Surcharge  : ₹" + res.lateFee + "\n" +
                "  • Total Authoritative Due : ₹" + res.paidAmount + "\n\n" +
                "Do you accept the updated amount and wish to proceed with settlement?",
                "Surcharge Amount Re-Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                BigDecimal confirmedAmount = parseBigDecimal(res.paidAmount);
                processPaymentWithWorker(res.billId, confirmedAmount, res.paymentMethod);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Payment cancelled. The bill remains unpaid with no charges settled.",
                    "Payment Cancelled",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }

        if (res.status == PaymentResult.Status.SUCCESS) {
            // Show professional printable tax receipt with bill_id
            PaymentReceiptDialog receiptDialog = new PaymentReceiptDialog(
                this,
                res.billId,
                res.customerName,
                res.meterNo,
                res.month,
                res.billedAmount,
                res.lateFee.toString(),
                res.paidAmount,
                (res.dueDate != null ? res.dueDate.toString() : "Not recorded (Historical)"),
                new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US).format(res.timestamp),
                res.paymentMethod,
                "PAID"
            );
            receiptDialog.setVisible(true);
            dispose();
        } else if (res.status == PaymentResult.Status.ALREADY_PAID) {
            JOptionPane.showMessageDialog(
                this,
                "Payment Blocked:\n" + res.message + "\n\nDuplicate payment is strictly prevented.",
                "Already Paid",
                JOptionPane.WARNING_MESSAGE
            );
            dispose();
        } else if (res.status == PaymentResult.Status.PROTECTED_RECORD) {
            JOptionPane.showMessageDialog(
                this,
                "Security Protection:\n" + res.message,
                "Record Protected",
                JOptionPane.WARNING_MESSAGE
            );
            dispose();
        } else if (res.status == PaymentResult.Status.BILL_NOT_FOUND) {
            JOptionPane.showMessageDialog(
                this,
                "Bill Not Found:\n" + res.message,
                "Invalid Selection",
                JOptionPane.ERROR_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Payment Transaction Error:\n" + res.message,
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        payUPI.setEnabled(enabled);
        payCard.setEnabled(enabled);
        payNetBanking.setEnabled(enabled);
        payWallet.setEnabled(enabled);
        backBtn.setEnabled(enabled);
    }

    /**
     * Primary Phase 9 transactional settlement method using primary key billId.
     * Enforces two-phase non-blocking concurrency:
     * 1. Lock bill row with FOR UPDATE.
     * 2. Recalculate LPSC using DB server time.
     * 3. Check for price drift; rollback before customer prompt if drifted.
     * 4. Settle atomically and write audit event in same transaction.
     */
    public static PaymentResult executePayment(Connection conn, long billId, BigDecimal expectedAmount, String paymentMethod) {
        if (conn == null) {
            return new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Database connection is null.",
                    null, "N/A", "N/A", "0.00", "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, null);
        }

        if (billId <= 0) {
            return new PaymentResult(PaymentResult.Status.BILL_NOT_FOUND, "Invalid Bill ID: " + billId,
                    null, "N/A", "N/A", "0.00", "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, null);
        }

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // 1. Lock bill row using FOR UPDATE
            String query = "SELECT b.bill_id, b.meter_no, b.month, b.units, b.total_bill, b.status, " +
                           "       b.due_date, b.original_due_date, b.late_fee, b.paid_at, b.paid_amount, c.name " +
                           "FROM bill b LEFT JOIN customer c ON b.meter_no = c.meter_no " +
                           "WHERE b.bill_id = ? FOR UPDATE";

            String cleanMeter = "N/A";
            String cleanMonth = "N/A";
            String custName = "Customer";
            String totalBillStr = "0.00";
            String currentStatus = null;
            java.sql.Date dueDate = null;

            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setLong(1, billId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cleanMeter = rs.getString("meter_no");
                        cleanMonth = rs.getString("month");
                        totalBillStr = rs.getString("total_bill");
                        currentStatus = rs.getString("status");
                        dueDate = rs.getDate("due_date");
                        String name = rs.getString("name");
                        if (name != null && !name.trim().isEmpty()) {
                            custName = name.trim();
                        }
                    } else {
                        conn.rollback();
                        String actorId = UserSession.getActorId();
                        String actorRole = UserSession.getActorRole();
                        AuditService.logIndependent(actorId, actorRole, AuditService.ACTION_PAYMENT_FAILED,
                            AuditService.ENTITY_BILL, String.valueOf(billId),
                            "Payment failed: Bill #" + billId + " not found", null, null);
                        return new PaymentResult(PaymentResult.Status.BILL_NOT_FOUND,
                            "No bill record found for Bill ID #" + billId,
                            custName, cleanMeter, cleanMonth, totalBillStr, "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, null);
                    }
                }
            }

            // 2. Check protections & status
            if ("Commercial".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                return new PaymentResult(PaymentResult.Status.PROTECTED_RECORD,
                    "Historical Commercial records are protected from payment modification.",
                    custName, cleanMeter, cleanMonth, totalBillStr, "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, dueDate);
            }

            if ("Paid".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                String actorId = UserSession.getActorId();
                String actorRole = UserSession.getActorRole();
                AuditService.logIndependent(actorId, actorRole, AuditService.ACTION_PAYMENT_DUPLICATE_ATTEMPT,
                    AuditService.ENTITY_BILL, String.valueOf(billId),
                    "Duplicate payment attempt for already Paid bill #" + billId + " (" + cleanMonth + ")", "Paid", "Paid");
                AuditService.logIndependent(actorId, actorRole, AuditService.ACTION_PAYMENT_DUPLICATE_ATTEMPT,
                    AuditService.ENTITY_PAYMENT, cleanMeter + ":" + cleanMonth,
                    "Duplicate payment attempt for already Paid bill (" + cleanMonth + ")", "Paid", "Paid");
                return new PaymentResult(PaymentResult.Status.ALREADY_PAID,
                    "Bill #" + billId + " for Meter " + cleanMeter + " (" + cleanMonth + ") is already Paid.",
                    custName, cleanMeter, cleanMonth, totalBillStr, "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, dueDate);
            }

            if (!"Not Paid".equalsIgnoreCase(currentStatus)) {
                conn.rollback();
                return new PaymentResult(PaymentResult.Status.ALREADY_PAID,
                    "Bill is not in an unpaid state (Current status: " + currentStatus + ").",
                    custName, cleanMeter, cleanMonth, totalBillStr, "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, dueDate);
            }

            // 3. Recalculate surcharge using authoritative DB server time (or simulated test date)
            java.sql.Date dbToday = LatePaymentService.getEffectiveDate(conn);
            BigDecimal principal = parseBigDecimal(totalBillStr);
            LatePaymentService.LpscResult lpsc = LatePaymentService.calculateLpsc(principal, dueDate, dbToday);

            // 4. Compare authoritative total payable with expected preview amount
            if (expectedAmount != null && expectedAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal expectedNormalized = expectedAmount.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
                if (lpsc.totalPayable.compareTo(expectedNormalized) != 0) {
                    // Non-blocking requirement: rollback immediately and release row lock before prompting
                    conn.rollback();
                    return new PaymentResult(PaymentResult.Status.PRICE_DRIFT,
                        "Authoritative amount updated: Principal ₹" + principal + " + Late Fee ₹" + lpsc.lateFee + " = Total ₹" + lpsc.totalPayable,
                        custName, cleanMeter, cleanMonth, totalBillStr, lpsc.totalPayable.toString(), paymentMethod, dbToday,
                        billId, lpsc.lateFee, dueDate);
                }
            }

            // 5. Perform atomic status update
            String updateQuery = "UPDATE bill SET status = 'Paid', late_fee = ?, paid_at = NOW(), paid_amount = ? WHERE bill_id = ? AND status = 'Not Paid'";
            int rowsUpdated = 0;
            try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                ps.setBigDecimal(1, lpsc.lateFee);
                ps.setBigDecimal(2, lpsc.totalPayable);
                ps.setLong(3, billId);
                rowsUpdated = ps.executeUpdate();
            }

            if (rowsUpdated == 1) {
                String actorId = UserSession.getActorId();
                String actorRole = UserSession.getActorRole();

                // Log audit event for bill
                AuditService.logEvent(conn, actorId, actorRole, AuditService.ACTION_PAYMENT_COMPLETED,
                    AuditService.ENTITY_BILL, String.valueOf(billId),
                    "Payment of ₹" + lpsc.totalPayable + " completed via " + paymentMethod + " (Principal: ₹" + principal + ", Late Fee: ₹" + lpsc.lateFee + ")",
                    "Not Paid", "Paid");

                // Also log for payment entity to maintain Phase 8 verification compatibility
                AuditService.logEvent(conn, actorId, actorRole, AuditService.ACTION_PAYMENT_COMPLETED,
                    AuditService.ENTITY_PAYMENT, cleanMeter + ":" + cleanMonth,
                    "Payment of ₹" + lpsc.totalPayable + " completed via " + paymentMethod,
                    "Not Paid", "Paid");

                conn.commit();

                // Automatic in-app notification dispatch (Phase 3.6)
                try {
                    String notifTitle = "Payment Confirmation - Bill #" + billId + " Settled";
                    String notifBody = "Dear Customer, your payment of ₹" + lpsc.totalPayable + " for bill #" + billId + " (" + cleanMonth + ") has been processed successfully via " + paymentMethod + ".\n"
                            + "Transaction Status: Settled\n"
                            + "Late Payment Surcharge: ₹" + lpsc.lateFee + "\n"
                            + "Thank you for your payment!";
                    NotificationService.ServiceResult notifResult = NotificationService.sendSystemNotification(
                            cleanMeter, notifTitle, notifBody, NotificationService.PRIORITY_IMPORTANT
                    );
                    if (!notifResult.isSuccess()) {
                        System.err.println("[NOTIFICATION WARNING] Automatic payment notification failed for meter "
                                + cleanMeter + ": " + notifResult.getMessage());
                    }
                } catch (Throwable notifEx) {
                    System.err.println("[NOTIFICATION ERROR] Exception dispatching automatic payment notification: " + notifEx.getMessage());
                }

                return new PaymentResult(PaymentResult.Status.SUCCESS,
                    "Payment processed and settled successfully.",
                    custName, cleanMeter, cleanMonth, totalBillStr, lpsc.totalPayable.toString(), paymentMethod, dbToday,
                    billId, lpsc.lateFee, dueDate);
            } else {
                conn.rollback();
                return new PaymentResult(PaymentResult.Status.ALREADY_PAID,
                    "Concurrent transaction detected or bill already settled.",
                    custName, cleanMeter, cleanMonth, totalBillStr, "0", paymentMethod, new Date(),
                    billId, BigDecimal.ZERO, dueDate);
            }

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {}
            String actorId = UserSession.getActorId();
            String actorRole = UserSession.getActorRole();
            AuditService.logIndependent(actorId, actorRole, AuditService.ACTION_PAYMENT_FAILED,
                AuditService.ENTITY_BILL, String.valueOf(billId),
                "Payment failed due to database transaction error: " + e.getMessage(), "Not Paid", "Not Paid");
            return new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Database transaction error: " + e.getMessage(),
                null, "N/A", "N/A", "0.00", "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, null);
        } finally {
            try {
                conn.setAutoCommit(originalAutoCommit);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Backward-compatible executePayment overload resolving billId by meter and month.
     */
    public static PaymentResult executePayment(Connection conn, String meter, String month, String expectedAmount, String paymentMethod) {
        if (conn == null) {
            return new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Database connection is null.", null, meter, month, expectedAmount, "0", paymentMethod, new Date());
        }

        if (meter == null || meter.trim().isEmpty() || month == null || month.trim().isEmpty()) {
            return new PaymentResult(PaymentResult.Status.BILL_NOT_FOUND, "Meter number or month is invalid.", null, meter, month, expectedAmount, "0", paymentMethod, new Date());
        }

        String cleanMeter = meter.trim();
        String cleanMonth = month.trim();

        // Resolve bill_id
        long billId = -1;
        String sql = "SELECT bill_id, status FROM bill WHERE meter_no = ? AND LOWER(month) = LOWER(?) ORDER BY bill_id ASC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cleanMeter);
            ps.setString(2, cleanMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    billId = rs.getLong("bill_id");
                    String st = rs.getString("status");
                    if ("Commercial".equalsIgnoreCase(st)) {
                        return new PaymentResult(PaymentResult.Status.PROTECTED_RECORD, "Historical Commercial records are protected from payment modification.", null, cleanMeter, cleanMonth, expectedAmount, "0", paymentMethod, new Date());
                    }
                } else {
                    String actorId = UserSession.getActorId();
                    String actorRole = UserSession.getActorRole();
                    AuditService.logIndependent(actorId, actorRole, AuditService.ACTION_PAYMENT_FAILED,
                        AuditService.ENTITY_PAYMENT, cleanMeter + ":" + cleanMonth,
                        "Payment failed: Bill not found for meter " + cleanMeter + " (" + cleanMonth + ")", null, null);
                    return new PaymentResult(PaymentResult.Status.BILL_NOT_FOUND, "No bill record found for Meter: " + cleanMeter + ", Month: " + cleanMonth, null, cleanMeter, cleanMonth, expectedAmount, "0", paymentMethod, new Date());
                }
            }
        } catch (SQLException e) {
            return new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Database lookup error: " + e.getMessage(), null, cleanMeter, cleanMonth, expectedAmount, "0", paymentMethod, new Date());
        }

        BigDecimal expAmt = parseBigDecimal(expectedAmount);
        return executePayment(conn, billId, expAmt, paymentMethod);
    }

    /**
     * Standalone executePayment helper opening its own connection.
     */
    public static PaymentResult executePayment(String meter, String month, String expectedAmount, String paymentMethod) {
        try (Conn c = new Conn()) {
            if (c.c == null) {
                return new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Database connection unavailable.", null, meter, month, expectedAmount, "0", paymentMethod, new Date());
            }
            return executePayment(c.c, meter, month, expectedAmount, paymentMethod);
        } catch (Exception e) {
            return new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Exception during payment: " + e.getMessage(), null, meter, month, expectedAmount, "0", paymentMethod, new Date());
        }
    }

    public static PaymentResult executePayment(long billId, BigDecimal expectedAmount, String paymentMethod) {
        try (Conn c = new Conn()) {
            if (c.c == null) {
                return new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Database connection unavailable.", null, null, null, "0", "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, null);
            }
            return executePayment(c.c, billId, expectedAmount, paymentMethod);
        } catch (Exception e) {
            return new PaymentResult(PaymentResult.Status.DATABASE_ERROR, "Exception during payment: " + e.getMessage(), null, null, null, "0", "0", paymentMethod, new Date(), billId, BigDecimal.ZERO, null);
        }
    }

    private static BigDecimal parseBigDecimal(String str) {
        if (str == null || str.trim().isEmpty()) {
            return BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
        }
        try {
            String clean = str.replace("₹", "").replace("$", "").replace(",", "").trim();
            return new BigDecimal(clean).setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(LpscConfig.CURRENCY_SCALE, LpscConfig.ROUNDING_MODE);
        }
    }

    public static void main(String[] args) {
        new PaymentGateway("", "", "0");
    }
}
