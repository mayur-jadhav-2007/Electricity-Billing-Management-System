package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class DashboardScreenshot {
    public static void main(String[] args) {
        try {
            // Admin Dashboard screenshot
            Project admin = new Project("Admin", "");
            admin.setSize(1280, 800);
            admin.validate();
            admin.repaint();
            Thread.sleep(1000); // allow layout
            
            BufferedImage imgAdmin = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = imgAdmin.createGraphics();
            admin.paint(g2);
            g2.dispose();
            
            File outAdmin = new File("admin_dashboard_improved.png");
            ImageIO.write(imgAdmin, "png", outAdmin);
            System.out.println("Saved Admin screenshot to: " + outAdmin.getAbsolutePath());
            admin.dispose();
            
            // Customer Dashboard screenshot
            Project customer = new Project("Customer", "MET001");
            customer.setSize(1280, 800);
            customer.validate();
            customer.repaint();
            Thread.sleep(1000);
            
            BufferedImage imgCust = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
            Graphics2D gCust = imgCust.createGraphics();
            customer.paint(gCust);
            gCust.dispose();
            
            File outCust = new File("customer_dashboard_improved.png");
            ImageIO.write(imgCust, "png", outCust);
            System.out.println("Saved Customer screenshot to: " + outCust.getAbsolutePath());
            customer.dispose();
            
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
