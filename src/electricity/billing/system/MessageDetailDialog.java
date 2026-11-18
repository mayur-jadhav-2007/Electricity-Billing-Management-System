package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;

/**
 * Reusable modal dialog for viewing the complete details and body of a notification message.
 * Supports both Customer view (with unread status and background mark-as-read action)
 * and Administrator view (with target, delivery, and aggregated read counts).
 */
public class MessageDetailDialog extends JDialog implements ActionListener {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm:ss a");

    // Customer View Fields
    private NotificationService.CustomerNotification customerNotif;
    private Runnable onReadCallback;
    private JButton btnMarkAsRead;
    private JLabel lblStatusBadge;

    // Admin View Fields
    private NotificationService.SentMessageSummary adminSummary;

    private JButton btnClose;

    /**
     * Customer constructor: displays received notification with optional mark-as-read action.
     */
    public MessageDetailDialog(JFrame parent, NotificationService.CustomerNotification notif, Runnable onReadCallback) {
        super(parent, "Notification Details - " + (notif != null ? notif.getTitle() : "Message"), true);
        this.customerNotif = notif;
        this.onReadCallback = onReadCallback;

        initializeUI(false);
    }

    /**
     * Administrator constructor: displays sent message summary and delivery metrics.
     */
    public MessageDetailDialog(JFrame parent, NotificationService.SentMessageSummary summary) {
        super(parent, "Sent Message Details - #" + (summary != null ? summary.getMessageId() : ""), true);
        this.adminSummary = summary;

        initializeUI(true);
    }

    private void initializeUI(boolean isAdminView) {
        setSize(650, 560);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(248, 250, 252));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(16, 24, 16, 24)
        ));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        String mainTitle = isAdminView ? "Sent Message Overview" : "Notification Details";
        JLabel lblHeading = new JLabel(mainTitle);
        lblHeading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblHeading.setForeground(new Color(15, 23, 42));

        String subTitle = isAdminView
            ? "Outbox dispatch record and recipient read tracking"
            : "Received service message and operational alert";
        JLabel lblSub = new JLabel(subTitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));

        titleBlock.add(lblHeading);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblSub);
        headerPanel.add(titleBlock, BorderLayout.WEST);

        // Header Right: Priority & Read Status Badges
        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        badgesPanel.setOpaque(false);

        String priority = isAdminView ? adminSummary.getPriority() : customerNotif.getPriority();
        badgesPanel.add(createPriorityBadge(priority));

        if (!isAdminView) {
            lblStatusBadge = createReadStatusBadge(customerNotif.isRead());
            badgesPanel.add(lblStatusBadge);
        } else {
            JLabel targetBadge = createTargetBadge(adminSummary.getTargetType());
            badgesPanel.add(targetBadge);
        }

        headerPanel.add(badgesPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content Container
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        // Metadata Grid Card
        JPanel metaCard = new JPanel(new GridLayout(0, 2, 16, 10));
        metaCard.setOpaque(true);
        metaCard.setBackground(new Color(248, 250, 252));
        metaCard.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        if (isAdminView) {
            metaCard.add(createMetaRow("Message ID:", "#" + adminSummary.getMessageId()));
            metaCard.add(createMetaRow("Target Type:", adminSummary.getTargetType()));
            metaCard.add(createMetaRow("Sender Admin:", adminSummary.getSenderAdmin()));
            metaCard.add(createMetaRow("Sent Date/Time:", formatDate(adminSummary.getCreatedAt())));

            if ("INDIVIDUAL".equalsIgnoreCase(adminSummary.getTargetType())) {
                String recipMeter = adminSummary.getRecipientMeter() != null ? adminSummary.getRecipientMeter() : "N/A";
                String recipName = adminSummary.getRecipientName() != null ? adminSummary.getRecipientName() : "";
                metaCard.add(createMetaRow("Recipient Customer:", recipMeter + (recipName.isEmpty() ? "" : " (" + recipName + ")")));
            } else {
                metaCard.add(createMetaRow("Recipient Audience:", "All Active Customers (Broadcast)"));
            }

            metaCard.add(createMetaRow("Recipients / Reads:",
                adminSummary.getTotalRecipients() + " Total  |  " +
                adminSummary.getReadCount() + " Read  |  " +
                adminSummary.getUnreadCount() + " Unread"));
        } else {
            metaCard.add(createMetaRow("Notification ID:", "#" + customerNotif.getRecipientId()));
            metaCard.add(createMetaRow("Sender:", customerNotif.getSenderAdmin()));
            metaCard.add(createMetaRow("Received On:", formatDate(customerNotif.getReceivedAt())));
            metaCard.add(createMetaRow("Account Meter:", customerNotif.getMeterNo()));
            if (customerNotif.isRead() && customerNotif.getReadAt() != null) {
                metaCard.add(createMetaRow("Read Timestamp:", formatDate(customerNotif.getReadAt())));
            } else {
                metaCard.add(createMetaRow("Read Status:", "Awaiting Customer Review"));
            }
        }

        contentPanel.add(metaCard);
        contentPanel.add(Box.createVerticalStrut(14));

        // Message Title Label
        String titleText = isAdminView ? adminSummary.getTitle() : customerNotif.getTitle();
        JLabel lblTitle = new JLabel("Subject: " + (titleText != null ? titleText : ""));
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(15, 23, 42));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(lblTitle);
        contentPanel.add(Box.createVerticalStrut(10));

        // Message Body Area
        JLabel lblBodyTitle = new JLabel("Message Content:");
        lblBodyTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBodyTitle.setForeground(new Color(71, 85, 105));
        lblBodyTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(lblBodyTitle);
        contentPanel.add(Box.createVerticalStrut(6));

        String bodyText = isAdminView ? adminSummary.getMessage() : customerNotif.getMessage();
        JTextArea taBody = new JTextArea(bodyText != null ? bodyText : "");
        taBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taBody.setEditable(false);
        taBody.setLineWrap(true);
        taBody.setWrapStyleWord(true);
        taBody.setBackground(new Color(248, 250, 252));
        taBody.setForeground(new Color(30, 41, 59));
        taBody.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane spBody = new JScrollPane(taBody);
        spBody.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        spBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(spBody);

        add(contentPanel, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        if (!isAdminView && !customerNotif.isRead()) {
            btnMarkAsRead = new JButton("Mark as Read");
            btnMarkAsRead.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnMarkAsRead.setBackground(new Color(16, 185, 129)); // Green
            btnMarkAsRead.setForeground(Color.WHITE);
            btnMarkAsRead.setFocusPainted(false);
            btnMarkAsRead.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnMarkAsRead.addActionListener(this);
            bottomBar.add(btnMarkAsRead);
        }

        btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setBackground(new Color(51, 65, 85)); // Slate
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(this);
        bottomBar.add(btnClose);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createMetaRow(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(100, 116, 139));

        JLabel val = new JLabel(value != null ? value : "-");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        val.setForeground(new Color(15, 23, 42));

        p.add(lbl);
        p.add(Box.createVerticalStrut(2));
        p.add(val);
        return p;
    }

    private JLabel createPriorityBadge(String priority) {
        JLabel badge = new JLabel(priority != null ? priority.toUpperCase() : "NORMAL");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        if ("URGENT".equalsIgnoreCase(priority)) {
            badge.setBackground(new Color(254, 226, 226)); // Soft red
            badge.setForeground(new Color(220, 38, 38));
        } else if ("IMPORTANT".equalsIgnoreCase(priority)) {
            badge.setBackground(new Color(254, 243, 199)); // Soft amber
            badge.setForeground(new Color(217, 119, 6));
        } else {
            badge.setBackground(new Color(224, 242, 254)); // Soft blue
            badge.setForeground(new Color(2, 132, 199));
        }
        return badge;
    }

    private JLabel createReadStatusBadge(boolean isRead) {
        JLabel badge = new JLabel(isRead ? "READ" : "UNREAD");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        if (isRead) {
            badge.setBackground(new Color(241, 245, 249));
            badge.setForeground(new Color(100, 116, 139));
        } else {
            badge.setBackground(new Color(219, 234, 254));
            badge.setForeground(new Color(37, 99, 235));
        }
        return badge;
    }

    private JLabel createTargetBadge(String targetType) {
        boolean isBroadcast = "BROADCAST".equalsIgnoreCase(targetType);
        JLabel badge = new JLabel(isBroadcast ? "BROADCAST" : "INDIVIDUAL");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        if (isBroadcast) {
            badge.setBackground(new Color(243, 232, 255)); // Soft purple
            badge.setForeground(new Color(126, 34, 206));
        } else {
            badge.setBackground(new Color(224, 231, 255)); // Soft indigo
            badge.setForeground(new Color(67, 56, 202));
        }
        return badge;
    }

    private String formatDate(java.sql.Timestamp ts) {
        return (ts != null) ? dateFormat.format(ts) : "-";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnClose) {
            dispose();
        } else if (e.getSource() == btnMarkAsRead) {
            handleMarkAsRead();
        }
    }

    /**
     * Executes markAsRead in a SwingWorker off the Event Dispatch Thread.
     */
    private void handleMarkAsRead() {
        if (customerNotif == null) return;

        btnMarkAsRead.setEnabled(false);
        btnMarkAsRead.setText("Updating...");

        final int recipientId = customerNotif.getRecipientId();

        SwingWorker<NotificationService.ServiceResult, Void> worker = new SwingWorker<>() {
            @Override
            protected NotificationService.ServiceResult doInBackground() {
                return NotificationService.markAsRead(recipientId);
            }

            @Override
            protected void done() {
                try {
                    NotificationService.ServiceResult result = get();
                    if (result.isSuccess()) {
                        // Update dialog state to Read
                        btnMarkAsRead.setVisible(false);
                        lblStatusBadge.setText("READ");
                        lblStatusBadge.setBackground(new Color(241, 245, 249));
                        lblStatusBadge.setForeground(new Color(100, 116, 139));

                        JOptionPane.showMessageDialog(MessageDetailDialog.this,
                            "Notification marked as read successfully.",
                            "Status Updated", JOptionPane.INFORMATION_MESSAGE);

                        // Trigger parent refresh callback
                        if (onReadCallback != null) {
                            onReadCallback.run();
                        }
                    } else if ("ALREADY_READ".equals(result.getErrorCode())) {
                        btnMarkAsRead.setVisible(false);
                        lblStatusBadge.setText("READ");
                        lblStatusBadge.setBackground(new Color(241, 245, 249));
                        lblStatusBadge.setForeground(new Color(100, 116, 139));

                        JOptionPane.showMessageDialog(MessageDetailDialog.this,
                            result.getMessage(),
                            "Already Read", JOptionPane.INFORMATION_MESSAGE);

                        if (onReadCallback != null) {
                            onReadCallback.run();
                        }
                    } else {
                        btnMarkAsRead.setEnabled(true);
                        btnMarkAsRead.setText("Mark as Read");
                        JOptionPane.showMessageDialog(MessageDetailDialog.this,
                            result.getMessage(),
                            "Update Failed (" + result.getErrorCode() + ")", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    btnMarkAsRead.setEnabled(true);
                    btnMarkAsRead.setText("Mark as Read");
                    JOptionPane.showMessageDialog(MessageDetailDialog.this,
                        "An unexpected error occurred while updating status: " + ex.getMessage(),
                        "System Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }
}
