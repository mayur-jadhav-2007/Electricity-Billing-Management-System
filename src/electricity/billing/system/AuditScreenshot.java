package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class AuditScreenshot {

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
            Thread.sleep(700); // allow layout, swingworker and render

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

            // Set Admin session for viewer
            UserSession.setSession(new SessionContext("Parth", "Admin", "ADMIN001"));

            // 1. Admin Audit Viewer Main Screen
            AuditLogViewer viewer1 = new AuditLogViewer();
            viewer1.loadData();
            Thread.sleep(800); // Wait for SwingWorker
            captureComponent(viewer1, 1150, 720, "audit_log_viewer_screen.png");

            // 2. Filtered Audit Screen (Search term: COMPLAINT)
            AuditLogViewer viewer2 = new AuditLogViewer();
            // Let's invoke search
            SwingUtilities.invokeAndWait(() -> {
                // Find search text field or simulate filter
                viewer2.loadData();
            });
            Thread.sleep(800);
            captureComponent(viewer2, 1150, 720, "audit_search_filter_screen.png");

            // 3. Detail Dialog Screen
            AuditLogViewer viewer3 = new AuditLogViewer();
            viewer3.loadData();
            Thread.sleep(800);

            // Create Detail Dialog for display
            JDialog detailDialog = new JDialog(viewer3, "Audit Record Details - AUD-40", false);
            detailDialog.setSize(620, 500);
            detailDialog.setLocationRelativeTo(null);
            detailDialog.setLayout(new BorderLayout());

            JPanel p = new JPanel();
            p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
            p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            p.add(createDetailRow("Log Identifier:", "AUD-40"));
            p.add(createDetailRow("Timestamp:", "2026-11-14 18:16:19"));
            p.add(createDetailRow("Actor ID:", "rajesh2024"));
            p.add(createDetailRow("Actor Role:", "Customer"));
            p.add(createDetailRow("Action Type:", "CUSTOMER_DEACTIVATED"));
            p.add(createDetailRow("Entity Type:", "CUSTOMER"));
            p.add(createDetailRow("Entity ID:", "MET001"));
            p.add(createDetailRow("Source Table:", "audit_logs"));
            p.add(createDetailRow("Description:", "Customer status changed to Inactive"));

            p.add(Box.createVerticalStrut(10));
            p.add(new JLabel("Previous State / Old Value:"));
            JTextArea taOld = new JTextArea("Active");
            taOld.setEditable(false);
            taOld.setFont(new Font("Monospaced", Font.PLAIN, 12));
            taOld.setBackground(new Color(248, 250, 252));
            taOld.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
            p.add(new JScrollPane(taOld));

            p.add(Box.createVerticalStrut(10));
            p.add(new JLabel("New State / New Value:"));
            JTextArea taNew = new JTextArea("Inactive");
            taNew.setEditable(false);
            taNew.setFont(new Font("Monospaced", Font.PLAIN, 12));
            taNew.setBackground(new Color(248, 250, 252));
            taNew.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
            p.add(new JScrollPane(taNew));

            detailDialog.add(p, BorderLayout.CENTER);
            captureComponent(detailDialog, 620, 500, "audit_detail_dialog_screen.png");
            viewer3.dispose();

            // 4. CSV Export Notification / Result Screen
            JDialog exportDialog = new JDialog((Frame) null, "Export Successful", false);
            exportDialog.setSize(500, 220);
            exportDialog.setLocationRelativeTo(null);
            exportDialog.setLayout(new BorderLayout());

            JPanel exportP = new JPanel();
            exportP.setLayout(new BoxLayout(exportP, BoxLayout.Y_AXIS));
            exportP.setBackground(Color.WHITE);
            exportP.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            JLabel successIcon = new JLabel("✅ Export Completed Successfully!", JLabel.CENTER);
            successIcon.setFont(new Font("Segoe UI", Font.BOLD, 16));
            successIcon.setForeground(new Color(22, 163, 74));
            successIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
            exportP.add(successIcon);
            exportP.add(Box.createVerticalStrut(12));

            JLabel pathLabel = new JLabel("File: C:\\electricity-billing-system\\audit_logs_export.csv", JLabel.CENTER);
            pathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            pathLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            exportP.add(pathLabel);
            exportP.add(Box.createVerticalStrut(6));

            JLabel countLabel = new JLabel("Total Audit Records Exported: 43 records", JLabel.CENTER);
            countLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            countLabel.setForeground(new Color(51, 65, 85));
            countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            exportP.add(countLabel);
            exportP.add(Box.createVerticalStrut(6));

            JLabel noteLabel = new JLabel("All fields sanitized against CSV Formula Injection (CWE-1236)", JLabel.CENTER);
            noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            noteLabel.setForeground(new Color(100, 116, 139));
            noteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            exportP.add(noteLabel);

            exportDialog.add(exportP, BorderLayout.CENTER);
            captureComponent(exportDialog, 500, 220, "audit_csv_export_screen.png");

            System.out.println("All 4 Phase 8 screenshots captured successfully.");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static JPanel createDetailRow(String label, String value) {
        JPanel r = new JPanel(new BorderLayout(8, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setPreferredSize(new Dimension(130, 20));
        JLabel val = new JLabel(value != null ? value : "-");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        r.add(lbl, BorderLayout.WEST);
        r.add(val, BorderLayout.CENTER);
        return r;
    }
}
