package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.sql.*;

public class MeterInfo extends JFrame implements ActionListener{

    JButton next, cancel;
    Choice meterlocation, metertype, phasecode, billtype;
    String meternumber;
    MeterInfo(String meternumber) {
        this.meternumber = meternumber;
        
        setTitle("Electricity Billing System - Meter Information");
        setSize(700, 500);
        setLocation(400, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBackground(new Color(173, 216, 230));
        add(p);
        
        JLabel heading = new JLabel("Meter Information");
        heading.setBounds(180, 10, 200, 25);
        heading.setFont(new Font("Tahoma", Font.PLAIN, 24));
        p.add(heading);
        
        JLabel lblname = new JLabel("Meter Number");
        lblname.setBounds(100, 80, 100, 20);
        p.add(lblname);
        
        JLabel lblmeternumber = new JLabel(meternumber);
        lblmeternumber.setBounds(240, 80, 150, 20);
        lblmeternumber.setFont(new Font("Tahoma", Font.BOLD, 14));
        p.add(lblmeternumber);
        
        JLabel lblmeterno = new JLabel("Meter Location");
        lblmeterno.setBounds(100, 120, 100, 20);
        p.add(lblmeterno);
        
        meterlocation = new Choice();
        meterlocation.add("Outside");
        meterlocation.add("Inside");
        meterlocation.add("Main Gate");
        meterlocation.add("Building Entrance");
        meterlocation.setBounds(240, 120, 200, 20);
        p.add(meterlocation);
        
        JLabel lbladdress = new JLabel("Meter Type");
        lbladdress.setBounds(100, 160, 100, 20);
        p.add(lbladdress);
        
        metertype = new Choice();
        metertype.add("Digital");
        metertype.add("Smart Meter");
        metertype.add("Electric Meter");
        metertype.add("Solar Meter");
        metertype.setBounds(240, 160, 200, 20);
        p.add(metertype);
        
        JLabel lblcity = new JLabel("Phase Code");
        lblcity.setBounds(100, 200, 100, 20);
        p.add(lblcity);
        
        phasecode = new Choice();
        phasecode.add("PC-001");
        phasecode.add("PC-002");
        phasecode.add("PC-003");
        phasecode.add("PC-004");
        phasecode.add("PC-005");
        phasecode.add("011");
        phasecode.add("022");
        phasecode.add("033");
        phasecode.setBounds(240, 200, 200, 20);
        p.add(phasecode);
        
        JLabel lblstate = new JLabel("Bill Type");
        lblstate.setBounds(100, 240, 100, 20);
        p.add(lblstate);
        
        billtype = new Choice();
        billtype.add("Residential");
        billtype.add("Commercial");
        billtype.add("Industrial");
        billtype.setBounds(240, 240, 200, 20);
        p.add(billtype);
        
        JLabel lblemail = new JLabel("Days");
        lblemail.setBounds(100, 280, 100, 20);
        p.add(lblemail);
        
        JLabel lblemails = new JLabel("30 Days");
        lblemails.setBounds(240, 280, 100, 20);
        p.add(lblemails);
        
        JLabel lblphone = new JLabel("Note");
        lblphone.setBounds(100, 320, 100, 20);
        p.add(lblphone);
        
        JLabel lblphones = new JLabel("By Default Bill is calculated for 30 days only");
        lblphones.setBounds(240, 320, 500, 20);
        p.add(lblphones);
        
        next = new JButton("Submit");
        next.setBounds(140, 390, 100, 25);
        next.setBackground(Color.BLACK);
        next.setForeground(Color.WHITE);
        ImageIcon meterIcon = Project.getScaledIcon("icon/Meter Info.png", 16, Color.WHITE);
        if (meterIcon != null) {
            next.setIcon(meterIcon);
            next.setIconTextGap(6);
        }
        next.addActionListener(this);
        p.add(next);

        cancel = new JButton("Cancel");
        cancel.setBounds(270, 390, 100, 25);
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
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == next) {
            String meter = meternumber;
            String location = meterlocation.getSelectedItem();
            String type = metertype.getSelectedItem();
            String code = phasecode.getSelectedItem();
            String typebill = billtype.getSelectedItem();
            String days = "30";
            
            Conn c = null;
            try {
                c = new Conn();
                if (c.c == null) {
                    JOptionPane.showMessageDialog(this, "Database connection failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String query = "insert into meter_info (meter_no, meter_location, meter_type, phase_code, bill_type, days) values (?, ?, ?, ?, ?, ?) "
                             + "on duplicate key update meter_location = values(meter_location), meter_type = values(meter_type), phase_code = values(phase_code), bill_type = values(bill_type), days = values(days)";
                
                try (PreparedStatement ps = c.c.prepareStatement(query)) {
                    ps.setString(1, meter);
                    ps.setString(2, location);
                    ps.setString(3, type);
                    ps.setString(4, code);
                    ps.setString(5, typebill);
                    ps.setString(6, days);
                    ps.executeUpdate();
                }
                
                JOptionPane.showMessageDialog(this, "Meter Information Added Successfully");
                dispose();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving meter information: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                if (c != null) {
                    if (c.s != null) try { c.s.close(); } catch (Exception ignored) {}
                    if (c.c != null) try { c.c.close(); } catch (Exception ignored) {}
                }
            }
        } else {
            dispose();
        }
    }
    
    public static void main(String[] args) {
        new MeterInfo("");
    }
}
