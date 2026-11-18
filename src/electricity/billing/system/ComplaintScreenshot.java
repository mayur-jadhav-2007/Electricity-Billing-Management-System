package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.sql.*;

public class ComplaintScreenshot {

    private static void captureComponent(Component comp, int width, int height, String filename) {
        try {
            comp.setSize(width, height);
            comp.addNotify();
            if (comp instanceof Window) {
                ((Window) comp).pack();
                ((Window) comp).setSize(width, height);
            }
            if (comp instanceof RootPaneContainer) {
                ((RootPaneContainer) comp).getRootPane().setSize(width, height);
                ((RootPaneContainer) comp).getRootPane().validate();
                ((RootPaneContainer) comp).getRootPane().doLayout();
            }
            comp.validate();
            comp.doLayout();
            Thread.sleep(600); // allow layout and render

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            if (comp instanceof RootPaneContainer) {
                ((RootPaneContainer) comp).getRootPane().paint(g2d);
            } else {
                comp.paint(g2d);
            }
            g2d.dispose();

            File outFile = new File(filename);
            ImageIO.write(image, "png", outFile);
            System.out.println("Captured " + filename + " successfully (" + width + "x" + height + ").");
            if (comp instanceof Window) {
                ((Window) comp).dispose();
            }
        } catch (Exception e) {
            System.err.println("Error capturing " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Populate sample demonstration complaint if table is currently empty
            try (Conn c = new Conn()) {
                int count = 0;
                try (Statement st = c.c.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM complaints")) {
                    if (rs.next()) count = rs.getInt(1);
                }
                if (count == 0) {
                    ComplaintService.ServiceResult sr1 = ComplaintService.submitComplaint(
                        "MET001", "Billing Issues", "January Bill Reading Discrepancy", 
                        "The units billed for January are 165 but meter reading showed 120 units.", "January", "Customer (MET001)"
                    );
                    if (sr1.success) {
                        ComplaintService.updatePriority(sr1.complaintId, "High", "Admin", "Priority escalated for billing inquiry");
                        ComplaintService.assignComplaint(sr1.complaintId, "Parth", "Admin", "Assigned to Parth for field check");
                        ComplaintService.updateStatus(sr1.complaintId, "In Progress", "Admin", "Field technician scheduled for inspection");
                    }
                    ComplaintService.ServiceResult sr2 = ComplaintService.submitComplaint(
                        "MET002", "Meter Problems", "LCD Display Blinking Error", 
                        "The meter digital screen is intermittently turning blank.", null, "Customer (MET002)"
                    );
                    if (sr2.success) {
                        ComplaintService.updatePriority(sr2.complaintId, "Urgent", "Admin", "Critical hardware fault");
                        ComplaintService.updateStatus(sr2.complaintId, "In Progress", "Admin", "Replacement meter dispatched");
                        ComplaintService.updateStatus(sr2.complaintId, "Resolved", "Admin", "New meter installed and calibrated.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 1. Submit Complaint Screen
            SwingUtilities.invokeAndWait(() -> {
                SubmitComplaint sc = new SubmitComplaint("MET001");
                captureComponent(sc, 700, 680, "submit_complaint_screen.png");
            });

            // 2. My Complaints Screen
            SwingUtilities.invokeAndWait(() -> {
                MyComplaints mc = new MyComplaints("MET001");
                captureComponent(mc, 1000, 600, "my_complaints_screen.png");
            });

            // 3. Admin Complaint Management Screen
            SwingUtilities.invokeAndWait(() -> {
                ComplaintManagement cm = new ComplaintManagement();
                captureComponent(cm, 1240, 720, "complaint_management_screen.png");
            });

            // 4. Admin Complaint History Screen
            SwingUtilities.invokeAndWait(() -> {
                ComplaintHistory ch = new ComplaintHistory();
                captureComponent(ch, 1080, 620, "complaint_history_screen.png");
            });

            System.out.println("All Phase 6 screenshots captured successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
