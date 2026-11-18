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
 * Administrator Message Centre providing full message composition (Individual and Broadcast),
 * input validation, background dispatch off the Event Dispatch Thread, and live sent-message history tracking.
 */
public class AdminMessageHub extends JFrame implements ActionListener {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a");

    // UI Tabbed Interface
    private JTabbedPane tabbedPane;

    // Compose Tab Controls
    private JRadioButton rbIndividual;
    private JRadioButton rbBroadcast;
    private JTextField tfMeter;
    private JLabel lblMeterPrompt;
    private JComboBox<String> cbPriority;
    private JTextField tfTitle;
    private JTextArea taBody;
    private JLabel lblCharCount;
    private JButton btnSend;
    private JButton btnClear;
    private JLabel lblSendProgress;
    private JProgressBar sendProgressBar;

    // History Tab Controls
    private JTable tableHistory;
    private DefaultTableModel historyModel;
    private JTextField tfSearchHistory;
    private JButton btnSearchHistory;
    private JButton btnResetHistory;
    private JButton btnRefreshHistory;
    private JButton btnViewDetails;
    private JButton btnClose;
    private JLabel lblHistoryCount;

    // Cache of retrieved history items for detail lookup
    private final List<NotificationService.SentMessageSummary> historyCache = new ArrayList<>();

    public AdminMessageHub() {
        super("Electricity Billing System - Admin Notification & Message Centre");

        // Role Authorization Guard
        if (!UserSession.isAdmin()) {
            JOptionPane.showMessageDialog(null,
                "Access denied. Administrator privileges are required to access the Message Centre.",
                "Unauthorized", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(1140, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Top Banner
        JPanel bannerPanel = new JPanel(new BorderLayout());
        bannerPanel.setBackground(new Color(15, 23, 42)); // Deep Slate Navy #0F172A
        bannerPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JPanel bannerLeft = new JPanel();
        bannerLeft.setLayout(new BoxLayout(bannerLeft, BoxLayout.Y_AXIS));
        bannerLeft.setOpaque(false);

        JLabel bannerTitle = new JLabel("ADMINISTRATOR MESSAGE & NOTIFICATION CENTRE");
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        bannerTitle.setForeground(Color.WHITE);

        String adminName = UserSession.getActorId();
        JLabel bannerSub = new JLabel("Logged in as: " + adminName + " (Administrator)  |  Direct customer dispatch & broadcast communications");
        bannerSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bannerSub.setForeground(new Color(148, 163, 184));

        bannerLeft.add(bannerTitle);
        bannerLeft.add(Box.createVerticalStrut(3));
        bannerLeft.add(bannerSub);
        bannerPanel.add(bannerLeft, BorderLayout.WEST);

        add(bannerPanel, BorderLayout.NORTH);

        // Tabbed Pane setup
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        // Build Tabs
        JPanel composeTab = createComposeTab();
        JPanel historyTab = createHistoryTab();

        tabbedPane.addTab("  Compose & Dispatch Message  ", composeTab);
        tabbedPane.addTab("  Sent Message History  ", historyTab);

        add(tabbedPane, BorderLayout.CENTER);

        // Initial background fetch of sent history
        loadSentHistory();

        setVisible(true);
    }

    // =========================================================================
    // COMPOSE TAB
    // =========================================================================

    private JPanel createComposeTab() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(248, 250, 252));

        JPanel formCard = new JPanel();
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            BorderFactory.createEmptyBorder(24, 32, 24, 32)
        ));

        Dimension labelDim = new Dimension(175, 24);

        // 1. Recipient Target Selection - Clean Single Horizontal Row
        JPanel pTarget = new JPanel();
        pTarget.setLayout(new BoxLayout(pTarget, BoxLayout.X_AXIS));
        pTarget.setOpaque(false);
        pTarget.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTarget = new JLabel("Target Audience:*");
        lblTarget.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTarget.setForeground(new Color(30, 41, 59));
        lblTarget.setPreferredSize(labelDim);
        lblTarget.setMinimumSize(labelDim);
        lblTarget.setMaximumSize(labelDim);
        lblTarget.setAlignmentY(Component.CENTER_ALIGNMENT);

        rbIndividual = new JRadioButton("Individual Customer", true);
        rbIndividual.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rbIndividual.setOpaque(false);
        rbIndividual.setForeground(new Color(15, 23, 42));
        rbIndividual.setAlignmentY(Component.CENTER_ALIGNMENT);

        rbBroadcast = new JRadioButton("All Active Customers (Broadcast)", false);
        rbBroadcast.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rbBroadcast.setOpaque(false);
        rbBroadcast.setForeground(new Color(15, 23, 42));
        rbBroadcast.setAlignmentY(Component.CENTER_ALIGNMENT);

        ButtonGroup bgTarget = new ButtonGroup();
        bgTarget.add(rbIndividual);
        bgTarget.add(rbBroadcast);

        pTarget.add(lblTarget);
        pTarget.add(Box.createHorizontalStrut(16));
        pTarget.add(rbIndividual);
        pTarget.add(Box.createHorizontalStrut(18));
        pTarget.add(rbBroadcast);
        pTarget.add(Box.createHorizontalGlue());

        pTarget.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        formCard.add(pTarget);
        formCard.add(Box.createVerticalStrut(14));

        // 2. Meter Number Field (Conditional) - Clean Single Horizontal Row
        JPanel pMeter = new JPanel();
        pMeter.setLayout(new BoxLayout(pMeter, BoxLayout.X_AXIS));
        pMeter.setOpaque(false);
        pMeter.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblMeter = new JLabel("Recipient Meter Number:*");
        lblMeter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMeter.setForeground(new Color(30, 41, 59));
        lblMeter.setPreferredSize(labelDim);
        lblMeter.setMinimumSize(labelDim);
        lblMeter.setMaximumSize(labelDim);
        lblMeter.setAlignmentY(Component.CENTER_ALIGNMENT);

        tfMeter = new JTextField(16);
        tfMeter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tfMeter.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tfMeter.setAlignmentY(Component.CENTER_ALIGNMENT);
        tfMeter.setMaximumSize(tfMeter.getPreferredSize());

        lblMeterPrompt = new JLabel("(Required for Individual Message)");
        lblMeterPrompt.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblMeterPrompt.setForeground(new Color(100, 116, 139));
        lblMeterPrompt.setAlignmentY(Component.CENTER_ALIGNMENT);

        pMeter.add(lblMeter);
        pMeter.add(Box.createHorizontalStrut(16));
        pMeter.add(tfMeter);
        pMeter.add(Box.createHorizontalStrut(14));
        pMeter.add(lblMeterPrompt);
        pMeter.add(Box.createHorizontalGlue());

        pMeter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        formCard.add(pMeter);
        formCard.add(Box.createVerticalStrut(14));

        // Radio button toggle event
        ItemListener targetToggleListener = e -> {
            boolean isInd = rbIndividual.isSelected();
            tfMeter.setEnabled(isInd);
            if (isInd) {
                tfMeter.setBackground(Color.WHITE);
                tfMeter.setText("");
                lblMeterPrompt.setText("(Required for Individual Message)");
            } else {
                tfMeter.setBackground(new Color(241, 245, 249));
                tfMeter.setText("(All Active Customers Selected)");
                lblMeterPrompt.setText("(Targeting all currently Active customer accounts)");
            }
        };
        rbIndividual.addItemListener(targetToggleListener);
        rbBroadcast.addItemListener(targetToggleListener);

        // 3. Priority Selection - Horizontal Row
        JPanel pPriority = new JPanel();
        pPriority.setLayout(new BoxLayout(pPriority, BoxLayout.X_AXIS));
        pPriority.setOpaque(false);
        pPriority.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPriority = new JLabel("Message Priority:*");
        lblPriority.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPriority.setForeground(new Color(30, 41, 59));
        lblPriority.setPreferredSize(labelDim);
        lblPriority.setMinimumSize(labelDim);
        lblPriority.setMaximumSize(labelDim);
        lblPriority.setAlignmentY(Component.CENTER_ALIGNMENT);

        cbPriority = new JComboBox<>(new String[]{"Normal", "Important", "Urgent"});
        cbPriority.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbPriority.setBackground(Color.WHITE);
        cbPriority.setPreferredSize(new Dimension(200, 32));
        cbPriority.setMaximumSize(new Dimension(200, 32));
        cbPriority.setAlignmentY(Component.CENTER_ALIGNMENT);

        pPriority.add(lblPriority);
        pPriority.add(Box.createHorizontalStrut(16));
        pPriority.add(cbPriority);
        pPriority.add(Box.createHorizontalGlue());

        pPriority.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        formCard.add(pPriority);
        formCard.add(Box.createVerticalStrut(14));

        // 4. Message Title - Horizontal Row
        JPanel pTitle = new JPanel();
        pTitle.setLayout(new BoxLayout(pTitle, BoxLayout.X_AXIS));
        pTitle.setOpaque(false);
        pTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitle = new JLabel("Message Title:*");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(30, 41, 59));
        lblTitle.setPreferredSize(labelDim);
        lblTitle.setMinimumSize(labelDim);
        lblTitle.setMaximumSize(labelDim);
        lblTitle.setAlignmentY(Component.CENTER_ALIGNMENT);

        tfTitle = new JTextField();
        tfTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tfTitle.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tfTitle.setAlignmentY(Component.CENTER_ALIGNMENT);
        tfTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        tfTitle.setToolTipText("Enter title (3 to 150 characters)");

        pTitle.add(lblTitle);
        pTitle.add(Box.createHorizontalStrut(16));
        pTitle.add(tfTitle);

        pTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        formCard.add(pTitle);
        formCard.add(Box.createVerticalStrut(14));

        // 5. Message Body - Vertical Section
        JPanel pBody = new JPanel();
        pBody.setLayout(new BoxLayout(pBody, BoxLayout.Y_AXIS));
        pBody.setOpaque(false);
        pBody.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblBody = new JLabel("Message Content / Body:* (5 to 5000 characters)");
        lblBody.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBody.setForeground(new Color(30, 41, 59));
        lblBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        pBody.add(lblBody);
        pBody.add(Box.createVerticalStrut(6));

        taBody = new JTextArea(7, 40);
        taBody.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taBody.setLineWrap(true);
        taBody.setWrapStyleWord(true);
        taBody.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane spBody = new JScrollPane(taBody);
        spBody.setBorder(new LineBorder(new Color(203, 213, 225)));
        spBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        pBody.add(spBody);

        formCard.add(pBody);
        formCard.add(Box.createVerticalStrut(8));

        // Character count & validation hints
        lblCharCount = new JLabel("Body characters: 0 / 5000");
        lblCharCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCharCount.setForeground(new Color(100, 116, 139));
        lblCharCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(lblCharCount);

        taBody.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int len = taBody.getText().trim().length();
                lblCharCount.setText("Body characters: " + len + " / 5000");
                if (len > 5000) {
                    lblCharCount.setForeground(new Color(220, 38, 38));
                } else {
                    lblCharCount.setForeground(new Color(100, 116, 139));
                }
            }
        });

        formCard.add(Box.createVerticalStrut(18));

        // Actions Row
        JPanel pButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pButtons.setOpaque(false);
        pButtons.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSend = new JButton("Dispatch Notification");
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSend.setBackground(new Color(37, 99, 235)); // Royal Blue
        btnSend.setForeground(Color.WHITE);
        btnSend.setFocusPainted(false);
        btnSend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSend.setPreferredSize(new Dimension(190, 38));
        btnSend.addActionListener(this);

        btnClear = new JButton("Clear Form");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClear.setBackground(new Color(241, 245, 249));
        btnClear.setForeground(new Color(51, 65, 85));
        btnClear.setFocusPainted(false);
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.setPreferredSize(new Dimension(120, 38));
        btnClear.addActionListener(this);

        sendProgressBar = new JProgressBar();
        sendProgressBar.setIndeterminate(true);
        sendProgressBar.setVisible(false);
        sendProgressBar.setPreferredSize(new Dimension(140, 20));

        lblSendProgress = new JLabel("");
        lblSendProgress.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblSendProgress.setForeground(new Color(37, 99, 235));

        pButtons.add(btnSend);
        pButtons.add(btnClear);
        pButtons.add(sendProgressBar);
        pButtons.add(lblSendProgress);

        formCard.add(pButtons);

        JScrollPane scrollForm = new JScrollPane(formCard);
        scrollForm.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        scrollForm.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollForm, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createFormRow(String labelText) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(30, 41, 59));
        row.add(lbl);
        row.add(Box.createVerticalStrut(4));
        return row;
    }

    // =========================================================================
    // HISTORY TAB
    // =========================================================================

    private JPanel createHistoryTab() {
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(Color.WHITE);

        // Top Filter & Search Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        topBar.setBackground(new Color(248, 250, 252));
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JLabel lblSearch = new JLabel("Search Title / Meter:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(new Color(51, 65, 85));
        topBar.add(lblSearch);

        tfSearchHistory = new JTextField(15);
        tfSearchHistory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfSearchHistory.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        topBar.add(tfSearchHistory);

        btnSearchHistory = new JButton("Filter");
        btnSearchHistory.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearchHistory.setBackground(new Color(37, 99, 235));
        btnSearchHistory.setForeground(Color.WHITE);
        btnSearchHistory.setFocusPainted(false);
        btnSearchHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearchHistory.addActionListener(this);
        topBar.add(btnSearchHistory);

        btnResetHistory = new JButton("Show All");
        btnResetHistory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnResetHistory.setBackground(new Color(241, 245, 249));
        btnResetHistory.setForeground(new Color(51, 65, 85));
        btnResetHistory.setFocusPainted(false);
        btnResetHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnResetHistory.addActionListener(this);
        topBar.add(btnResetHistory);

        btnRefreshHistory = new JButton("Refresh");
        btnRefreshHistory.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefreshHistory.setBackground(new Color(15, 23, 42));
        btnRefreshHistory.setForeground(Color.WHITE);
        btnRefreshHistory.setFocusPainted(false);
        btnRefreshHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshHistory.addActionListener(this);
        topBar.add(btnRefreshHistory);

        lblHistoryCount = new JLabel("Loading sent message history...");
        lblHistoryCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHistoryCount.setForeground(new Color(100, 116, 139));
        topBar.add(lblHistoryCount);

        historyPanel.add(topBar, BorderLayout.NORTH);

        // Center Table Setup
        String[] columns = {
            "ID", "Target", "Recipient Audience", "Message Title", "Priority",
            "Total Recipients", "Read Count", "Unread Count", "Dispatch Date/Time"
        };

        historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tableHistory = new JTable(historyModel);
        tableHistory.setRowHeight(30);
        tableHistory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableHistory.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tableHistory.getTableHeader().setBackground(new Color(241, 245, 249));
        tableHistory.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableHistory.setShowGrid(true);
        tableHistory.setGridColor(new Color(226, 232, 240));

        // Column widths
        tableHistory.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        tableHistory.getColumnModel().getColumn(1).setPreferredWidth(100);  // Target
        tableHistory.getColumnModel().getColumn(2).setPreferredWidth(180);  // Recipient
        tableHistory.getColumnModel().getColumn(3).setPreferredWidth(260);  // Title
        tableHistory.getColumnModel().getColumn(4).setPreferredWidth(80);   // Priority
        tableHistory.getColumnModel().getColumn(5).setPreferredWidth(95);   // Total
        tableHistory.getColumnModel().getColumn(6).setPreferredWidth(75);   // Read
        tableHistory.getColumnModel().getColumn(7).setPreferredWidth(75);   // Unread
        tableHistory.getColumnModel().getColumn(8).setPreferredWidth(160);  // Date

        // Custom Cell Renderer
        tableHistory.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFoc, row, col);
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(new Color(15, 23, 42));

                    // Priority Column (4)
                    if (col == 4 && val != null) {
                        String pri = val.toString();
                        if ("Urgent".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(220, 38, 38)); // Red
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else if ("Important".equalsIgnoreCase(pri)) {
                            c.setForeground(new Color(217, 119, 6)); // Amber
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else {
                            c.setForeground(new Color(2, 132, 199)); // Blue
                        }
                    }

                    // Target Column (1)
                    if (col == 1 && val != null) {
                        String tgt = val.toString();
                        if ("BROADCAST".equalsIgnoreCase(tgt)) {
                            c.setForeground(new Color(126, 34, 206)); // Purple
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else {
                            c.setForeground(new Color(67, 56, 202)); // Indigo
                        }
                    }
                }
                return c;
            }
        });

        // Double-click row to view details
        tableHistory.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedDetail();
                }
            }
        });

        JScrollPane spHistory = new JScrollPane(tableHistory);
        spHistory.setBorder(null);
        historyPanel.add(spHistory, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        btnViewDetails = new JButton("View Complete Details");
        btnViewDetails.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewDetails.setBackground(new Color(37, 99, 235));
        btnViewDetails.setForeground(Color.WHITE);
        btnViewDetails.setFocusPainted(false);
        btnViewDetails.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewDetails.addActionListener(this);

        btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setBackground(new Color(51, 65, 85));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(this);

        bottomBar.add(btnViewDetails);
        bottomBar.add(btnClose);

        historyPanel.add(bottomBar, BorderLayout.SOUTH);

        return historyPanel;
    }

    // =========================================================================
    // BACKGROUND OPERATIONS OFF THE EDT
    // =========================================================================

    /**
     * Dispatches the message via SwingWorker off the Event Dispatch Thread.
     */
    private void handleSendMessage() {
        boolean isBroadcast = rbBroadcast.isSelected();
        String meterNo = tfMeter.getText() != null ? tfMeter.getText().trim() : "";
        String title = tfTitle.getText() != null ? tfTitle.getText().trim() : "";
        String body = taBody.getText() != null ? taBody.getText().trim() : "";
        String priority = (String) cbPriority.getSelectedItem();

        // 1. Client-Side Input Validations
        if (!isBroadcast && meterNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Recipient meter number is required for individual messages.",
                "Input Validation", JOptionPane.WARNING_MESSAGE);
            tfMeter.requestFocus();
            return;
        }

        if (title.length() < 3 || title.length() > 150) {
            JOptionPane.showMessageDialog(this, "Message title must be between 3 and 150 characters.",
                "Input Validation", JOptionPane.WARNING_MESSAGE);
            tfTitle.requestFocus();
            return;
        }

        if (body.length() < 5 || body.length() > 5000) {
            JOptionPane.showMessageDialog(this, "Message body must be between 5 and 5000 characters.",
                "Input Validation", JOptionPane.WARNING_MESSAGE);
            taBody.requestFocus();
            return;
        }

        // 2. Disable UI controls while background operation executes
        setFormEnabled(false);
        sendProgressBar.setVisible(true);
        lblSendProgress.setText("Dispatching notification...");

        SwingWorker<NotificationService.ServiceResult, Void> worker = new SwingWorker<>() {
            @Override
            protected NotificationService.ServiceResult doInBackground() {
                if (isBroadcast) {
                    return NotificationService.sendBroadcastMessage(title, body, priority);
                } else {
                    return NotificationService.sendIndividualMessage(meterNo, title, body, priority);
                }
            }

            @Override
            protected void done() {
                setFormEnabled(true);
                sendProgressBar.setVisible(false);
                lblSendProgress.setText("");

                try {
                    NotificationService.ServiceResult result = get();

                    if (result.isSuccess()) {
                        JOptionPane.showMessageDialog(AdminMessageHub.this,
                            result.getMessage(), "Message Dispatched Successfully",
                            JOptionPane.INFORMATION_MESSAGE);

                        // Clear form fields
                        tfTitle.setText("");
                        taBody.setText("");
                        lblCharCount.setText("Body characters: 0 / 5000");
                        if (!isBroadcast) {
                            tfMeter.setText("");
                        }

                        // Automatically reload sent history
                        loadSentHistory();

                    } else if ("COMMIT_OUTCOME_UNKNOWN".equals(result.getErrorCode())) {
                        // Crucial safety warning: Instruct admin to check history, do NOT auto retry
                        JOptionPane.showMessageDialog(AdminMessageHub.this,
                            result.getMessage() + "\n\n" +
                            "IMPORTANT: The commit status could not be conclusively confirmed.\n" +
                            "Please check the 'Sent Message History' tab before retrying to prevent duplicate notifications.",
                            "Transaction Outcome Uncertain", JOptionPane.WARNING_MESSAGE);

                        loadSentHistory();

                    } else {
                        JOptionPane.showMessageDialog(AdminMessageHub.this,
                            result.getMessage(), "Dispatch Failed (" + result.getErrorCode() + ")",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminMessageHub.this,
                        "An unexpected error occurred during dispatch: " + ex.getMessage(),
                        "System Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    /**
     * Loads sent message history off the EDT using SwingWorker.
     */
    public void loadSentHistory() {
        btnRefreshHistory.setEnabled(false);
        lblHistoryCount.setText("Loading history...");

        SwingWorker<NotificationService.QueryResult<List<NotificationService.SentMessageSummary>>, Void> worker = new SwingWorker<>() {
            @Override
            protected NotificationService.QueryResult<List<NotificationService.SentMessageSummary>> doInBackground() {
                return NotificationService.getSentMessageHistory();
            }

            @Override
            protected void done() {
                btnRefreshHistory.setEnabled(true);
                try {
                    NotificationService.QueryResult<List<NotificationService.SentMessageSummary>> qr = get();
                    if (qr.isSuccess()) {
                        historyCache.clear();
                        if (qr.getData() != null) {
                            historyCache.addAll(qr.getData());
                        }
                        applyHistoryFilter(tfSearchHistory.getText());
                    } else {
                        lblHistoryCount.setText("Failed to load history: " + qr.getMessage());
                    }
                } catch (Exception ex) {
                    lblHistoryCount.setText("Error loading history: " + ex.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void applyHistoryFilter(String filter) {
        historyModel.setRowCount(0);
        String term = (filter != null) ? filter.trim().toLowerCase() : "";

        int matchingCount = 0;
        for (NotificationService.SentMessageSummary s : historyCache) {
            String title = s.getTitle() != null ? s.getTitle().toLowerCase() : "";
            String recip = s.getRecipientMeter() != null ? s.getRecipientMeter().toLowerCase() : "";

            if (term.isEmpty() || title.contains(term) || recip.contains(term)) {
                String recipDisplay;
                if ("INDIVIDUAL".equalsIgnoreCase(s.getTargetType())) {
                    String m = s.getRecipientMeter() != null ? s.getRecipientMeter() : "N/A";
                    String n = s.getRecipientName() != null ? s.getRecipientName() : "";
                    recipDisplay = m + (n.isEmpty() ? "" : " (" + n + ")");
                } else {
                    recipDisplay = "All Active Customers (" + s.getTotalRecipients() + ")";
                }

                historyModel.addRow(new Object[]{
                    s.getMessageId(),
                    s.getTargetType(),
                    recipDisplay,
                    s.getTitle(),
                    s.getPriority(),
                    s.getTotalRecipients(),
                    s.getReadCount(),
                    s.getUnreadCount(),
                    s.getCreatedAt() != null ? dateFormat.format(s.getCreatedAt()) : "-"
                });
                matchingCount++;
            }
        }

        lblHistoryCount.setText(matchingCount + " message(s) displayed");
    }

    private void openSelectedDetail() {
        int selectedRow = tableHistory.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Please select a message from the table to view complete details.",
                "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int messageId = (int) historyModel.getValueAt(selectedRow, 0);

        NotificationService.SentMessageSummary match = null;
        for (NotificationService.SentMessageSummary s : historyCache) {
            if (s.getMessageId() == messageId) {
                match = s;
                break;
            }
        }

        if (match != null) {
            new MessageDetailDialog(this, match).setVisible(true);
        }
    }

    private void setFormEnabled(boolean enabled) {
        rbIndividual.setEnabled(enabled);
        rbBroadcast.setEnabled(enabled);
        if (enabled && rbBroadcast.isSelected()) {
            tfMeter.setEnabled(false);
        } else {
            tfMeter.setEnabled(enabled);
        }
        cbPriority.setEnabled(enabled);
        tfTitle.setEnabled(enabled);
        taBody.setEnabled(enabled);
        btnSend.setEnabled(enabled);
        btnClear.setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSend) {
            handleSendMessage();
        } else if (e.getSource() == btnClear) {
            tfTitle.setText("");
            taBody.setText("");
            lblCharCount.setText("Body characters: 0 / 5000");
            if (rbIndividual.isSelected()) {
                tfMeter.setText("");
            }
        } else if (e.getSource() == btnRefreshHistory) {
            loadSentHistory();
        } else if (e.getSource() == btnSearchHistory) {
            applyHistoryFilter(tfSearchHistory.getText());
        } else if (e.getSource() == btnResetHistory) {
            tfSearchHistory.setText("");
            applyHistoryFilter("");
        } else if (e.getSource() == btnViewDetails) {
            openSelectedDetail();
        } else if (e.getSource() == btnClose) {
            dispose();
        }
    }
}
