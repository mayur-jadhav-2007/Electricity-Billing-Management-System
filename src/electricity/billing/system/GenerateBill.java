package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GenerateBill extends JFrame implements ActionListener {

    String meter;
    long targetBillId = -1;
    JButton bill, print, close;
    Choice cmonth, meterChoice;
    JTextArea area;
    JLabel lblMeterDisplay;
    boolean isInvoiceLoaded = false;

    public GenerateBill(long billId) {
        this("");
        this.targetBillId = billId;
    }

    public GenerateBill(String meter) {
        this.meter = (meter != null) ? meter.trim() : "";

        setTitle("Electricity Billing System - Tax Invoice Generator");
        setSize(650, 840);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // North control panel
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(Color.WHITE);
        topContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        // Title Row
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        titleRow.setBackground(new Color(248, 250, 252));
        JLabel titleLabel = new JLabel("Official Electricity Invoice Statement");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(15, 23, 42));
        titleRow.add(titleLabel);
        topContainer.add(titleRow);

        // Filter / Controls Row
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        filterRow.setBackground(Color.WHITE);

        if (this.meter.isEmpty()) {
            JLabel lblSelectMeter = new JLabel("Select Meter:");
            lblSelectMeter.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblSelectMeter.setForeground(new Color(51, 65, 85));
            filterRow.add(lblSelectMeter);

            meterChoice = new Choice();
            meterChoice.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            try (Conn c = new Conn()) {
                if (c.c != null) {
                    try (Statement stmt = c.c.createStatement();
                         ResultSet rs = stmt.executeQuery("select meter_no from customer order by meter_no")) {
                        while (rs.next()) {
                            meterChoice.add(rs.getString("meter_no"));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            filterRow.add(meterChoice);
        } else {
            lblMeterDisplay = new JLabel("Account Meter: " + this.meter);
            lblMeterDisplay.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblMeterDisplay.setForeground(new Color(37, 99, 235));
            filterRow.add(lblMeterDisplay);
        }

        JLabel lblMonth = new JLabel("Billing Month:");
        lblMonth.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMonth.setForeground(new Color(51, 65, 85));
        filterRow.add(lblMonth);

        cmonth = new Choice();
        cmonth.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String[] monthsList = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        for (String m : monthsList) {
            cmonth.add(m);
        }
        filterRow.add(cmonth);

        bill = new JButton("Generate Bill");
        bill.setBackground(new Color(37, 99, 235)); // Royal blue
        bill.setForeground(Color.WHITE);
        bill.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bill.setFocusPainted(false);
        bill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon genIcon = Project.getScaledIcon("icon/Generate Bill.png", 16, Color.WHITE);
        if (genIcon != null) {
            bill.setIcon(genIcon);
            bill.setIconTextGap(6);
        }
        bill.addActionListener(this);
        filterRow.add(bill);

        topContainer.add(filterRow);
        add(topContainer, BorderLayout.NORTH);

        // Center: Formatted Monospaced Invoice Area
        area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setForeground(new Color(15, 23, 42));
        area.setBackground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        area.setText("\n\n\t============================================================\n"
                  + "\t              ELECTRICITY BILLING SYSTEM\n"
                  + "\t                  TAX INVOICE PORTAL\n"
                  + "\t============================================================\n\n"
                  + "\t  Select the billing cycle month and click 'Generate Bill'\n"
                  + "\t  to produce the official customer statement.");

        JScrollPane pane = new JScrollPane(area);
        pane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 15, 10, 15),
            new LineBorder(new Color(226, 232, 240), 1, true)
        ));
        add(pane, BorderLayout.CENTER);

        // South: Action Buttons (Print & Close)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        buttonPanel.setBackground(new Color(248, 250, 252));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        print = new JButton("Print Bill");
        print.setBackground(new Color(22, 163, 74)); // Forest Green
        print.setForeground(Color.WHITE);
        print.setFont(new Font("Segoe UI", Font.BOLD, 12));
        print.setFocusPainted(false);
        print.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon printIcon = Project.getScaledIcon("icon/icons8-receipt-48.png", 16, Color.WHITE);
        if (printIcon != null) {
            print.setIcon(printIcon);
            print.setIconTextGap(6);
        }
        print.setEnabled(false); // Enabled after bill generation
        print.addActionListener(this);

        close = new JButton("Close");
        close.setBackground(new Color(71, 85, 105)); // Slate 600
        close.setForeground(Color.WHITE);
        close.setFont(new Font("Segoe UI", Font.BOLD, 12));
        close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(this);

        buttonPanel.add(print);
        buttonPanel.add(close);
        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private String getSelectedMeter() {
        if (!this.meter.isEmpty()) {
            return this.meter;
        }
        if (meterChoice != null && meterChoice.getSelectedItem() != null) {
            return meterChoice.getSelectedItem().trim();
        }
        return "";
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == print) {
            if (!isInvoiceLoaded) {
                JOptionPane.showMessageDialog(this, "Please generate a valid invoice before printing.", "Print Notice", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                String targetMeter = getSelectedMeter();
                String targetMonth = cmonth.getSelectedItem();
                MessageFormat header = new MessageFormat("Electricity Billing System - Invoice (" + targetMeter + " - " + targetMonth + ")");
                MessageFormat footer = new MessageFormat("Page {0,number,integer}");
                boolean complete = area.print(header, footer, true, null, null, true);
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Invoice printing completed successfully.", "Print Status", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Printing Error: " + e.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        if (ae.getSource() == close) {
            dispose();
            return;
        }

        // Generate Bill Action
        String targetMeter = getSelectedMeter();
        if (targetMeter.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a valid meter number.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String month = cmonth.getSelectedItem();
        generateBillStatement(targetMeter, month, true);
    }

    public boolean generateBillStatement(String targetMeter, String month, boolean notifyDialog) {
        if (targetMeter == null || targetMeter.trim().isEmpty() || month == null || month.trim().isEmpty()) {
            if (notifyDialog) {
                JOptionPane.showMessageDialog(this, "Please select a valid meter and month.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }

        try (Conn c = new Conn()) {
            if (c.c == null) {
                if (notifyDialog) {
                    JOptionPane.showMessageDialog(this, "Database connection failed.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }

            // 1. Fetch Customer Details
            String custName = "N/A", address = "N/A", city = "N/A", state = "N/A", email = "N/A", phone = "N/A";
            boolean custFound = false;
            try (PreparedStatement ps = c.c.prepareStatement("select name, address, city, state, email, phone from customer where meter_no = ?")) {
                ps.setString(1, targetMeter);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        custFound = true;
                        custName = rs.getString("name");
                        address = rs.getString("address");
                        city = rs.getString("city");
                        state = rs.getString("state");
                        email = rs.getString("email");
                        phone = rs.getString("phone");
                    }
                }
            }

            if (!custFound) {
                area.setText("\n\t============================================================\n"
                           + "\t  CUSTOMER RECORD NOT FOUND\n"
                           + "\t  Meter Number: " + targetMeter + "\n"
                           + "\t============================================================\n"
                           + "\t  No customer record exists in the system for this meter.");
                print.setEnabled(false);
                isInvoiceLoaded = false;
                if (notifyDialog) {
                    JOptionPane.showMessageDialog(this, "No customer record found for meter '" + targetMeter + "'.", "Customer Not Found", JOptionPane.WARNING_MESSAGE);
                }
                return false;
            }

            // 2. Fetch Meter Specifications
            String location = "N/A", type = "N/A", phase = "N/A", billType = "N/A", days = "30";
            try (PreparedStatement ps = c.c.prepareStatement("select meter_location, meter_type, phase_code, bill_type, days from meter_info where meter_no = ?")) {
                ps.setString(1, targetMeter);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        location = rs.getString("meter_location");
                        type = rs.getString("meter_type");
                        phase = rs.getString("phase_code");
                        billType = rs.getString("bill_type");
                        days = rs.getString("days");
                    }
                }
            }

            // 3. Fetch Tariff Rates
            double costPerUnit = 3.00, meterRent = 50.00, serviceCharge = 20.00, swacchTax = 15.00, fixedTax = 25.00;
            try (PreparedStatement ps = c.c.prepareStatement("select cost_per_unit, meter_rent, service_charge, swacch_bharat_tax, fixed_tax from tax LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    costPerUnit = rs.getDouble("cost_per_unit");
                    meterRent = rs.getDouble("meter_rent");
                    serviceCharge = rs.getDouble("service_charge");
                    swacchTax = rs.getDouble("swacch_bharat_tax");
                    fixedTax = rs.getDouble("fixed_tax");
                }
            }

            // 4. Fetch Stored Bill for Month or Bill ID
            String billUnits = null;
            String billTotal = null;
            String billStatus = null;
            long loadedBillId = -1;
            java.sql.Date loadedDueDate = null;
            java.math.BigDecimal loadedLateFee = java.math.BigDecimal.ZERO;
            java.math.BigDecimal loadedTotalPayable = java.math.BigDecimal.ZERO;
            boolean billFound = false;

            if (this.targetBillId > 0) {
                LatePaymentService.BillRecord br = LatePaymentService.getBillById(c.c, this.targetBillId);
                if (br != null) {
                    billFound = true;
                    loadedBillId = br.billId;
                    targetMeter = br.meterNo;
                    month = br.month;
                    billUnits = br.units;
                    billTotal = br.totalBill;
                    billStatus = br.status;
                    loadedDueDate = br.dueDate;
                    LatePaymentService.LpscResult lpsc = LatePaymentService.calculateLpsc(br.getPrincipalBigDecimal(), br.dueDate, LatePaymentService.getDatabaseCurrentDate(c.c));
                    loadedLateFee = lpsc.lateFee;
                    loadedTotalPayable = lpsc.totalPayable;
                }
            } else {
                java.util.List<LatePaymentService.BillRecord> matches = LatePaymentService.getBillsByMeterAndMonth(c.c, targetMeter, month);
                if (!matches.isEmpty()) {
                    LatePaymentService.BillRecord br;
                    if (matches.size() == 1) {
                        br = matches.get(0);
                    } else {
                        // Multi-bill disambiguation modal
                        String[] options = new String[matches.size()];
                        for (int i = 0; i < matches.size(); i++) {
                            LatePaymentService.BillRecord r = matches.get(i);
                            options[i] = String.format("Bill #%d - %s (%s, Units: %s, Due: %s)",
                                    r.billId, r.totalBill, r.status, r.units, (r.dueDate != null ? r.dueDate.toString() : "Historical"));
                        }
                        String picked = (String) JOptionPane.showInputDialog(this,
                                "Multiple bills found for Meter " + targetMeter + " in " + month + ".\nPlease select the exact Bill to view:",
                                "Select Bill Record",
                                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                        if (picked == null) return false;
                        int idx = 0;
                        for (int i = 0; i < options.length; i++) {
                            if (options[i].equals(picked)) {
                                idx = i;
                                break;
                            }
                        }
                        br = matches.get(idx);
                    }

                    billFound = true;
                    loadedBillId = br.billId;
                    billUnits = br.units;
                    billTotal = br.totalBill;
                    billStatus = br.status;
                    loadedDueDate = br.dueDate;
                    LatePaymentService.LpscResult lpsc = LatePaymentService.calculateLpsc(br.getPrincipalBigDecimal(), br.dueDate, LatePaymentService.getDatabaseCurrentDate(c.c));
                    loadedLateFee = lpsc.lateFee;
                    loadedTotalPayable = lpsc.totalPayable;
                }
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
            String currentDate = dateFormat.format(new Date());
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

            if (!billFound) {
                // Graceful Missing Bill Template
                StringBuilder sb = new StringBuilder();
                sb.append("================================================================================\n");
                sb.append("                   ELECTRICITY BILLING MANAGEMENT SYSTEM\n");
                sb.append("                               INVOICE NOTICE\n");
                sb.append("================================================================================\n");
                sb.append(String.format("  Notice Date  : %-22s  Billing Cycle : %s %d\n", currentDate, month.toUpperCase(), currentYear));
                sb.append(String.format("  Account Meter: %-22s  Status        : NO BILL RECORD FOUND\n", targetMeter));
                sb.append("--------------------------------------------------------------------------------\n");
                sb.append("  1. CUSTOMER INFORMATION\n");
                sb.append(String.format("     Customer Name     : %s\n", custName));
                sb.append(String.format("     Service Address   : %s, %s, %s\n", address, city, state));
                sb.append(String.format("     Contact Phone     : %s\n", phone));
                sb.append(String.format("     Email Address     : %s\n", email));
                sb.append("--------------------------------------------------------------------------------\n");
                sb.append("  2. BILLING STATUS & ADVISORY\n");
                sb.append("     [!] No electricity invoice has been computed for this cycle.\n\n");
                sb.append("     Action Required:\n");
                sb.append("     • If you are an administrator, open 'Calculate Bill' to enter\n");
                sb.append("       the units consumed for this billing month.\n");
                sb.append("     • If you are a customer, please contact EBS support or wait until\n");
                sb.append("       meter reading generation completes for this cycle.\n");
                sb.append("================================================================================\n");

                area.setText(sb.toString());
                area.setCaretPosition(0);
                print.setEnabled(false);
                isInvoiceLoaded = false;
                if (notifyDialog) {
                    JOptionPane.showMessageDialog(this, 
                        "No billing record found for Meter " + targetMeter + " in " + month + ".\nPlease calculate the bill first.",
                        "No Bill Record", JOptionPane.INFORMATION_MESSAGE);
                }
                return false;
            }

            // Calculate Tariff Breakdown
            double parsedUnits = 0.0;
            try {
                parsedUnits = Double.parseDouble(billUnits.trim());
            } catch (Exception ignored) {}

            double energyCharges = parsedUnits * costPerUnit;
            double totalFixed = meterRent + serviceCharge + swacchTax + fixedTax;
            double calculatedTotal = energyCharges + totalFixed;

            // Formatted Bill Display
            StringBuilder sb = new StringBuilder();
            sb.append("================================================================================\n");
            sb.append("                   ELECTRICITY BILLING MANAGEMENT SYSTEM\n");
            sb.append("                                TAX INVOICE\n");
            sb.append("================================================================================\n");
            sb.append(String.format("  Invoice Date : %-22s  Billing Cycle  : %s %d\n", currentDate, month.toUpperCase(), currentYear));
            sb.append(String.format("  Account Meter: %-22s  Bill Identifier: %s\n", targetMeter, (loadedBillId > 0 ? "Bill #" + loadedBillId : "Historical")));
            sb.append(String.format("  Payment Due  : %-22s  Payment Status : %s\n", (loadedDueDate != null ? loadedDueDate.toString() : "Not recorded (Historical)"), billStatus.toUpperCase()));
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("  1. CUSTOMER DETAILS\n");
            sb.append(String.format("     Customer Name     : %s\n", custName));
            sb.append(String.format("     Service Address   : %s\n", address));
            sb.append(String.format("     City / State      : %s, %s\n", city, state));
            sb.append(String.format("     Contact Phone     : %s\n", phone));
            sb.append(String.format("     Email Address     : %s\n", email));
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("  2. METER & HARDWARE SPECIFICATIONS\n");
            sb.append(String.format("     Meter Number      : %s\n", targetMeter));
            sb.append(String.format("     Meter Location    : %s\n", location));
            sb.append(String.format("     Meter Type        : %s\n", type));
            sb.append(String.format("     Phase Code        : %s\n", phase));
            sb.append(String.format("     Billing Type      : %s\n", billType));
            sb.append(String.format("     Cycle Duration    : %s Days\n", days));
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("  3. CONSUMPTION & TARIFF BREAKDOWN\n");
            sb.append(String.format("     Units Consumed    : %s Units\n", billUnits));
            sb.append(String.format("     Rate Per Unit     : ₹%.2f / Unit\n", costPerUnit));
            sb.append(String.format("     Energy Charges    : ₹%-12.2f (%s Units × ₹%.2f)\n", energyCharges, billUnits, costPerUnit));
            sb.append(String.format("     Meter Rent        : ₹%.2f\n", meterRent));
            sb.append(String.format("     Service Charge    : ₹%.2f\n", serviceCharge));
            sb.append(String.format("     Swacch Bharat Tax : ₹%.2f\n", swacchTax));
            sb.append(String.format("     Fixed Tax         : ₹%.2f\n", fixedTax));
            sb.append(String.format("     Total Fixed Fees  : ₹%.2f\n", totalFixed));
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append("  4. INVOICE CHARGES & PAYMENT SUMMARY\n");
            sb.append(String.format("     Calculated Amount : ₹%.2f\n", calculatedTotal));
            sb.append(String.format("     Stored Bill Amount: ₹%s\n", billTotal));
            if (loadedLateFee.compareTo(java.math.BigDecimal.ZERO) > 0) {
                sb.append(String.format("     Late Fee (LPSC)   : ₹%s\n", loadedLateFee));
                sb.append(String.format("     Current Status    : %s (Overdue)\n", billStatus.toUpperCase()));
            } else {
                sb.append(String.format("     Late Fee (LPSC)   : ₹0.00\n"));
                sb.append(String.format("     Current Status    : %s\n", billStatus.toUpperCase()));
            }
            sb.append("================================================================================\n");
            String displayPayable = (loadedTotalPayable.compareTo(java.math.BigDecimal.ZERO) > 0) ? loadedTotalPayable.toString() : billTotal;
            sb.append(String.format("     NET PAYABLE AMOUNT: ₹%s\n", displayPayable));
            sb.append("================================================================================\n");
            sb.append("  Thank you for paying on time! For queries: support@ebs.gov.in | +91 1800-EBS\n");

            area.setText(sb.toString());
            area.setCaretPosition(0);
            print.setEnabled(true);
            isInvoiceLoaded = true;
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (notifyDialog) {
                JOptionPane.showMessageDialog(this, "Error generating bill statement: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
    }

    public static void main(String[] args) {
        new GenerateBill("");
    }
}
