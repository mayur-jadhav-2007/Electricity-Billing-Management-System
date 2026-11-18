package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class AnalyticsScreenshot {

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
            Thread.sleep(700); // allow layout and render

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

            final AnalyticsDashboard[] adHolder = new AnalyticsDashboard[1];
            SwingUtilities.invokeAndWait(() -> {
                adHolder[0] = new AnalyticsDashboard();
                adHolder[0].setSize(1260, 760);
                adHolder[0].validate();
            });

            AnalyticsDashboard ad = adHolder[0];

            // Allow background SwingWorker to fetch database analytics and populate UI
            Thread.sleep(1500);

            // Find the JTabbedPane inside AnalyticsDashboard
            JTabbedPane tp = findTabbedPane(ad.getContentPane());
            if (tp != null) {
                // 1. Overview Tab
                SwingUtilities.invokeAndWait(() -> tp.setSelectedIndex(0));
                Thread.sleep(400);
                captureComponent(ad, 1260, 760, "analytics_dashboard_overview.png");

                // 2. Billing & Revenue Tab
                SwingUtilities.invokeAndWait(() -> tp.setSelectedIndex(1));
                Thread.sleep(400);
                captureComponent(ad, 1260, 760, "analytics_billing_revenue.png");

                // 3. Power Consumption Tab
                SwingUtilities.invokeAndWait(() -> tp.setSelectedIndex(2));
                Thread.sleep(400);
                captureComponent(ad, 1260, 760, "analytics_consumption.png");

                // 4. Customer Insights Tab
                SwingUtilities.invokeAndWait(() -> tp.setSelectedIndex(3));
                Thread.sleep(400);
                captureComponent(ad, 1260, 760, "analytics_customer_insights.png");

                // 5. Complaint Analytics Tab
                SwingUtilities.invokeAndWait(() -> tp.setSelectedIndex(4));
                Thread.sleep(400);
                captureComponent(ad, 1260, 760, "analytics_complaints.png");

                // 6. Reports & CSV Export Tab
                SwingUtilities.invokeAndWait(() -> tp.setSelectedIndex(5));
                Thread.sleep(400);
                captureComponent(ad, 1260, 760, "analytics_reports_export.png");
            }

            SwingUtilities.invokeAndWait(ad::dispose);
            System.out.println("All Phase 7 analytics screenshots captured successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JTabbedPane findTabbedPane(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JTabbedPane) {
                return (JTabbedPane) comp;
            } else if (comp instanceof Container) {
                JTabbedPane found = findTabbedPane((Container) comp);
                if (found != null) return found;
            }
        }
        return null;
    }
}
