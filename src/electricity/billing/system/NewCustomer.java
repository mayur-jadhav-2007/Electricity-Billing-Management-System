package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.sql.*;

public class NewCustomer extends JFrame implements ActionListener{

    JTextField tfname, tfaddress, tfstate, tfcity, tfemail, tfphone;
    JButton next, cancel;
    JLabel lblmeter;
    NewCustomer() {
        setTitle("Electricity Billing System - New Customer Registration");
        setSize(700, 500);
        setLocation(400, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBackground(new Color(173, 216, 230));
        add(p);
        
        JLabel heading = new JLabel("New Customer");
        heading.setBounds(180, 10, 200, 25);
        heading.setFont(new Font("Tahoma", Font.PLAIN, 24));
        p.add(heading);
        
        JLabel lblname = new JLabel("Customer Name");
        lblname.setBounds(100, 80, 100, 20);
        p.add(lblname);
        
        tfname = new JTextField();
        tfname.setBounds(240, 80, 200, 20);
        p.add(tfname);
        
        JLabel lblmeterno = new JLabel("Meter Number");
        lblmeterno.setBounds(100, 120, 100, 20);
        p.add(lblmeterno);
        
        lblmeter = new JLabel("");
        lblmeter.setBounds(240, 120, 100, 20);
        p.add(lblmeter);
        
        lblmeter.setText(generateUniqueMeterNumber());
        
        JLabel lbladdress = new JLabel("Address");
        lbladdress.setBounds(100, 160, 100, 20);
        p.add(lbladdress);
        
        tfaddress = new JTextField();
        tfaddress.setBounds(240, 160, 200, 20);
        p.add(tfaddress);
        
        JLabel lblcity = new JLabel("City");
        lblcity.setBounds(100, 200, 100, 20);
        p.add(lblcity);
        
        tfcity = new JTextField();
        tfcity.setBounds(240, 200, 200, 20);
        p.add(tfcity);
        
        JLabel lblstate = new JLabel("State");
        lblstate.setBounds(100, 240, 100, 20);
        p.add(lblstate);
        
        tfstate = new JTextField();
        tfstate.setBounds(240, 240, 200, 20);
        p.add(tfstate);
        
        JLabel lblemail = new JLabel("Email");
        lblemail.setBounds(100, 280, 100, 20);
        p.add(lblemail);
        
        tfemail = new JTextField();
        tfemail.setBounds(240, 280, 200, 20);
        p.add(tfemail);
        
        JLabel lblphone = new JLabel("Phone Number");
        lblphone.setBounds(100, 320, 100, 20);
        p.add(lblphone);
        
        tfphone = new JTextField();
        tfphone.setBounds(240, 320, 200, 20);
        p.add(tfphone);
        
        next = new JButton("Next");
        next.setBounds(120, 390, 100,25);
        next.setBackground(Color.BLACK);
        next.setForeground(Color.WHITE);
        ImageIcon nextIcon = Project.getScaledIcon("icon/New Customer.png", 16, Color.WHITE);
        if (nextIcon != null) {
            next.setIcon(nextIcon);
            next.setIconTextGap(6);
        }
        next.addActionListener(this);
        p.add(next);
        
        cancel = new JButton("Cancel");
        cancel.setBounds(250, 390, 100,25);
        cancel.setBackground(Color.BLACK);
        cancel.setForeground(Color.WHITE);
        cancel.addActionListener(this);
        p.add(cancel);
        
        setLayout(new BorderLayout());
        
        add(p, "Center");
        
        ImageIcon i1 = new ImageIcon(ClassLoader.getSystemResource("icon/hicon1.jpg"));
        Image i2 = i1.getImage().getScaledInstance(150, 300, Image.SCALE_DEFAULT);
        ImageIcon i3 = new ImageIcon(i2);
        JLabel image = new JLabel(i3);
        add(image, "West");
        
        getContentPane().setBackground(Color.WHITE);
        
        setVisible(true);
    }
    
    private String generateUniqueMeterNumber() {
        Random ran = new Random();
        try (Conn c = new Conn()) {
            if (c.c != null) {
                String q = "select count(*) from customer where meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(q)) {
                    for (int attempts = 0; attempts < 100; attempts++) {
                        String candidate = String.format("%06d", Math.abs(ran.nextLong() % 1000000));
                        ps.setString(1, candidate);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) == 0) {
                                return candidate;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return String.format("%06d", Math.abs(ran.nextLong() % 1000000));
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == next) {
            String name = tfname.getText() != null ? tfname.getText().trim() : "";
            String meter = lblmeter.getText();
            String address = tfaddress.getText() != null ? tfaddress.getText().trim() : "";
            String city = tfcity.getText() != null ? tfcity.getText().trim() : "";
            String state = tfstate.getText() != null ? tfstate.getText().trim() : "";
            String email = tfemail.getText() != null ? tfemail.getText().trim() : "";
            String phone = tfphone.getText() != null ? tfphone.getText().trim() : "";
            
            if (name.isEmpty() || address.isEmpty() || city.isEmpty() || state.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all customer fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!email.contains("@") || !email.contains(".")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (phone.length() < 7) {
                JOptionPane.showMessageDialog(this, "Please enter a valid phone number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Conn c = null;
            try {
                c = new Conn();
                if (c.c == null) {
                    JOptionPane.showMessageDialog(this, "Database connection failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                c.c.setAutoCommit(false);
                
                String query1 = "insert into customer (name, meter_no, address, city, state, email, phone, status) values (?, ?, ?, ?, ?, ?, ?, 'Active')";
                try (PreparedStatement ps1 = c.c.prepareStatement(query1)) {
                    ps1.setString(1, name);
                    ps1.setString(2, meter);
                    ps1.setString(3, address);
                    ps1.setString(4, city);
                    ps1.setString(5, state);
                    ps1.setString(6, email);
                    ps1.setString(7, phone);
                    ps1.executeUpdate();
                }
                
                String query2 = "insert into login (meter_no, username, name, password, user) values (?, '', ?, '', 'Customer')";
                try (PreparedStatement ps2 = c.c.prepareStatement(query2)) {
                    ps2.setString(1, meter);
                    ps2.setString(2, name);
                    ps2.executeUpdate();
                }
                
                String actorId = UserSession.getActorId();
                String actorRole = UserSession.getActorRole();
                AuditService.logEvent(c.c, actorId, actorRole, AuditService.ACTION_CUSTOMER_CREATED,
                    AuditService.ENTITY_CUSTOMER, meter,
                    "Registered new customer: " + name + " with meter: " + meter, null, "Active");
                
                c.c.commit();
                
                JOptionPane.showMessageDialog(this, "Customer Details Added Successfully!\nMeter Number: " + meter);
                dispose();
                new MeterInfo(meter);
            } catch (Exception e) {
                if (c != null && c.c != null) {
                    try { c.c.rollback(); } catch (Exception ignored) {}
                }
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding customer: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                if (c != null) {
                    if (c.c != null) try { c.c.setAutoCommit(true); } catch (Exception ignored) {}
                    if (c.s != null) try { c.s.close(); } catch (Exception ignored) {}
                    if (c.c != null) try { c.c.close(); } catch (Exception ignored) {}
                }
            }
        } else {
            dispose();
        }
    }
    
    public static void main(String[] args) {
        new NewCustomer();
    }
}
