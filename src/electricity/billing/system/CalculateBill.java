package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.Locale;

public class CalculateBill extends JFrame implements ActionListener {

    JTextField tfunits;
    JButton next, cancel;
    JLabel lblname, labeladdress, lblTariffInfo;
    Choice meternumber, cmonth, cdueperiod;

    public CalculateBill() {
        setTitle("Electricity Billing System - Calculate Bill");
        setSize(780, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // West image panel
        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.setBackground(Color.WHITE);
        westPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
        try {
            ImageIcon i1 = new ImageIcon(ClassLoader.getSystemResource("icon/hicon2.jpg"));
            Image i2 = i1.getImage().getScaledInstance(180, 340, Image.SCALE_SMOOTH);
            JLabel image = new JLabel(new ImageIcon(i2));
            westPanel.add(image, BorderLayout.CENTER);
        } catch (Exception ignored) {}
        add(westPanel, BorderLayout.WEST);

        // Center form panel
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBackground(new Color(248, 250, 252)); // Light slate #F8FAFC
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 20),
            new LineBorder(new Color(226, 232, 240), 1, true)
        ));

        JLabel heading = new JLabel("Calculate Electricity Bill");
        heading.setBounds(30, 20, 450, 28);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(new Color(15, 23, 42)); // Slate 900
        p.add(heading);

        JLabel subheading = new JLabel("Compute monthly consumption charges and update billing ledger");
        subheading.setBounds(30, 48, 480, 20);
        subheading.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subheading.setForeground(new Color(100, 116, 139)); // Slate 500
        p.add(subheading);

        // Divider
        JSeparator sep = new JSeparator();
        sep.setBounds(30, 75, 480, 2);
        sep.setForeground(new Color(226, 232, 240));
        p.add(sep);

        // Meter Number
        JLabel lblmeternumber = new JLabel("Meter Number");
        lblmeternumber.setBounds(30, 95, 130, 25);
        lblmeternumber.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblmeternumber.setForeground(new Color(51, 65, 85));
        p.add(lblmeternumber);

        meternumber = new Choice();
        meternumber.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement();
                     ResultSet rs = stmt.executeQuery("select meter_no from customer order by meter_no")) {
                    while (rs.next()) {
                        meternumber.add(rs.getString("meter_no"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        meternumber.setBounds(180, 95, 240, 25);
        p.add(meternumber);

        // Customer Name
        JLabel lblmeterno = new JLabel("Customer Name");
        lblmeterno.setBounds(30, 135, 130, 25);
        lblmeterno.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblmeterno.setForeground(new Color(51, 65, 85));
        p.add(lblmeterno);

        lblname = new JLabel("-");
        lblname.setBounds(180, 135, 300, 25);
        lblname.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblname.setForeground(new Color(15, 23, 42));
        p.add(lblname);

        // Address
        JLabel lbladdress = new JLabel("Address");
        lbladdress.setBounds(30, 175, 130, 25);
        lbladdress.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbladdress.setForeground(new Color(51, 65, 85));
        p.add(lbladdress);

        labeladdress = new JLabel("-");
        labeladdress.setBounds(180, 175, 330, 25);
        labeladdress.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labeladdress.setForeground(new Color(71, 85, 105));
        p.add(labeladdress);

        if (meternumber.getItemCount() > 0) {
            loadCustomerDetails(meternumber.getSelectedItem());
        }

        meternumber.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent ie) {
                loadCustomerDetails(meternumber.getSelectedItem());
            }
        });

        // Units Consumed
        JLabel lblunits = new JLabel("Units Consumed");
        lblunits.setBounds(30, 215, 130, 25);
        lblunits.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblunits.setForeground(new Color(51, 65, 85));
        p.add(lblunits);

        tfunits = new JTextField();
        tfunits.setBounds(180, 215, 240, 28);
        tfunits.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tfunits.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(203, 213, 225), 1, true),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        p.add(tfunits);

        // Month
        JLabel lblmonth = new JLabel("Billing Month");
        lblmonth.setBounds(30, 260, 130, 25);
        lblmonth.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblmonth.setForeground(new Color(51, 65, 85));
        p.add(lblmonth);

        cmonth = new Choice();
        cmonth.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmonth.setBounds(180, 260, 240, 25);
        String[] monthsList = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        for (String m : monthsList) {
            cmonth.add(m);
        }
        p.add(cmonth);

        // Due Period
        JLabel lbldueperiod = new JLabel("Due Period");
        lbldueperiod.setBounds(30, 300, 130, 25);
        lbldueperiod.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbldueperiod.setForeground(new Color(51, 65, 85));
        p.add(lbldueperiod);

        cdueperiod = new Choice();
        cdueperiod.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cdueperiod.setBounds(180, 300, 240, 25);
        cdueperiod.add("15 Days (Default / Regulatory)");
        cdueperiod.add("1 Day (Demonstration Mode)");
        if (LpscConfig.getActiveGraceDays() == LpscConfig.DEMO_GRACE_DAYS) {
            cdueperiod.select(1);
        } else {
            cdueperiod.select(0);
        }
        p.add(cdueperiod);

        // Tariff Rate Info Banner
        lblTariffInfo = new JLabel("Loading tariff breakdown...");
        lblTariffInfo.setBounds(30, 345, 480, 35);
        lblTariffInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTariffInfo.setForeground(new Color(71, 85, 105));
        lblTariffInfo.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        p.add(lblTariffInfo);
        loadTariffInfoBanner();

        // Submit Button
        next = new JButton("Submit");
        next.setBounds(180, 400, 110, 34);
        next.setBackground(new Color(37, 99, 235)); // Primary Royal Blue
        next.setForeground(Color.WHITE);
        next.setFont(new Font("Segoe UI", Font.BOLD, 13));
        next.setFocusPainted(false);
        next.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon calcIcon = Project.getScaledIcon("icon/Calculate Bill.png", 16, Color.WHITE);
        if (calcIcon != null) {
            next.setIcon(calcIcon);
            next.setIconTextGap(6);
        }
        next.addActionListener(this);
        p.add(next);

        // Cancel Button
        cancel = new JButton("Cancel");
        cancel.setBounds(310, 400, 110, 34);
        cancel.setBackground(new Color(71, 85, 105)); // Slate 600
        cancel.setForeground(Color.WHITE);
        cancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cancel.setFocusPainted(false);
        cancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancel.addActionListener(this);
        p.add(cancel);

        add(p, BorderLayout.CENTER);
        setVisible(true);
    }

    private void loadTariffInfoBanner() {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                TariffRates r = loadTariffRates(c.c);
                if (r != null) {
                    lblTariffInfo.setText("<html><b>Live Tariff:</b> ₹" + String.format(Locale.US, "%.2f", r.costPerUnit) 
                        + "/unit &nbsp;|&nbsp; <b>Fixed Taxes:</b> ₹" + String.format(Locale.US, "%.2f", r.totalFixed()) 
                        + " (Rent: ₹" + String.format(Locale.US, "%.2f", r.meterRent) 
                        + ", Svc: ₹" + String.format(Locale.US, "%.2f", r.serviceCharge) 
                        + ", Tax: ₹" + String.format(Locale.US, "%.2f", (r.swacchTax + r.fixedTax)) + ")</html>");
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadCustomerDetails(String meterNo) {
        if (meterNo == null || meterNo.trim().isEmpty()) return;
        try (Conn c = new Conn()) {
            if (c.c != null) {
                String q = "select name, address from customer where meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(q)) {
                    ps.setString(1, meterNo.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            lblname.setText(rs.getString("name"));
                            labeladdress.setText(rs.getString("address"));
                        } else {
                            lblname.setText("Not Found");
                            labeladdress.setText("Not Found");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // TARIFF CALCULATION & DATA STRUCTURES
    // =========================================================================

    public static class TariffRates {
        public double costPerUnit = 3.00;
        public double meterRent = 50.00;
        public double serviceCharge = 20.00;
        public double swacchTax = 15.00;
        public double fixedTax = 25.00;

        public double totalFixed() {
            return meterRent + serviceCharge + swacchTax + fixedTax;
        }
    }

    public static TariffRates loadTariffRates(Connection conn) throws SQLException {
        String query = "select cost_per_unit, meter_rent, service_charge, swacch_bharat_tax, fixed_tax from tax LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                TariffRates rates = new TariffRates();
                rates.costPerUnit = rs.getDouble("cost_per_unit");
                rates.meterRent = rs.getDouble("meter_rent");
                rates.serviceCharge = rs.getDouble("service_charge");
                rates.swacchTax = rs.getDouble("swacch_bharat_tax");
                rates.fixedTax = rs.getDouble("fixed_tax");
                return rates;
            }
        }
        return null;
    }

    /**
     * Exact tariff formula: (units * cost_per_unit) + meter_rent + service_charge + swacch_bharat_tax + fixed_tax.
     * Uses BigDecimal arithmetic to guarantee exact decimal precision without floating point artifacts.
     */
    public static double calculateBillAmount(double units, TariffRates rates) {
        if (rates == null) rates = new TariffRates();
        BigDecimal bUnits = BigDecimal.valueOf(units);
        BigDecimal bRate = BigDecimal.valueOf(rates.costPerUnit);
        BigDecimal bEnergy = bUnits.multiply(bRate);
        BigDecimal bFixed = BigDecimal.valueOf(rates.totalFixed());
        BigDecimal bTotal = bEnergy.add(bFixed).setScale(2, RoundingMode.HALF_UP);
        return bTotal.doubleValue();
    }

    /**
     * Validates input units.
     * Returns "OK" if valid, or an error description string starting with "ERROR:".
     */
    public static String validateUnitsInput(String unitsStr) {
        if (unitsStr == null || unitsStr.trim().isEmpty()) {
            return "ERROR:empty";
        }
        try {
            double u = Double.parseDouble(unitsStr.trim());
            if (u < 0) {
                return "ERROR:negative";
            }
            return "OK:" + unitsStr.trim();
        } catch (NumberFormatException e) {
            return "ERROR:non_numeric";
        }
    }

    // Result object for calculation operations
    public static class BillProcessResult {
        public enum Status {
            SUCCESS_INSERTED,
            SUCCESS_UPDATED,
            CONFIRMATION_REQUIRED,
            REJECTED_ALREADY_PAID,
            REJECTED_HISTORICAL_PROTECTED,
            REJECTED_INVALID_METER,
            REJECTED_VALIDATION_ERROR,
            ERROR_DATABASE
        }

        public Status status;
        public String message;
        public String existingUnits;
        public String existingTotal;
        public String newUnits;
        public String newTotal;
        public long billId;
        public Date dueDate;
        public Date originalDueDate;

        public BillProcessResult(Status status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    /**
     * Core business calculation logic:
     * - Validates meter existence in customer table
     * - Validates units input (rejects empty, negative, invalid)
     * - Retrieves live tariff rates from MySQL tax table
     * - Checks for existing bills (safeguards Paid bills and Commercial historical bills)
     * - Handles unpaid bill updates with explicit confirmation
     */
    public static BillProcessResult processBill(Connection conn, String meter, String month, String unitsStr, boolean confirmUpdate) {
        return processBill(conn, meter, month, unitsStr, confirmUpdate, null, LpscConfig.getActiveGraceDays(), null);
    }

    public static BillProcessResult processBill(Connection conn, String meter, String month, String unitsStr, boolean confirmUpdate, String recalculationReason) {
        return processBill(conn, meter, month, unitsStr, confirmUpdate, recalculationReason, LpscConfig.getActiveGraceDays(), null);
    }

    public static BillProcessResult processBill(Connection conn, String meter, String month, String unitsStr, boolean confirmUpdate, String recalculationReason, int graceDays) {
        return processBill(conn, meter, month, unitsStr, confirmUpdate, recalculationReason, graceDays, null);
    }

    public static BillProcessResult processBill(Connection conn, String meter, String month, String unitsStr, boolean confirmUpdate, String recalculationReason, int graceDays, java.sql.Date customGenDate) {
        if (conn == null) {
            return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Database connection failed.");
        }
        if (meter == null || meter.trim().isEmpty()) {
            return new BillProcessResult(BillProcessResult.Status.REJECTED_VALIDATION_ERROR, "Please select a valid meter number.");
        }

        // Validate meter exists in customer table
        try (PreparedStatement ps = conn.prepareStatement("select count(*) from customer where meter_no = ?")) {
            ps.setString(1, meter.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    return new BillProcessResult(BillProcessResult.Status.REJECTED_INVALID_METER, 
                        "Meter number '" + meter + "' does not exist in customer records.");
                }
            }
        } catch (SQLException e) {
            return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Error checking customer meter: " + e.getMessage());
        }

        // Validate units input
        String validation = validateUnitsInput(unitsStr);
        if (validation.startsWith("ERROR:")) {
            String errType = validation.substring(6);
            if ("empty".equals(errType)) {
                return new BillProcessResult(BillProcessResult.Status.REJECTED_VALIDATION_ERROR, "Please enter units consumed.");
            } else if ("negative".equals(errType)) {
                return new BillProcessResult(BillProcessResult.Status.REJECTED_VALIDATION_ERROR, "Units consumed cannot be negative.");
            } else {
                return new BillProcessResult(BillProcessResult.Status.REJECTED_VALIDATION_ERROR, "Units consumed must be a valid number.");
            }
        }

        double unitsVal = Double.parseDouble(unitsStr.trim());

        // Format units display string (whole number if no decimal, else 2 decimal places)
        String formattedUnits = (unitsVal % 1 == 0) ? String.valueOf((long) unitsVal) : String.format(Locale.US, "%.2f", unitsVal);

        // Load tax rates
        TariffRates rates;
        try {
            rates = loadTariffRates(conn);
            if (rates == null) {
                return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Tax rates configuration not found in database.");
            }
        } catch (SQLException e) {
            return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Failed to load tax rates: " + e.getMessage());
        }

        // Calculate total amount
        double totalAmount = calculateBillAmount(unitsVal, rates);
        String formattedTotal = String.format(Locale.US, "%.2f", totalAmount);

        // Check existing bill
        String checkQuery = "select bill_id, status, total_bill, units, due_date, original_due_date from bill where meter_no = ? and month = ? order by bill_id asc";
        boolean billExists = false;
        long existingBillId = -1;
        String existingStatus = "";
        String existingTotal = "";
        String existingUnits = "";
        java.sql.Date existingDueDate = null;
        java.sql.Date existingOrigDueDate = null;

        try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
            ps.setString(1, meter.trim());
            ps.setString(2, month.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    billExists = true;
                    existingBillId = rs.getLong("bill_id");
                    existingStatus = rs.getString("status");
                    existingTotal = rs.getString("total_bill");
                    existingUnits = rs.getString("units");
                    existingDueDate = rs.getDate("due_date");
                    existingOrigDueDate = rs.getDate("original_due_date");
                }
            }
        } catch (SQLException e) {
            return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Error verifying existing bills: " + e.getMessage());
        }

        if (billExists) {
            if ("Paid".equalsIgnoreCase(existingStatus)) {
                BillProcessResult res = new BillProcessResult(BillProcessResult.Status.REJECTED_ALREADY_PAID,
                    "A bill for " + month + " on meter " + meter + " has ALREADY been paid (Amount: ₹" + existingTotal + ").\nPaid bills cannot be overwritten.");
                res.billId = existingBillId;
                res.existingTotal = existingTotal;
                res.existingUnits = existingUnits;
                return res;
            } else if (!"Not Paid".equalsIgnoreCase(existingStatus)) {
                // Safeguard historical special statuses (such as 'Commercial' on MET005)
                BillProcessResult res = new BillProcessResult(BillProcessResult.Status.REJECTED_HISTORICAL_PROTECTED,
                    "A bill for " + month + " on meter " + meter + " exists with status '" + existingStatus + "'.\nHistorical billing records cannot be overwritten without explicit approval.");
                res.billId = existingBillId;
                res.existingTotal = existingTotal;
                res.existingUnits = existingUnits;
                return res;
            } else {
                // Bill exists with 'Not Paid'
                if (!confirmUpdate) {
                    BillProcessResult res = new BillProcessResult(BillProcessResult.Status.CONFIRMATION_REQUIRED,
                        "An unpaid bill already exists for " + month + ".\nCurrent: " + existingUnits + " units (₹" + existingTotal + ").\nRecalculated: " + formattedUnits + " units (₹" + formattedTotal + ").");
                    res.billId = existingBillId;
                    res.existingUnits = existingUnits;
                    res.existingTotal = existingTotal;
                    res.newUnits = formattedUnits;
                    res.newTotal = formattedTotal;
                    res.dueDate = existingDueDate;
                    res.originalDueDate = existingOrigDueDate;
                    return res;
                }

                // Update existing unpaid bill by bill_id
                // Existing bill due_date and original_due_date are strictly preserved;
                // never replaced with current date + 15 days, nor affected by the demo 1-day setting.
                // If existing due_date is NULL, it remains NULL.
                String reasonToLog = (recalculationReason != null && !recalculationReason.trim().isEmpty())
                        ? recalculationReason.trim()
                        : "Administrative meter recalculation";

                String updateQuery = "update bill set units = ?, total_bill = ? where bill_id = ? and status = 'Not Paid'";
                try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
                    ps.setString(1, formattedUnits);
                    ps.setString(2, formattedTotal);
                    ps.setLong(3, existingBillId);
                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        String actorId = UserSession.getActorId();
                        String actorRole = UserSession.getActorRole();
                        String dueDisplay = (existingDueDate != null) ? existingDueDate.toString() : "NULL (Historical)";
                        AuditService.logEvent(conn, actorId, actorRole, AuditService.ACTION_BILL_RECALCULATED,
                            AuditService.ENTITY_BILL, meter.trim() + ":" + month.trim(),
                            "Recalculated bill #" + existingBillId + " for " + month + " (" + formattedUnits + " units, ₹" + formattedTotal + ", Preserved Due Date: " + dueDisplay + "). Reason: " + reasonToLog,
                            existingTotal, formattedTotal);
                        AuditService.logBillRecalculation(conn, actorId, actorRole, existingBillId, meter.trim(), month.trim(), existingTotal, formattedTotal, reasonToLog);

                        BillProcessResult res = new BillProcessResult(BillProcessResult.Status.SUCCESS_UPDATED,
                            "Existing unpaid bill #" + existingBillId + " for " + month + " updated successfully (₹" + formattedTotal + ").\nPreserved Due Date: " + dueDisplay);
                        res.billId = existingBillId;
                        res.newTotal = formattedTotal;
                        res.newUnits = formattedUnits;
                        res.dueDate = existingDueDate;
                        res.originalDueDate = existingOrigDueDate;
                        return res;
                    } else {
                        return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Failed to update unpaid bill. Status may have changed.");
                    }
                } catch (SQLException e) {
                    return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Database update error: " + e.getMessage());
                }
            }
        }

        // Insert new bill with due_date, original_due_date, and late_fee
        java.sql.Date genDate = (customGenDate != null) ? customGenDate : LatePaymentService.getEffectiveDate(conn);
        int effectiveGraceDays = (graceDays == LpscConfig.DEMO_GRACE_DAYS || graceDays == LpscConfig.STANDARD_GRACE_DAYS)
                ? graceDays
                : LpscConfig.getActiveGraceDays();
        java.sql.Date dueDate = LatePaymentService.calculateDueDate(genDate, effectiveGraceDays);
        java.sql.Date origDueDate = dueDate;

        String insertQuery = "insert into bill (meter_no, month, units, total_bill, status, due_date, original_due_date, late_fee) values (?, ?, ?, ?, 'Not Paid', ?, ?, 0.00)";
        try (PreparedStatement ps = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, meter.trim());
            ps.setString(2, month.trim());
            ps.setString(3, formattedUnits);
            ps.setString(4, formattedTotal);
            ps.setDate(5, dueDate);
            ps.setDate(6, origDueDate);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                long generatedBillId = -1;
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) generatedBillId = gk.getLong(1);
                }
                String actorId = UserSession.getActorId();
                String actorRole = UserSession.getActorRole();
                AuditService.logEvent(conn, actorId, actorRole, AuditService.ACTION_BILL_GENERATED,
                    AuditService.ENTITY_BILL, meter.trim() + ":" + month.trim(),
                    "Generated bill #" + generatedBillId + " for " + month + " (" + formattedUnits + " units, ₹" + formattedTotal + ", Due: " + dueDate + ")",
                    null, formattedTotal);
                if (generatedBillId > 0) {
                    AuditService.logBillEvent(conn, actorId, actorRole, AuditService.ACTION_BILL_DUE_DATE_CREATED,
                        generatedBillId, "Assigned due date " + dueDate + " (Grace: " + effectiveGraceDays + " days)", null, dueDate.toString());
                }

                // Automatic in-app notification dispatch (Phase 3.6)
                try {
                    String notifTitle = "New Electricity Bill Generated - " + month.trim();
                    String notifBody = "Dear Customer, your electricity bill for " + month.trim() + " has been generated.\n"
                            + "Bill ID: #" + generatedBillId + "\n"
                            + "Units Consumed: " + formattedUnits + "\n"
                            + "Total Amount Payable: ₹" + formattedTotal + "\n"
                            + "Due Date: " + dueDate + "\n"
                            + "Please pay on or before the due date to avoid late payment surcharge (LPSC).";
                    NotificationService.ServiceResult notifResult = NotificationService.sendSystemNotification(
                            meter.trim(), notifTitle, notifBody, NotificationService.PRIORITY_IMPORTANT
                    );
                    if (!notifResult.isSuccess()) {
                        System.err.println("[NOTIFICATION WARNING] Automatic bill generation notification failed for meter "
                                + meter.trim() + ": " + notifResult.getMessage());
                    }
                } catch (Throwable notifEx) {
                    System.err.println("[NOTIFICATION ERROR] Exception dispatching automatic bill notification: " + notifEx.getMessage());
                }

                BillProcessResult res = new BillProcessResult(BillProcessResult.Status.SUCCESS_INSERTED,
                    "Customer Bill of ₹" + formattedTotal + " generated successfully for " + month + " (Bill ID: #" + generatedBillId + ", Due Date: " + dueDate + ").");
                res.billId = generatedBillId;
                res.newTotal = formattedTotal;
                res.newUnits = formattedUnits;
                res.dueDate = dueDate;
                res.originalDueDate = origDueDate;
                return res;
            } else {
                return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Failed to insert bill into database.");
            }
        } catch (SQLException e) {
            return new BillProcessResult(BillProcessResult.Status.ERROR_DATABASE, "Database insert error: " + e.getMessage());
        }
    }

    // =========================================================================
    // ACTION LISTENER
    // =========================================================================

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == next) {
            String meter = meternumber.getSelectedItem();
            String units = tfunits.getText();
            String month = cmonth.getSelectedItem();

            int selectedGraceDays = LpscConfig.getActiveGraceDays();
            if (cdueperiod != null && cdueperiod.getSelectedIndex() == 1) {
                selectedGraceDays = LpscConfig.DEMO_GRACE_DAYS;
                LpscConfig.setActiveGraceDays(LpscConfig.DEMO_GRACE_DAYS);
            } else if (cdueperiod != null && cdueperiod.getSelectedIndex() == 0) {
                selectedGraceDays = LpscConfig.STANDARD_GRACE_DAYS;
                LpscConfig.setActiveGraceDays(LpscConfig.STANDARD_GRACE_DAYS);
            }

            try (Conn c = new Conn()) {
                if (c.c == null) {
                    JOptionPane.showMessageDialog(this, "Database connection failed.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Initial process call without auto-confirming update
                BillProcessResult result = processBill(c.c, meter, month, units, false, null, selectedGraceDays);

                if (result.status == BillProcessResult.Status.CONFIRMATION_REQUIRED) {
                    // Show clear confirmation dialog with old vs new amounts
                    int choice = JOptionPane.showConfirmDialog(this,
                        "An unpaid bill already exists for " + month + " on meter " + meter + ".\n\n"
                        + "Current Bill Record:\n"
                        + "  • Units Consumed: " + result.existingUnits + "\n"
                        + "  • Bill Amount   : ₹" + result.existingTotal + "\n\n"
                        + "Recalculated Bill:\n"
                        + "  • New Units     : " + result.newUnits + "\n"
                        + "  • New Amount    : ₹" + result.newTotal + "\n\n"
                        + "Do you want to update this unpaid bill with the recalculated amount?",
                        "Confirm Bill Recalculation",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                    if (choice == JOptionPane.YES_OPTION) {
                        String reason = JOptionPane.showInputDialog(this,
                            "Enter mandatory reason for bill recalculation:\n(Required for administrative audit trail)",
                            "Recalculation Reason",
                            JOptionPane.QUESTION_MESSAGE);
                        if (reason == null || reason.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(this,
                                "Recalculation cancelled: A non-empty reason is required.",
                                "Recalculation Cancelled",
                                JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        BillProcessResult updateResult = processBill(c.c, meter, month, units, true, reason.trim(), selectedGraceDays);
                        if (updateResult.status == BillProcessResult.Status.SUCCESS_UPDATED) {
                            JOptionPane.showMessageDialog(this, updateResult.message, "Bill Updated", JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(this, updateResult.message, "Update Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    return;
                } else if (result.status == BillProcessResult.Status.SUCCESS_INSERTED) {
                    JOptionPane.showMessageDialog(this, result.message, "Bill Generated", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    return;
                } else if (result.status == BillProcessResult.Status.REJECTED_ALREADY_PAID) {
                    JOptionPane.showMessageDialog(this, result.message, "Bill Already Paid", JOptionPane.WARNING_MESSAGE);
                    return;
                } else if (result.status == BillProcessResult.Status.REJECTED_HISTORICAL_PROTECTED) {
                    JOptionPane.showMessageDialog(this, result.message, "Historical Record Protected", JOptionPane.WARNING_MESSAGE);
                    return;
                } else if (result.status == BillProcessResult.Status.REJECTED_INVALID_METER) {
                    JOptionPane.showMessageDialog(this, result.message, "Invalid Meter", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (result.status == BillProcessResult.Status.REJECTED_VALIDATION_ERROR) {
                    JOptionPane.showMessageDialog(this, result.message, "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    JOptionPane.showMessageDialog(this, result.message, "Calculation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Unexpected error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            dispose();
        }
    }

    public static void main(String[] args) {
        new CalculateBill();
    }
}
