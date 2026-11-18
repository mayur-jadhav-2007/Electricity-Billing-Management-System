package electricity.billing.system;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.awt.event.*;

public class DepositDetails extends JFrame implements ActionListener {

    JComboBox<String> meternumber, cmonth, cstatus;
    JTable table;
    JButton search, print, viewReceipt, reset, close;
    JLabel lblStatus;

    public DepositDetails() {
        super("Electricity Billing System - Deposit & Payment Ledger");

        setSize(920, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Top Filter Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBackground(new Color(248, 250, 252)); // Light slate #F8FAFC
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JLabel lblmeternumber = new JLabel("Meter:");
        lblmeternumber.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblmeternumber.setForeground(new Color(51, 65, 85));
        topPanel.add(lblmeternumber);

        meternumber = new JComboBox<>();
        meternumber.setEditable(true); // Allows both selecting existing meters and typing arbitrary/invalid meters for testing
        meternumber.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        meternumber.addItem("All Meters");

        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement();
                     ResultSet rs = stmt.executeQuery("select meter_no from customer order by meter_no")) {
                    while (rs.next()) {
                        meternumber.addItem(rs.getString("meter_no"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        topPanel.add(meternumber);

        JLabel lblmonth = new JLabel("Month:");
        lblmonth.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblmonth.setForeground(new Color(51, 65, 85));
        topPanel.add(lblmonth);

        String[] months = {
            "All Months", "January", "February", "March", "April",
            "May", "June", "July", "August", "September",
            "October", "November", "December"
        };
        cmonth = new JComboBox<>(months);
        cmonth.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topPanel.add(cmonth);

        JLabel lblstatusFilter = new JLabel("Status:");
        lblstatusFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblstatusFilter.setForeground(new Color(51, 65, 85));
        topPanel.add(lblstatusFilter);

        String[] statuses = {"All Statuses", "Paid Only", "Not Paid Only"};
        cstatus = new JComboBox<>(statuses);
        cstatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topPanel.add(cstatus);

        search = new JButton("Search");
        search.setBackground(new Color(37, 99, 235)); // Royal Blue
        search.setForeground(Color.WHITE);
        search.setFont(new Font("Segoe UI", Font.BOLD, 12));
        search.setFocusPainted(false);
        search.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon searchIcon = Project.getScaledIcon("icon/icons8-search-50.png", 16, Color.WHITE);
        if (searchIcon != null) {
            search.setIcon(searchIcon);
            search.setIconTextGap(6);
        }
        search.addActionListener(this);
        topPanel.add(search);

        reset = new JButton("Show All");
        reset.setBackground(new Color(71, 85, 105)); // Slate 600
        reset.setForeground(Color.WHITE);
        reset.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reset.setFocusPainted(false);
        reset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reset.addActionListener(this);
        topPanel.add(reset);

        lblStatus = new JLabel("Loading records...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(100, 116, 139));
        topPanel.add(lblStatus);

        add(topPanel, BorderLayout.NORTH);

        // Center Table
        table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(15, 23, 42));
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 232, 240));

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(sp, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        bottomPanel.setBackground(new Color(248, 250, 252));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        viewReceipt = new JButton("View Receipt");
        viewReceipt.setBackground(new Color(37, 99, 235)); // Royal blue
        viewReceipt.setForeground(Color.WHITE);
        viewReceipt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        viewReceipt.setFocusPainted(false);
        viewReceipt.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon receiptIcon = Project.getScaledIcon("icon/icons8-receipt-48.png", 16, Color.WHITE);
        if (receiptIcon != null) {
            viewReceipt.setIcon(receiptIcon);
            viewReceipt.setIconTextGap(6);
        }
        viewReceipt.addActionListener(this);

        print = new JButton("Print Records");
        print.setBackground(new Color(22, 163, 74)); // Forest green
        print.setForeground(Color.WHITE);
        print.setFont(new Font("Segoe UI", Font.BOLD, 12));
        print.setFocusPainted(false);
        print.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (receiptIcon != null) {
            print.setIcon(receiptIcon);
            print.setIconTextGap(6);
        }
        print.addActionListener(this);

        close = new JButton("Close");
        close.setBackground(new Color(71, 85, 105)); // Slate 600
        close.setForeground(Color.WHITE);
        close.setFont(new Font("Segoe UI", Font.BOLD, 12));
        close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(this);

        bottomPanel.add(viewReceipt);
        bottomPanel.add(print);
        bottomPanel.add(close);
        add(bottomPanel, BorderLayout.SOUTH);

        // Initial Load
        loadBills("All Meters", "All Months", "All Statuses", false);

        setVisible(true);
    }

    /**
     * Backward-compatible 3-arg overload for previous phase tests.
     */
    public int loadBills(String meter, String month, boolean notifyIfEmpty) {
        return loadBills(meter, month, "All Statuses", notifyIfEmpty);
    }

    /**
     * Searches bills by meter, month, and payment status with:
     * - Parameterized PreparedStatement protection
     * - Case-insensitive month filtering
     * - Chronological month ordering
     */
    public int loadBills(String meter, String month, String status, boolean notifyIfEmpty) {
        StringBuilder q = new StringBuilder(
            "select bill_id as 'Bill ID', meter_no as 'Meter No', month as 'Month', units as 'Units', " +
            "concat('₹', total_bill) as 'Total Bill', due_date as 'Due Date', status as 'Status' from bill where 1=1"
        );
        List<String> params = new ArrayList<>();

        String cleanMeter = (meter != null) ? meter.trim() : "";
        String cleanMonth = (month != null) ? month.trim() : "";
        String cleanStatus = (status != null) ? status.trim() : "";

        if (!cleanMeter.isEmpty() && !"All Meters".equalsIgnoreCase(cleanMeter)) {
            q.append(" and meter_no = ?");
            params.add(cleanMeter);
        }
        if (!cleanMonth.isEmpty() && !"All Months".equalsIgnoreCase(cleanMonth)) {
            q.append(" and LOWER(month) = LOWER(?)");
            params.add(cleanMonth);
        }
        if (!cleanStatus.isEmpty() && !"All Statuses".equalsIgnoreCase(cleanStatus)) {
            if ("Paid Only".equalsIgnoreCase(cleanStatus) || "Paid".equalsIgnoreCase(cleanStatus)) {
                q.append(" and status = 'Paid'");
            } else if ("Not Paid Only".equalsIgnoreCase(cleanStatus) || "Not Paid".equalsIgnoreCase(cleanStatus)) {
                q.append(" and status = 'Not Paid'");
            }
        }
        q.append(" order by meter_no, FIELD(month, 'January','February','March','April','May','June','July','August','September','October','November','December')");

        int rowCount = 0;
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (PreparedStatement ps = c.c.prepareStatement(q.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        ps.setString(i + 1, params.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        table.setModel(DbUtils.resultSetToTableModel(rs));
                        rowCount = table.getRowCount();

                        if (lblStatus != null) {
                            lblStatus.setText("Records found: " + rowCount);
                        }

                        if (rowCount == 0 && notifyIfEmpty) {
                            String searchedMeter = (!cleanMeter.isEmpty() && !"All Meters".equalsIgnoreCase(cleanMeter)) ? cleanMeter : "All";
                            String searchedMonth = (!cleanMonth.isEmpty() && !"All Months".equalsIgnoreCase(cleanMonth)) ? cleanMonth : "All";
                            JOptionPane.showMessageDialog(this,
                                "No billing records found matching your search criteria.\n" +
                                "Meter: " + searchedMeter + "\n" +
                                "Month: " + searchedMonth,
                                "No Records Found",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error querying bills: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return rowCount;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == search) {
            Object selMeter = meternumber.getSelectedItem();
            Object selMonth = cmonth.getSelectedItem();
            Object selStatus = (cstatus != null) ? cstatus.getSelectedItem() : "All Statuses";
            String m = (selMeter != null) ? selMeter.toString().trim() : "";
            String mo = (selMonth != null) ? selMonth.toString().trim() : "";
            String st = (selStatus != null) ? selStatus.toString().trim() : "";
            loadBills(m, mo, st, true);
        } else if (ae.getSource() == reset) {
            meternumber.setSelectedIndex(0);
            cmonth.setSelectedIndex(0);
            if (cstatus != null) cstatus.setSelectedIndex(0);
            loadBills("All Meters", "All Months", "All Statuses", false);
        } else if (ae.getSource() == viewReceipt) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this,
                    "Please select a Paid record from the table to view its payment receipt.",
                    "Select Record",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int statusCol = -1;
            int billIdCol = 0;
            for (int i = 0; i < table.getColumnCount(); i++) {
                if ("Status".equalsIgnoreCase(table.getColumnName(i))) {
                    statusCol = i;
                }
                if ("Bill ID".equalsIgnoreCase(table.getColumnName(i))) {
                    billIdCol = i;
                }
            }
            String selectedStatus = (statusCol != -1) ? String.valueOf(table.getValueAt(selectedRow, statusCol)) : "";

            if (!"Paid".equalsIgnoreCase(selectedStatus)) {
                JOptionPane.showMessageDialog(this,
                    "Receipt is only available for Paid bills.\nCurrent status: " + selectedStatus,
                    "Not Paid",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                long selectedBillId = Long.parseLong(String.valueOf(table.getValueAt(selectedRow, billIdCol)));
                PaymentReceiptDialog receiptDialog = new PaymentReceiptDialog(this, selectedBillId);
                receiptDialog.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error loading receipt for selected bill: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        } else if (ae.getSource() == print) {
            try {
                MessageFormat header = new MessageFormat("Electricity Billing System - Deposit Details Statement");
                MessageFormat footer = new MessageFormat("Page {0,number,integer}");
                table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Print Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (ae.getSource() == close) {
            dispose();
        }
    }

    public static void main(String[] args) {
        new DepositDetails();
    }
}
