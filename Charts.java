package src;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Charts = กราฟที่วาดเอง (ไม่ต้องใช้ไลบรารีเพิ่ม)
 *   Charts.PieChart = กราฟวงกลม
 *   Charts.BarChart = กราฟแท่ง
 */
public class Charts {
    private static final Color[] PALETTE = {
            new Color(231, 76, 60), new Color(52, 152, 219), new Color(46, 204, 113),
            new Color(241, 196, 15), new Color(155, 89, 182), new Color(230, 126, 34),
            new Color(26, 188, 156), new Color(52, 73, 94), new Color(149, 165, 166)
    };
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0");

    /** โหมดมืดหรือไม่ (Main เป็นคนตั้งค่า) กราฟจะเปลี่ยนสีข้อความ/เส้นตามนี้ */
    public static boolean dark = false;
    private static Color textColor()  { return dark ? new Color(220, 220, 220) : new Color(60, 60, 60); }
    private static Color mutedColor() { return dark ? new Color(160, 160, 160) : Color.GRAY; }
    private static Color gridColor()  { return dark ? new Color(80, 80, 80) : new Color(225, 225, 225); }

    // ===================== แถบความคืบหน้า (วาดเอง เพราะ JProgressBar เปลี่ยนสีไม่ได้ในบางธีม) =====================
    public static class MeterBar extends JComponent {
        private int percent = 0;
        private String text = "";
        private Color color = new Color(46, 204, 113);

        public void set(int percent, String text, Color color) {
            this.percent = Math.max(0, Math.min(100, percent));
            this.text = text;
            this.color = color;
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(200, 24); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(dark ? new Color(70, 70, 70) : new Color(215, 215, 215));
            g2.fillRoundRect(0, 0, w, h, 10, 10);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, Math.max(percent * w / 100, percent > 0 ? 10 : 0), h, 10, 10);
            g2.setColor(dark ? Color.WHITE : new Color(30, 30, 30));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    public static Color meterColor(int percent) {
        if (percent < 75) {
            return new Color(46, 204, 113); // สีเขียว (ยังอยู่ในงบ)
        } else if (percent < 90) {
            return new Color(241, 196, 15); // สีเหลือง (เริ่มใกล้เกินงบ)
        } else {
            return new Color(231, 76, 60);  // สีแดง (เกินงบ หรือใกล้เกินงบมาก)
        }
    }
    // ===================== กราฟวงกลม =====================
    public static class PieChart extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();

        public void setData(Map<String, Double> data) {
            this.data = data;
            repaint();   // สั่งให้วาดใหม่
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double total = 0;
            for (double v : data.values()) total += v;
            int w = getWidth(), h = getHeight();

            if (total <= 0) {
                String msg = "ยังไม่มีข้อมูลรายจ่าย";
                g2.setColor(mutedColor());
                g2.drawString(msg, (w - g2.getFontMetrics().stringWidth(msg)) / 2, h / 2);
                return;
            }

            int size = Math.max(60, Math.min(w / 2, h - 40));
            int x = 20, y = (h - size) / 2;
            double start = 90;   // เริ่มวาดจากด้านบน
            int i = 0;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                double angle = e.getValue() / total * 360;
                g2.setColor(PALETTE[i % PALETTE.length]);
                g2.fill(new Arc2D.Double(x, y, size, size, start, -angle, Arc2D.PIE));
                start -= angle;
                i++;
            }

            // คำอธิบาย (legend)
            int lx = x + size + 20, ly = Math.max(20, y + 10);
            i = 0;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                g2.setColor(PALETTE[i % PALETTE.length]);
                g2.fillRect(lx, ly - 11, 12, 12);
                g2.setColor(textColor());
                double pct = e.getValue() / total * 100;
                g2.drawString(e.getKey() + "  " + String.format("%.1f", pct) + "%  (" + MONEY.format(e.getValue()) + ")", lx + 18, ly);
                ly += 22;
                i++;
            }
        }
    }

    // ===================== กราฟแท่ง =====================
    public static class BarChart extends JPanel {
        private List<String> labels = List.of();
        private double[] income = new double[0];
        private double[] expense = new double[0];

        public void setData(List<String> labels, double[] income, double[] expense) {
            this.labels = labels;
            this.income = income;
            this.expense = expense;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double max = 0;
            for (double v : income) max = Math.max(max, v);
            for (double v : expense) max = Math.max(max, v);

            int w = getWidth(), h = getHeight();
            if (max <= 0) {
                String msg = "ยังไม่มีข้อมูล";
                g2.setColor(mutedColor());
                g2.drawString(msg, (w - g2.getFontMetrics().stringWidth(msg)) / 2, h / 2);
                return;
            }

            int left = 60, right = 15, top = 30, bottom = 30;
            int plotW = w - left - right, plotH = h - top - bottom;

            // เส้นกริด + ตัวเลขแกน Y
            g2.setFont(g2.getFont().deriveFont(11f));
            for (int i = 0; i <= 4; i++) {
                int yy = top + plotH - plotH * i / 4;
                g2.setColor(gridColor());
                g2.drawLine(left, yy, left + plotW, yy);
                g2.setColor(mutedColor());
                String s = MONEY.format(max * i / 4);
                g2.drawString(s, left - 8 - g2.getFontMetrics().stringWidth(s), yy + 4);
            }

            // แท่งกราฟ
            int n = labels.size();
            int groupW = plotW / n;
            int barW = Math.max(6, groupW / 3);
            for (int i = 0; i < n; i++) {
                int gx = left + i * groupW + (groupW - 2 * barW) / 2;
                int hi = (int) (income[i] / max * plotH);
                int he = (int) (expense[i] / max * plotH);
                g2.setColor(new Color(46, 204, 113));
                g2.fillRect(gx, top + plotH - hi, barW, hi);
                g2.setColor(new Color(231, 76, 60));
                g2.fillRect(gx + barW, top + plotH - he, barW, he);

                g2.setColor(textColor());
                String lb = labels.get(i);
                g2.drawString(lb, left + i * groupW + (groupW - g2.getFontMetrics().stringWidth(lb)) / 2, top + plotH + 18);
            }

            // legend มุมบนขวา
            int lx = w - 170;
            g2.setColor(new Color(46, 204, 113)); g2.fillRect(lx, 10, 12, 12);
            g2.setColor(textColor());         g2.drawString("รายรับ", lx + 16, 21);
            g2.setColor(new Color(231, 76, 60));  g2.fillRect(lx + 75, 10, 12, 12);
            g2.setColor(textColor());         g2.drawString("รายจ่าย", lx + 91, 21);
        }
    }

   
}