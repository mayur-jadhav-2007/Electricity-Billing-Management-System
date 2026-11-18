package electricity.billing.system;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class NavigationScreenshotDemo {

    public static void main(String[] args) {
        try {
            UserSession.setSession(new SessionContext("admin", "Admin", ""));
            
            // 1. Admin Dashboard with "Dashboard" active
            Project admin = new Project("Admin", "");
            admin.setSize(1280, 800);
            admin.validate();
            admin.repaint();
            Thread.sleep(500);
            captureFrame(admin, "admin_nav_dashboard_active.png");

            // 2. Open section "Customer Details" -> highlights Customer Details
            admin.actionPerformed(new ActionEvent(admin, ActionEvent.ACTION_PERFORMED, "Customer Details"));
            Thread.sleep(500);
            admin.repaint();
            captureFrame(admin, "admin_nav_section_active.png");

            // 3. Close the sub-window -> highlight returns to Dashboard
            JFrame subWinAdmin = admin.getActiveSubWindow();
            if (subWinAdmin != null) {
                subWinAdmin.dispose();
            }
            Thread.sleep(500);
            admin.repaint();
            captureFrame(admin, "admin_nav_returned_to_dashboard.png");
            admin.dispose();

            // 4. Customer Dashboard with "Dashboard" active
            UserSession.setSession(new SessionContext("customer", "Customer", "MET001"));
            Project customer = new Project("Customer", "MET001");
            customer.setSize(1280, 800);
            customer.validate();
            customer.repaint();
            Thread.sleep(500);
            captureFrame(customer, "customer_nav_dashboard_active.png");

            // 5. Open section "Pay Bill" -> highlights Pay Bill
            customer.actionPerformed(new ActionEvent(customer, ActionEvent.ACTION_PERFORMED, "Pay Bill"));
            Thread.sleep(500);
            customer.repaint();
            captureFrame(customer, "customer_nav_section_active.png");

            // 6. Close the sub-window -> highlight returns to Dashboard
            JFrame subWinCust = customer.getActiveSubWindow();
            if (subWinCust != null) {
                subWinCust.dispose();
            }
            Thread.sleep(500);
            customer.repaint();
            captureFrame(customer, "customer_nav_returned_to_dashboard.png");
            customer.dispose();

            System.out.println("All demonstration screenshots generated successfully!");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void captureFrame(JFrame frame, String fileName) throws Exception {
        BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        frame.paint(g2);
        g2.dispose();
        File outFile = new File(fileName);
        ImageIO.write(img, "png", outFile);
        System.out.println("Saved screenshot: " + outFile.getAbsolutePath());
    }
}
