package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

public class MyComplaints extends JFrame implements ActionListener {

    private final String meter;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField tfSearchRef;
    private JComboBox<String> cbStatusFilter;
    private JButton btnSearch, btnReset, btnViewDetails, btnClose;
    private JLabel lblCount;

    public MyComplaints(String meter) {
        this.meter = (meter != null) ? meter.trim() : "";
        setTitle("Electricity Billing System - My Complaints (Meter: " + this.meter + ")");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header and Filter Panel
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(new Color(248, 250, 252));
        topContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        // Title Row
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        titlePanel.setOpaque(false);
        JLabel titleLbl = new JLabel("My Complaints & Grievance Tracking");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(new Color(15, 23, 42));
        titlePanel.add(titleLbl);

        JLabel meterTag = new JLabel("Account: " + this.meter);
        meterTag.setFont(new Font("Segoe UI", Font.BOLD, 12));
        meterTag.setForeground(new Color(37, 99, 235));
        titlePanel.add(meterTag);

        topContainer.add(titlePanel);

        // Filter Controls Row
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        filterPanel.setOpaque(false);

        JLabel lblRef = new JLabel("Search Reference:");
        lblRef.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRef.setForeground(new Color(51, 65, 85));
        filterPanel.add(lblRef);

        tfSearchRef = new JTextField(12);
        tfSearchRef.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfSearchRef.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        filterPanel.add(tfSearchRef);

        JLabel lblStatus = new JLabel("Status:");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(new Color(51, 65, 85));
        filterPanel.add(lblStatus);

        cbStatusFilter = new JComboBox<>(new String[]{"All Statuses", "Pending", "In Progress", "Resolved", "Rejected"});
        cbStatusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbStatusFilter.setBackground(Color.WHITE);
        filterPanel.add(cbStatusFilter);

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

        // Center Complaints Table
        String[] columns = {
            "ID", "Reference No", "Category", "Subject", "Date Submitted", 
            "Priority", "Status", "Assigned Admin", "Resolution Remarks"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Strictly read-only for customer
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

        // Hide Internal ID column from customer view (column 0)
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Custom Cell Renderer for Status and Priority Badges
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(new Color(15, 23, 42));

                    // Priority formatting (Column 5)
                    if (col == 5 && value != null) {
                        String pri = value.toString();
                        if ("Low".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(22, 163, 74)); // Green
                        } else if ("Medium".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(37, 99, 235)); // Blue
                        } else if ("High".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(234, 88, 12)); // Orange
                        } else if ("Urgent".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(220, 38, 38)); // Red
                        }
                        setFont(getFont().deriveFont(Font.BOLD));
                    }

                    // Status formatting (Column 6)
                    if (col == 6 && value != null) {
                        String st = value.toString();
                        if ("Pending".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(217, 119, 6)); // Amber
                        } else if ("In Progress".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(2, 132, 199)); // Royal Blue
                        } else if ("Resolved".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(5, 150, 105)); // Green
                        } else if ("Rejected".equalsIgnoreCase(st)) {
                            c.setForeground(new Color(220, 38, 38)); // Red
                        }
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Action Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        bottomPanel.setBackground(new Color(248, 250, 252));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        btnViewDetails = new JButton("View Complaint Details");
        btnViewDetails.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewDetails.setBackground(new Color(37, 99, 235));
        btnViewDetails.setForeground(Color.WHITE);
        btnViewDetails.setFocusPainted(false);
        btnViewDetails.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewDetails.addActionListener(this);

        btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setBackground(new Color(15, 23, 42));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(this);

        bottomPanel.add(btnViewDetails);
        bottomPanel.add(btnClose);
        add(bottomPanel, BorderLayout.SOUTH);

        loadCustomerComplaints("", "All Statuses");

        setVisible(true);
    }

    public int loadCustomerComplaints(String refFilter, String statusFilter) {
        tableModel.setRowCount(0);
        int count = 0;

        StringBuilder sql = new StringBuilder(
            "SELECT complaint_id, reference_number, category, subject, created_at, priority, status, assigned_admin, resolution_remarks " +
            "FROM complaints WHERE meter_no = ?"
        );

        String cleanRef = (refFilter != null) ? refFilter.trim() : "";
        String cleanStatus = (statusFilter != null) ? statusFilter.trim() : "All Statuses";

        if (!cleanRef.isEmpty()) {
            sql.append(" AND LOWER(reference_number) LIKE LOWER(?)");
        }
        if (!cleanStatus.equalsIgnoreCase("All Statuses")) {
            sql.append(" AND status = ?");
        }
        sql.append(" ORDER BY created_at DESC");

        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (PreparedStatement ps = c.c.prepareStatement(sql.toString())) {
                    int pIndex = 1;
                    ps.setString(pIndex++, this.meter);

                    if (!cleanRef.isEmpty()) {
                        ps.setString(pIndex++, "%" + cleanRef + "%");
                    }
                    if (!cleanStatus.equalsIgnoreCase("All Statuses")) {
                        ps.setString(pIndex++, cleanStatus);
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getInt("complaint_id"));
                            row.add(rs.getString("reference_number"));
                            row.add(rs.getString("category"));
                            row.add(rs.getString("subject"));
                            row.add(rs.getString("created_at"));
                            row.add(rs.getString("priority"));
                            row.add(rs.getString("status"));
                            String admin = rs.getString("assigned_admin");
                            row.add(admin != null ? admin : "Unassigned");
                            String remarks = rs.getString("resolution_remarks");
                            row.add(remarks != null ? remarks : "None");
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

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnSearch) {
            loadCustomerComplaints(tfSearchRef.getText(), (String) cbStatusFilter.getSelectedItem());
        } else if (ae.getSource() == btnReset) {
            tfSearchRef.setText("");
            cbStatusFilter.setSelectedIndex(0);
            loadCustomerComplaints("", "All Statuses");
        } else if (ae.getSource() == btnViewDetails) {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Please select a complaint from the table to view details.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int complaintId = (int) tableModel.getValueAt(selectedRow, 0);
            showComplaintDetails(complaintId);
        } else if (ae.getSource() == btnClose) {
            dispose();
        }
    }

    private void showComplaintDetails(int complaintId) {
        try (Conn c = new Conn()) {
            if (c.c != null) {
                String q = "SELECT reference_number, category, subject, description, bill_month, priority, status, assigned_admin, resolution_remarks, created_at, updated_at " +
                           "FROM complaints WHERE complaint_id = ? AND meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(q)) {
                    ps.setInt(1, complaintId);
                    ps.setString(2, this.meter);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            JDialog detailDialog = new JDialog(this, "Complaint Details - " + rs.getString("reference_number"), true);
                            detailDialog.setSize(600, 520);
                            detailDialog.setLocationRelativeTo(this);
                            detailDialog.setLayout(new BorderLayout());

                            JPanel p = new JPanel();
                            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
                            p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
                            p.setBackground(Color.WHITE);

                            p.add(makeDetailRow("Reference Number:", rs.getString("reference_number")));
                            p.add(makeDetailRow("Category:", rs.getString("category")));
                            p.add(makeDetailRow("Subject:", rs.getString("subject")));
                            String bm = rs.getString("bill_month");
                            p.add(makeDetailRow("Related Bill Month:", bm != null ? bm : "N/A"));
                            p.add(makeDetailRow("Submission Date:", rs.getString("created_at")));
                            p.add(makeDetailRow("Priority:", rs.getString("priority")));
                            p.add(makeDetailRow("Current Status:", rs.getString("status")));
                            String admin = rs.getString("assigned_admin");
                            p.add(makeDetailRow("Assigned Administrator:", admin != null ? admin : "Unassigned"));

                            JLabel descLbl = new JLabel("Description:");
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
                            spDesc.setPreferredSize(new Dimension(500, 70));
                            p.add(spDesc);
                            p.add(Box.createVerticalStrut(10));

                            JLabel remLbl = new JLabel("Administrator Resolution Remarks:");
                            remLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            p.add(remLbl);
                            p.add(Box.createVerticalStrut(4));

                            String remarks = rs.getString("resolution_remarks");
                            JTextArea taRem = new JTextArea(remarks != null ? remarks : "No administrator remarks added yet.");
                            taRem.setEditable(false);
                            taRem.setLineWrap(true);
                            taRem.setWrapStyleWord(true);
                            taRem.setBackground(new Color(248, 250, 252));
                            taRem.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
                            JScrollPane spRem = new JScrollPane(taRem);
                            spRem.setPreferredSize(new Dimension(500, 60));
                            p.add(spRem);

                            JScrollPane scroll = new JScrollPane(p);
                            scroll.setBorder(null);
                            detailDialog.add(scroll, BorderLayout.CENTER);

                            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
                            btnPanel.setBackground(new Color(248, 250, 252));
                            JButton okBtn = new JButton("Close");
                            okBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                            okBtn.setBackground(new Color(15, 23, 42));
                            okBtn.setForeground(Color.WHITE);
                            okBtn.addActionListener(e -> detailDialog.dispose());
                            btnPanel.add(okBtn);
                            detailDialog.add(btnPanel, BorderLayout.SOUTH);

                            detailDialog.setVisible(true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        new MyComplaints("MET001");
    }
}
