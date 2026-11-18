package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.awt.event.*;
import java.util.Date;
import java.util.List;

public class PayBill extends JFrame implements ActionListener {

    Choice cmonth, meterChoice;
    JButton pay, viewReceipt, back;
    String meter;
    JLabel labelname, labelunits, labeltotalbill, labelstatus, meternumberLabel;
    JLabel labelbillid, labelduedate, labelsurcharge, labeltotalpayable;

    long currentBillId = -1;
    String currentBillAmount = "0";
    java.sql.Date currentDueDate = null;
    BigDecimal currentLateFee = BigDecimal.ZERO;
    BigDecimal currentTotalPayable = BigDecimal.ZERO;

    public PayBill(String meter) {
        this.meter = (meter != null) ? meter.trim() : "";
        setTitle("Electricity Billing System - Pay Bill " + (this.meter.isEmpty() ? "(All Meters)" : "(Meter: " + this.meter + ")"));
        setSize(880, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 14));
        headerPanel.setBackground(new Color(248, 250, 252));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JLabel heading = new JLabel("Electricity Bill Payment Portal");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(new Color(15, 23, 42));
        headerPanel.add(heading);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content: Left Form + Right Illustration
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(null);
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(500, 520));

        // Meter Number
        JLabel lblmeternumber = new JLabel("Meter Number");
        lblmeternumber.setBounds(35, 15, 140, 25);
        lblmeternumber.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblmeternumber.setForeground(new Color(51, 65, 85));
        formPanel.add(lblmeternumber);

        if (this.meter.isEmpty()) {
            meterChoice = new Choice();
            meterChoice.setBounds(190, 15, 230, 25);
            meterChoice.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            loadMeterNumbers();
            formPanel.add(meterChoice);
            if (meterChoice.getItemCount() > 0) {
                this.meter = meterChoice.getSelectedItem();
            }
            meterChoice.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    PayBill.this.meter = meterChoice.getSelectedItem();
                    loadCustomerName();
                    loadBillDetails(cmonth.getSelectedItem());
                }
            });
        } else {
            meternumberLabel = new JLabel(this.meter);
            meternumberLabel.setBounds(190, 15, 230, 25);
            meternumberLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            meternumberLabel.setForeground(new Color(37, 99, 235)); // Royal Blue
            formPanel.add(meternumberLabel);
        }

        // Customer Name
        JLabel lblname = new JLabel("Customer Name");
        lblname.setBounds(35, 55, 140, 25);
        lblname.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblname.setForeground(new Color(51, 65, 85));
        formPanel.add(lblname);

        labelname = new JLabel("-");
        labelname.setBounds(190, 55, 250, 25);
        labelname.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelname.setForeground(new Color(15, 23, 42));
        formPanel.add(labelname);

        // Billing Month
        JLabel lblmonth = new JLabel("Billing Month");
        lblmonth.setBounds(35, 95, 140, 25);
        lblmonth.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblmonth.setForeground(new Color(51, 65, 85));
        formPanel.add(lblmonth);

        cmonth = new Choice();
        cmonth.setBounds(190, 95, 230, 25);
        cmonth.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        String[] monthsList = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        for (String m : monthsList) {
            cmonth.add(m);
        }
        formPanel.add(cmonth);

        // Bill ID Identifier
        JLabel lblbillid = new JLabel("Bill Identifier");
        lblbillid.setBounds(35, 135, 140, 25);
        lblbillid.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblbillid.setForeground(new Color(51, 65, 85));
        formPanel.add(lblbillid);

        labelbillid = new JLabel("-");
        labelbillid.setBounds(190, 135, 250, 25);
        labelbillid.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelbillid.setForeground(new Color(30, 58, 138));
        formPanel.add(labelbillid);

        // Units Consumed
        JLabel lblunits = new JLabel("Units Consumed");
        lblunits.setBounds(35, 175, 140, 25);
        lblunits.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblunits.setForeground(new Color(51, 65, 85));
        formPanel.add(lblunits);

        labelunits = new JLabel("-");
        labelunits.setBounds(190, 175, 230, 25);
        labelunits.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelunits.setForeground(new Color(15, 23, 42));
        formPanel.add(labelunits);

        // Bill Principal
        JLabel lbltotalbill = new JLabel("Original Principal");
        lbltotalbill.setBounds(35, 215, 140, 25);
        lbltotalbill.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbltotalbill.setForeground(new Color(51, 65, 85));
        formPanel.add(lbltotalbill);

        labeltotalbill = new JLabel("₹0.00");
        labeltotalbill.setBounds(190, 215, 250, 25);
        labeltotalbill.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labeltotalbill.setForeground(new Color(15, 23, 42));
        formPanel.add(labeltotalbill);

        // Due Date
        JLabel lblduedate = new JLabel("Payment Due Date");
        lblduedate.setBounds(35, 255, 140, 25);
        lblduedate.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblduedate.setForeground(new Color(51, 65, 85));
        formPanel.add(lblduedate);

        labelduedate = new JLabel("-");
        labelduedate.setBounds(190, 255, 250, 25);
        labelduedate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelduedate.setForeground(new Color(71, 85, 105));
        formPanel.add(labelduedate);

        // Late Payment Surcharge
        JLabel lblsurcharge = new JLabel("Late Fee (LPSC)");
        lblsurcharge.setBounds(35, 295, 140, 25);
        lblsurcharge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblsurcharge.setForeground(new Color(51, 65, 85));
        formPanel.add(lblsurcharge);

        labelsurcharge = new JLabel("₹0.00");
        labelsurcharge.setBounds(190, 295, 250, 25);
        labelsurcharge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelsurcharge.setForeground(new Color(185, 28, 28));
        formPanel.add(labelsurcharge);

        // Total Amount Payable
        JLabel lbltotalpayable = new JLabel("Total Amount Due");
        lbltotalpayable.setBounds(35, 335, 140, 25);
        lbltotalpayable.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbltotalpayable.setForeground(new Color(51, 65, 85));
        formPanel.add(lbltotalpayable);

        labeltotalpayable = new JLabel("₹0.00");
        labeltotalpayable.setBounds(190, 335, 250, 25);
        labeltotalpayable.setFont(new Font("Segoe UI", Font.BOLD, 16));
        labeltotalpayable.setForeground(new Color(225, 29, 72));
        formPanel.add(labeltotalpayable);

        // Payment Status
        JLabel lblstatus = new JLabel("Payment Status");
        lblstatus.setBounds(35, 375, 140, 25);
        lblstatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblstatus.setForeground(new Color(51, 65, 85));
        formPanel.add(lblstatus);

        labelstatus = new JLabel("Checking...");
        labelstatus.setBounds(190, 375, 230, 25);
        labelstatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelstatus.setForeground(Color.GRAY);
        formPanel.add(labelstatus);

        // Action Buttons
        pay = new JButton("Proceed to Pay");
        pay.setBackground(new Color(22, 163, 74)); // Forest green
        pay.setForeground(Color.WHITE);
        pay.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pay.setBounds(35, 430, 135, 36);
        pay.setFocusPainted(false);
        pay.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon payIcon = Project.getScaledIcon("icon/Pay Bill.png", 16, Color.WHITE);
        if (payIcon != null) {
            pay.setIcon(payIcon);
            pay.setIconTextGap(6);
        }
        pay.setEnabled(false);
        pay.addActionListener(this);
        formPanel.add(pay);

        viewReceipt = new JButton("View Receipt");
        viewReceipt.setBackground(new Color(37, 99, 235)); // Royal blue
        viewReceipt.setForeground(Color.WHITE);
        viewReceipt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        viewReceipt.setBounds(180, 430, 125, 36);
        viewReceipt.setFocusPainted(false);
        viewReceipt.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon receiptIcon = Project.getScaledIcon("icon/icons8-receipt-48.png", 16, Color.WHITE);
        if (receiptIcon != null) {
            viewReceipt.setIcon(receiptIcon);
            viewReceipt.setIconTextGap(6);
        }
        viewReceipt.setEnabled(false);
        viewReceipt.addActionListener(this);
        formPanel.add(viewReceipt);

        back = new JButton("Close");
        back.setBackground(new Color(71, 85, 105)); // Slate 600
        back.setForeground(Color.WHITE);
        back.setFont(new Font("Segoe UI", Font.BOLD, 12));
        back.setBounds(315, 430, 90, 36);
        back.setFocusPainted(false);
        back.setCursor(new Cursor(Cursor.HAND_CURSOR));
        back.addActionListener(this);
        formPanel.add(back);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Right Illustration
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.setBackground(Color.WHITE);
        eastPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 30));
        try {
            ImageIcon i1 = new ImageIcon(ClassLoader.getSystemResource("icon/bill.png"));
            Image i2 = i1.getImage().getScaledInstance(330, 215, Image.SCALE_SMOOTH);
            JLabel image = new JLabel(new ImageIcon(i2));
            eastPanel.add(image, BorderLayout.CENTER);
        } catch (Exception ignored) {}
        mainPanel.add(eastPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        loadCustomerName();
        loadBillDetails(cmonth.getSelectedItem());

        cmonth.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent ae) {
                loadBillDetails(cmonth.getSelectedItem());
            }
        });

        setVisible(true);
    }

    private void loadMeterNumbers() {
        if (meterChoice == null) return;
        meterChoice.removeAll();
        try (Conn c = new Conn()) {
            if (c.c != null) {
                String q = "SELECT meter_no FROM customer ORDER BY meter_no";
                try (Statement stmt = c.c.createStatement();
                     ResultSet rs = stmt.executeQuery(q)) {
                    while (rs.next()) {
                        meterChoice.add(rs.getString("meter_no"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCustomerName() {
        String effectiveMeter = getEffectiveMeter();
        labelname.setText("-");
        if (effectiveMeter.isEmpty()) return;

        try (Conn c = new Conn()) {
            if (c.c != null) {
                String custQuery = "SELECT name FROM customer WHERE meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(custQuery)) {
                    ps.setString(1, effectiveMeter);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            labelname.setText(rs.getString("name"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getEffectiveMeter() {
        if (meterChoice != null && meterChoice.getSelectedItem() != null) {
            return meterChoice.getSelectedItem().trim();
        }
        return (this.meter != null) ? this.meter.trim() : "";
    }

    public void loadBillDetails(String month) {
        labelbillid.setText("-");
        labelunits.setText("-");
        labeltotalbill.setText("₹0.00");
        labelduedate.setText("-");
        labelsurcharge.setText("₹0.00");
        labeltotalpayable.setText("₹0.00");
        labelstatus.setText("Checking...");
        labelstatus.setForeground(Color.GRAY);
        pay.setEnabled(false);
        viewReceipt.setEnabled(false);
        currentBillId = -1;
        currentBillAmount = "0";
        currentDueDate = null;
        currentLateFee = BigDecimal.ZERO;
        currentTotalPayable = BigDecimal.ZERO;

        String effectiveMeter = getEffectiveMeter();
        if (effectiveMeter.isEmpty() || month == null || month.trim().isEmpty()) {
            labelstatus.setText("Meter not selected");
            return;
        }

        try (Conn c = new Conn()) {
            if (c.c != null) {
                List<LatePaymentService.BillRecord> matches = LatePaymentService.getBillsByMeterAndMonth(c.c, effectiveMeter, month.trim());
                if (matches.isEmpty()) {
                    labelunits.setText("0 Units");
                    labeltotalbill.setText("₹0.00");
                    labelstatus.setText("No Bill Generated");
                    labelstatus.setForeground(new Color(100, 116, 139));
                    pay.setEnabled(false);
                    viewReceipt.setEnabled(false);
                    return;
                }

                LatePaymentService.BillRecord selected;
                if (matches.size() == 1) {
                    selected = matches.get(0);
                } else {
                    // Multi-bill disambiguation modal
                    String[] options = new String[matches.size()];
                    for (int i = 0; i < matches.size(); i++) {
                        LatePaymentService.BillRecord br = matches.get(i);
                        options[i] = String.format("Bill #%d - Amount: %s (Status: %s, Due: %s)",
                                br.billId, br.totalBill, br.status, (br.dueDate != null ? br.dueDate.toString() : "Historical"));
                    }
                    String picked = (String) JOptionPane.showInputDialog(this,
                            "Multiple bills found for Meter " + effectiveMeter + " in " + month + ".\nPlease select the exact Bill to process:",
                            "Select Bill Record",
                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                    if (picked == null) {
                        labelstatus.setText("Selection Cancelled");
                        return;
                    }

                    int chosenIdx = 0;
                    for (int i = 0; i < options.length; i++) {
                        if (options[i].equals(picked)) {
                            chosenIdx = i;
                            break;
                        }
                    }
                    selected = matches.get(chosenIdx);
                }

                currentBillId = selected.billId;
                currentBillAmount = (selected.totalBill != null) ? selected.totalBill.trim() : "0";
                currentDueDate = selected.dueDate;

                labelbillid.setText("Bill #" + currentBillId);
                labelunits.setText((selected.units != null) ? selected.units + " Units" : "0 Units");
                labeltotalbill.setText("₹" + currentBillAmount);
                labelduedate.setText(currentDueDate != null ? currentDueDate.toString() : "Not recorded (Historical)");

                java.sql.Date dbToday = LatePaymentService.getEffectiveDate(c.c);
                LatePaymentService.LpscResult lpsc = LatePaymentService.calculateLpsc(selected.getPrincipalBigDecimal(), selected.dueDate, dbToday);
                currentLateFee = lpsc.lateFee;
                currentTotalPayable = lpsc.totalPayable;

                if (lpsc.delayDays > 0) {
                    labelsurcharge.setText("₹" + currentLateFee + " (" + lpsc.delayDays + " days late)");
                } else {
                    labelsurcharge.setText("₹0.00 (On-Time)");
                }
                labeltotalpayable.setText("₹" + currentTotalPayable);

                if ("Paid".equalsIgnoreCase(selected.status)) {
                    labelstatus.setText("Paid");
                    labelstatus.setForeground(new Color(22, 163, 74)); // Forest green
                    String settledAmt = (selected.paidAmount != null ? selected.paidAmount.toString() : currentBillAmount);
                    labeltotalbill.setText("Settled: ₹" + settledAmt);
                    labeltotalpayable.setText("₹0.00 (Settled: ₹" + settledAmt + ")");
                    pay.setEnabled(false);
                    pay.setToolTipText("Bill already paid");
                    viewReceipt.setEnabled(true);
                    viewReceipt.setToolTipText("View & Print Payment Receipt");
                } else if ("Commercial".equalsIgnoreCase(selected.status)) {
                    labelstatus.setText("Commercial (Protected)");
                    labelstatus.setForeground(new Color(217, 119, 6)); // Amber
                    pay.setEnabled(false);
                    pay.setToolTipText("Historical Commercial record is protected");
                    viewReceipt.setEnabled(false);
                } else {
                    labelstatus.setText((selected.status != null) ? selected.status : "Not Paid");
                    labelstatus.setForeground(new Color(225, 29, 72)); // Rose red
                    pay.setEnabled(true);
                    pay.setToolTipText("Click to proceed with payment");
                    viewReceipt.setEnabled(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            labelstatus.setText("Error loading bill");
            labelstatus.setForeground(Color.RED);
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == pay) {
            if ("0".equals(currentBillAmount) || "Paid".equalsIgnoreCase(labelstatus.getText())) {
                JOptionPane.showMessageDialog(this, "No outstanding bill balance to pay for " + cmonth.getSelectedItem(), "Notice", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Payment Confirmation Dialog before navigating to Gateway
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Confirm Payment Navigation:\n\n" +
                "  Bill Identifier    : Bill #" + currentBillId + "\n" +
                "  Customer Name      : " + labelname.getText() + "\n" +
                "  Meter Number       : " + getEffectiveMeter() + "\n" +
                "  Billing Month      : " + cmonth.getSelectedItem() + "\n" +
                "  Original Principal : ₹" + currentBillAmount + "\n" +
                "  Late Surcharge     : ₹" + currentLateFee + "\n" +
                "  Total Amount Due   : ₹" + currentTotalPayable + "\n\n" +
                "Proceed to Payment Gateway?",
                "Confirm Payment Intent",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            PaymentGateway pg = new PaymentGateway(currentBillId, getEffectiveMeter(), cmonth.getSelectedItem(), currentTotalPayable.toString());
            pg.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    loadBillDetails(cmonth.getSelectedItem());
                }
            });
        } else if (ae.getSource() == viewReceipt) {
            if (currentBillId > 0) {
                PaymentReceiptDialog receiptDialog = new PaymentReceiptDialog(this, currentBillId);
                receiptDialog.setVisible(true);
            } else {
                PaymentReceiptDialog receiptDialog = new PaymentReceiptDialog(
                    this,
                    labelname.getText(),
                    getEffectiveMeter(),
                    cmonth.getSelectedItem(),
                    currentBillAmount,
                    "Historical Ledger Record",
                    new Date()
                );
                receiptDialog.setVisible(true);
            }
        } else {
            dispose();
        }
    }

    public static void main(String[] args) {
        new PayBill("");
    }
}
