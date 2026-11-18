package electricity.billing.system;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.text.NumberFormat;
import java.util.*;

public class ChartComponents {

    public static class BarChartPanel extends JPanel {
        private String title;
        private Map<String, Double> data = new LinkedHashMap<>();
        private Map<String, Color> customColors = new HashMap<>();
        private Color defaultColor = new Color(37, 99, 235); // Royal Blue
        private String valuePrefix = "";
        private String valueSuffix = "";

        public BarChartPanel(String title, String valuePrefix, String valueSuffix) {
            this.title = title;
            this.valuePrefix = valuePrefix != null ? valuePrefix : "";
            this.valueSuffix = valueSuffix != null ? valueSuffix : "";
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(480, 260));
        }

        public void setData(Map<String, ? extends Number> rawData) {
            this.data.clear();
            if (rawData != null) {
                for (Map.Entry<String, ? extends Number> entry : rawData.entrySet()) {
                    this.data.put(entry.getKey(), entry.getValue() != null ? entry.getValue().doubleValue() : 0.0);
                }
            }
            repaint();
        }

        public void setCustomColor(String key, Color color) {
            if (key != null && color != null) {
                this.customColors.put(key, color);
            }
        }

        public void setDefaultColor(Color color) {
            if (color != null) {
                this.defaultColor = color;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background card border
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            // Chart Title
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(new Color(15, 23, 42));
            g2.drawString(title, 16, 24);

            if (data == null || data.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(new Color(148, 163, 184));
                String emptyMsg = "No Data Available";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(emptyMsg, (w - fm.stringWidth(emptyMsg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int topMargin = 45;
            int bottomMargin = 40;
            int leftMargin = 55;
            int rightMargin = 25;

            int plotWidth = w - leftMargin - rightMargin;
            int plotHeight = h - topMargin - bottomMargin;

            double maxVal = 0.0;
            for (Double v : data.values()) {
                if (v > maxVal) maxVal = v;
            }
            if (maxVal <= 0.0) maxVal = 1.0;

            // Horizontal Grid Lines
            g2.setColor(new Color(226, 232, 240));
            int gridSteps = 4;
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(0);

            for (int i = 0; i <= gridSteps; i++) {
                int y = topMargin + plotHeight - (int) ((double) i / gridSteps * plotHeight);
                g2.drawLine(leftMargin, y, leftMargin + plotWidth, y);

                double gridVal = (maxVal / gridSteps) * i;
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(148, 163, 184));
                String gridLabel = nf.format(gridVal);
                g2.drawString(gridLabel, 8, y + 4);
            }

            // Draw Bars
            int numBars = data.size();
            int slotWidth = plotWidth / Math.max(1, numBars);
            int barWidth = Math.min(46, (int) (slotWidth * 0.58));

            int index = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                String label = entry.getKey();
                double val = entry.getValue();

                int barHeight = (int) ((val / maxVal) * plotHeight);
                int x = leftMargin + (index * slotWidth) + (slotWidth - barWidth) / 2;
                int y = topMargin + plotHeight - barHeight;

                Color barColor = customColors.getOrDefault(label, defaultColor);

                // Draw Bar with rounded top
                g2.setColor(barColor);
                g2.fillRoundRect(x, y, barWidth, barHeight, 6, 6);
                if (barHeight > 6) {
                    g2.fillRect(x, y + 4, barWidth, barHeight - 4);
                }

                // Value Label above bar
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(new Color(30, 41, 59));
                String valStr = valuePrefix + (val >= 1000 ? nf.format(val) : String.format(Locale.US, "%.1f", val).replaceAll("\\.0$", "")) + valueSuffix;
                FontMetrics fm = g2.getFontMetrics();
                int valX = x + (barWidth - fm.stringWidth(valStr)) / 2;
                g2.drawString(valStr, valX, Math.max(topMargin - 4, y - 5));

                // Category Label below bar
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                g2.setColor(new Color(71, 85, 105));
                fm = g2.getFontMetrics();
                String displayLabel = label;
                if (fm.stringWidth(displayLabel) > slotWidth + 4 && displayLabel.length() > 6) {
                    displayLabel = displayLabel.substring(0, 5) + "..";
                }
                int lblX = x + (barWidth - fm.stringWidth(displayLabel)) / 2;
                g2.drawString(displayLabel, lblX, topMargin + plotHeight + 18);

                index++;
            }

            g2.dispose();
        }
    }

    public static class DoughnutChartPanel extends JPanel {
        private String title;
        private Map<String, Double> data = new LinkedHashMap<>();
        private Map<String, Color> sliceColors = new HashMap<>();
        private Color[] palette = {
            new Color(16, 185, 129), // Emerald
            new Color(239, 68, 68),  // Red
            new Color(100, 116, 139),// Slate
            new Color(37, 99, 235),  // Royal Blue
            new Color(245, 158, 11), // Amber
            new Color(139, 92, 246)  // Purple
        };

        public DoughnutChartPanel(String title) {
            this.title = title;
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(480, 260));
        }

        public void setData(Map<String, ? extends Number> rawData) {
            this.data.clear();
            if (rawData != null) {
                for (Map.Entry<String, ? extends Number> entry : rawData.entrySet()) {
                    this.data.put(entry.getKey(), entry.getValue() != null ? entry.getValue().doubleValue() : 0.0);
                }
            }
            repaint();
        }

        public void setCustomColor(String key, Color color) {
            if (key != null && color != null) {
                this.sliceColors.put(key, color);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background card border
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            // Chart Title
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(new Color(15, 23, 42));
            g2.drawString(title, 16, 24);

            double total = 0.0;
            for (Double v : data.values()) {
                total += v;
            }

            if (data == null || data.isEmpty() || total <= 0.0) {
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(new Color(148, 163, 184));
                String emptyMsg = "No Data Available";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(emptyMsg, (w - fm.stringWidth(emptyMsg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            // Doughnut dimensions
            int size = Math.min(w / 2, h - 60);
            int cx = 24 + size / 2;
            int cy = 35 + size / 2;
            int outerRadius = size / 2;
            int innerRadius = (int) (outerRadius * 0.58);

            double startAngle = 90.0;
            int colorIdx = 0;

            for (Map.Entry<String, Double> entry : data.entrySet()) {
                double val = entry.getValue();
                double arcAngle = (val / total) * 360.0;

                Color c = sliceColors.getOrDefault(entry.getKey(), palette[colorIdx % palette.length]);
                g2.setColor(c);
                g2.fill(new Arc2D.Double(cx - outerRadius, cy - outerRadius, outerRadius * 2, outerRadius * 2, 
                                         startAngle, -arcAngle, Arc2D.PIE));

                startAngle -= arcAngle;
                colorIdx++;
            }

            // Cut out doughnut center hole
            g2.setColor(new Color(241, 245, 249));
            g2.fillOval(cx - innerRadius, cy - innerRadius, innerRadius * 2, innerRadius * 2);

            // Center Summary text
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(new Color(15, 23, 42));
            String totalStr = (int) total + "";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(totalStr, cx - fm.stringWidth(totalStr) / 2, cy + 2);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(new Color(100, 116, 139));
            String subStr = "Total";
            fm = g2.getFontMetrics();
            g2.drawString(subStr, cx - fm.stringWidth(subStr) / 2, cy + 16);

            // Right Legend
            int legendX = cx + outerRadius + 30;
            int legendY = 60;
            int rowHeight = 26;

            colorIdx = 0;
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                String label = entry.getKey();
                double val = entry.getValue();
                double pct = (val / total) * 100.0;
                Color c = sliceColors.getOrDefault(label, palette[colorIdx % palette.length]);

                // Color pill
                g2.setColor(c);
                g2.fillRoundRect(legendX, legendY + (colorIdx * rowHeight), 12, 12, 4, 4);

                // Label + Value + Pct
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g2.setColor(new Color(15, 23, 42));
                String text = label + ": " + (int) val + " (" + String.format(Locale.US, "%.1f", pct) + "%)";
                g2.drawString(text, legendX + 20, legendY + (colorIdx * rowHeight) + 11);

                colorIdx++;
            }

            g2.dispose();
        }
    }
}
