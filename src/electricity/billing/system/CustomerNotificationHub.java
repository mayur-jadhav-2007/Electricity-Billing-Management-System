package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer Notification Inbox allowing authenticated customers to view their notifications,
 * filter unread messages, inspect detailed message bodies, and mark messages as read.
 * Guarantees customer isolation by never accepting or passing meter numbers from the UI.
 */
public class CustomerNotificationHub extends JFrame implements ActionListener {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a");

    private JTable table;
    private DefaultTableModel tableModel;
    private JRadioButton rbAll;
    private JRadioButton rbUnreadOnly;
    private JTextField tfSearch;
    private JButton btnFilter;
    private JButton btnReset;
    private JButton btnRefresh;
    private JButton btnViewDetails;
    private JButton btnMarkRead;
    private JButton btnClose;
    private JLabel lblCount;
    private JLabel lblUnreadBadge;

    // Cache of retrieved notifications for row selection lookup
    private final List<NotificationService.CustomerNotification> notificationCache = new ArrayList<>();

    public CustomerNotificationHub() {
        super("Electricity Billing System - Customer Notification & Alert Inbox");

        // Role Authorization Guard
        if (!UserSession.isCustomer()) {
            JOptionPane.showMessageDialog(null,
                "Access denied. Customer privileges are required to view notifications.",
                "Unauthorized", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(1020, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(15, 23, 42)); // Deep Slate Navy #0F172A
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel heading = new JLabel("MY NOTIFICATIONS & ANNOUNCEMENTS");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 17));
        heading.setForeground(Color.WHITE);

        String username = UserSession.getActorId();
        JLabel subHeading = new JLabel("Account user: " + username + "  |  Service notifications, alerts, and operational notices");
        subHeading.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subHeading.setForeground(new Color(148, 163, 184));

        titleBlock.add(heading);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subHeading);
        headerPanel.add(titleBlock, BorderLayout.WEST);

        // Right side: Unread Badge
        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        badgeWrapper.setOpaque(false);

        lblUnreadBadge = new JLabel("Unread: ...", JLabel.CENTER);
        lblUnreadBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUnreadBadge.setForeground(Color.WHITE);
        lblUnreadBadge.setBackground(new Color(37, 99, 235)); // Blue badge
        lblUnreadBadge.setOpaque(true);
        lblUnreadBadge.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        badgeWrapper.add(lblUnreadBadge);
        headerPanel.add(badgeWrapper, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Filter & Controls Bar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBackground(new Color(248, 250, 252));
        topContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        filterPanel.setOpaque(false);

        JLabel lblFilter = new JLabel("View:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblFilter.setForeground(new Color(51, 65, 85));
        filterPanel.add(lblFilter);

        rbAll = new JRadioButton("All Messages", true);
        rbAll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rbAll.setOpaque(false);
        rbAll.setForeground(new Color(15, 23, 42));

        rbUnreadOnly = new JRadioButton("Unread Only", false);
        rbUnreadOnly.setFont(new Font("Segoe UI", Font.BOLD, 12));
        rbUnreadOnly.setOpaque(false);
        rbUnreadOnly.setForeground(new Color(37, 99, 235));

        ButtonGroup bgFilter = new ButtonGroup();
        bgFilter.add(rbAll);
        bgFilter.add(rbUnreadOnly);

        ItemListener radioListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                loadNotifications();
            }
        };
        rbAll.addItemListener(radioListener);
        rbUnreadOnly.addItemListener(radioListener);

        filterPanel.add(rbAll);
        filterPanel.add(rbUnreadOnly);

        JLabel lblSearch = new JLabel("Search Subject:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(new Color(51, 65, 85));
        filterPanel.add(lblSearch);

        tfSearch = new JTextField(12);
        tfSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfSearch.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        filterPanel.add(tfSearch);

        btnFilter = new JButton("Filter");
        btnFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnFilter.setBackground(new Color(37, 99, 235));
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setFocusPainted(false);
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilter.addActionListener(this);
        filterPanel.add(btnFilter);

        btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReset.setBackground(new Color(241, 245, 249));
        btnReset.setForeground(new Color(51, 65, 85));
        btnReset.setFocusPainted(false);
        btnReset.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnReset.addActionListener(this);
        filterPanel.add(btnReset);

        btnRefresh = new JButton("Refresh");
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setBackground(new Color(15, 23, 42));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(this);
        filterPanel.add(btnRefresh);

        lblCount = new JLabel("Loading notifications...");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCount.setForeground(new Color(100, 116, 139));
        filterPanel.add(lblCount);

        topContainer.add(filterPanel);
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        String[] columns = {
            "Recipient ID", "Status", "Priority", "Subject / Title", "Sender", "Date Received"
        };

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
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 232, 240));

        // Hide Recipient ID column (internal identifier)
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Preferred column widths
        table.getColumnModel().getColumn(1).setPreferredWidth(100); // Status
        table.getColumnModel().getColumn(2).setPreferredWidth(90);  // Priority
        table.getColumnModel().getColumn(3).setPreferredWidth(450); // Title
        table.getColumnModel().getColumn(4).setPreferredWidth(110); // Sender
        table.getColumnModel().getColumn(5).setPreferredWidth(170); // Received Date

        // Custom Cell Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                if (!isSel) {
                    // Check read status from model column 1
                    Object statusVal = tbl.getValueAt(row, 1);
                    boolean isUnread = "● UNREAD".equals(statusVal);

                    if (isUnread) {
                        c.setBackground(new Color(240, 249, 255)); // Soft blue highlight for unread
                        c.setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                        c.setFont(getFont().deriveFont(Font.PLAIN));
                    }

                    // Status Column Styling
                    if (col == 1 && val != null) {
                        if (isUnread) {
                            c.setForeground(new Color(37, 99, 235)); // Bold Blue
                        } else {
                            c.setForeground(new Color(100, 116, 139)); // Muted Slate
                        }
                    }

                    // Priority Column Styling
                    if (col == 2 && val != null) {
                        String pri = val.toString();
                        if ("Urgent".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(220, 38, 38)); // Red
                        } else if ("Important".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(217, 119, 6)); // Amber
                        } else {
                            c.setForeground(new Color(2, 132, 199)); // Blue
                        }
                    }

                    // Title Column Styling
                    if (col == 3 && val != null) {
                        c.setForeground(new Color(15, 23, 42));
                    }
                }
                return c;
            }
        });

        // Double-click row to open detail dialog
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedDetail();
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        add(sp, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        btnViewDetails = new JButton("Read / View Details");
        btnViewDetails.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewDetails.setBackground(new Color(37, 99, 235));
        btnViewDetails.setForeground(Color.WHITE);
        btnViewDetails.setFocusPainted(false);
        btnViewDetails.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewDetails.addActionListener(this);

        btnMarkRead = new JButton("Mark as Read");
        btnMarkRead.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnMarkRead.setBackground(new Color(16, 185, 129));
        btnMarkRead.setForeground(Color.WHITE);
        btnMarkRead.setFocusPainted(false);
        btnMarkRead.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMarkRead.addActionListener(this);

        btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setBackground(new Color(51, 65, 85));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(this);

        bottomBar.add(btnViewDetails);
        bottomBar.add(btnMarkRead);
        bottomBar.add(btnClose);

        add(bottomBar, BorderLayout.SOUTH);

        // Initial load
        loadNotifications();

        setVisible(true);
    }

    // =========================================================================
    // BACKGROUND OPERATIONS OFF THE EDT
    // =========================================================================

    /**
     * Loads notifications and unread count asynchronously off the Event Dispatch Thread.
     */
    public void loadNotifications() {
        btnRefresh.setEnabled(false);
        lblCount.setText("Loading notifications...");

        final boolean unreadOnly = rbUnreadOnly.isSelected();

        SwingWorker<NotificationService.QueryResult<List<NotificationService.CustomerNotification>>, Void> worker = new SwingWorker<>() {
            private NotificationService.QueryResult<Integer> unreadResult;

            @Override
            protected NotificationService.QueryResult<List<NotificationService.CustomerNotification>> doInBackground() {
                // Fetch unread count and notification list in background
                unreadResult = NotificationService.getUnreadNotificationCount();
                return NotificationService.getCustomerNotifications(unreadOnly);
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                try {
                    // Update unread count badge
                    if (unreadResult != null && unreadResult.isSuccess()) {
                        int unread = unreadResult.getData();
                        lblUnreadBadge.setText("Unread: " + unread);
                        if (unread > 0) {
                            lblUnreadBadge.setBackground(new Color(220, 38, 38)); // Red badge if unread > 0
                        } else {
                            lblUnreadBadge.setBackground(new Color(16, 185, 129)); // Green badge if all read
                        }
                    }

                    // Update notification list
                    NotificationService.QueryResult<List<NotificationService.CustomerNotification>> qr = get();
                    if (qr.isSuccess()) {
                        notificationCache.clear();
                        if (qr.getData() != null) {
                            notificationCache.addAll(qr.getData());
                        }
                        applyTableFilter(tfSearch.getText());
                    } else {
                        lblCount.setText("Failed to load: " + qr.getMessage());
                    }
                } catch (Exception ex) {
                    lblCount.setText("Error: " + ex.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void applyTableFilter(String filter) {
        tableModel.setRowCount(0);
        String term = (filter != null) ? filter.trim().toLowerCase() : "";

        int matchingCount = 0;
        for (NotificationService.CustomerNotification n : notificationCache) {
            String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
            if (term.isEmpty() || title.contains(term)) {
                String statusDisplay = n.isRead() ? "✓ Read" : "● UNREAD";
                tableModel.addRow(new Object[]{
                    n.getRecipientId(),
                    statusDisplay,
                    n.getPriority(),
                    n.getTitle(),
                    n.getSenderAdmin(),
                    n.getReceivedAt() != null ? dateFormat.format(n.getReceivedAt()) : "-"
                });
                matchingCount++;
            }
        }

        lblCount.setText(matchingCount + " notification(s) displayed");
    }

    private void openSelectedDetail() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select a notification from the list to view.",
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int recipientId = (int) tableModel.getValueAt(selectedRow, 0);

        NotificationService.CustomerNotification match = null;
        for (NotificationService.CustomerNotification n : notificationCache) {
            if (n.getRecipientId() == recipientId) {
                match = n;
                break;
            }
        }

        if (match != null) {
            new MessageDetailDialog(this, match, this::loadNotifications).setVisible(true);
        }
    }

    /**
     * Executes markAsRead in the background via SwingWorker.
     */
    private void handleMarkAsRead() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select an unread notification to mark as read.",
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String statusStr = (String) tableModel.getValueAt(selectedRow, 1);
        if ("✓ Read".equals(statusStr)) {
            JOptionPane.showMessageDialog(this,
                "The selected notification is already marked as read.",
                "Already Read", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int recipientId = (int) tableModel.getValueAt(selectedRow, 0);

        btnMarkRead.setEnabled(false);
        btnMarkRead.setText("Updating...");

        SwingWorker<NotificationService.ServiceResult, Void> worker = new SwingWorker<>() {
            @Override
            protected NotificationService.ServiceResult doInBackground() {
                return NotificationService.markAsRead(recipientId);
            }

            @Override
            protected void done() {
                btnMarkRead.setEnabled(true);
                btnMarkRead.setText("Mark as Read");

                try {
                    NotificationService.ServiceResult res = get();
                    if (res.isSuccess()) {
                        JOptionPane.showMessageDialog(CustomerNotificationHub.this,
                            "Notification marked as read successfully.",
                            "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                        loadNotifications();
                    } else if ("ALREADY_READ".equals(res.getErrorCode())) {
                        JOptionPane.showMessageDialog(CustomerNotificationHub.this,
                            res.getMessage(), "Already Read", JOptionPane.INFORMATION_MESSAGE);
                        loadNotifications();
                    } else {
                        JOptionPane.showMessageDialog(CustomerNotificationHub.this,
                            res.getMessage(), "Operation Failed (" + res.getErrorCode() + ")",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CustomerNotificationHub.this,
                        "Error marking notification as read: " + ex.getMessage(),
                        "System Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnRefresh) {
            loadNotifications();
        } else if (e.getSource() == btnFilter) {
            applyTableFilter(tfSearch.getText());
        } else if (e.getSource() == btnReset) {
            tfSearch.setText("");
            applyTableFilter("");
        } else if (e.getSource() == btnViewDetails) {
            openSelectedDetail();
        } else if (e.getSource() == btnMarkRead) {
            handleMarkAsRead();
        } else if (e.getSource() == btnClose) {
            dispose();
        }
    }
}
