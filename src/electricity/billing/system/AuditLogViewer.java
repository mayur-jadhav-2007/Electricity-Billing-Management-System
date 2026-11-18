package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Admin-only Audit Log Viewer featuring search, multi-criteria filtering,
 * asynchronous pagination via SwingWorker, detailed inspection dialogs,
 * and formula-safe CSV export.
 */
public class AuditLogViewer extends JFrame implements ActionListener {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // UI Components
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField tfSearch;
    private JComboBox<String> cbAction;
    private JComboBox<String> cbRole;
    private JComboBox<String> cbDateRange;
    private JCheckBox chkIncludeComplaintHistory;
    private JButton btnApply, btnReset, btnExport, btnViewDetails, btnRefresh;
    private JButton btnFirst, btnPrev, btnNext, btnLast;
    private JComboBox<Integer> cbPageSize;
    private JLabel lblPageInfo, lblTotalRecords, lblLoading;
    private JPanel emptyStatePanel, tableContainer;

    // State
    private int currentPage = 1;
    private int pageSize = 25;
    private int totalPages = 1;
    private int totalRecords = 0;
    private List<AuditService.AuditLogEntry> currentEntries = new ArrayList<>();
    private SwingWorker<AuditService.QueryResult, Void> currentWorker;

    public AuditLogViewer() {
        super("Electricity Billing System - Audit Logs & Activity Tracking");

        // Role verification: Administrator access only
        if (!UserSession.isAdmin()) {
            JOptionPane.showMessageDialog(null,
                "Access Denied: You do not have permission to view Audit Logs.\nAdministrator privileges are required.",
                "Authorization Error [Ref: ERR-AUTH-DENIED]",
                JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(1150, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 0));

        initializeUI();
        loadData();
        setVisible(true);
    }

    private void initializeUI() {
        // 1. Top Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. Center Content Panel (Filter Bar + Table Container)
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel filterPanel = createFilterPanel();
        centerPanel.add(filterPanel, BorderLayout.NORTH);

        tableContainer = createTableContainer();
        centerPanel.add(tableContainer, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Pagination & Action Bar
        JPanel bottomBar = createBottomBar();
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42)); // Slate 900
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Audit Logs & System Activity");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Tamper-evident operational audit trail and administrative activity tracking");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(148, 163, 184)); // Slate 400

        left.add(title);
        left.add(Box.createVerticalStrut(3));
        left.add(subtitle);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        lblLoading = new JLabel("Loading...");
        lblLoading.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblLoading.setForeground(new Color(56, 189, 248)); // Sky 400
        lblLoading.setVisible(false);
        right.add(lblLoading);

        btnRefresh = createStyledButton("Refresh", new Color(30, 41, 59), Color.WHITE);
        btnRefresh.addActionListener(this);
        right.add(btnRefresh);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Search Input
        panel.add(new JLabel("Search:"));
        tfSearch = new JTextField(12);
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfSearch.setToolTipText("Search actor, entity ID, or description");
        tfSearch.addActionListener(this);
        panel.add(tfSearch);

        // Action Filter
        panel.add(new JLabel("Action:"));
        cbAction = new JComboBox<>(new String[]{
            "All Actions",
            AuditService.ACTION_LOGIN_SUCCESS,
            AuditService.ACTION_LOGIN_FAILED,
            AuditService.ACTION_LOGOUT,
            AuditService.ACTION_CUSTOMER_CREATED,
            AuditService.ACTION_CUSTOMER_UPDATED,
            AuditService.ACTION_CUSTOMER_DEACTIVATED,
            AuditService.ACTION_CUSTOMER_REACTIVATED,
            AuditService.ACTION_BILL_GENERATED,
            AuditService.ACTION_BILL_RECALCULATED,
            AuditService.ACTION_PAYMENT_COMPLETED,
            AuditService.ACTION_PAYMENT_FAILED,
            AuditService.ACTION_PAYMENT_DUPLICATE_ATTEMPT,
            AuditService.ACTION_COMPLAINT_SUBMITTED,
            AuditService.ACTION_COMPLAINT_ASSIGNED,
            AuditService.ACTION_COMPLAINT_STATUS_CHANGED,
            AuditService.ACTION_COMPLAINT_PRIORITY_CHANGED,
            AuditService.ACTION_COMPLAINT_REMARK_ADDED,
            AuditService.ACTION_COMPLAINT_REOPENED
        });
        cbAction.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(cbAction);

        // Role Filter
        panel.add(new JLabel("Role:"));
        cbRole = new JComboBox<>(new String[]{"All Roles", "Admin", "Customer", "Unknown", "Legacy/Unknown"});
        cbRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(cbRole);

        // Date Range
        panel.add(new JLabel("Date:"));
        cbDateRange = new JComboBox<>(new String[]{"All Time", "Today", "Last 7 Days", "Last 30 Days"});
        cbDateRange.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(cbDateRange);

        // Complaint History Checkbox
        chkIncludeComplaintHistory = new JCheckBox("Include Complaint History");
        chkIncludeComplaintHistory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkIncludeComplaintHistory.setOpaque(false);
        chkIncludeComplaintHistory.setSelected(false);
        panel.add(chkIncludeComplaintHistory);

        // Buttons
        btnApply = createStyledButton("Filter", new Color(37, 99, 235), Color.WHITE);
        ImageIcon searchIcon = Project.getScaledIcon("icon/icons8-search-50.png", 14, Color.WHITE);
        if (searchIcon != null) {
            btnApply.setIcon(searchIcon);
            btnApply.setIconTextGap(6);
        }
        btnApply.addActionListener(this);
        panel.add(btnApply);

        btnReset = createStyledButton("Reset", new Color(100, 116, 139), Color.WHITE);
        btnReset.addActionListener(this);
        panel.add(btnReset);

        return panel;
    }

    private JPanel createTableContainer() {
        JPanel container = new JPanel(new CardLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));

        // 1. Table
        String[] columns = {"Log ID", "Timestamp", "Actor", "Role", "Action", "Entity", "Entity ID", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(30, 41, 59));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(241, 245, 249));

        // Custom column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);  // Log ID
        table.getColumnModel().getColumn(1).setPreferredWidth(140);  // Timestamp
        table.getColumnModel().getColumn(2).setPreferredWidth(100);  // Actor
        table.getColumnModel().getColumn(3).setPreferredWidth(90);   // Role
        table.getColumnModel().getColumn(4).setPreferredWidth(160);  // Action
        table.getColumnModel().getColumn(5).setPreferredWidth(90);   // Entity
        table.getColumnModel().getColumn(6).setPreferredWidth(100);  // Entity ID
        table.getColumnModel().getColumn(7).setPreferredWidth(280);  // Description

        // Custom cell renderer for Action styling
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component comp = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected && value != null) {
                    String act = value.toString();
                    if (act.contains("SUCCESS") || act.contains("CREATED") || act.contains("REACTIVATED") || act.contains("COMPLETED")) {
                        comp.setForeground(new Color(22, 163, 74)); // Green
                    } else if (act.contains("FAILED") || act.contains("DEACTIVATED") || act.contains("DUPLICATE")) {
                        comp.setForeground(new Color(220, 38, 38)); // Red
                    } else {
                        comp.setForeground(new Color(30, 41, 59));
                    }
                }
                return comp;
            }
        });

        // Double click to view details
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetailsDialog();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        container.add(scrollPane, "TABLE");

        // 2. Empty State Panel
        emptyStatePanel = new JPanel();
        emptyStatePanel.setLayout(new BoxLayout(emptyStatePanel, BoxLayout.Y_AXIS));
        emptyStatePanel.setBackground(Color.WHITE);
        emptyStatePanel.setBorder(BorderFactory.createEmptyBorder(60, 20, 60, 20));

        JLabel emptyIcon = new JLabel("📋", JLabel.CENTER);
        emptyIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptyTitle = new JLabel("No Audit Logs Found", JLabel.CENTER);
        emptyTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        emptyTitle.setForeground(new Color(71, 85, 105));
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptySub = new JLabel("No records match your selected filter criteria or search query.", JLabel.CENTER);
        emptySub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        emptySub.setForeground(new Color(148, 163, 184));
        emptySub.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyStatePanel.add(emptyIcon);
        emptyStatePanel.add(Box.createVerticalStrut(12));
        emptyStatePanel.add(emptyTitle);
        emptyStatePanel.add(Box.createVerticalStrut(4));
        emptyStatePanel.add(emptySub);

        container.add(emptyStatePanel, "EMPTY");

        return container;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // Left: Actions (View Details, Export CSV)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        btnViewDetails = createStyledButton("View Details", new Color(15, 23, 42), Color.WHITE);
        btnViewDetails.addActionListener(this);
        left.add(btnViewDetails);

        btnExport = createStyledButton("Export to CSV", new Color(16, 185, 129), Color.WHITE);
        ImageIcon exportIcon = Project.getScaledIcon("icon/icons8-edit-text-file-50.png", 14, Color.WHITE);
        if (exportIcon != null) {
            btnExport.setIcon(exportIcon);
            btnExport.setIconTextGap(6);
        }
        btnExport.addActionListener(this);
        left.add(btnExport);

        lblTotalRecords = new JLabel("Total Records: 0");
        lblTotalRecords.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTotalRecords.setForeground(new Color(100, 116, 139));
        left.add(Box.createHorizontalStrut(10));
        left.add(lblTotalRecords);

        bar.add(left, BorderLayout.WEST);

        // Right: Pagination Controls
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        right.add(new JLabel("Page Size:"));
        cbPageSize = new JComboBox<>(new Integer[]{10, 25, 50, 100});
        cbPageSize.setSelectedItem(pageSize);
        cbPageSize.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbPageSize.addActionListener(e -> {
            Integer selected = (Integer) cbPageSize.getSelectedItem();
            if (selected != null && selected != pageSize) {
                pageSize = selected;
                currentPage = 1;
                loadData();
            }
        });
        right.add(cbPageSize);

        btnFirst = createPaginationButton("<<");
        btnPrev = createPaginationButton("<");
        lblPageInfo = new JLabel("Page 1 of 1");
        lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPageInfo.setForeground(new Color(51, 65, 85));
        btnNext = createPaginationButton(">");
        btnLast = createPaginationButton(">>");

        right.add(btnFirst);
        right.add(btnPrev);
        right.add(lblPageInfo);
        right.add(btnNext);
        right.add(btnLast);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JButton createPaginationButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(new Color(241, 245, 249));
        btn.setForeground(new Color(30, 41, 59));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(this);
        return btn;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return btn;
    }

    // =========================================================================
    // DATA LOADING (ASYNC)
    // =========================================================================

    public void loadData() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        lblLoading.setVisible(true);

        final String action = (String) cbAction.getSelectedItem();
        final String role = (String) cbRole.getSelectedItem();
        final String search = tfSearch.getText() != null ? tfSearch.getText().trim() : "";
        final String dateRange = (String) cbDateRange.getSelectedItem();
        final boolean includeComplaintHistory = chkIncludeComplaintHistory.isSelected();
        final int targetPage = currentPage;
        final int targetPageSize = pageSize;

        Date startDate = null;
        Date endDate = null;
        if ("Today".equalsIgnoreCase(dateRange)) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            startDate = cal.getTime();
        } else if ("Last 7 Days".equalsIgnoreCase(dateRange)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            startDate = cal.getTime();
        } else if ("Last 30 Days".equalsIgnoreCase(dateRange)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -30);
            startDate = cal.getTime();
        }

        final Date finalStart = startDate;
        final Date finalEnd = endDate;

        currentWorker = new SwingWorker<AuditService.QueryResult, Void>() {
            @Override
            protected AuditService.QueryResult doInBackground() throws Exception {
                return AuditService.queryLogs(
                    action, role, search, finalStart, finalEnd,
                    targetPage, targetPageSize, includeComplaintHistory
                );
            }

            @Override
            protected void done() {
                lblLoading.setVisible(false);
                if (isCancelled()) return;

                try {
                    AuditService.QueryResult result = get();
                    currentEntries = result.entries;
                    totalRecords = result.totalRecords;
                    totalPages = result.totalPages;
                    currentPage = result.page;

                    // Update Table Model
                    tableModel.setRowCount(0);
                    for (AuditService.AuditLogEntry e : currentEntries) {
                        tableModel.addRow(new Object[]{
                            e.displayId,
                            e.createdAt != null ? dateFormat.format(e.createdAt) : "-",
                            e.actorId,
                            e.actorRole,
                            e.actionType,
                            e.entityType,
                            e.entityId,
                            e.description
                        });
                    }

                    // Card Layout toggle for empty state
                    CardLayout cl = (CardLayout) tableContainer.getLayout();
                    if (totalRecords == 0) {
                        cl.show(tableContainer, "EMPTY");
                    } else {
                        cl.show(tableContainer, "TABLE");
                    }

                    // Update Pagination Controls
                    lblTotalRecords.setText("Total Records: " + totalRecords);
                    lblPageInfo.setText("Page " + (totalPages == 0 ? 0 : currentPage) + " of " + totalPages);
                    btnFirst.setEnabled(currentPage > 1);
                    btnPrev.setEnabled(currentPage > 1);
                    btnNext.setEnabled(currentPage < totalPages);
                    btnLast.setEnabled(currentPage < totalPages);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AuditLogViewer.this,
                        "Unable to load audit logs at this time.\nPlease contact system administrator if this persists. [Ref: ERR-AUDIT-QUERY]",
                        "Audit Query Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        currentWorker.execute();
    }

    // =========================================================================
    // ACTIONS & DETAILS
    // =========================================================================

    private void showDetailsDialog() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= currentEntries.size()) {
            JOptionPane.showMessageDialog(this, "Please select an audit record to view details.", "Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        AuditService.AuditLogEntry entry = currentEntries.get(selectedRow);

        JDialog dialog = new JDialog(this, "Audit Record Details - " + entry.displayId, true);
        dialog.setSize(580, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        p.add(createDetailRow("Log Identifier:", entry.displayId));
        p.add(createDetailRow("Timestamp:", entry.createdAt != null ? dateFormat.format(entry.createdAt) : "-"));
        p.add(createDetailRow("Actor ID:", entry.actorId));
        p.add(createDetailRow("Actor Role:", entry.actorRole));
        p.add(createDetailRow("Action Type:", entry.actionType));
        p.add(createDetailRow("Entity Type:", entry.entityType));
        p.add(createDetailRow("Entity ID:", entry.entityId));
        p.add(createDetailRow("Source Table:", entry.sourceTable));
        p.add(createDetailRow("Description:", entry.description));

        p.add(Box.createVerticalStrut(10));
        p.add(new JLabel("Previous State / Old Value:"));
        JTextArea taOld = new JTextArea(entry.oldValue != null ? entry.oldValue : "(None)");
        taOld.setEditable(false);
        taOld.setFont(new Font("Monospaced", Font.PLAIN, 12));
        taOld.setBackground(new Color(248, 250, 252));
        taOld.setBorder(new LineBorder(new Color(226, 232, 240), 1));
        p.add(new JScrollPane(taOld));

        p.add(Box.createVerticalStrut(10));
        p.add(new JLabel("New State / New Value:"));
        JTextArea taNew = new JTextArea(entry.newValue != null ? entry.newValue : "(None)");
        taNew.setEditable(false);
        taNew.setFont(new Font("Monospaced", Font.PLAIN, 12));
        taNew.setBackground(new Color(248, 250, 252));
        taNew.setBorder(new LineBorder(new Color(226, 232, 240), 1));
        p.add(new JScrollPane(taNew));

        dialog.add(p, BorderLayout.CENTER);

        JButton close = createStyledButton("Close", new Color(15, 23, 42), Color.WHITE);
        close.addActionListener(e -> dialog.dispose());
        JPanel btnP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnP.setBackground(new Color(241, 245, 249));
        btnP.add(close);
        dialog.add(btnP, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel r = new JPanel(new BorderLayout(8, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setPreferredSize(new Dimension(130, 20));
        JLabel val = new JLabel(value != null ? value : "-");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        r.add(lbl, BorderLayout.WEST);
        r.add(val, BorderLayout.CENTER);
        return r;
    }

    private void handleExport() {
        if (totalRecords == 0) {
            JOptionPane.showMessageDialog(this, "No records to export.", "Export Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("audit_logs_export.csv"));
        int ret = fc.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File target = fc.getSelectedFile();
            try {
                // Fetch all matching records without pagination limit for full export
                final String action = (String) cbAction.getSelectedItem();
                final String role = (String) cbRole.getSelectedItem();
                final String search = tfSearch.getText() != null ? tfSearch.getText().trim() : "";
                final String dateRange = (String) cbDateRange.getSelectedItem();
                final boolean includeComplaintHistory = chkIncludeComplaintHistory.isSelected();

                Date startDate = null;
                if ("Today".equalsIgnoreCase(dateRange)) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    startDate = cal.getTime();
                } else if ("Last 7 Days".equalsIgnoreCase(dateRange)) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -7);
                    startDate = cal.getTime();
                } else if ("Last 30 Days".equalsIgnoreCase(dateRange)) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -30);
                    startDate = cal.getTime();
                }

                AuditService.QueryResult allResults = AuditService.queryLogs(
                    action, role, search, startDate, null, 1, Math.max(totalRecords, 1000), includeComplaintHistory
                );

                AuditService.exportToCsv(allResults.entries, target);
                JOptionPane.showMessageDialog(this,
                    "Export completed successfully!\nFile: " + target.getAbsolutePath() +
                    "\nTotal records: " + allResults.entries.size(),
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Failed to export audit report to CSV. Please check file permissions. [Ref: ERR-AUDIT-EXPORT]",
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Object src = ae.getSource();
        if (src == btnApply || src == tfSearch) {
            currentPage = 1;
            loadData();
        } else if (src == btnReset) {
            tfSearch.setText("");
            cbAction.setSelectedIndex(0);
            cbRole.setSelectedIndex(0);
            cbDateRange.setSelectedIndex(0);
            chkIncludeComplaintHistory.setSelected(false);
            currentPage = 1;
            loadData();
        } else if (src == btnRefresh) {
            loadData();
        } else if (src == btnViewDetails) {
            showDetailsDialog();
        } else if (src == btnExport) {
            handleExport();
        } else if (src == btnFirst) {
            if (currentPage > 1) {
                currentPage = 1;
                loadData();
            }
        } else if (src == btnPrev) {
            if (currentPage > 1) {
                currentPage--;
                loadData();
            }
        } else if (src == btnNext) {
            if (currentPage < totalPages) {
                currentPage++;
                loadData();
            }
        } else if (src == btnLast) {
            if (currentPage < totalPages) {
                currentPage = totalPages;
                loadData();
            }
        }
    }

    public static void main(String[] args) {
        // Set mock admin session for standalone execution
        UserSession.setSession(new SessionContext("admin", "Admin", ""));
        SwingUtilities.invokeLater(AuditLogViewer::new);
    }
}
