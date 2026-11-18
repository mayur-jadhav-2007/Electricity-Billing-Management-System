package electricity.billing.system;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.sql.*;
import java.awt.event.*;

public class CustomerDetails extends JFrame implements ActionListener {

    JTable table;
    JButton print, close, deactivate, reactivate, filterBtn, resetBtn;
    JTextField tfSearch;
    JLabel lblCount;
    
    CustomerDetails() {
        super("Electricity Billing System - Customer Details & Management");
        
        setSize(1200, 680);
        setLocation(180, 120);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Top search / filter bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        
        JLabel lblSearch = new JLabel("Filter Customer (Name or Meter):");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topPanel.add(lblSearch);
        
        tfSearch = new JTextField(16);
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topPanel.add(tfSearch);
        
        filterBtn = new JButton("Filter");
        filterBtn.setBackground(new Color(41, 128, 185));
        filterBtn.setForeground(Color.WHITE);
        filterBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        ImageIcon searchIcon = Project.getScaledIcon("icon/icons8-search-50.png", 16, Color.WHITE);
        if (searchIcon != null) {
            filterBtn.setIcon(searchIcon);
            filterBtn.setIconTextGap(6);
        }
        filterBtn.addActionListener(this);
        topPanel.add(filterBtn);
        
        resetBtn = new JButton("Show All");
        resetBtn.setBackground(new Color(52, 73, 94));
        resetBtn.setForeground(Color.WHITE);
        resetBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resetBtn.addActionListener(this);
        topPanel.add(resetBtn);
        
        lblCount = new JLabel("Loading customers...");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(100, 100, 100));
        topPanel.add(lblCount);
        
        add(topPanel, BorderLayout.NORTH);
        
        table = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane sp = new JScrollPane(table);
        add(sp, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        
        deactivate = new JButton("Deactivate Customer");
        deactivate.setBackground(new Color(231, 76, 60));
        deactivate.setForeground(Color.WHITE);
        deactivate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deactivate.setToolTipText("Deactivate selected customer (prevents login without deleting data)");
        ImageIcon inactiveIcon = Project.getScaledIcon("icon/Inactive Customers.png", 16, Color.WHITE);
        if (inactiveIcon != null) {
            deactivate.setIcon(inactiveIcon);
            deactivate.setIconTextGap(6);
        }
        deactivate.addActionListener(this);
        
        reactivate = new JButton("Reactivate Customer");
        reactivate.setBackground(new Color(39, 174, 96));
        reactivate.setForeground(Color.WHITE);
        reactivate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reactivate.setToolTipText("Reactivate selected customer (restores login access)");
        ImageIcon activeIcon = Project.getScaledIcon("icon/Active Customers.png", 16, Color.WHITE);
        if (activeIcon != null) {
            reactivate.setIcon(activeIcon);
            reactivate.setIconTextGap(6);
        }
        reactivate.addActionListener(this);
        
        print = new JButton("Print Records");
        print.setBackground(new Color(41, 128, 185));
        print.setForeground(Color.WHITE);
        print.setFont(new Font("Segoe UI", Font.BOLD, 12));
        ImageIcon printIcon = Project.getScaledIcon("icon/icons8-receipt-48.png", 16, Color.WHITE);
        if (printIcon != null) {
            print.setIcon(printIcon);
            print.setIconTextGap(6);
        }
        print.addActionListener(this);
        
        close = new JButton("Close");
        close.setBackground(Color.BLACK);
        close.setForeground(Color.WHITE);
        close.setFont(new Font("Segoe UI", Font.BOLD, 12));
        close.addActionListener(this);
        
        buttonPanel.add(deactivate);
        buttonPanel.add(reactivate);
        buttonPanel.add(print);
        buttonPanel.add(close);
        add(buttonPanel, BorderLayout.SOUTH);
        
        loadCustomers("");
        
        setVisible(true);
    }
    
    public int loadCustomers(String filter) {
        StringBuilder q = new StringBuilder("select meter_no as 'Meter No', name as 'Customer Name', address as 'Address', city as 'City', state as 'State', email as 'Email', phone as 'Phone', status as 'Status' from customer where 1=1");
        String cleanFilter = (filter != null) ? filter.trim() : "";
        
        if (!cleanFilter.isEmpty()) {
            q.append(" and (LOWER(meter_no) LIKE LOWER(?) OR LOWER(name) LIKE LOWER(?))");
        }
        q.append(" order by meter_no");
        
        int count = 0;
        Conn c = null;
        try {
            c = new Conn();
            if (c.c != null) {
                try (PreparedStatement ps = c.c.prepareStatement(q.toString())) {
                    if (!cleanFilter.isEmpty()) {
                        String pattern = "%" + cleanFilter + "%";
                        ps.setString(1, pattern);
                        ps.setString(2, pattern);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        table.setModel(DbUtils.resultSetToTableModel(rs));
                        count = table.getRowCount();
                        if (lblCount != null) {
                            lblCount.setText("Customers found: " + count);
                        }
                        
                        // Custom renderer to clearly highlight Active vs Inactive status
                        if (table.getColumnCount() >= 8) {
                            table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
                                @Override
                                public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                                    Component comp = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                                    if (value != null) {
                                        String val = value.toString();
                                        if ("Active".equalsIgnoreCase(val)) {
                                            comp.setForeground(new Color(39, 174, 96)); // Green
                                            setFont(getFont().deriveFont(Font.BOLD));
                                        } else if ("Inactive".equalsIgnoreCase(val)) {
                                            comp.setForeground(new Color(231, 76, 60)); // Red
                                            setFont(getFont().deriveFont(Font.BOLD));
                                        } else {
                                            comp.setForeground(Color.BLACK);
                                        }
                                    }
                                    return comp;
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error querying customer records: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (c != null) {
                if (c.s != null) try { c.s.close(); } catch (Exception ignored) {}
                if (c.c != null) try { c.c.close(); } catch (Exception ignored) {}
            }
        }
        return count;
    }
    
    public static boolean setCustomerStatus(String meterNo, String newStatus) {
        if (meterNo == null || meterNo.trim().isEmpty() || newStatus == null) return false;
        Conn c = null;
        try {
            c = new Conn();
            if (c.c != null) {
                c.c.setAutoCommit(false);
                
                // Read current status for old_value
                String oldStatus = "Unknown";
                String selectQ = "select status from customer where meter_no = ?";
                try (PreparedStatement selPs = c.c.prepareStatement(selectQ)) {
                    selPs.setString(1, meterNo.trim());
                    try (ResultSet rs = selPs.executeQuery()) {
                        if (rs.next()) {
                            oldStatus = rs.getString("status");
                        }
                    }
                }
                
                String q = "update customer set status = ? where meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(q)) {
                    ps.setString(1, newStatus);
                    ps.setString(2, meterNo.trim());
                    int updated = ps.executeUpdate();
                    if (updated > 0) {
                        String action = "Active".equalsIgnoreCase(newStatus) ?
                                AuditService.ACTION_CUSTOMER_REACTIVATED :
                                AuditService.ACTION_CUSTOMER_DEACTIVATED;
                        String actorId = UserSession.getActorId();
                        String actorRole = UserSession.getActorRole();
                        AuditService.logEvent(c.c, actorId, actorRole, action,
                            AuditService.ENTITY_CUSTOMER, meterNo.trim(),
                            "Customer status changed to " + newStatus, oldStatus, newStatus);
                        c.c.commit();
                        return true;
                    } else {
                        c.c.rollback();
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            if (c != null && c.c != null) {
                try { c.c.rollback(); } catch (Exception ignored) {}
            }
            e.printStackTrace();
        } finally {
            if (c != null) {
                if (c.c != null) try { c.c.setAutoCommit(true); } catch (Exception ignored) {}
                if (c.s != null) try { c.s.close(); } catch (Exception ignored) {}
                if (c.c != null) try { c.c.close(); } catch (Exception ignored) {}
            }
        }
        return false;
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == filterBtn) {
            loadCustomers(tfSearch.getText());
        } else if (ae.getSource() == resetBtn) {
            tfSearch.setText("");
            loadCustomers("");
        } else if (ae.getSource() == deactivate) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a customer from the table to deactivate.", "No Customer Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String meterNo = table.getValueAt(selectedRow, 0).toString();
            String custName = table.getValueAt(selectedRow, 1).toString();
            String currentStatus = table.getValueAt(selectedRow, 7).toString();
            
            if ("Inactive".equalsIgnoreCase(currentStatus)) {
                JOptionPane.showMessageDialog(this, "Customer " + custName + " (" + meterNo + ") is already Inactive.", "Already Inactive", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to deactivate this customer?\n\n" +
                "Customer Name : " + custName + "\n" +
                "Meter Number  : " + meterNo + "\n\n" +
                "Effects:\n" +
                "• Login access will be immediately blocked.\n" +
                "• All customer data, meter info, bills, and payment records remain preserved.\n" +
                "• Account can be reactivated at any time.",
                "Confirm Deactivation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
                
            if (opt == JOptionPane.YES_OPTION) {
                boolean ok = setCustomerStatus(meterNo, "Inactive");
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Customer " + custName + " (" + meterNo + ") has been deactivated.", "Deactivation Successful", JOptionPane.INFORMATION_MESSAGE);
                    loadCustomers(tfSearch.getText());
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to deactivate customer in database.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (ae.getSource() == reactivate) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a customer from the table to reactivate.", "No Customer Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String meterNo = table.getValueAt(selectedRow, 0).toString();
            String custName = table.getValueAt(selectedRow, 1).toString();
            String currentStatus = table.getValueAt(selectedRow, 7).toString();
            
            if ("Active".equalsIgnoreCase(currentStatus)) {
                JOptionPane.showMessageDialog(this, "Customer " + custName + " (" + meterNo + ") is already Active.", "Already Active", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reactivate this customer?\n\n" +
                "Customer Name : " + custName + "\n" +
                "Meter Number  : " + meterNo + "\n\n" +
                "Effects:\n" +
                "• Login access to the customer portal will be restored.\n" +
                "• All billing history and account settings remain preserved.",
                "Confirm Reactivation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
                
            if (opt == JOptionPane.YES_OPTION) {
                boolean ok = setCustomerStatus(meterNo, "Active");
                if (ok) {
                    JOptionPane.showMessageDialog(this, "Customer " + custName + " (" + meterNo + ") has been reactivated successfully.", "Reactivation Successful", JOptionPane.INFORMATION_MESSAGE);
                    loadCustomers(tfSearch.getText());
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to reactivate customer in database.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (ae.getSource() == print) {
            try {
                table.print();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Print Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            dispose();
        }
    }

    public static void main(String[] args) {
        new CustomerDetails();
    }
}
