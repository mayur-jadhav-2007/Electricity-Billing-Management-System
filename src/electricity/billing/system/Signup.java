package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class Signup extends JFrame implements ActionListener{

    JButton create, back;
    Choice accountType;
    JTextField meter, username, name;
    JPasswordField password;
    Signup(){
        setTitle("Electricity Billing System - Create Account");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        setBounds(450, 150, 700, 400);
        getContentPane().setBackground(Color.WHITE);
        setLayout(null);
        
        JPanel panel = new JPanel();
        panel.setBounds(30, 30, 650, 300);
        panel.setBorder(new TitledBorder(new LineBorder(new Color(173, 216, 230), 2), "Create-Account", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(172, 216, 230)));
        panel.setBackground(Color.WHITE);
        panel.setLayout(null);
        panel.setForeground(new Color(34, 139, 34));
        add(panel);
        
        JLabel heading = new JLabel("Create Account As");
        heading.setBounds(100, 50, 140, 20);
        heading.setForeground(Color.GRAY);
        heading.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(heading);
        
        accountType = new Choice();
        accountType.add("Admin");
        accountType.add("Customer");
        accountType.setBounds(260, 50, 150, 20);
        panel.add(accountType);
        
        JLabel lblmeter = new JLabel("Meter Number");
        lblmeter.setBounds(100, 90, 140, 20);
        lblmeter.setForeground(Color.GRAY);
        lblmeter.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblmeter.setVisible(false);
        panel.add(lblmeter);
        
        meter = new JTextField();
        meter.setBounds(260, 90, 150, 20);
        meter.setVisible(false);
        panel.add(meter);
        
        JLabel lblusername = new JLabel("Username");
        lblusername.setBounds(100, 130, 140, 20);
        lblusername.setForeground(Color.GRAY);
        lblusername.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(lblusername);
        
        username = new JTextField();
        username.setBounds(260, 130, 150, 20);
        panel.add(username);
        
        JLabel lblname = new JLabel("Name");
        lblname.setBounds(100, 170, 140, 20);
        lblname.setForeground(Color.GRAY);
        lblname.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(lblname);
        
        name = new JTextField();
        name.setBounds(260, 170, 150, 20);
        panel.add(name);
        
        meter.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent fe) {}
            
            @Override
            public void focusLost(FocusEvent fe) {
                String smeter = meter.getText() != null ? meter.getText().trim() : "";
                if (smeter.isEmpty()) return;
                
                Conn c = null;
                try {
                    c = new Conn();
                    if (c.c != null) {
                        String q = "select name from customer where meter_no = ?";
                        try (PreparedStatement ps = c.c.prepareStatement(q)) {
                            ps.setString(1, smeter);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    name.setText(rs.getString("name"));
                                } else {
                                    // Fallback to login table if customer record not yet in customer table
                                    String q2 = "select name from login where meter_no = ?";
                                    try (PreparedStatement ps2 = c.c.prepareStatement(q2)) {
                                        ps2.setString(1, smeter);
                                        try (ResultSet rs2 = ps2.executeQuery()) {
                                            if (rs2.next()) {
                                                name.setText(rs2.getString("name"));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        if (c.s != null) try { c.s.close(); } catch (Exception ignored) {}
                        if (c.c != null) try { c.c.close(); } catch (Exception ignored) {}
                    }
                }
            }
        });
        
        
        JLabel lblpassword = new JLabel("Password");
        lblpassword.setBounds(100, 210, 140, 20);
        lblpassword.setForeground(Color.GRAY);
        lblpassword.setFont(new Font("Tahoma", Font.BOLD, 14));
        panel.add(lblpassword);
        
        password = new JPasswordField();
        password.setBounds(260, 210, 150, 20);
        panel.add(password);
        
        accountType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ae) {
                String user = accountType.getSelectedItem();
                if (user.equals("Customer")) {
                    lblmeter.setVisible(true);
                    meter.setVisible(true);
                    name.setEditable(false);
                } else {
                    lblmeter.setVisible(false);
                    meter.setVisible(false);
                    name.setEditable(true);
                }
            }
        });
        
        create = new JButton("Create");
        create.setBackground(Color.BLACK);
        create.setForeground(Color.WHITE);
        create.setBounds(140, 260, 120, 25);
        create.addActionListener(this);
        panel.add(create);

        back = new JButton("Back");
        back.setBackground(Color.BLACK);
        back.setForeground(Color.WHITE);
        back.setBounds(300, 260, 120, 25);
        back.addActionListener(this);
        panel.add(back);
        
        ImageIcon i1 = new ImageIcon(ClassLoader.getSystemResource("icon/signupImage.png"));
        Image i2 = i1.getImage().getScaledInstance(250, 250, Image.SCALE_DEFAULT);
        ImageIcon i3 = new ImageIcon(i2);
        JLabel image = new JLabel(i3);
        image.setBounds(415, 30, 250, 250);
        panel.add(image);
        
        setVisible(true);
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == create) {
            String atype = accountType.getSelectedItem();
            String susername = username.getText() != null ? username.getText().trim() : "";
            String sname = name.getText() != null ? name.getText().trim() : "";
            String spassword = new String(password.getPassword());
            String smeter = meter.getText() != null ? meter.getText().trim() : "";
           
            if (susername.isEmpty() || spassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and Password cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (atype.equals("Customer") && smeter.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your Meter Number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Conn c = null;
            try {
                c = new Conn();
                if (c.c == null) {
                    JOptionPane.showMessageDialog(this, "Database connection failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Check if username is already taken
                String checkUserQuery = "select count(*) as cnt from login where username = ?";
                try (PreparedStatement ps = c.c.prepareStatement(checkUserQuery)) {
                    ps.setString(1, susername);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt("cnt") > 0) {
                            JOptionPane.showMessageDialog(this, "Username '" + susername + "' is already taken. Please choose another.", "Duplicate Username", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                }
                
                String hashedPassword = PasswordUtils.hashPassword(spassword);
                
                if (atype.equals("Admin")) {
                    // For Admin: determine next consistent Admin ID (e.g. ADMIN001, ADMIN002, ...)
                    String nextAdminId = "ADMIN001";
                    String findAdminQuery = "select meter_no from login where user = 'Admin' and meter_no like 'ADMIN%' order by meter_no desc limit 1";
                    try (PreparedStatement ps = c.c.prepareStatement(findAdminQuery);
                         ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String lastId = rs.getString("meter_no");
                            try {
                                int num = Integer.parseInt(lastId.replaceAll("[^0-9]", ""));
                                nextAdminId = String.format("ADMIN%03d", num + 1);
                            } catch (Exception ex) {
                                nextAdminId = "ADMIN" + System.currentTimeMillis() % 1000;
                            }
                        }
                    }
                    
                    String insertQuery = "insert into login (meter_no, username, name, password, user) values (?, ?, ?, ?, 'Admin')";
                    try (PreparedStatement ps = c.c.prepareStatement(insertQuery)) {
                        ps.setString(1, nextAdminId);
                        ps.setString(2, susername);
                        ps.setString(3, sname.isEmpty() ? susername : sname);
                        ps.setString(4, hashedPassword);
                        ps.executeUpdate();
                    }
                } else {
                    // For Customer: check if meter exists in login
                    String checkMeterQuery = "select count(*) as cnt from login where meter_no = ?";
                    boolean meterExistsInLogin = false;
                    try (PreparedStatement ps = c.c.prepareStatement(checkMeterQuery)) {
                        ps.setString(1, smeter);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt("cnt") > 0) {
                                meterExistsInLogin = true;
                            }
                        }
                    }
                    
                    if (meterExistsInLogin) {
                        String updateQuery = "update login set username = ?, name = ?, password = ?, user = 'Customer' where meter_no = ?";
                        try (PreparedStatement ps = c.c.prepareStatement(updateQuery)) {
                            ps.setString(1, susername);
                            ps.setString(2, sname);
                            ps.setString(3, hashedPassword);
                            ps.setString(4, smeter);
                            ps.executeUpdate();
                        }
                    } else {
                        // Check if meter exists in customer table
                        String checkCustQuery = "select name from customer where meter_no = ?";
                        String foundName = sname;
                        try (PreparedStatement ps = c.c.prepareStatement(checkCustQuery)) {
                            ps.setString(1, smeter);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    foundName = rs.getString("name");
                                } else {
                                    JOptionPane.showMessageDialog(this, "Meter Number " + smeter + " is not registered with electricity board.", "Meter Not Found", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                            }
                        }
                        
                        String insertQuery = "insert into login (meter_no, username, name, password, user) values (?, ?, ?, ?, 'Customer')";
                        try (PreparedStatement ps = c.c.prepareStatement(insertQuery)) {
                            ps.setString(1, smeter);
                            ps.setString(2, susername);
                            ps.setString(3, foundName);
                            ps.setString(4, hashedPassword);
                            ps.executeUpdate();
                        }
                    }
                }
                
                JOptionPane.showMessageDialog(this, "Account Created Successfully!");
                dispose();
                new Login();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error creating account: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                if (c != null) {
                    if (c.s != null) try { c.s.close(); } catch (Exception ignored) {}
                    if (c.c != null) try { c.c.close(); } catch (Exception ignored) {}
                }
            }
        } else if (ae.getSource() == back) {
            dispose();
            new Login();
        }
    }

    public static void main(String[] args) {
        new Signup();
    }
}
