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

public class ComplaintHistory extends JFrame implements ActionListener {

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField tfSearchRef;
    private JComboBox<String> cbActionFilter;
    private JButton btnSearch, btnReset, btnClose;
    private JLabel lblCount;
    private Integer initialComplaintId = null;

    public ComplaintHistory() {
        this(null, null);
    }

    public ComplaintHistory(Integer complaintId, String initialRef) {
        this.initialComplaintId = complaintId;
        setTitle("Electricity Billing System - Complaint Audit & History Trail" + 
            (initialRef != null ? " (" + initialRef + ")" : ""));
        setSize(1080, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Top Filter Bar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(new Color(248, 250, 252));
        topContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12));
        titlePanel.setOpaque(false);
        JLabel titleLbl = new JLabel("Audit & Traceability History Log");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(new Color(15, 23, 42));
        titlePanel.add(titleLbl);
        topContainer.add(titlePanel);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        filterPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("Search Ref / Meter:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(new Color(51, 65, 85));
        filterPanel.add(lblSearch);

        tfSearchRef = new JTextField(initialRef != null ? initialRef : "", 14);
        tfSearchRef.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfSearchRef.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        filterPanel.add(tfSearchRef);

        JLabel lblAction = new JLabel("Action:");
        lblAction.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAction.setForeground(new Color(51, 65, 85));
        filterPanel.add(lblAction);

        String[] actions = {
            "All Actions",
            "Complaint Submitted",
            "Status Changed",
            "Complaint Assigned",
            "Priority Changed",
            "Resolution Remarks Added",
            "Complaint Reopened",
            "Complaint Resolved",
            "Complaint Rejected"
        };
        cbActionFilter = new JComboBox<>(actions);
        cbActionFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbActionFilter.setBackground(Color.WHITE);
        filterPanel.add(cbActionFilter);

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

        lblCount = new JLabel("Loading history...");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(100, 116, 139));
        filterPanel.add(lblCount);

        topContainer.add(filterPanel);
        add(topContainer, BorderLayout.NORTH);

        // Center History Table
        String[] columns = {
            "History ID", "Ref Number", "Meter No", "Action", "Previous Value", 
            "New Value", "Performed By", "Action Remarks / Reason", "Timestamp"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 232, 240));

        // Format alternating rows
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(new Color(15, 23, 42));
                    if (col == 3) {
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        bottomPanel.setBackground(new Color(248, 250, 252));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setBackground(new Color(15, 23, 42));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(this);
        bottomPanel.add(btnClose);
        add(bottomPanel, BorderLayout.SOUTH);

        loadHistory(tfSearchRef.getText(), (String) cbActionFilter.getSelectedItem(), initialComplaintId);

        setVisible(true);
    }

    public int loadHistory(String searchFilter, String actionFilter, Integer complaintId) {
        tableModel.setRowCount(0);
        int count = 0;

        StringBuilder sql = new StringBuilder(
            "SELECT h.history_id, c.reference_number, c.meter_no, h.action, h.previous_value, h.new_value, h.performed_by, h.remarks, h.created_at " +
            "FROM complaint_history h " +
            "JOIN complaints c ON h.complaint_id = c.complaint_id " +
            "WHERE 1=1"
        );

        String cleanSearch = (searchFilter != null) ? searchFilter.trim() : "";
        String cleanAction = (actionFilter != null) ? actionFilter.trim() : "All Actions";

        if (complaintId != null && complaintId > 0) {
            sql.append(" AND h.complaint_id = ?");
        }
        if (!cleanSearch.isEmpty()) {
            sql.append(" AND (LOWER(c.reference_number) LIKE LOWER(?) OR LOWER(c.meter_no) LIKE LOWER(?))");
        }
        if (!cleanAction.equalsIgnoreCase("All Actions")) {
            sql.append(" AND h.action = ?");
        }
        sql.append(" ORDER BY h.history_id DESC");

        try (Conn conn = new Conn()) {
            if (conn.c != null) {
                try (PreparedStatement ps = conn.c.prepareStatement(sql.toString())) {
                    int pIndex = 1;
                    if (complaintId != null && complaintId > 0) {
                        ps.setInt(pIndex++, complaintId);
                    }
                    if (!cleanSearch.isEmpty()) {
                        ps.setString(pIndex++, "%" + cleanSearch + "%");
                        ps.setString(pIndex++, "%" + cleanSearch + "%");
                    }
                    if (!cleanAction.equalsIgnoreCase("All Actions")) {
                        ps.setString(pIndex++, cleanAction);
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getInt("history_id"));
                            row.add(rs.getString("reference_number"));
                            row.add(rs.getString("meter_no"));
                            row.add(rs.getString("action"));
                            String prev = rs.getString("previous_value");
                            row.add(prev != null ? prev : "-");
                            String next = rs.getString("new_value");
                            row.add(next != null ? next : "-");
                            row.add(rs.getString("performed_by"));
                            String rem = rs.getString("remarks");
                            row.add(rem != null ? rem : "");
                            row.add(rs.getString("created_at"));
                            tableModel.addRow(row);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lblCount.setText(count + " history record(s) found");
        return count;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnSearch) {
            loadHistory(tfSearchRef.getText(), (String) cbActionFilter.getSelectedItem(), null);
        } else if (ae.getSource() == btnReset) {
            tfSearchRef.setText("");
            cbActionFilter.setSelectedIndex(0);
            loadHistory("", "All Actions", null);
        } else if (ae.getSource() == btnClose) {
            dispose();
        }
    }

    public static void main(String[] args) {
        new ComplaintHistory();
    }
}
