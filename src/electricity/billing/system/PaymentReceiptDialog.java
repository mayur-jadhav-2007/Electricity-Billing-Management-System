package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentReceiptDialog extends JDialog implements ActionListener {

    JTextArea area;
    JButton print, close;
    long billId = -1;
    String meter, month, customerName, paymentMethod, paymentStatus;
    String originalAmount;
    String lateFee;
    String totalPaid;
    String dueDateStr;
    String paymentDateStr;

    /**
     * Primary constructor: opens payment tax receipt strictly by bill_id.
     * Loads authoritative billing details from database.
     */
    public PaymentReceiptDialog(Window parent, long billId) {
        super(parent, "Official Payment Receipt - Electricity Billing System", ModalityType.APPLICATION_MODAL);
        this.billId = billId;
        loadBillDetails(billId);
        initComponents(parent);
    }

    /**
     * Legacy constructor for backwards compatibility with earlier phase callers and test suites.
     */
    public PaymentReceiptDialog(Frame parent, String customerName, String meter, String month,
                                String amount, String paymentMethod, Date paymentDate) {
        super(parent, "Official Payment Receipt - Electricity Billing System", true);
        this.customerName = (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : "Valued Customer";
        this.meter = (meter != null) ? meter.trim() : "N/A";
        this.month = (month != null) ? month.trim() : "N/A";
        this.originalAmount = (amount != null) ? amount.replace("₹", "").trim() : "0.00";
        this.lateFee = "0.00";
        this.totalPaid = this.originalAmount;
        this.dueDateStr = "Not recorded (Historical)";
        this.paymentMethod = (paymentMethod != null) ? paymentMethod.trim() : "Simulated Sandbox";
        this.paymentStatus = "PAID";
        if (paymentDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
            this.paymentDateStr = sdf.format(paymentDate);
        } else {
            this.paymentDateStr = "Not recorded";
        }

        // Attempt to enrich with bill_id from database if available
        enrichFromDatabase();
        initComponents(parent);
    }

    /**
     * Full parameter constructor for explicit receipts.
     */
    public PaymentReceiptDialog(Window parent, long billId, String customerName, String meter, String month,
                                String originalAmount, String lateFee, String totalPaid,
                                String dueDateStr, String paymentDateStr, String paymentMethod, String paymentStatus) {
        super(parent, "Official Payment Receipt - Electricity Billing System", ModalityType.APPLICATION_MODAL);
        this.billId = billId;
        this.customerName = (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : "Valued Customer";
        this.meter = (meter != null) ? meter.trim() : "N/A";
        this.month = (month != null) ? month.trim() : "N/A";
        this.originalAmount = (originalAmount != null) ? originalAmount.replace("₹", "").trim() : "0.00";
        this.lateFee = (lateFee != null) ? lateFee.replace("₹", "").trim() : "0.00";
        this.totalPaid = (totalPaid != null) ? totalPaid.replace("₹", "").trim() : this.originalAmount;
        this.dueDateStr = (dueDateStr != null) ? dueDateStr : "Not recorded (Historical)";
        this.paymentDateStr = (paymentDateStr != null) ? paymentDateStr : "Not recorded";
        this.paymentMethod = (paymentMethod != null) ? paymentMethod.trim() : "Simulated Sandbox";
        this.paymentStatus = (paymentStatus != null) ? paymentStatus.toUpperCase() : "PAID";
        initComponents(parent);
    }

    private void loadBillDetails(long id) {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                String sql = "SELECT b.bill_id, b.meter_no, b.month, b.units, b.total_bill, b.status, " +
                             "       b.due_date, b.original_due_date, b.paid_at, b.late_fee, b.paid_amount, c.name " +
                             "FROM bill b LEFT JOIN customer c ON b.meter_no = c.meter_no WHERE b.bill_id = ?";
                try (PreparedStatement ps = c.c.prepareStatement(sql)) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            this.meter = rs.getString("meter_no");
                            this.month = rs.getString("month");
                            String name = rs.getString("name");
                            this.customerName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Valued Customer";
                            this.originalAmount = rs.getString("total_bill");
                            this.paymentStatus = rs.getString("status");

                            java.sql.Date due = rs.getDate("due_date");
                            this.dueDateStr = (due != null) ? due.toString() : "Not recorded (Historical)";

                            java.sql.Timestamp paidAt = rs.getTimestamp("paid_at");
                            if (paidAt != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
                                this.paymentDateStr = sdf.format(paidAt);
                            } else {
                                // Rule: Legacy historical Paid bills display "Not recorded"
                                this.paymentDateStr = "Not recorded";
                            }

                            BigDecimal lf = rs.getBigDecimal("late_fee");
                            this.lateFee = (lf != null) ? lf.setScale(2, LpscConfig.ROUNDING_MODE).toString() : "0.00";

                            BigDecimal pa = rs.getBigDecimal("paid_amount");
                            if (pa != null) {
                                this.totalPaid = pa.setScale(2, LpscConfig.ROUNDING_MODE).toString();
                            } else {
                                // Rule: Legacy historical Paid bills display total_bill as settled amount
                                this.totalPaid = (this.originalAmount != null) ? this.originalAmount.replace("₹", "").trim() : "0.00";
                            }
                            this.paymentMethod = "Simulated Settlement";
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallbacks if not found
        this.customerName = "Valued Customer";
        this.meter = "N/A";
        this.month = "N/A";
        this.originalAmount = "0.00";
        this.lateFee = "0.00";
        this.totalPaid = "0.00";
        this.dueDateStr = "Not recorded";
        this.paymentDateStr = "Not recorded";
        this.paymentMethod = "Simulated Sandbox";
        this.paymentStatus = "PAID";
    }

    private void enrichFromDatabase() {
        if (this.billId > 0) return;
        try (Conn c = new Conn()) {
            if (c.c != null) {
                String sql = "SELECT bill_id, due_date, paid_at, late_fee, paid_amount FROM bill WHERE meter_no = ? AND LOWER(month) = LOWER(?) ORDER BY bill_id ASC LIMIT 1";
                try (PreparedStatement ps = c.c.prepareStatement(sql)) {
                    ps.setString(1, this.meter);
                    ps.setString(2, this.month);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            this.billId = rs.getLong("bill_id");
                            java.sql.Date due = rs.getDate("due_date");
                            if (due != null) {
                                this.dueDateStr = due.toString();
                            }
                            java.sql.Timestamp paidAt = rs.getTimestamp("paid_at");
                            if (paidAt != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
                                this.paymentDateStr = sdf.format(paidAt);
                            }
                            BigDecimal lf = rs.getBigDecimal("late_fee");
                            if (lf != null) {
                                this.lateFee = lf.setScale(2, LpscConfig.ROUNDING_MODE).toString();
                            }
                            BigDecimal pa = rs.getBigDecimal("paid_amount");
                            if (pa != null) {
                                this.totalPaid = pa.setScale(2, LpscConfig.ROUNDING_MODE).toString();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void initComponents(Window parent) {
        setSize(580, 700);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        headerPanel.setBackground(new Color(248, 250, 252));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JLabel titleLabel = new JLabel("Official Payment Tax Receipt");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(new Color(15, 23, 42));
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Center Receipt Text Area
        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(new Color(255, 255, 255));
        area.setForeground(new Color(15, 23, 42));
        area.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(15, 20, 15, 20),
            new LineBorder(new Color(226, 232, 240), 1)
        ));

        renderReceiptText();

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(new EmptyBorder(10, 15, 10, 15));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        bottomPanel.setBackground(new Color(248, 250, 252));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        print = new JButton("Print Receipt");
        print.setBackground(new Color(22, 163, 74)); // Forest green
        print.setForeground(Color.WHITE);
        print.setFont(new Font("Segoe UI", Font.BOLD, 12));
        print.setFocusPainted(false);
        print.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon printIcon = Project.getScaledIcon("icon/icons8-receipt-48.png", 16, Color.WHITE);
        if (printIcon != null) {
            print.setIcon(printIcon);
            print.setIconTextGap(6);
        }
        print.addActionListener(this);

        close = new JButton("Done");
        close.setBackground(new Color(71, 85, 105)); // Slate 600
        close.setForeground(Color.WHITE);
        close.setFont(new Font("Segoe UI", Font.BOLD, 12));
        close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(this);

        bottomPanel.add(print);
        bottomPanel.add(close);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void renderReceiptText() {
        String cleanOrig = (originalAmount != null) ? originalAmount.replace("₹", "").trim() : "0.00";
        String cleanFee = (lateFee != null) ? lateFee.replace("₹", "").trim() : "0.00";
        String cleanPaid = (totalPaid != null) ? totalPaid.replace("₹", "").trim() : cleanOrig;

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================\n");
        sb.append("                ELECTRICITY BILLING SYSTEM (EBS)                \n");
        sb.append("                  OFFICIAL PAYMENT TAX RECEIPT                  \n");
        sb.append("================================================================\n\n");
        sb.append(String.format(" Receipt Date       : %s\n", paymentDateStr));
        sb.append(String.format(" Payment Status     : %s\n", (paymentStatus != null ? paymentStatus.toUpperCase() : "PAID")));
        sb.append(String.format(" Payment Channel    : %s\n", paymentMethod));
        sb.append(" Transaction Ref    : None (Not stored in schema - Simulated)\n");
        sb.append(String.format(" Bill Identifier    : %s\n", (billId > 0 ? "Bill #" + billId : "Historical / Not assigned")));
        sb.append("----------------------------------------------------------------\n");
        sb.append(" CUSTOMER & ACCOUNT DETAILS\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format(" Customer Name      : %s\n", customerName));
        sb.append(String.format(" Meter Number       : %s\n", meter));
        sb.append(String.format(" Billing Month      : %s\n", month));
        sb.append(String.format(" Due Date           : %s\n", dueDateStr));
        sb.append(String.format(" Payment Date       : %s\n", paymentDateStr));
        sb.append("----------------------------------------------------------------\n");
        sb.append(" ITEMIZED SETTLEMENT BREAKDOWN\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format(" Original Bill Amt  : ₹%s\n", cleanOrig));
        sb.append(String.format(" Late Fee Surcharge : ₹%s\n", cleanFee));
        sb.append("----------------------------------------------------------------\n");
        sb.append(String.format(" Total Amount Paid  : ₹%s\n", cleanPaid));
        sb.append(" Outstanding Due    : ₹0.00\n");
        sb.append("----------------------------------------------------------------\n");
        sb.append(" NOTICE & REGULATORY COMPLIANCE\n");
        sb.append(" • Late Payment Surcharge (LPSC) computed in accordance with\n");
        sb.append("   UPERC Rate Schedule provisions (1.25% <=90d, 2.00% >90d).\n");
        sb.append(" • Historical bills settled prior to Phase 9 reflect settled principal.\n");
        sb.append(" • Simulated Sandbox Notice: Database status updated to 'Paid'.\n");
        sb.append("================================================================\n");
        sb.append("               THANK YOU FOR YOUR TIMELY PAYMENT!               \n");
        sb.append("================================================================\n");

        area.setText(sb.toString());
        area.setCaretPosition(0);
    }

    public String getReceiptText() {
        return (area != null) ? area.getText() : "";
    }

    public long getBillId() {
        return billId;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == print) {
            try {
                MessageFormat header = new MessageFormat("Electricity Billing System - Payment Receipt");
                MessageFormat footer = new MessageFormat("Page {0,number,integer}");
                boolean complete = area.print(header, footer);
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Receipt sent to printer successfully.", "Print Status", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Print Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            dispose();
        }
    }
}
