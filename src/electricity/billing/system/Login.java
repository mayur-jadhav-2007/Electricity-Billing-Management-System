package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Login extends JFrame implements ActionListener{

    JButton login, cancel, signup;
    JTextField username;
    JPasswordField password;
    Choice logginin;
    Login() {
        super("Electricity Billing System - Login");
        
        // Modern gradient background
        setContentPane(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                Color color1 = new Color(135, 206, 250); // Light sky blue
                Color color2 = new Color(25, 25, 112);   // Midnight blue
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        });
        setLayout(null);
        
        // Create main login panel
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(null);
        loginPanel.setBounds(320, 35, 350, 305);
        loginPanel.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white
        loginPanel.setBorder(new CompoundBorder(
            new RoundedBorder(15), 
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        add(loginPanel);
        
        // Title
        JLabel titleLabel = new JLabel("ELECTRICITY BILLING SYSTEM", JLabel.CENTER);
        titleLabel.setBounds(0, 10, 350, 30);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(25, 25, 112));
        loginPanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Please login to continue", JLabel.CENTER);
        subtitleLabel.setBounds(0, 35, 350, 20);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        loginPanel.add(subtitleLabel);
        
        JLabel lblusername = new JLabel("Username:");
        lblusername.setBounds(30, 80, 100, 25);
        lblusername.setFont(new Font("Arial", Font.BOLD, 14));
        lblusername.setForeground(new Color(25, 25, 112));
        loginPanel.add(lblusername);
        
        username = new JTextField();
        username.setBounds(30, 105, 290, 30);
        username.setFont(new Font("Arial", Font.PLAIN, 14));
        username.setBorder(new CompoundBorder(
            new LineBorder(new Color(100, 149, 237), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        loginPanel.add(username);
        
        JLabel lblpassword = new JLabel("Password:");
        lblpassword.setBounds(30, 145, 100, 25);
        lblpassword.setFont(new Font("Arial", Font.BOLD, 14));
        lblpassword.setForeground(new Color(25, 25, 112));
        loginPanel.add(lblpassword);
        
        password = new JPasswordField();
        password.setBounds(30, 170, 290, 30);
        password.setFont(new Font("Arial", Font.PLAIN, 14));
        password.setBorder(new CompoundBorder(
            new LineBorder(new Color(100, 149, 237), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        loginPanel.add(password);
        
        JLabel loggininas = new JLabel("Login as:");
        loggininas.setBounds(30, 210, 100, 25);
        loggininas.setFont(new Font("Arial", Font.BOLD, 14));
        loggininas.setForeground(new Color(25, 25, 112));
        loginPanel.add(loggininas);
        
        logginin = new Choice();
        logginin.add("Admin");
        logginin.add("Customer");
        logginin.setBounds(140, 212, 180, 25);
        logginin.setFont(new Font("Arial", Font.PLAIN, 14));
        loginPanel.add(logginin);
        
        // Modern styled buttons
        login = createStyledButton("LOGIN", new Color(76, 175, 80), Color.WHITE);
        login.setBounds(50, 250, 90, 35);
        login.addActionListener(this);
        loginPanel.add(login);
        
        cancel = createStyledButton("CANCEL", new Color(244, 67, 54), Color.WHITE);
        cancel.setBounds(150, 250, 90, 35);
        cancel.addActionListener(this);
        loginPanel.add(cancel);
        
        signup = createStyledButton("SIGNUP", new Color(33, 150, 243), Color.WHITE);
        signup.setBounds(250, 250, 90, 35);
        signup.addActionListener(this);
        loginPanel.add(signup);
        
        // Side image panel
        JPanel imagePanel = new JPanel();
        imagePanel.setBounds(30, 60, 270, 270);
        imagePanel.setBackground(new Color(255, 255, 255, 150));
        imagePanel.setBorder(new RoundedBorder(15));
        imagePanel.setLayout(new BorderLayout());
        
        try {
            ImageIcon i7 = new ImageIcon(ClassLoader.getSystemResource("icon/second.jpg"));
            Image i8 = i7.getImage().getScaledInstance(250, 250, Image.SCALE_SMOOTH);
            ImageIcon i9 = new ImageIcon(i8);
            JLabel image = new JLabel(i9, JLabel.CENTER);
            imagePanel.add(image, BorderLayout.CENTER);
        } catch (Exception e) {
            // Fallback if image not found
            JLabel fallback = new JLabel("⚡", JLabel.CENTER);
            fallback.setFont(new Font("Arial", Font.BOLD, 80));
            fallback.setForeground(new Color(255, 215, 0));
            imagePanel.add(fallback, BorderLayout.CENTER);
        }
        add(imagePanel);
        
        setSize(720, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
    }
    
    /**
     * Authenticate credentials and check customer active status.
     * Returns: "SUCCESS", "DEACTIVATED", "INVALID_CREDENTIALS", or "DB_ERROR".
     */
    public static String authenticateUser(String susername, String spassword, String user) {
        if (susername == null || susername.trim().isEmpty() || spassword == null || spassword.isEmpty()) {
            return "INVALID_CREDENTIALS";
        }
        
        Conn c = null;
        try {
            c = new Conn();
            if (c.c == null) {
                return "DB_ERROR";
            }
            
            String query = "select meter_no, password, user from login where username = ? and user = ?";
            try (PreparedStatement ps = c.c.prepareStatement(query)) {
                ps.setString(1, susername.trim());
                ps.setString(2, user);
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        String meter = rs.getString("meter_no");
                        String dbRole = rs.getString("user");
                        
                        if (PasswordUtils.verifyPassword(spassword, storedPassword)) {
                            // Transparent lazy migration: Upgrade legacy plaintext password to secure hash
                            if (!PasswordUtils.isHashed(storedPassword)) {
                                String upgradedHash = PasswordUtils.hashPassword(spassword);
                                String updateHashQuery = "update login set password = ? where username = ? and user = ?";
                                try (PreparedStatement updatePs = c.c.prepareStatement(updateHashQuery)) {
                                    updatePs.setString(1, upgradedHash);
                                    updatePs.setString(2, susername.trim());
                                    updatePs.setString(3, user);
                                    updatePs.executeUpdate();
                                }
                            }
                            
                            // Check customer deactivation status for Customer role
                            if ("Customer".equalsIgnoreCase(user) && meter != null && !meter.trim().isEmpty()) {
                                String statusQuery = "select status from customer where meter_no = ?";
                                try (PreparedStatement statusPs = c.c.prepareStatement(statusQuery)) {
                                    statusPs.setString(1, meter.trim());
                                    try (ResultSet statusRs = statusPs.executeQuery()) {
                                        if (statusRs.next()) {
                                            String custStatus = statusRs.getString("status");
                                            if ("Inactive".equalsIgnoreCase(custStatus)) {
                                                AuditService.logIndependent(susername.trim(), "Customer", AuditService.ACTION_LOGIN_FAILED, AuditService.ENTITY_USER, susername.trim(), "Failed authentication: Account deactivated", null, null);
                                                return "DEACTIVATED";
                                            }
                                        }
                                    }
                                }
                            }
                            
                            UserSession.setSession(new SessionContext(susername.trim(), dbRole != null ? dbRole : user, meter));
                            AuditService.logIndependent(susername.trim(), dbRole != null ? dbRole : user, AuditService.ACTION_LOGIN_SUCCESS, AuditService.ENTITY_USER, susername.trim(), "User authenticated successfully as " + user, null, null);
                            return "SUCCESS";
                        }
                    }
                }
            }
            AuditService.logIndependent(susername.trim(), "Unknown", AuditService.ACTION_LOGIN_FAILED, AuditService.ENTITY_USER, susername.trim(), "Failed authentication: Invalid credentials", null, null);
            return "INVALID_CREDENTIALS";
        } catch (Exception e) {
            e.printStackTrace();
            return "DB_ERROR";
        } finally {
            if (c != null) {
                if (c.s != null) try { c.s.close(); } catch (Exception ignored) {}
                if (c.c != null) try { c.c.close(); } catch (Exception ignored) {}
            }
        }
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == login) {
            String susername = username.getText() != null ? username.getText().trim() : "";
            String spassword = new String(password.getPassword());
            String user = logginin.getSelectedItem();
            
            if (susername.isEmpty() || spassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Conn c = null;
            try {
                c = new Conn();
                if (c.c == null) {
                    JOptionPane.showMessageDialog(this, "Database connection failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String query = "select meter_no, password, user from login where username = ? and user = ?";
                try (PreparedStatement ps = c.c.prepareStatement(query)) {
                    ps.setString(1, susername);
                    ps.setString(2, user);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password");
                            String meter = rs.getString("meter_no");
                            String dbRole = rs.getString("user");
                            String verifiedRole = (dbRole != null && !dbRole.isEmpty()) ? dbRole : user;
                            
                            // Check password using PasswordUtils (supports secure hash & legacy plaintext)
                            if (PasswordUtils.verifyPassword(spassword, storedPassword)) {
                                // Transparent lazy migration: Upgrade legacy plaintext password to secure hash
                                if (!PasswordUtils.isHashed(storedPassword)) {
                                    String upgradedHash = PasswordUtils.hashPassword(spassword);
                                    String updateHashQuery = "update login set password = ? where username = ? and user = ?";
                                    try (PreparedStatement updatePs = c.c.prepareStatement(updateHashQuery)) {
                                        updatePs.setString(1, upgradedHash);
                                        updatePs.setString(2, susername);
                                        updatePs.setString(3, user);
                                        updatePs.executeUpdate();
                                    }
                                }
                                
                                // Check customer active status
                                if ("Customer".equalsIgnoreCase(user) && meter != null && !meter.trim().isEmpty()) {
                                    String statusQuery = "select status from customer where meter_no = ?";
                                    try (PreparedStatement statusPs = c.c.prepareStatement(statusQuery)) {
                                        statusPs.setString(1, meter.trim());
                                        try (ResultSet statusRs = statusPs.executeQuery()) {
                                            if (statusRs.next()) {
                                                String custStatus = statusRs.getString("status");
                                                if ("Inactive".equalsIgnoreCase(custStatus)) {
                                                    AuditService.logIndependent(susername, "Customer", AuditService.ACTION_LOGIN_FAILED, AuditService.ENTITY_USER, susername, "Failed login attempt: Account deactivated", null, null);
                                                    JOptionPane.showMessageDialog(this,
                                                        "Your account has been deactivated. Please contact administration.",
                                                        "Account Deactivated",
                                                        JOptionPane.ERROR_MESSAGE);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                UserSession.setSession(new SessionContext(susername, verifiedRole, meter));
                                AuditService.logIndependent(susername, verifiedRole, AuditService.ACTION_LOGIN_SUCCESS, AuditService.ENTITY_USER, susername, "User logged in successfully as " + verifiedRole, null, null);
                                
                                dispose();
                                new Project(verifiedRole, meter);
                                return;
                            }
                        }
                    }
                }
                
                AuditService.logIndependent(susername, "Unknown", AuditService.ACTION_LOGIN_FAILED, AuditService.ENTITY_USER, susername, "Failed login attempt: Invalid username or password", null, null);
                JOptionPane.showMessageDialog(this, "Invalid Username or Password.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                password.setText("");
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error during authentication: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                if (c != null) {
                    if (c.s != null) try { c.s.close(); } catch (Exception ignored) {}
                    if (c.c != null) try { c.c.close(); } catch (Exception ignored) {}
                }
            }
        } else if (ae.getSource() == cancel) {
            dispose();
            System.exit(0);
        } else if (ae.getSource() == signup) {
            dispose();
            new Signup();
        }
    }
    
    // Helper method to create styled buttons
    private JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effects
        button.addMouseListener(new MouseAdapter() {
            Color originalColor = bgColor;
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(originalColor.brighter());
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalColor);
            }
        });
        
        return button;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Login();
            }
        });
    }
}

// Custom rounded border class
class RoundedBorder extends AbstractBorder {
    private int radius;
    
    public RoundedBorder(int radius) {
        this.radius = radius;
    }
    
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(100, 149, 237, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x + 1, y + 1, width - 2, height - 2, radius, radius);
        g2.dispose();
    }
    
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
    }
}
