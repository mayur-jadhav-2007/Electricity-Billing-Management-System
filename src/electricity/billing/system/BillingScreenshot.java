package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class BillingScreenshot {

    private static void captureFrame(JFrame frame, int width, int height, String filename) {
        try {
            frame.setSize(width, height);
            frame.validate();
            frame.repaint();
            Thread.sleep(800); // allow layout and async painting

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            frame.paint(g2d);
            g2d.dispose();

            File outFile = new File(filename);
            ImageIO.write(image, "png", outFile);
            System.out.println("Captured " + filename + " successfully (" + width + "x" + height + ").");
            frame.dispose();
        } catch (Exception e) {
            System.err.println("Error capturing " + filename + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // Set System Look and Feel if available
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // 1. CalculateBill screen
            SwingUtilities.invokeAndWait(() -> {
                CalculateBill calcFrame = new CalculateBill();
                captureFrame(calcFrame, 780, 560, "calculate_bill_screen.png");
            });

            // 2. GenerateBill screen with invoice populated
            SwingUtilities.invokeAndWait(() -> {
                GenerateBill genFrame = new GenerateBill("MET001");
                genFrame.generateBillStatement("MET001", "January", false);
                captureFrame(genFrame, 650, 840, "generate_bill_screen.png");
            });

            // 3. BillDetails screen
            SwingUtilities.invokeAndWait(() -> {
                BillDetails billFrame = new BillDetails("");
                captureFrame(billFrame, 860, 680, "bill_details_screen.png");
            });

            // 4. DepositDetails screen
            SwingUtilities.invokeAndWait(() -> {
                DepositDetails depFrame = new DepositDetails();
                captureFrame(depFrame, 860, 680, "deposit_details_screen.png");
            });

            // 5. PayBill screen
            SwingUtilities.invokeAndWait(() -> {
                PayBill payFrame = new PayBill("MET001");
                captureFrame(payFrame, 850, 560, "pay_bill_screen.png");
            });

            System.out.println("ALL_BILLING_SCREENSHOTS_CAPTURED_SUCCESSFULLY");
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
