package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ComplaintManagement extends JFrame implements ActionListener {

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField tfSearch;
    private JComboBox<String> cbCategoryFilter;
    private JComboBox<String> cbStatusFilter;
    private JComboBox<String> cbPriorityFilter;
    private JButton btnSearch, btnReset;
    private JButton btnViewDetails, btnAssign, btnChangeStatus, btnChangePriority, btnAddRemarks, btnViewHistory, btnClose;
    private JLabel lblCount;

    public ComplaintManagement() {
        super("Electricity Billing System - Administrator Complaint Management");
        setSize(1240, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Top Filter Bar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(new Color(248, 250, 252));
        topContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        // Title row
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        titlePanel.setOpaque(false);
        JLabel titleLbl = new JLabel("Customer Grievances & Complaints Management Hub");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(new Color(15, 23, 42));
        titlePanel.add(titleLbl);
        topContainer.add(titlePanel);

        // Controls row
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("Search Ref / Meter:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterPanel.add(lblSearch);

        tfSearch = new JTextField(12);
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfSearch.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        filterPanel.add(tfSearch);

        JLabel lblCat = new JLabel("Category:");
        lblCat.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterPanel.add(lblCat);

        List<String> catOptions = new ArrayList<>();
        catOptions.add("All Categories");
        for (String c : ComplaintService.CATEGORIES) catOptions.add(c);
        cbCategoryFilter = new JComboBox<>(catOptions.toArray(new String[0]));
        cbCategoryFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbCategoryFilter.setBackground(Color.WHITE);
        filterPanel.add(cbCategoryFilter);

        JLabel lblStatus = new JLabel("Status:");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterPanel.add(lblStatus);

        cbStatusFilter = new JComboBox<>(new String[]{"All Statuses", "Pending", "In Progress", "Resolved", "Rejected"});
        cbStatusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbStatusFilter.setBackground(Color.WHITE);
        filterPanel.add(cbStatusFilter);

        JLabel lblPri = new JLabel("Priority:");
        lblPri.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterPanel.add(lblPri);

        cbPriorityFilter = new JComboBox<>(new String[]{"All Priorities", "Low", "Medium", "High", "Urgent"});
        cbPriorityFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbPriorityFilter.setBackground(Color.WHITE);
        filterPanel.add(cbPriorityFilter);

        btnSearch = new JButton("Filter");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setBackground(new Color(37, 99, 235));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon searchIcon = Project.getScaledIcon("icon/icons8-search-50.png", 14, Color.WHITE);
        if (searchIcon != null) {
            btnSearch.setIcon(searchIcon);
            btnSearch.setIconTextGap(6);
        }
        btnSearch.addActionListener(this);
        filterPanel.add(btnSearch);

        btnReset = new JButton("Show All");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReset.setBackground(new Color(241, 245, 249));
        btnReset.setForeground(new Color(51, 65, 85));
        btnReset.setFocusPainted(false);
        btnReset.addActionListener(this);
        filterPanel.add(btnReset);

        lblCount = new JLabel("Loading complaints...");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(100, 116, 139));
        filterPanel.add(lblCount);

        topContainer.add(filterPanel);
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        String[] columns = {
            "Complaint ID", "Reference Number", "Customer Name", "Meter Number", 
            "Category", "Subject", "Submission Date", "Priority", "Status", "Assigned Administrator"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 232, 240));

        // Custom Cell Renderer for Priority and Status Badges
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(new Color(15, 23, 42));

                    // Priority (Column 7): Low: Green, Medium: Blue, High: Orange, Urgent: Red
                    if (col == 7 && value != null) {
                        String pri = value.toString();
                        if ("Low".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(22, 163, 74));
                        } else if ("Medium".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(37, 99, 235));
                        } else if ("High".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(234, 88, 12));
                        } else if ("Urgent".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(220, 38, 38));
                        }
                        setFont(getFont().deriveFont(Font.BOLD));
                    }

                    // Status (Column 8)
                    if (col == 8 && value != null) {
                        String st = value.toString();
                        if ("Pending".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(217, 119, 6));
                        } else if ("In Progress".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(2, 132, 199));
                        } else if ("Resolved".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(5, 150, 105));
                        } else if ("Rejected".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(220, 38, 38));
                        }
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottomPanel.setBackground(new Color(248, 250, 252));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        btnViewDetails = createActionButton("View Details", new Color(37, 99, 235));
        btnAssign = createActionButton("Assign Admin", new Color(79, 70, 229));
        btnChangeStatus = createActionButton("Update Status", new Color(13, 148, 136));
        btnChangePriority = createActionButton("Change Priority", new Color(217, 119, 6));
        btnAddRemarks = createActionButton("Add Remarks", new Color(71, 85, 105));
        btnViewHistory = createActionButton("View History", new Color(15, 23, 42));
        btnClose = createActionButton("Close", new Color(51, 65, 85));

        bottomPanel.add(btnViewDetails);
        bottomPanel.add(btnAssign);
        bottomPanel.add(btnChangeStatus);
        bottomPanel.add(btnChangePriority);
        bottomPanel.add(btnAddRemarks);
        bottomPanel.add(btnViewHistory);
        bottomPanel.add(btnClose);

        add(bottomPanel, BorderLayout.SOUTH);

        loadComplaints("", "All Categories", "All Statuses", "All Priorities");

        setVisible(true);
    }

    private JButton createActionButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(this);
        return btn;
    }

    public int loadComplaints(String searchFilter, String catFilter, String statusFilter, String priFilter) {
        tableModel.setRowCount(0);
        int count = 0;

        StringBuilder sql = new StringBuilder(
            "SELECT c.complaint_id, c.reference_number, cust.name AS customer_name, c.meter_no, " +
            "c.category, c.subject, c.created_at, c.priority, c.status, c.assigned_admin " +
            "FROM complaints c " +
            "LEFT JOIN customer cust ON c.meter_no = cust.meter_no " +
            "WHERE 1=1"
        );

        String cleanSearch = (searchFilter != null) ? searchFilter.trim() : "";
        String cleanCat = (catFilter != null) ? catFilter.trim() : "All Categories";
        String cleanStatus = (statusFilter != null) ? statusFilter.trim() : "All Statuses";
        String cleanPri = (priFilter != null) ? priFilter.trim() : "All Priorities";

        if (!cleanSearch.isEmpty()) {
            sql.append(" AND (LOWER(c.reference_number) LIKE LOWER(?) OR LOWER(c.meter_no) LIKE LOWER(?) OR LOWER(cust.name) LIKE LOWER(?))");
        }
        if (!cleanCat.equalsIgnoreCase("All Categories")) {
            sql.append(" AND c.category = ?");
        }
        if (!cleanStatus.equalsIgnoreCase("All Statuses")) {
            sql.append(" AND c.status = ?");
        }
        if (!cleanPri.equalsIgnoreCase("All Priorities")) {
            sql.append(" AND c.priority = ?");
        }
        sql.append(" ORDER BY c.complaint_id DESC");

        try (Conn conn = new Conn()) {
            if (conn.c != null) {
                try (PreparedStatement ps = conn.c.prepareStatement(sql.toString())) {
                    int pIndex = 1;
                    if (!cleanSearch.isEmpty()) {
                        ps.setString(pIndex++, "%" + cleanSearch + "%");
                        ps.setString(pIndex++, "%" + cleanSearch + "%");
                        ps.setString(pIndex++, "%" + cleanSearch + "%");
                    }
                    if (!cleanCat.equalsIgnoreCase("All Categories")) {
                        ps.setString(pIndex++, cleanCat);
                    }
                    if (!cleanStatus.equalsIgnoreCase("All Statuses")) {
                        ps.setString(pIndex++, cleanStatus);
                    }
                    if (!cleanPri.equalsIgnoreCase("All Priorities")) {
                        ps.setString(pIndex++, cleanPri);
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getInt("complaint_id"));
                            row.add(rs.getString("reference_number"));
                            String cname = rs.getString("customer_name");
                            row.add(cname != null ? cname : "N/A");
                            row.add(rs.getString("meter_no"));
                            row.add(rs.getString("category"));
                            row.add(rs.getString("subject"));
                            row.add(rs.getString("created_at"));
                            row.add(rs.getString("priority"));
                            row.add(rs.getString("status"));
                            String admin = rs.getString("assigned_admin");
                            row.add(admin != null ? admin : "Unassigned");
                            tableModel.addRow(row);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lblCount.setText(count + " complaint(s) found");
        return count;
    }

    private int getSelectedComplaintId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a complaint from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        return (int) tableModel.getValueAt(row, 0);
    }

    private String getSelectedReferenceNumber() {
        int row = table.getSelectedRow();
        if (row < 0) return "";
        return (String) tableModel.getValueAt(row, 1);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnSearch) {
            loadComplaints(tfSearch.getText(), 
                (String) cbCategoryFilter.getSelectedItem(), 
                (String) cbStatusFilter.getSelectedItem(), 
                (String) cbPriorityFilter.getSelectedItem());
        } else if (ae.getSource() == btnReset) {
            tfSearch.setText("");
            cbCategoryFilter.setSelectedIndex(0);
            cbStatusFilter.setSelectedIndex(0);
            cbPriorityFilter.setSelectedIndex(0);
            loadComplaints("", "All Categories", "All Statuses", "All Priorities");
        } else if (ae.getSource() == btnViewDetails) {
            int cid = getSelectedComplaintId();
            if (cid > 0) openComplaintDetailDialog(cid);
        } else if (ae.getSource() == btnAssign) {
            int cid = getSelectedComplaintId();
            if (cid > 0) openAssignDialog(cid);
        } else if (ae.getSource() == btnChangeStatus) {
            int cid = getSelectedComplaintId();
            if (cid > 0) openStatusDialog(cid);
        } else if (ae.getSource() == btnChangePriority) {
            int cid = getSelectedComplaintId();
            if (cid > 0) openPriorityDialog(cid);
        } else if (ae.getSource() == btnAddRemarks) {
            int cid = getSelectedComplaintId();
            if (cid > 0) openRemarksDialog(cid);
        } else if (ae.getSource() == btnViewHistory) {
            int cid = getSelectedComplaintId();
            if (cid > 0) {
                new ComplaintHistory(cid, getSelectedReferenceNumber());
            }
        } else if (ae.getSource() == btnClose) {
            dispose();
        }
    }

    private void openComplaintDetailDialog(int complaintId) {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                String q = "SELECT c.reference_number, c.meter_no, cust.name AS customer_name, c.category, c.subject, " +
                           "c.description, c.bill_month, c.priority, c.status, c.assigned_admin, c.resolution_remarks, " +
                           "c.created_at, c.updated_at " +
                           "FROM complaints c LEFT JOIN customer cust ON c.meter_no = cust.meter_no " +
                           "WHERE c.complaint_id = ?";
                try (PreparedStatement ps = c.c.prepareStatement(q)) {
                    ps.setInt(1, complaintId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            JDialog d = new JDialog(this, "Complaint Detail - " + rs.getString("reference_number"), true);
                            d.setSize(640, 560);
                            d.setLocationRelativeTo(this);
                            d.setLayout(new BorderLayout());

                            JPanel p = new JPanel();
                            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
                            p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
                            p.setBackground(Color.WHITE);

                            p.add(makeDetailRow("Reference Number:", rs.getString("reference_number")));
                            p.add(makeDetailRow("Customer Name:", rs.getString("customer_name")));
                            p.add(makeDetailRow("Meter Number:", rs.getString("meter_no")));
                            p.add(makeDetailRow("Category:", rs.getString("category")));
                            p.add(makeDetailRow("Subject:", rs.getString("subject")));
                            String bm = rs.getString("bill_month");
                            p.add(makeDetailRow("Related Bill Month:", bm != null ? bm : "None"));
                            p.add(makeDetailRow("Submission Date:", rs.getString("created_at")));
                            p.add(makeDetailRow("Last Updated:", rs.getString("updated_at")));
                            p.add(makeDetailRow("Current Priority:", rs.getString("priority")));
                            p.add(makeDetailRow("Current Status:", rs.getString("status")));
                            String admin = rs.getString("assigned_admin");
                            p.add(makeDetailRow("Assigned Administrator:", admin != null ? admin : "Unassigned"));

                            JLabel descLbl = new JLabel("Complaint Description:");
                            descLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            p.add(descLbl);
                            p.add(Box.createVerticalStrut(4));

                            JTextArea taDesc = new JTextArea(rs.getString("description"));
                            taDesc.setEditable(false);
                            taDesc.setLineWrap(true);
                            taDesc.setWrapStyleWord(true);
                            taDesc.setBackground(new Color(248, 250, 252));
                            taDesc.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                            JScrollPane spDesc = new JScrollPane(taDesc);
                            spDesc.setPreferredSize(new Dimension(540, 75));
                            p.add(spDesc);
                            p.add(Box.createVerticalStrut(10));

                            JLabel remLbl = new JLabel("Resolution Remarks:");
                            remLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            p.add(remLbl);
                            p.add(Box.createVerticalStrut(4));

                            String remarks = rs.getString("resolution_remarks");
                            JTextArea taRem = new JTextArea(remarks != null ? remarks : "No remarks recorded.");
                            taRem.setEditable(false);
                            taRem.setLineWrap(true);
                            taRem.setWrapStyleWord(true);
                            taRem.setBackground(new Color(248, 250, 252));
                            taRem.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                            JScrollPane spRem = new JScrollPane(taRem);
                            spRem.setPreferredSize(new Dimension(540, 65));
                            p.add(spRem);

                            JScrollPane scroll = new JScrollPane(p);
                            scroll.setBorder(null);
                            d.add(scroll, BorderLayout.CENTER);

                            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
                            btnPanel.setBackground(new Color(248, 250, 252));
                            JButton okBtn = new JButton("Close");
                            okBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                            okBtn.setBackground(new Color(15, 23, 42));
                            okBtn.setForeground(Color.WHITE);
                            okBtn.addActionListener(e -> d.dispose());
                            btnPanel.add(okBtn);
                            d.add(btnPanel, BorderLayout.SOUTH);

                            d.setVisible(true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openAssignDialog(int complaintId) {
        List<String> adminList = new ArrayList<>();
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement st = c.c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT DISTINCT username FROM login WHERE user = 'Admin' ORDER BY username")) {
                    while (rs.next()) {
                        adminList.add(rs.getString("username"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (adminList.isEmpty()) {
            adminList.add("Admin");
        }

        JComboBox<String> cbAdmins = new JComboBox<>(adminList.toArray(new String[0]));
        JTextField tfRemarks = new JTextField("Assigned for investigation");

        Object[] msg = {
            "Select Administrator to Assign:", cbAdmins,
            "Assignment Remarks (Optional):", tfRemarks
        };

        int option = JOptionPane.showConfirmDialog(this, msg, "Assign Administrator", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String selectedAdmin = (String) cbAdmins.getSelectedItem();
            ComplaintService.ServiceResult res = ComplaintService.assignComplaint(
                complaintId, selectedAdmin, "Admin", tfRemarks.getText()
            );

            if (res.success) {
                JOptionPane.showMessageDialog(this, res.message, "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshCurrentView();
            } else {
                JOptionPane.showMessageDialog(this, res.message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openStatusDialog(int complaintId) {
        String currentStatus = "";
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (PreparedStatement ps = c.c.prepareStatement("SELECT status FROM complaints WHERE complaint_id = ?")) {
                    ps.setInt(1, complaintId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) currentStatus = rs.getString("status");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JComboBox<String> cbNewStatus = new JComboBox<>(ComplaintService.STATUSES);
        cbNewStatus.setSelectedItem(currentStatus);

        JTextArea taRemarks = new JTextArea(4, 25);
        taRemarks.setLineWrap(true);
        taRemarks.setWrapStyleWord(true);
        JScrollPane spRemarks = new JScrollPane(taRemarks);

        JLabel infoLbl = new JLabel("<html>Current Status: <b>" + currentStatus + "</b><br>" +
            "<small>* Note: 'Resolved' & 'Rejected' require mandatory remarks.<br>" +
            "* Reopening a resolved/rejected complaint requires an explanation.</small></html>");

        Object[] msg = {
            infoLbl,
            "Select New Status:", cbNewStatus,
            "Resolution / Action Remarks:*", spRemarks
        };

        int option = JOptionPane.showConfirmDialog(this, msg, "Update Complaint Status", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String newStatus = (String) cbNewStatus.getSelectedItem();
            String remarks = taRemarks.getText() != null ? taRemarks.getText().trim() : "";

            // Confirm prompt to prevent accidental status changes
            int confirm = JOptionPane.showConfirmDialog(
                this, 
                "Are you sure you want to transition complaint status from '" + currentStatus + "' to '" + newStatus + "'?",
                "Confirm Status Change", 
                JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            ComplaintService.ServiceResult res = ComplaintService.updateStatus(complaintId, newStatus, "Admin", remarks);
            if (res.success) {
                JOptionPane.showMessageDialog(this, res.message, "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                refreshCurrentView();
            } else {
                JOptionPane.showMessageDialog(this, res.message, "Transition Blocked", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openPriorityDialog(int complaintId) {
        JComboBox<String> cbPriority = new JComboBox<>(ComplaintService.PRIORITIES);
        JTextField tfRemarks = new JTextField();

        Object[] msg = {
            "Select Priority Level:", cbPriority,
            "Reason / Remarks (Optional):", tfRemarks
        };

        int option = JOptionPane.showConfirmDialog(this, msg, "Change Priority", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String newPri = (String) cbPriority.getSelectedItem();
            ComplaintService.ServiceResult res = ComplaintService.updatePriority(complaintId, newPri, "Admin", tfRemarks.getText());
            if (res.success) {
                JOptionPane.showMessageDialog(this, res.message, "Priority Changed", JOptionPane.INFORMATION_MESSAGE);
                refreshCurrentView();
            } else {
                JOptionPane.showMessageDialog(this, res.message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openRemarksDialog(int complaintId) {
        JTextArea taRemarks = new JTextArea(4, 25);
        taRemarks.setLineWrap(true);
        taRemarks.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(taRemarks);

        Object[] msg = {
            "Enter Additional Resolution Remarks:", sp
        };

        int option = JOptionPane.showConfirmDialog(this, msg, "Add Resolution Remarks", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String remarks = taRemarks.getText() != null ? taRemarks.getText().trim() : "";
            if (remarks.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Remarks cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ComplaintService.ServiceResult res = ComplaintService.addRemarks(complaintId, remarks, "Admin");
            if (res.success) {
                JOptionPane.showMessageDialog(this, res.message, "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshCurrentView();
            } else {
                JOptionPane.showMessageDialog(this, res.message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refreshCurrentView() {
        loadComplaints(tfSearch.getText(), 
            (String) cbCategoryFilter.getSelectedItem(), 
            (String) cbStatusFilter.getSelectedItem(), 
            (String) cbPriorityFilter.getSelectedItem());
    }

    private JPanel makeDetailRow(String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setPreferredSize(new Dimension(170, 20));

        JLabel v = new JLabel(value != null ? value : "-");
        v.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        row.add(l);
        row.add(v);
        return row;
    }

    public static void main(String[] args) {
        new ComplaintManagement();
    }
}
