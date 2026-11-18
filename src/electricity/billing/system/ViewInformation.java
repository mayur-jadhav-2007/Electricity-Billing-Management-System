package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.event.*;

public class ViewInformation extends JFrame implements ActionListener{

    JButton cancel;
    ViewInformation(String meter) {
        setTitle("Electricity Billing System - View Customer Information");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(350, 150, 850, 650);
        getContentPane().setBackground(Color.WHITE);
        setLayout(null);
        
        
        JLabel heading = new JLabel("VIEW CUSTOMER INFORMATION");
        heading.setBounds(250, 0, 500, 40);
        heading.setFont(new Font("Tahoma", Font.BOLD, 20));
        add(heading);
        
        JLabel lblname = new JLabel("Name");
        lblname.setBounds(70, 80, 100, 20);
        lblname.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lblname);
        
        JLabel name = new JLabel("");
        name.setBounds(200, 80, 250, 20);
        name.setFont(new Font("Tahoma", Font.PLAIN, 13));
        add(name);
        
        JLabel lblmeternumber = new JLabel("Meter Number");
        lblmeternumber.setBounds(70, 140, 100, 20);
        lblmeternumber.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lblmeternumber);
        
        JLabel meternumber = new JLabel("");
        meternumber.setBounds(200, 140, 250, 20);
        meternumber.setFont(new Font("Tahoma", Font.PLAIN, 13));
        add(meternumber);
        
        JLabel lbladdress = new JLabel("Address");
        lbladdress.setBounds(70, 200, 100, 20);
        lbladdress.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lbladdress);
        
        JLabel address = new JLabel("");
        address.setBounds(200, 200, 250, 20);
        address.setFont(new Font("Tahoma", Font.PLAIN, 13));
        add(address);
        
        JLabel lblcity = new JLabel("City");
        lblcity.setBounds(70, 260, 100, 20);
        lblcity.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lblcity);
        
        JLabel city = new JLabel("");
        city.setBounds(200, 260, 250, 20);
        city.setFont(new Font("Tahoma", Font.PLAIN, 13));
        add(city);
        
        JLabel lblstate = new JLabel("State");
        lblstate.setBounds(480, 80, 100, 20);
        lblstate.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lblstate);
        
        JLabel state = new JLabel("");
        state.setBounds(600, 80, 220, 20);
        state.setFont(new Font("Tahoma", Font.PLAIN, 13));
        add(state);
        
        JLabel lblemail = new JLabel("Email");
        lblemail.setBounds(480, 140, 100, 20);
        lblemail.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lblemail);
        
        JLabel email = new JLabel("");
        email.setBounds(600, 140, 220, 20);
        email.setFont(new Font("Tahoma", Font.PLAIN, 13));
        add(email);
        
        JLabel lblphone = new JLabel("Phone");
        lblphone.setBounds(480, 200, 100, 20);
        lblphone.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lblphone);
        
        JLabel phone = new JLabel("");
        phone.setBounds(600, 200, 220, 20);
        phone.setFont(new Font("Tahoma", Font.PLAIN, 13));
        add(phone);
        
        JLabel lblmeterinfo = new JLabel("Meter Details");
        lblmeterinfo.setBounds(480, 260, 100, 20);
        lblmeterinfo.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lblmeterinfo);
        
        JLabel meterinfo = new JLabel("");
        meterinfo.setBounds(600, 260, 220, 20);
        meterinfo.setFont(new Font("Tahoma", Font.PLAIN, 13));
        add(meterinfo);
        
        JLabel lblstatus = new JLabel("Account Status");
        lblstatus.setBounds(70, 310, 110, 20);
        lblstatus.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(lblstatus);
        
        JLabel status = new JLabel("");
        status.setBounds(200, 310, 200, 20);
        status.setFont(new Font("Tahoma", Font.BOLD, 13));
        add(status);
        
        Conn c = null;
        try {
            c = new Conn();
            if (c.c != null) {
                String query = "select * from customer where meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(query)) {
                    ps.setString(1, meter);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            name.setText(rs.getString("name"));
                            address.setText(rs.getString("address"));
                            city.setText(rs.getString("city"));
                            state.setText(rs.getString("state"));
                            email.setText(rs.getString("email"));
                            phone.setText(rs.getString("phone"));
                            meternumber.setText(rs.getString("meter_no"));
                            String sVal = rs.getString("status");
                            status.setText(sVal != null ? sVal : "Active");
                            if ("Inactive".equalsIgnoreCase(sVal)) {
                                status.setForeground(Color.RED);
                            } else {
                                status.setForeground(new Color(39, 174, 96));
                            }
                        }
                    }
                }
                
                String mQuery = "select meter_type, meter_location, bill_type from meter_info where meter_no = ?";
                try (PreparedStatement ps = c.c.prepareStatement(mQuery)) {
                    ps.setString(1, meter);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            meterinfo.setText(rs.getString("meter_type") + " (" + rs.getString("bill_type") + ")");
                        } else {
                            meterinfo.setText("Standard Digital");
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
        
        cancel = new JButton("Close");
        cancel.setBackground(Color.BLACK);
        cancel.setForeground(Color.WHITE);
        cancel.setBounds(360, 310, 100, 25);
        add(cancel);
        cancel.addActionListener(this);
        
        ImageIcon i1 = new ImageIcon(ClassLoader.getSystemResource("icon/viewcustomer.jpg"));
        Image i2 = i1.getImage().getScaledInstance(600, 300, Image.SCALE_DEFAULT);
        ImageIcon i3 = new ImageIcon(i2);
        JLabel image = new JLabel(i3);
        image.setBounds(20, 350, 600, 260);
        add(image);
        
        setVisible(true);
    }
    
    public void actionPerformed(ActionEvent ae) {
        dispose();
    }
    
    public static void main(String[] args) {
        new ViewInformation("");
    }
}
