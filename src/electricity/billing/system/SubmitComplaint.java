package electricity.billing.system;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SubmitComplaint extends JFrame implements ActionListener {

    private final String meter;
    private JTextField tfSubject;
    private JTextArea taDescription;
    private JComboBox<String> cbCategory;
    private JComboBox<String> cbBillMonth;
    private JTextField tfMeter;
    private JLabel lblCharCount;
    private JButton btnSubmit, btnReset, btnClose;
    private boolean isSubmitting = false;

    public SubmitComplaint(String meter) {
        this.meter = (meter != null) ? meter.trim() : "";
        setTitle("Electricity Billing System - Submit Complaint (Meter: " + this.meter + ")");
        setSize(700, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(248, 250, 252));

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 16));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        JLabel heading = new JLabel("Lodge a Customer Grievance / Complaint");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        heading.setForeground(new Color(15, 23, 42));
        headerPanel.add(heading);
        add(headerPanel, BorderLayout.NORTH);

        // Center Form Panel
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 1. Meter Number (Read-only for customer security)
        JPanel pMeter = createFormField("Account Meter Number:");
        tfMeter = new JTextField(this.meter);
        tfMeter.setEditable(false);
        tfMeter.setBackground(new Color(241, 245, 249));
        tfMeter.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tfMeter.setForeground(new Color(37, 99, 235));
        tfMeter.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        pMeter.add(tfMeter);
        formContainer.add(pMeter);
        formContainer.add(Box.createVerticalStrut(12));

        // 2. Complaint Category
        JPanel pCategory = createFormField("Complaint Category:*");
        cbCategory = new JComboBox<>(ComplaintService.CATEGORIES);
        cbCategory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbCategory.setBackground(Color.WHITE);
        pCategory.add(cbCategory);
        formContainer.add(pCategory);
        formContainer.add(Box.createVerticalStrut(12));

        // 3. Optional Related Bill Month
        JPanel pBillMonth = createFormField("Related Bill Month (Optional):");
        cbBillMonth = new JComboBox<>();
        cbBillMonth.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbBillMonth.setBackground(Color.WHITE);
        cbBillMonth.addItem("None / Not Applicable");
        loadCustomerBillMonths();
        pBillMonth.add(cbBillMonth);
        formContainer.add(pBillMonth);
        formContainer.add(Box.createVerticalStrut(12));

        // 4. Complaint Subject
        JPanel pSubject = createFormField("Subject / Summary:* (max 200 chars)");
        tfSubject = new JTextField();
        tfSubject.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tfSubject.setBorder(new CompoundBorder(new LineBorder(new Color(203, 213, 225)), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        pSubject.add(tfSubject);
        formContainer.add(pSubject);
        formContainer.add(Box.createVerticalStrut(12));

        // 5. Complaint Description
        JPanel pDesc = createFormField("Detailed Description:* (max 2000 chars)");
        taDescription = new JTextArea(6, 40);
        taDescription.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taDescription.setLineWrap(true);
        taDescription.setWrapStyleWord(true);
        taDescription.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane spDesc = new JScrollPane(taDescription);
        spDesc.setBorder(new LineBorder(new Color(203, 213, 225)));
        pDesc.add(spDesc);
        formContainer.add(pDesc);

        lblCharCount = new JLabel("0 / 2000 characters");
        lblCharCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCharCount.setForeground(new Color(100, 116, 139));
        lblCharCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContainer.add(lblCharCount);

        taDescription.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                int len = taDescription.getText().length();
                lblCharCount.setText(len + " / 2000 characters");
                if (len > 2000) {
                    lblCharCount.setForeground(Color.RED);
                } else {
                    lblCharCount.setForeground(new Color(100, 116, 139));
                }
            }
        });

        JScrollPane mainScroll = new JScrollPane(formContainer);
        mainScroll.setBorder(null);
        add(mainScroll, BorderLayout.CENTER);

        // Bottom Actions Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        bottomPanel.setBackground(new Color(248, 250, 252));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        btnSubmit = new JButton("Submit Complaint");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.setBackground(new Color(37, 99, 235)); // Royal Blue
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ImageIcon submitIcon = Project.getScaledIcon("icon/icons8-pen-50.png", 16, Color.WHITE);
        if (submitIcon != null) {
            btnSubmit.setIcon(submitIcon);
            btnSubmit.setIconTextGap(6);
        }
        btnSubmit.addActionListener(this);

        btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnReset.setBackground(new Color(241, 245, 249));
        btnReset.setForeground(new Color(51, 65, 85));
        btnReset.setFocusPainted(false);
        btnReset.addActionListener(this);

        btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClose.setBackground(new Color(15, 23, 42));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(this);

        bottomPanel.add(btnSubmit);
        bottomPanel.add(btnReset);
        bottomPanel.add(btnClose);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createFormField(String labelText) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(51, 65, 85));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(label);
        p.add(Box.createVerticalStrut(4));
        return p;
    }

    private void loadCustomerBillMonths() {
        if (this.meter.isEmpty()) return;
        try (Conn c = new Conn()) {
            if (c.c != null) {
                String q = "SELECT month, total_bill, status FROM bill WHERE meter_no = ? ORDER BY month";
                try (PreparedStatement ps = c.c.prepareStatement(q)) {
                    ps.setString(1, this.meter);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String m = rs.getString("month");
                            String bill = rs.getString("total_bill");
                            String st = rs.getString("status");
                            cbBillMonth.addItem(m + " (Bill: ₹" + bill + ", " + st + ")");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == btnSubmit) {
            handleSubmit();
        } else if (ae.getSource() == btnReset) {
            tfSubject.setText("");
            taDescription.setText("");
            cbCategory.setSelectedIndex(0);
            if (cbBillMonth.getItemCount() > 0) cbBillMonth.setSelectedIndex(0);
            lblCharCount.setText("0 / 2000 characters");
        } else if (ae.getSource() == btnClose) {
            dispose();
        }
    }

    private void handleSubmit() {
        if (isSubmitting) return;

        String category = (String) cbCategory.getSelectedItem();
        String subject = tfSubject.getText() != null ? tfSubject.getText().trim() : "";
        String description = taDescription.getText() != null ? taDescription.getText().trim() : "";
        String selectedBill = (String) cbBillMonth.getSelectedItem();
        String billMonth = null;
        if (selectedBill != null && !selectedBill.startsWith("None")) {
            billMonth = selectedBill.split(" ")[0].trim();
        }

        // Basic GUI client-side validations
        if (subject.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a subject for the complaint.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            tfSubject.requestFocus();
            return;
        }
        if (subject.length() > 200) {
            JOptionPane.showMessageDialog(this, "Subject is too long (maximum 200 characters).", "Validation Error", JOptionPane.WARNING_MESSAGE);
            tfSubject.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a detailed description of your issue.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            taDescription.requestFocus();
            return;
        }
        if (description.length() > 2000) {
            JOptionPane.showMessageDialog(this, "Description exceeds the maximum allowed 2000 characters.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            taDescription.requestFocus();
            return;
        }

        // Prevent duplicate rapid submissions by locking the button
        isSubmitting = true;
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        try {
            ComplaintService.ServiceResult result = ComplaintService.submitComplaint(
                this.meter, category, subject, description, billMonth, "Customer (" + this.meter + ")"
            );

            if (result.success) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                String now = sdf.format(new Date());

                String message = 
                    "Complaint Submitted Successfully!\n\n" +
                    "Reference Number: " + result.referenceNumber + "\n" +
                    "Account Meter: " + this.meter + "\n" +
                    "Category: " + category + "\n" +
                    "Submission Date: " + now + "\n" +
                    "Initial Status: Pending\n\n" +
                    "You can track the status of this complaint under 'My Complaints'.";

                JOptionPane.showMessageDialog(this, message, "Complaint Confirmation", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Submission Failed: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Complaint");
                isSubmitting = false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Complaint");
            isSubmitting = false;
        }
    }

    public static void main(String[] args) {
        new SubmitComplaint("MET001");
    }
}
