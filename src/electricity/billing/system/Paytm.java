package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class Paytm extends JFrame implements ActionListener {

    String meter;
    JButton back, proceedToPayBill;

    public Paytm(String meter) {
        this.meter = (meter != null) ? meter.trim() : "";
        setTitle("Electricity Billing System - External Payment Advisory");
        setSize(700, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        headerPanel.setBackground(new Color(248, 250, 252));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JLabel titleLabel = new JLabel("Online Payment Integration Notice");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(15, 23, 42));
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content Card
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(248, 250, 252));
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(226, 232, 240), 1, true),
            new EmptyBorder(25, 30, 25, 30)
        ));
        card.setPreferredSize(new Dimension(540, 260));

        JLabel iconLabel = new JLabel("🔒", JLabel.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Simulated Desktop Environment", JLabel.CENTER);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.setForeground(new Color(15, 23, 42));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg1 = new JLabel("Direct third-party web gateways (e.g., Paytm/Razorpay) are disabled", JLabel.CENTER);
        msg1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg1.setForeground(new Color(71, 85, 105));
        msg1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg2 = new JLabel("in this desktop environment to ensure offline transaction security.", JLabel.CENTER);
        msg2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg2.setForeground(new Color(71, 85, 105));
        msg2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg3 = new JLabel("Please use the built-in simulated Payment Gateway from Pay Bill.", JLabel.CENTER);
        msg3.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        msg3.setForeground(new Color(37, 99, 235));
        msg3.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(iconLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(heading);
        card.add(Box.createVerticalStrut(12));
        card.add(msg1);
        card.add(Box.createVerticalStrut(4));
        card.add(msg2);
        card.add(Box.createVerticalStrut(12));
        card.add(msg3);

        centerPanel.add(card);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        bottomPanel.setBackground(new Color(248, 250, 252));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        proceedToPayBill = new JButton("Open Pay Bill Portal");
        proceedToPayBill.setBackground(new Color(22, 163, 74)); // Forest green
        proceedToPayBill.setForeground(Color.WHITE);
        proceedToPayBill.setFont(new Font("Segoe UI", Font.BOLD, 12));
        proceedToPayBill.setFocusPainted(false);
        proceedToPayBill.setCursor(new Cursor(Cursor.HAND_CURSOR));
        proceedToPayBill.addActionListener(this);

        back = new JButton("Back");
        back.setBackground(new Color(71, 85, 105)); // Slate 600
        back.setForeground(Color.WHITE);
        back.setFont(new Font("Segoe UI", Font.BOLD, 12));
        back.setFocusPainted(false);
        back.setCursor(new Cursor(Cursor.HAND_CURSOR));
        back.addActionListener(this);

        bottomPanel.add(proceedToPayBill);
        bottomPanel.add(back);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        dispose();
        new PayBill(meter);
    }

    public static void main(String[] args) {
        new Paytm("");
    }
}
