package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class BillDetails extends JFrame implements ActionListener {

    JButton search, reset, print, close;
    JComboBox<String> cmonth, meternumber;
    JTable table;
    JLabel lblStatus;
    String meter;

    public BillDetails(String meter) {
        this.meter = (meter != null) ? meter.trim() : "";
        setTitle("Electricity Billing System - Bill History " + (this.meter.isEmpty() ? "(All Meters)" : "(Meter: " + this.meter + ")"));
        setSize(860, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Top filter bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        topPanel.setBackground(new Color(248, 250, 252)); // Light slate #F8FAFC
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        // Meter filter
        if (this.meter.isEmpty()) {
            JLabel lblMeter = new JLabel("Search Meter:");
            lblMeter.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblMeter.setForeground(new Color(51, 65, 85));
            topPanel.add(lblMeter);

            meternumber = new JComboBox<>();
            meternumber.setEditable(true); // Allows selecting or typing arbitrary meters for search
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
        } else {
            JLabel lblCustMeter = new JLabel("Account Meter: " + this.meter);
            lblCustMeter.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblCustMeter.setForeground(new Color(37, 99, 235));
            topPanel.add(lblCustMeter);
        }

        // Month filter
        JLabel lblMonth = new JLabel("Search Month:");
        lblMonth.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMonth.setForeground(new Color(51, 65, 85));
        topPanel.add(lblMonth);

        String[] months = {
            "All Months", "January", "February", "March", "April",
            "May", "June", "July", "August", "September",
            "October", "November", "December"
        };
        cmonth = new JComboBox<>(months);
        cmonth.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topPanel.add(cmonth);

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

        print = new JButton("Print Records");
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

        close = new JButton("Close");
        close.setBackground(new Color(71, 85, 105)); // Slate 600
        close.setForeground(Color.WHITE);
        close.setFont(new Font("Segoe UI", Font.BOLD, 12));
        close.setFocusPainted(false);
        close.setCursor(new Cursor(Cursor.HAND_CURSOR));
        close.addActionListener(this);

        bottomPanel.add(print);
        bottomPanel.add(close);
        add(bottomPanel, BorderLayout.SOUTH);

        // Initial Load
        loadBills(this.meter, "All Months", false);

        setVisible(true);
    }

    /**
     * Backward-compatible loadBills signature (month only, using instance meter).
     */
    public int loadBills(String month, boolean notifyIfEmpty) {
        return loadBills(this.meter, month, notifyIfEmpty);
    }

    /**
     * Full search and query implementation:
     * - Filters by meter number (or All Meters)
     * - Filters by month (case-insensitive, or All Months)
     * - Chronological month ordering: January, February, March...
     * - Parameterized PreparedStatement protection
     */
    public int loadBills(String targetMeter, String month, boolean notifyIfEmpty) {
        StringBuilder query = new StringBuilder(
            "select bill_id as 'Bill ID', meter_no as 'Meter No', month as 'Billing Month', units as 'Units Consumed', " +
            "concat('₹', total_bill) as 'Total Bill', due_date as 'Due Date', status as 'Status' from bill where 1=1"
        );
        List<String> params = new ArrayList<>();

        String cleanMeter = (targetMeter != null) ? targetMeter.trim() : "";
        if (!cleanMeter.isEmpty() && !"All Meters".equalsIgnoreCase(cleanMeter)) {
            query.append(" and meter_no = ?");
            params.add(cleanMeter);
        }

        String cleanMonth = (month != null) ? month.trim() : "";
        if (!cleanMonth.isEmpty() && !"All Months".equalsIgnoreCase(cleanMonth)) {
            query.append(" and LOWER(month) = LOWER(?)");
            params.add(cleanMonth);
        }

        query.append(" order by meter_no, FIELD(month, 'January','February','March','April','May','June','July','August','September','October','November','December')");

        int rowCount = 0;
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (PreparedStatement ps = c.c.prepareStatement(query.toString())) {
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
                                "No billing records found matching criteria.\nMeter: " + searchedMeter + "\nMonth: " + searchedMonth,
                                "No Records Found",
                                JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error querying bill history: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return rowCount;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == search) {
            String m = this.meter;
            if (m.isEmpty() && meternumber != null) {
                Object sel = meternumber.getSelectedItem();
                m = (sel != null) ? sel.toString().trim() : "";
            }
            Object selMonth = cmonth.getSelectedItem();
            String mo = (selMonth != null) ? selMonth.toString().trim() : "";
            loadBills(m, mo, true);
        } else if (ae.getSource() == reset) {
            cmonth.setSelectedIndex(0);
            if (meternumber != null) {
                meternumber.setSelectedIndex(0);
            }
            loadBills(this.meter, "All Months", false);
        } else if (ae.getSource() == print) {
            try {
                MessageFormat header = new MessageFormat("Electricity Billing System - Bill History Statement");
                MessageFormat footer = new MessageFormat("Page {0,number,integer}");
                table.print(JTable.PrintMode.FIT_WIDTH, header, footer);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Print Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            dispose();
        }
    }

    public static void main(String[] args) {
        new BillDetails("");
    }
}
