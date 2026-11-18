package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.Date;

public class PaymentScreenshot {

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
            // Set System Look & Feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // 1. PayBill Screen (Unpaid bill for February MET001)
            SwingUtilities.invokeAndWait(() -> {
                PayBill pb = new PayBill("MET001");
                pb.loadBillDetails("February");
                captureComponent(pb, 860, 570, "pay_bill_screen.png");
            });

            // 2. PaymentGateway Screen
            SwingUtilities.invokeAndWait(() -> {
                PaymentGateway pg = new PaymentGateway("MET001", "February", "607.25");
                captureComponent(pg, 600, 530, "payment_gateway_screen.png");
            });

            // 3. PaymentReceiptDialog Screen
            SwingUtilities.invokeAndWait(() -> {
                PaymentReceiptDialog receipt = new PaymentReceiptDialog(
                    null,
                    "Parth",
                    "MET001",
                    "February",
                    "607.25",
                    "UPI Payment (Simulated)",
                    new Date()
                );
                captureComponent(receipt, 580, 680, "payment_receipt_screen.png");
            });

            // 4. DepositDetails Screen filtered to "Paid Only" (Payment History Ledger)
            SwingUtilities.invokeAndWait(() -> {
                DepositDetails dd = new DepositDetails();
                dd.loadBills("All Meters", "All Months", "Paid Only", false);
                captureComponent(dd, 920, 680, "payment_history_screen.png");
            });

            System.out.println("ALL_PAYMENT_SCREENSHOTS_CAPTURED_SUCCESSFULLY");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
