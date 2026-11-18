package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.math.BigDecimal;
import java.util.Date;

public class Phase9Screenshot {

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
            System.out.println("Captured " + filename + " (" + width + "x" + height + ").");
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

            // 1. CalculateBill Phase 9 Screen
            SwingUtilities.invokeAndWait(() -> {
                CalculateBill cb = new CalculateBill();
                captureComponent(cb, 760, 640, "calculate_bill_phase9.png");
            });

            // 2. PaymentGateway Phase 9 Screen (LPSC itemized breakdown)
            SwingUtilities.invokeAndWait(() -> {
                // Bill 4 is MET001 February (unpaid)
                PaymentGateway pg = new PaymentGateway(4L, "MET001", "February", "607.25");
                captureComponent(pg, 620, 600, "payment_gateway_phase9.png");
            });

            // 3. PaymentReceiptDialog Phase 9 Screen (Itemized receipt with Bill ID & late fee)
            SwingUtilities.invokeAndWait(() -> {
                PaymentReceiptDialog receipt = new PaymentReceiptDialog(
                    null, 4L, "Rajesh Kumar", "MET001", "February",
                    "607.25", "25.30", "632.55", "2026-03-01", "2026-04-10 11:20:00",
                    "Unified Payments Interface (UPI)", "PAID"
                );
                captureComponent(receipt, 580, 680, "payment_receipt_phase9.png");
            });

            // 4. DepositDetails Screen with Bill ID in Column 0
            SwingUtilities.invokeAndWait(() -> {
                DepositDetails dd = new DepositDetails();
                dd.loadBills("All Meters", "All Months", "All Statuses", false);
                captureComponent(dd, 920, 680, "deposit_details_phase9.png");
            });

            System.out.println("ALL_PHASE9_SCREENSHOTS_CAPTURED_SUCCESSFULLY");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
