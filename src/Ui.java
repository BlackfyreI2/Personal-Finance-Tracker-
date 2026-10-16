package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class Ui {
    private Ui() {}

    public static final Color BG_DARK = new Color(25, 25, 28);
    public static final Color BG_CARD = new Color(35, 35, 40);
    public static final Color TEXT_LIGHT = new Color(220, 220, 225);
    public static final Color TEXT_MUTED = new Color(150, 150, 155);
    public static final Color ACCENT_BLUE = new Color(45, 136, 255);
    public static final Color BORDER_COLOR = new Color(55, 55, 60);

    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_LIGHT);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(ACCENT_BLUE);
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);
        
        JTableHeader h = table.getTableHeader();
        h.setBackground(new Color(30, 30, 35));
        h.setForeground(TEXT_MUTED);
        h.setPreferredSize(new Dimension(0, 40));
        h.setFont(h.getFont().deriveFont(Font.BOLD, 13f));
        h.setBorder(new LineBorder(BORDER_COLOR, 1));
    }

    public static JPanel card(JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(15, 15, 15, 15)));
        inner.setOpaque(false);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    public static JPanel titledCard(String title, JComponent inner) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(15, 15, 15, 15)));
        
        JLabel t = new JLabel(title);
        t.setForeground(TEXT_LIGHT);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14f));
        
        inner.setOpaque(false);
        p.add(t, BorderLayout.NORTH);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    // --- เมธอดที่เพิ่มกลับเข้ามาเพื่อแก้ Error ให้ไฟล์ Dialog เดิม ---
    public static JPanel titled(String title, JComponent inner) {
        return titledCard(title, inner);
    }

    public static JPanel metricCard(String title, JLabel value) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(15, 15, 15, 15)));
                
        JLabel t = new JLabel(title);
        t.setForeground(TEXT_MUTED);
        t.setFont(t.getFont().deriveFont(12f));
        
        value.setHorizontalAlignment(SwingConstants.LEFT);
        value.setForeground(TEXT_LIGHT);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 22f));
        
        p.add(t, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(ACCENT_BLUE);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setMargin(new Insets(8, 16, 8, 16));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(60, 60, 65));
        b.setForeground(TEXT_LIGHT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setMargin(new Insets(8, 16, 8, 16));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JPanel toolbar(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        for (JComponent c : items) p.add(c);
        return p;
    }

    public static JLabel hint(String text) {
        JLabel l = new JLabel("<html><body>" + text + "</body></html>");
        l.setForeground(TEXT_MUTED);
        l.setBorder(new EmptyBorder(4, 4, 8, 4));
        return l;
    }

    public static Double parseAmount(String text) {
        try {
            double v = Double.parseDouble(text.trim().replace(",", ""));
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static JPanel formGrid(int rows, JComponent... cells) {
        JPanel grid = new JPanel(new GridLayout(rows, 2, 10, 10));
        grid.setOpaque(false);
        for (JComponent c : cells) grid.add(c);
        return grid;
    }
}