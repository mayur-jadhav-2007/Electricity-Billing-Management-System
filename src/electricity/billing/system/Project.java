package electricity.billing.system;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class Project extends JFrame implements ActionListener {

    String atype, meter;
    JPanel sidebar, mainContent;
    JLabel welcomeLabel, userTypeLabel;
    private JButton btnCustomerNotifBadge;
    
    private String activeCommand = "Dashboard";
    private final List<JButton> navButtons = new ArrayList<>();
    private static final Map<String, ImageIcon> iconCache = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean windowClosed = new java.util.concurrent.atomic.AtomicBoolean(false);
    private JFrame activeSubWindow = null;
    private static volatile Project activeInstance = null;

    public Project(String atype, String meter) {
        activeInstance = this;
        this.atype = (atype == null || atype.trim().isEmpty()) ? "Admin" : atype.trim();
        this.meter = (meter == null) ? "" : meter.trim();
        
        UserSession.setSession(new SessionContext(this.atype.equalsIgnoreCase("Admin") ? "admin" : "customer", this.atype, this.meter));
        
        // Initialize default active navigation item to Dashboard
        this.activeCommand = "Dashboard";
        
        setTitle("Electricity Billing Management System - " + this.atype);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1024, 700));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
            @Override
            public void windowActivated(WindowEvent e) {
                if (activeSubWindow == null || !activeSubWindow.isDisplayable()) {
                    activeSubWindow = null;
                    if (!"Dashboard".equalsIgnoreCase(activeCommand)) {
                        setActiveNavigation("Dashboard");
                    }
                }
            }
        });
        
        initializeModernUI();
        setVisible(true);
    }
    
    public void handleLogout() {
        if (!windowClosed.compareAndSet(false, true)) {
            return;
        }
        if (activeInstance == this) {
            activeInstance = null;
        }
        closeActiveSubWindow();
        SessionContext session = UserSession.consumeSession();
        if (session != null) {
            AuditService.logAsync(
                session.getUsername(),
                session.getRole(),
                AuditService.ACTION_LOGOUT,
                AuditService.ENTITY_USER,
                session.getUsername(),
                "User logged out successfully.",
                null,
                null
            );
        }
        dispose();
        new Login();
    }
    
    /**
     * Safely loads and resizes icon assets from classpath resources or filesystem.
     * Uses synchronous ImageIO loading to guarantee fully-realized pixel buffers.
     * Tints icons to the specified color while preserving alpha transparency.
     * Handles missing icons gracefully without crashing.
     */
    public static ImageIcon getScaledIcon(String resourcePath, int size, Color tint) {
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            return null;
        }
        String key = resourcePath + "@" + size + "@" + (tint == null ? "null" : tint.getRGB());
        ImageIcon cached = iconCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        try {
            BufferedImage srcImg = null;
            
            // 1. Try ClassLoader resources
            java.net.URL url = Project.class.getClassLoader().getResource(resourcePath);
            if (url == null) {
                url = ClassLoader.getSystemResource(resourcePath);
            }
            if (url == null) {
                String alt = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
                url = Project.class.getResource(alt);
            }
            
            if (url != null) {
                try {
                    srcImg = ImageIO.read(url);
                } catch (Exception ignored) {}
            }
            
            // 2. Fallback to direct file system lookup
            if (srcImg == null) {
                String cleanPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
                File[] candidates = new File[] {
                    new File(cleanPath),
                    new File("src/" + cleanPath),
                    new File("build/" + cleanPath),
                    new File("build/classes/" + cleanPath)
                };
                for (File cand : candidates) {
                    if (cand.exists() && cand.isFile()) {
                        try {
                            srcImg = ImageIO.read(cand);
                            if (srcImg != null) break;
                        } catch (Exception ignored) {}
                    }
                }
            }
            
            // 3. Fallback: try ImageIcon on URL if ImageIO failed
            if (srcImg == null && url != null) {
                ImageIcon ii = new ImageIcon(url);
                Image img = ii.getImage();
                if (img != null && img.getWidth(null) > 0) {
                    srcImg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = srcImg.createGraphics();
                    g2.drawImage(img, 0, 0, null);
                    g2.dispose();
                }
            }
            
            if (srcImg == null) {
                return null;
            }
            
            // Scaled buffer (synchronously rendered with bilinear interpolation)
            BufferedImage bi = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bi.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(srcImg, 0, 0, size, size, null);
            g2.dispose();
            
            // Apply color tint if specified (preserving alpha channel)
            if (tint != null) {
                int rgb = tint.getRGB() & 0x00FFFFFF;
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        int pixel = bi.getRGB(x, y);
                        int alpha = (pixel >> 24) & 0xFF;
                        if (alpha > 0) {
                            bi.setRGB(x, y, (alpha << 24) | rgb);
                        }
                    }
                }
            }
            
            ImageIcon result = new ImageIcon(bi);
            iconCache.put(key, result);
            return result;
        } catch (Exception e) {
            // Gracefully handle missing icons without throwing or crashing
            return null;
        }
    }
    
    private void initializeModernUI() {
        setLayout(new BorderLayout());
        
        // Create modern sidebar
        createModernSidebar();
        
        // Create main content area
        createMainContent();
        
        // Add components
        add(sidebar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);
    }
    
    /**
     * Professional dark navy/blue sidebar with consistent white icons,
     * active state indicator, hover animations, and utility actions.
     */
    private void createModernSidebar() {
        sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Dark Navy gradient
                Color colorTop = new Color(15, 23, 42);    // #0F172A (Deep Slate Navy)
                Color colorBottom = new Color(30, 41, 59); // #1E293B (Midnight Navy)
                GradientPaint gp = new GradientPaint(0, 0, colorTop, 0, h, colorBottom);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
                // Subtle right divider border
                g2d.setColor(new Color(51, 65, 85, 120));
                g2d.drawLine(w - 1, 0, w - 1, h);
                g2d.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(270, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 14, 20, 14));
        
        // Brand Header section
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        brandPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 20, 8));
        
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        logoRow.setOpaque(false);
        ImageIcon powerIcon = getScaledIcon("icon/icons8-power-24.png", 24, new Color(56, 189, 248));
        JLabel logoImg = new JLabel(powerIcon);
        JLabel titleLabel = new JLabel("ELECTRICITY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        logoRow.add(logoImg);
        logoRow.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Billing Management System", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        brandPanel.add(logoRow);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(subtitleLabel);
        sidebar.add(brandPanel);
        
        // User profile card section
        JPanel userPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 41, 59, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(51, 65, 85));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setOpaque(false);
        userPanel.setMaximumSize(new Dimension(248, 90));
        userPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        userPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        
        JPanel userInfoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userInfoRow.setOpaque(false);
        
        ImageIcon userIconImg = getScaledIcon("icon/icons8-users-30.png", 26, Color.WHITE);
        JLabel userIconLabel = new JLabel(userIconImg);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        userTypeLabel = new JLabel(atype.equalsIgnoreCase("Admin") ? "ADMINISTRATOR" : "CUSTOMER");
        userTypeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userTypeLabel.setForeground(new Color(96, 165, 250));
        
        String meterText = (meter != null && !meter.isEmpty()) ? "Meter: " + meter : "Control Hub";
        JLabel meterLabel = new JLabel(meterText);
        meterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        meterLabel.setForeground(new Color(148, 163, 184));
        
        textPanel.add(userTypeLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(meterLabel);
        
        userInfoRow.add(userIconLabel);
        userInfoRow.add(textPanel);
        
        userPanel.add(userInfoRow);
        userPanel.add(Box.createVerticalStrut(6));
        
        ImageIcon sessionIcon = getScaledIcon("icon/icons8-power-24.png", 12, new Color(74, 222, 128));
        JLabel statusDot = new JLabel("Online Session Active", sessionIcon, JLabel.CENTER);
        statusDot.setIconTextGap(6);
        statusDot.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusDot.setForeground(new Color(74, 222, 128)); // Bright Green
        statusDot.setAlignmentX(Component.CENTER_ALIGNMENT);
        userPanel.add(statusDot);
        
        sidebar.add(userPanel);
        sidebar.add(Box.createVerticalStrut(20));
        
        // Section Header
        JLabel navHeader = new JLabel("MAIN NAVIGATION");
        navHeader.setFont(new Font("Segoe UI", Font.BOLD, 10));
        navHeader.setForeground(new Color(100, 116, 139));
        navHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(navHeader);
        sidebar.add(Box.createVerticalStrut(8));
        
        // Navigation buttons with white icons on dark sidebar
        addNavButton("icon/icons8-dashboard-48.png", "Dashboard", "Dashboard");
        if (atype.equalsIgnoreCase("Admin")) {
            addNavButton("icon/New Customer.png", "Customer Management", "Customer Management");
            addNavButton("icon/Customer Details.png", "Customer Details", "Customer Details");
            addNavButton("icon/Meter Info.png", "Meter Information", "Meter Information");
            addNavButton("icon/Deposit Details.png", "Deposit Details", "Deposit Details");
            addNavButton("icon/Calculate Bill.png", "Calculate Bill", "Calculate Bill");
            addNavButton("icon/icons8-edit-text-file-50.png", "Complaint Management", "Complaint Management");
            addNavButton("icon/icons8-search-50.png", "Complaint History", "Complaint History");
            addNavButton("icon/icons8-receipt-48.png", "Message Centre", "Message Centre");
            addNavButton("icon/icons8-graph-axes-24.png", "Analytics & Reports", "Analytics & Reports");
            addNavButton("icon/icons8-settings-50.png", "Audit Logs", "Audit Logs");
        } else {
            addNavButton("icon/View Info.png", "View Information", "View Information");
            addNavButton("icon/Update Info.png", "Update Information", "Update Information");
            addNavButton("icon/Pay Bill.png", "Pay Bill", "Pay Bill");
            addNavButton("icon/Bill Details.png", "Bill Details", "Bill Details");
            addNavButton("icon/Generate Bill.png", "Generate Bill", "Generate Bill");
            addNavButton("icon/icons8-pen-50.png", "Submit Complaint", "Submit Complaint");
            addNavButton("icon/icons8-search-50.png", "My Complaints", "My Complaints");
            addNavButton("icon/icons8-receipt-48.png", "Notifications", "Notifications");
        }
        
        // Utilities Section
        sidebar.add(Box.createVerticalStrut(18));
        JLabel utilHeader = new JLabel("SYSTEM UTILITIES");
        utilHeader.setFont(new Font("Segoe UI", Font.BOLD, 10));
        utilHeader.setForeground(new Color(100, 116, 139));
        utilHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(utilHeader);
        sidebar.add(Box.createVerticalStrut(8));
        
        JPanel utilitySection = new JPanel(new GridLayout(1, 2, 8, 0));
        utilitySection.setOpaque(false);
        utilitySection.setMaximumSize(new Dimension(248, 38));
        
        JButton notepadBtn = createSidebarUtilityButton("icon/icons8-edit-text-file-50.png", "Notepad", "Notepad");
        JButton calcBtn = createSidebarUtilityButton("icon/icons8-calculator-50.png", "Calculator", "Calculator");
        
        utilitySection.add(notepadBtn);
        utilitySection.add(calcBtn);
        sidebar.add(utilitySection);
        
        // Bottom logout button
        sidebar.add(Box.createVerticalGlue());
        JButton logoutBtn = createSidebarLogoutButton();
        sidebar.add(logoutBtn);
        sidebar.add(Box.createVerticalStrut(10));
    }
    
    public static Project getActiveInstance() {
        return activeInstance;
    }

    public synchronized String getActiveNavigation() {
        return this.activeCommand;
    }

    public synchronized JFrame getActiveSubWindow() {
        return this.activeSubWindow;
    }

    public synchronized void closeActiveSubWindow() {
        if (activeSubWindow != null) {
            JFrame win = activeSubWindow;
            activeSubWindow = null;
            try {
                if (win.isDisplayable()) {
                    win.dispose();
                }
            } catch (Exception ignored) {}
        }
    }

    public synchronized void trackSubWindow(JFrame window, String navItem) {
        if (activeSubWindow != null && activeSubWindow != window) {
            try {
                if (activeSubWindow.isDisplayable()) {
                    activeSubWindow.dispose();
                }
            } catch (Exception ignored) {}
        }
        activeSubWindow = window;
        setActiveNavigation(navItem);
        
        if (window != null) {
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    onSubWindowClosed(e.getWindow());
                }
                @Override
                public void windowClosing(WindowEvent e) {
                    onSubWindowClosed(e.getWindow());
                }
            });
        }
    }

    private synchronized void onSubWindowClosed(Window w) {
        if (activeSubWindow == w) {
            activeSubWindow = null;
            setActiveNavigation("Dashboard");
            toFront();
        }
    }

    public synchronized void setActiveNavigation(String command) {
        String normalized = (command == null || command.trim().isEmpty()) ? "Dashboard" : command.trim();
        if (normalized.equalsIgnoreCase("New Customer")) {
            normalized = "Customer Management";
        }
        this.activeCommand = normalized;
        
        for (JButton btn : navButtons) {
            String btnCmd = btn.getActionCommand();
            String btnTxt = btn.getText();
            if (isCommandActive(btnCmd, btnTxt)) {
                btn.setForeground(Color.WHITE);
            } else {
                btn.setForeground(new Color(226, 232, 240));
            }
        }
        
        if (sidebar != null) {
            sidebar.repaint();
        }
    }

    public boolean isCommandActive(String command, String text) {
        if (activeCommand == null || activeCommand.isEmpty()) {
            return false;
        }
        if (command != null && command.equalsIgnoreCase(activeCommand)) {
            return true;
        }
        if (text != null && text.equalsIgnoreCase(activeCommand)) {
            return true;
        }
        // Aliases for Customer Management / New Customer
        boolean isCustMgmtActive = "Customer Management".equalsIgnoreCase(activeCommand) || "New Customer".equalsIgnoreCase(activeCommand);
        boolean isCustMgmtTarget = "Customer Management".equalsIgnoreCase(command) || "New Customer".equalsIgnoreCase(command)
                || "Customer Management".equalsIgnoreCase(text) || "New Customer".equalsIgnoreCase(text);
        if (isCustMgmtActive && isCustMgmtTarget) {
            return true;
        }
        return false;
    }

    /**
     * Adds an interactive navigation button to the sidebar with:
     * - White 22px icon on dark navy background
     * - Distinct active/selected indicator state
     * - Smooth hover effects
     * - Hand cursor
     */
    private void addNavButton(String iconPath, String text, String command) {
        ImageIcon icon = getScaledIcon(iconPath, 22, Color.WHITE);
        
        JButton button = new JButton(text, icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                boolean isSelected = isCommandActive(command, text);
                boolean isHover = getModel().isRollover();
                
                if (isSelected) {
                    // Active Selected State: Royal Blue
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    // Active left indicator bar
                    g2.setColor(new Color(96, 165, 250));
                    g2.fillRoundRect(2, 6, 4, getHeight() - 12, 4, 4);
                } else if (isHover) {
                    // Hover State: Translucent White
                    g2.setColor(new Color(255, 255, 255, 24));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                
                super.paintComponent(g);
                g2.dispose();
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        boolean initialSelected = isCommandActive(command, text);
        button.setForeground(initialSelected ? Color.WHITE : new Color(226, 232, 240));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(14);
        button.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setActionCommand(command);
        button.addActionListener(this);
        button.setMinimumSize(new Dimension(248, 36));
        button.setMaximumSize(new Dimension(248, 44));
        button.setPreferredSize(new Dimension(248, 44));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!isCommandActive(command, text)) {
                    button.setForeground(new Color(226, 232, 240));
                }
            }
        });
        
        navButtons.add(button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(5));
    }
    
    private JButton createSidebarUtilityButton(String iconPath, String label, String command) {
        ImageIcon icon = getScaledIcon(iconPath, 18, Color.WHITE);
        JButton btn = new JButton(label, icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(51, 65, 85));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(71, 85, 105));
                } else {
                    g2.setColor(new Color(51, 65, 85, 160));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setForeground(Color.WHITE);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setIconTextGap(6);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setActionCommand(command);
        btn.addActionListener(this);
        return btn;
    }
    
    private JButton createSidebarLogoutButton() {
        ImageIcon icon = getScaledIcon("icon/icons8-logout-50.png", 18, Color.WHITE);
        JButton button = new JButton("Logout", icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(153, 27, 27));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(220, 38, 38));
                } else {
                    g2.setColor(new Color(185, 28, 28, 200));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setIconTextGap(10);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setActionCommand("Exit");
        button.addActionListener(this);
        button.setMaximumSize(new Dimension(248, 42));
        button.setPreferredSize(new Dimension(248, 42));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        return button;
    }
    
    /**
     * Creates top header and main content with summary cards and quick actions.
     */
    private void createMainContent() {
        mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(new Color(248, 250, 252)); // Light slate #F8FAFC
        
        // 1. Dashboard Header
        JPanel headerPanel = createDashboardHeader();
        mainContent.add(headerPanel, BorderLayout.NORTH);
        
        // 2. Dashboard Body (Scrollable for all desktop resolutions)
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(new Color(248, 250, 252));
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        
        if (atype.equalsIgnoreCase("Admin")) {
            createAdminDashboardContent(bodyPanel);
        } else {
            createCustomerDashboardContent(bodyPanel);
        }
        
        JScrollPane scrollPane = new JScrollPane(bodyPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBackground(new Color(248, 250, 252));
        
        mainContent.add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Dashboard Header displaying title, welcome message, status badge, and styled logout.
     */
    private JPanel createDashboardHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
            BorderFactory.createEmptyBorder(16, 28, 16, 28)
        ));
        
        // Left Title & Welcome
        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setOpaque(false);
        
        welcomeLabel = new JLabel(atype.equalsIgnoreCase("Admin") ? "Admin Dashboard" : "Customer Portal");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLabel.setForeground(new Color(15, 23, 42)); // Slate 900
        
        String welcomeSub = atype.equalsIgnoreCase("Admin") 
            ? "Welcome back, Admin! Real-time operational metrics & billing oversight."
            : "Welcome back! Account Meter: " + (meter.isEmpty() ? "N/A" : meter) + " — Real-time consumption overview.";
        JLabel subLabel = new JLabel(welcomeSub);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(new Color(100, 116, 139)); // Slate 500
        
        leftHeader.add(welcomeLabel);
        leftHeader.add(Box.createVerticalStrut(3));
        leftHeader.add(subLabel);
        
        // Right Controls: Status Badge + Logout Button
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightHeader.setOpaque(false);
        
        // System Online Badge
        JPanel statusBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        statusBadge.setBackground(new Color(240, 253, 244)); // Light green
        statusBadge.setBorder(BorderFactory.createLineBorder(new Color(187, 247, 208), 1, true));
        ImageIcon onlineIcon = getScaledIcon("icon/System Health.png", 14, new Color(22, 101, 52));
        JLabel statusText = new JLabel("System Online", onlineIcon, JLabel.LEFT);
        statusText.setIconTextGap(6);
        statusText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusText.setForeground(new Color(22, 101, 52));
        statusBadge.add(statusText);
        
        // Header Logout Button
        ImageIcon logoutIcon = getScaledIcon("icon/icons8-logout-50.png", 16, new Color(220, 38, 38));
        JButton headerLogout = new JButton("Logout", logoutIcon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(254, 202, 202));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(254, 226, 226));
                } else {
                    g2.setColor(new Color(254, 242, 242));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        headerLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        headerLogout.setForeground(new Color(220, 38, 38));
        headerLogout.setIconTextGap(6);
        headerLogout.setFocusPainted(false);
        headerLogout.setContentAreaFilled(false);
        headerLogout.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(254, 202, 202), 1, true),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        headerLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        headerLogout.setActionCommand("Exit");
        headerLogout.addActionListener(this);
        
        if (!atype.equalsIgnoreCase("Admin")) {
            btnCustomerNotifBadge = createCustomerNotificationBadgeButton();
            rightHeader.add(btnCustomerNotifBadge);
            refreshCustomerUnreadBadge();
        }
        
        rightHeader.add(statusBadge);
        rightHeader.add(headerLogout);
        
        header.add(leftHeader, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        
        return header;
    }
    
    private JButton createCustomerNotificationBadgeButton() {
        ImageIcon icon = getScaledIcon("icon/icons8-receipt-48.png", 14, new Color(37, 99, 235));
        JButton btn = new JButton("Notifications (0)", icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(224, 242, 254));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(240, 249, 255));
                } else {
                    g2.setColor(new Color(248, 250, 252));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(new Color(37, 99, 235));
        btn.setIconTextGap(6);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(203, 213, 225), 1, true),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setActionCommand("Notifications");
        btn.addActionListener(this);
        return btn;
    }
    
    public void refreshCustomerUnreadBadge() {
        if (atype.equalsIgnoreCase("Admin") || btnCustomerNotifBadge == null) {
            return;
        }
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                NotificationService.QueryResult<Integer> res = NotificationService.getUnreadNotificationCount();
                return (res != null && res.isSuccess() && res.getData() != null) ? res.getData() : 0;
            }
            
            @Override
            protected void done() {
                try {
                    int unread = get();
                    if (btnCustomerNotifBadge != null) {
                        btnCustomerNotifBadge.setText("Notifications (" + unread + ")");
                        if (unread > 0) {
                            btnCustomerNotifBadge.setForeground(new Color(220, 38, 38));
                            btnCustomerNotifBadge.setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(new Color(254, 202, 202), 1, true),
                                BorderFactory.createEmptyBorder(6, 12, 6, 12)
                            ));
                        } else {
                            btnCustomerNotifBadge.setForeground(new Color(37, 99, 235));
                            btnCustomerNotifBadge.setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(new Color(203, 213, 225), 1, true),
                                BorderFactory.createEmptyBorder(6, 12, 6, 12)
                            ));
                        }
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
    
    private void openCustomerNotifications() {
        CustomerNotificationHub hub = new CustomerNotificationHub();
        trackSubWindow(hub, "Notifications");
        hub.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                refreshCustomerUnreadBadge();
            }
        });
    }
    
    /**
     * Builds Admin dashboard statistics cards and quick action cards.
     */
    private void createAdminDashboardContent(JPanel container) {
        // Section Header
        JLabel sectionTitle = new JLabel("LIVE SYSTEM METRICS");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sectionTitle.setForeground(new Color(71, 85, 105));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(sectionTitle);
        container.add(Box.createVerticalStrut(12));
        
        // Fetch live database statistics safely
        int totalCustomers = 0;
        int activeCustomers = 0;
        int inactiveCustomers = 0;
        int totalBills = 0;
        int paidBills = 0;
        int unpaidBills = 0;
        double totalRevenue = 0.0;
        
        try (Conn c = new Conn()) {
            if (c.c != null) {
                try (Statement stmt = c.c.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM customer")) {
                        if (rs.next()) totalCustomers = rs.getInt(1);
                    }
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM customer WHERE status = 'Active'")) {
                        if (rs.next()) activeCustomers = rs.getInt(1);
                    }
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM customer WHERE status = 'Inactive'")) {
                        if (rs.next()) inactiveCustomers = rs.getInt(1);
                    }
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM bill")) {
                        if (rs.next()) totalBills = rs.getInt(1);
                    }
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM bill WHERE status = 'Paid'")) {
                        if (rs.next()) paidBills = rs.getInt(1);
                    }
                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM bill WHERE status != 'Paid'")) {
                        if (rs.next()) unpaidBills = rs.getInt(1);
                    }
                    try (ResultSet rs = stmt.executeQuery("SELECT coalesce(sum(cast(total_bill as decimal(10,2))), 0) FROM bill WHERE status = 'Paid'")) {
                        if (rs.next()) totalRevenue = rs.getDouble(1);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Database error loading dashboard statistics: " + e.getMessage());
        }
        
        // 8 Clean Summary Cards in a 2x4 grid
        JPanel statsGrid = new JPanel(new GridLayout(2, 4, 16, 16));
        statsGrid.setOpaque(false);
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        
        statsGrid.add(createStatCard("icon/Total Customers.png", "Total Customers", String.valueOf(totalCustomers), "Registered accounts", new Color(79, 70, 229)));
        statsGrid.add(createStatCard("icon/Active Customers.png", "Active Customers", String.valueOf(activeCustomers), "Operational accounts", new Color(16, 185, 129)));
        statsGrid.add(createStatCard("icon/Inactive Customers.png", "Inactive Customers", String.valueOf(inactiveCustomers), "Deactivated accounts", new Color(245, 158, 11)));
        statsGrid.add(createStatCard("icon/Total Revenue.png", "Total Revenue", "₹" + String.format(Locale.US, "%,.2f", totalRevenue), "Collected payments", new Color(5, 150, 105)));
        
        statsGrid.add(createStatCard("icon/Total Bills.png", "Total Bills", String.valueOf(totalBills), "Invoices generated", new Color(2, 132, 199)));
        statsGrid.add(createStatCard("icon/Paid Bills.png", "Paid Bills", String.valueOf(paidBills), "Settled invoices", new Color(13, 148, 136)));
        statsGrid.add(createStatCard("icon/Unpaid Bills.png", "Unpaid Bills", String.valueOf(unpaidBills), "Awaiting payment", new Color(225, 29, 72)));
        statsGrid.add(createStatCard("icon/System Health.png", "System Health", "100% Online", "Database connected", new Color(139, 92, 246)));
        
        container.add(statsGrid);
        container.add(Box.createVerticalStrut(30));
        
        // Quick Action Section
        JLabel quickActionTitle = new JLabel("QUICK MANAGEMENT ACTIONS");
        quickActionTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        quickActionTitle.setForeground(new Color(71, 85, 105));
        quickActionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(quickActionTitle);
        container.add(Box.createVerticalStrut(12));
        
        JPanel actionGrid = new JPanel(new GridLayout(1, 6, 12, 12));
        actionGrid.setOpaque(false);
        actionGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        
        actionGrid.add(createQuickActionButton("icon/New Customer.png", "New Customer", "Add and assign meter", "New Customer", new Color(79, 70, 229)));
        actionGrid.add(createQuickActionButton("icon/Customer Details.png", "Customer Details", "Search customer directory", "Customer Details", new Color(2, 132, 199)));
        actionGrid.add(createQuickActionButton("icon/Meter Info.png", "Meter Info", "Configure meter specs", "Meter Information", new Color(13, 148, 136)));
        actionGrid.add(createQuickActionButton("icon/Calculate Bill.png", "Calculate Bill", "Process monthly consumption", "Calculate Bill", new Color(245, 158, 11)));
        actionGrid.add(createQuickActionButton("icon/Deposit Details.png", "Deposit Details", "Inspect security deposits", "Deposit Details", new Color(139, 92, 246)));
        actionGrid.add(createQuickActionButton("icon/icons8-receipt-48.png", "Message Centre", "Direct & broadcast notices", "Message Centre", new Color(37, 99, 235)));
        
        container.add(actionGrid);
    }
    
    /**
     * Builds Customer dashboard statistics cards and quick action cards.
     */
    private void createCustomerDashboardContent(JPanel container) {
        JLabel sectionTitle = new JLabel("ACCOUNT & CONSUMPTION SUMMARY");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sectionTitle.setForeground(new Color(71, 85, 105));
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(sectionTitle);
        container.add(Box.createVerticalStrut(12));
        
        String latestBill = "₹0.00";
        String latestUsage = "0 Units";
        String latestStatus = "No Record";
        int custTotalBills = 0;
        int custPaidBills = 0;
        int custUnpaidBills = 0;
        double custTotalPaid = 0.0;
        double custOutstanding = 0.0;
        
        try (Conn c = new Conn()) {
            if (c.c != null && meter != null && !meter.isEmpty()) {
                String qLatest = "SELECT total_bill, units, status FROM bill WHERE meter_no = ? ORDER BY FIELD(month, 'December','November','October','September','August','July','June','May','April','March','February','January') LIMIT 1";
                try (PreparedStatement ps = c.c.prepareStatement(qLatest)) {
                    ps.setString(1, meter);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            latestBill = "₹" + rs.getString("total_bill");
                            latestUsage = rs.getString("units") + " Units";
                            latestStatus = rs.getString("status");
                        }
                    }
                }
                
                String qCounts = "SELECT "
                    + "(SELECT count(*) FROM bill WHERE meter_no = ?) AS total_b, "
                    + "(SELECT count(*) FROM bill WHERE meter_no = ? AND status = 'Paid') AS paid_b, "
                    + "(SELECT count(*) FROM bill WHERE meter_no = ? AND status != 'Paid') AS unpaid_b, "
                    + "(SELECT coalesce(sum(cast(total_bill as decimal(10,2))), 0) FROM bill WHERE meter_no = ? AND status = 'Paid') AS paid_sum, "
                    + "(SELECT coalesce(sum(cast(total_bill as decimal(10,2))), 0) FROM bill WHERE meter_no = ? AND status != 'Paid') AS unpaid_sum";
                try (PreparedStatement ps = c.c.prepareStatement(qCounts)) {
                    ps.setString(1, meter);
                    ps.setString(2, meter);
                    ps.setString(3, meter);
                    ps.setString(4, meter);
                    ps.setString(5, meter);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            custTotalBills = rs.getInt("total_b");
                            custPaidBills = rs.getInt("paid_b");
                            custUnpaidBills = rs.getInt("unpaid_b");
                            custTotalPaid = rs.getDouble("paid_sum");
                            custOutstanding = rs.getDouble("unpaid_sum");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Database error loading customer statistics: " + e.getMessage());
        }
        
        JPanel statsGrid = new JPanel(new GridLayout(2, 4, 16, 16));
        statsGrid.setOpaque(false);
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        
        statsGrid.add(createStatCard("icon/Latest Bill.png", "Latest Bill", latestBill, "Current cycle amount", new Color(37, 99, 235)));
        statsGrid.add(createStatCard("icon/Latest Usage.png", "Latest Usage", latestUsage, "Metered units", new Color(13, 148, 136)));
        statsGrid.add(createStatCard("icon/Bill Status.png", "Bill Status", latestStatus, "Current invoice status", latestStatus.equalsIgnoreCase("Paid") ? new Color(16, 185, 129) : new Color(225, 29, 72)));
        statsGrid.add(createStatCard("icon/Meter Number.png", "Meter Number", meter.isEmpty() ? "Unassigned" : meter, "Active hardware ID", new Color(79, 70, 229)));
        
        statsGrid.add(createStatCard("icon/Total Invoices.png", "Total Invoices", String.valueOf(custTotalBills), "Lifetime billings", new Color(2, 132, 199)));
        statsGrid.add(createStatCard("icon/Paid Invoics.png", "Paid Invoices", String.valueOf(custPaidBills), "Cleared payments", new Color(16, 185, 129)));
        statsGrid.add(createStatCard("icon/Total Paid.png", "Total Paid", "₹" + String.format(Locale.US, "%,.2f", custTotalPaid), "Cumulative settlements", new Color(5, 150, 105)));
        statsGrid.add(createStatCard("icon/Outstanding Due.png", "Outstanding Due", "₹" + String.format(Locale.US, "%,.2f", custOutstanding), "Pending balance", new Color(225, 29, 72)));
        
        container.add(statsGrid);
        container.add(Box.createVerticalStrut(30));
        
        // Customer Quick Actions
        JLabel quickActionTitle = new JLabel("PORTAL SERVICES");
        quickActionTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        quickActionTitle.setForeground(new Color(71, 85, 105));
        quickActionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(quickActionTitle);
        container.add(Box.createVerticalStrut(12));
        
        JPanel actionGrid = new JPanel(new GridLayout(1, 6, 12, 12));
        actionGrid.setOpaque(false);
        actionGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        
        actionGrid.add(createQuickActionButton("icon/Pay Bill.png", "Pay Bill", "Process bill payment", "Pay Bill", new Color(5, 150, 105)));
        actionGrid.add(createQuickActionButton("icon/Generate Bill.png", "Generate Bill", "View and print invoice", "Generate Bill", new Color(2, 132, 199)));
        actionGrid.add(createQuickActionButton("icon/Bill Details.png", "Bill Details", "Past invoice history", "Bill Details", new Color(79, 70, 229)));
        actionGrid.add(createQuickActionButton("icon/View Info.png", "View Info", "Account profile & meter", "View Information", new Color(13, 148, 136)));
        actionGrid.add(createQuickActionButton("icon/Update Info.png", "Update Info", "Update contact details", "Update Information", new Color(245, 158, 11)));
        actionGrid.add(createQuickActionButton("icon/icons8-receipt-48.png", "Notifications", "View alerts & notices", "Notifications", new Color(37, 99, 235)));
        
        container.add(actionGrid);
    }
    
    /**
     * Creates a clean, professional summary card:
     * - White rounded background with subtle border
     * - Top accent colored stripe
     * - Consistent card dimensions
     * - Black icon inside a soft tinted badge (Prefer black icons on light backgrounds)
     * - Currency properly formatted with ₹
     */
    private JPanel createStatCard(String iconPath, String title, String value, String subtext, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                
                // Card Background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                
                // Subtle Border
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                
                // Top Accent Stripe
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, w, 4, 4, 4);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(200, 115));
        card.setMinimumSize(new Dimension(170, 110));
        
        // Top Row: Title + Icon Badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(new Color(100, 116, 139)); // Slate 500
        
        // Icon Badge Container (tinted background with black icon)
        JPanel iconBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setPreferredSize(new Dimension(36, 36));
        iconBadge.setOpaque(false);
        
        // Crisp scaled icon on light badge container
        ImageIcon icon = getScaledIcon(iconPath, 24, new Color(30, 41, 59));
        JLabel iconLabel = new JLabel(icon);
        iconBadge.add(iconLabel);
        
        topRow.add(titleLabel, BorderLayout.WEST);
        topRow.add(iconBadge, BorderLayout.EAST);
        
        // Center: Metric Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(new Color(15, 23, 42)); // Slate 900
        valueLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));
        
        // Bottom: Context Subtext
        JLabel subLabel = new JLabel(subtext);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(new Color(148, 163, 184)); // Slate 400
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.add(valueLabel);
        centerPanel.add(subLabel);
        
        card.add(topRow, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    /**
     * Creates a Quick Action Card Button with icon, title, description, and hover animation.
     */
    private JButton createQuickActionButton(String iconPath, String title, String description, String command, Color accentColor) {
        ImageIcon icon = getScaledIcon(iconPath, 22, new Color(30, 41, 59));
        
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                
                if (getModel().isPressed()) {
                    g2.setColor(new Color(241, 245, 249));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(240, 249, 255));
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                
                if (getModel().isRollover()) {
                    g2.setColor(new Color(147, 197, 253)); // Soft blue highlight
                } else {
                    g2.setColor(new Color(226, 232, 240));
                }
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setLayout(new BorderLayout(12, 0));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setActionCommand(command);
        btn.addActionListener(this);
        
        // Icon Container - centered square badge
        JPanel iconPill = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 26));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconPill.setPreferredSize(new Dimension(36, 36));
        iconPill.setMinimumSize(new Dimension(36, 36));
        iconPill.setMaximumSize(new Dimension(36, 36));
        iconPill.setOpaque(false);
        iconPill.add(new JLabel(icon));
        
        // Wrapper prevents BorderLayout.WEST from stretching height vertically into a rectangle
        JPanel iconWrapper = new JPanel(new GridBagLayout());
        iconWrapper.setOpaque(false);
        iconWrapper.add(iconPill);
        
        // Text Container
        JPanel textP = new JPanel();
        textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));
        textP.setOpaque(false);
        
        JLabel tLbl = new JLabel(title);
        tLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tLbl.setForeground(new Color(30, 41, 59));
        
        JLabel dLbl = new JLabel(description);
        dLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dLbl.setForeground(new Color(100, 116, 139));
        
        textP.add(tLbl);
        textP.add(Box.createVerticalStrut(2));
        textP.add(dLbl);
        
        btn.add(iconWrapper, BorderLayout.WEST);
        btn.add(textP, BorderLayout.CENTER);
        
        return btn;
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        String msg = ae.getActionCommand();
        if (msg == null) return;
        
        if (msg.equals("Dashboard")) {
            closeActiveSubWindow();
            setActiveNavigation("Dashboard");
            toFront();
            return;
        } else if (msg.equals("Exit")) {
            handleLogout();
            return;
        } else if (msg.equals("Notepad")) {
            try {
                new ProcessBuilder("notepad.exe").start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        } else if (msg.equals("Calculator")) {
            try {
                new ProcessBuilder("calc.exe").start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        
        // Execute existing actions with sub-window tracking and active navigation updates
        if (msg.equals("New Customer") || msg.equals("Customer Management")) {
            trackSubWindow(new NewCustomer(), "Customer Management");
        } else if (msg.equals("Customer Details")) {
            trackSubWindow(new CustomerDetails(), "Customer Details");
        } else if (msg.equals("Meter Information")) {
            trackSubWindow(new MeterInfo(meter != null ? meter : ""), "Meter Information");
        } else if (msg.equals("Deposit Details")) {
            trackSubWindow(new DepositDetails(), "Deposit Details");
        } else if (msg.equals("Calculate Bill")) {
            trackSubWindow(new CalculateBill(), "Calculate Bill");
        } else if (msg.equals("View Information")) {
            trackSubWindow(new ViewInformation(meter), "View Information");
        } else if (msg.equals("Update Information")) {
            trackSubWindow(new UpdateInformation(meter), "Update Information");
        } else if (msg.equals("Bill Details")) {
            trackSubWindow(new BillDetails(meter), "Bill Details");
        } else if (msg.equals("Pay Bill")) {
            trackSubWindow(new PayBill(meter), "Pay Bill");
        } else if (msg.equals("Generate Bill")) {
            trackSubWindow(new GenerateBill(meter), "Generate Bill");
        } else if (msg.equals("Submit Complaint")) {
            trackSubWindow(new SubmitComplaint(meter), "Submit Complaint");
        } else if (msg.equals("My Complaints")) {
            trackSubWindow(new MyComplaints(meter), "My Complaints");
        } else if (msg.equals("Complaint Management")) {
            trackSubWindow(new ComplaintManagement(), "Complaint Management");
        } else if (msg.equals("Complaint History")) {
            trackSubWindow(new ComplaintHistory(), "Complaint History");
        } else if (msg.equals("Analytics & Reports")) {
            trackSubWindow(new AnalyticsDashboard(), "Analytics & Reports");
        } else if (msg.equals("Audit Logs")) {
            trackSubWindow(new AuditLogViewer(), "Audit Logs");
        } else if (msg.equals("Message Centre")) {
            trackSubWindow(new AdminMessageHub(), "Message Centre");
        } else if (msg.equals("Notifications")) {
            openCustomerNotifications();
        }
    }

    public static void main(String[] args) {
        new Project("Admin", "");
    }
}
