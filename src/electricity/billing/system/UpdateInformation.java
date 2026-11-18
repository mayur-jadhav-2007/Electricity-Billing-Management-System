package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.event.*;

public class UpdateInformation extends JFrame implements ActionListener{

    JTextField tfaddress, tfstate, tfcity, tfemail, tfphone;
    JButton update, cancel;
    String meter;
    JLabel name;
    UpdateInformation(String meter) {
        this.meter = meter;
        setTitle("Electricity Billing System - Update Customer Information");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(300, 150, 1050, 450);
        getContentPane().setBackground(Color.WHITE);
        setLayout(null);
        
        JLabel heading = new JLabel("UPDATE CUSTOMER INFORMATION");
        heading.setBounds(110, 0, 400, 30);
        heading.setFont(new Font("Tahoma", Font.PLAIN, 20));
        add(heading);
        
        JLabel lblname = new JLabel("Name");
        lblname.setBounds(30, 70, 100, 20);
        add(lblname);
        
        name = new JLabel("");
        name.setBounds(230, 70, 200, 20);
        add(name);
        
        JLabel lblmeternumber = new JLabel("Meter Number");
        lblmeternumber.setBounds(30, 110, 100, 20);
        add(lblmeternumber);
        
        JLabel meternumber = new JLabel("");
        meternumber.setBounds(230, 110, 200, 20);
        add(meternumber);
        
        JLabel lbladdress = new JLabel("Address");
        lbladdress.setBounds(30, 150, 100, 20);
        add(lbladdress);
        
        tfaddress = new JTextField();
        tfaddress.setBounds(230, 150, 200, 20);
        add(tfaddress);
        
        JLabel lblcity = new JLabel("City");
        lblcity.setBounds(30, 190, 100, 20);
        add(lblcity);
        
        tfcity = new JTextField();
        tfcity.setBounds(230, 190, 200, 20);
        add(tfcity);
        
        JLabel lblstate = new JLabel("State");
        lblstate.setBounds(30, 230, 100, 20);
        add(lblstate);
        
        tfstate = new JTextField();
        tfstate.setBounds(230, 230, 200, 20);
        add(tfstate);
        
        JLabel lblemail = new JLabel("Email");
        lblemail.setBounds(30, 270, 100, 20);
        add(lblemail);
        
        tfemail = new JTextField();
        tfemail.setBounds(230, 270, 200, 20);
        add(tfemail);
        
        JLabel lblphone = new JLabel("Phone");
        lblphone.setBounds(30, 310, 100, 20);
        add(lblphone);
        
        tfphone = new JTextField();
        tfphone.setBounds(230, 310, 200, 20);
        add(tfphone);
        
        Conn c = null;
        try {
            c = new Conn();
            if (c.c != null) {
                String selectQuery = "select * from customer where meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(selectQuery)) {
                    ps.setString(1, meter);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            name.setText(rs.getString("name"));
                            tfaddress.setText(rs.getString("address"));
                            tfcity.setText(rs.getString("city"));
                            tfstate.setText(rs.getString("state"));
                            tfemail.setText(rs.getString("email"));
                            tfphone.setText(rs.getString("phone"));
                            meternumber.setText(rs.getString("meter_no"));
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
        
        update = new JButton("Update");
        update.setBackground(Color.BLACK);
        update.setForeground(Color.WHITE);
        update.setBounds(70, 360, 100, 25);
        ImageIcon updIcon = Project.getScaledIcon("icon/Update Info.png", 16, Color.WHITE);
        if (updIcon != null) {
            update.setIcon(updIcon);
            update.setIconTextGap(6);
        }
        add(update);
        update.addActionListener(this);
        
        cancel = new JButton("Cancel");
        cancel.setBackground(Color.BLACK);
        cancel.setForeground(Color.WHITE);
        cancel.setBounds(230, 360, 100, 25);
        add(cancel);
        cancel.addActionListener(this);

        ImageIcon i1 = new ImageIcon(ClassLoader.getSystemResource("icon/update.jpg"));
        Image i2 = i1.getImage().getScaledInstance(400, 300, Image.SCALE_DEFAULT);
        ImageIcon i3 = new ImageIcon(i2);
        JLabel image = new JLabel(i3);
        image.setBounds(550, 50, 400, 300);
        add(image);
        
        setVisible(true);
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == update) {
            String address = tfaddress.getText() != null ? tfaddress.getText().trim() : "";
            String city = tfcity.getText() != null ? tfcity.getText().trim() : "";
            String state = tfstate.getText() != null ? tfstate.getText().trim() : "";
            String email = tfemail.getText() != null ? tfemail.getText().trim() : "";
            String phone = tfphone.getText() != null ? tfphone.getText().trim() : "";
            
            if (address.isEmpty() || city.isEmpty() || state.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!email.contains("@") || !email.contains(".")) {
                JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Validation Error", JOptionPane.WARNING_MESSAGE);
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
                
                String updateQuery = "update customer set address = ?, city = ?, state = ?, email = ?, phone = ? where meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(updateQuery)) {
                    ps.setString(1, address);
                    ps.setString(2, city);
                    ps.setString(3, state);
                    ps.setString(4, email);
                    ps.setString(5, phone);
                    ps.setString(6, meter);
                    int rows = ps.executeUpdate();
                    
                    if (rows > 0) {
                        String actorId = UserSession.getActorId();
                        String actorRole = UserSession.getActorRole();
                        AuditService.logEvent(c.c, actorId, actorRole, AuditService.ACTION_CUSTOMER_UPDATED,
                            AuditService.ENTITY_CUSTOMER, meter,
                            "Updated customer profile fields: address, city, state, email, phone", null, "Updated");
                        c.c.commit();
                        JOptionPane.showMessageDialog(this, "Customer Information Updated Successfully");
                        dispose();
                    } else {
                        c.c.rollback();
                        JOptionPane.showMessageDialog(this, "Customer record not found for meter: " + meter, "Update Notice", JOptionPane.WARNING_MESSAGE);
                    }
                }
            } catch (Exception e) {
                if (c != null && c.c != null) {
                    try { c.c.rollback(); } catch (Exception ignored) {}
                }
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating information: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        new UpdateInformation("");
    }
}
