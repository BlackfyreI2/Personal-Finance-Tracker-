package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * ปุ่มเลือกวันที่แบบปฏิทิน (Dark Theme สมบูรณ์ แสดงตัวเลขชัดเจน)
 */
public class DatePicker extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] TH_MONTHS = {
            "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
            "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    };
    private static final String[] TH_DAYS = {"อา", "จ", "อ", "พ", "พฤ", "ศ", "ส"};

    private final boolean optional;
    private final JButton display = new JButton();
    private LocalDate date;

    public DatePicker() {
        this(false);
    }

    public DatePicker(boolean optional) {
        this.optional = optional;
        this.date = optional ? null : LocalDate.now();
        setLayout(new BorderLayout());
        setOpaque(false);
        
        display.setHorizontalAlignment(SwingConstants.LEFT);
        display.setBackground(Ui.BG_CARD);
        display.setForeground(Ui.TEXT_LIGHT);
        display.setFocusPainted(false);
        display.setBorder(new LineBorder(Ui.BORDER_COLOR, 1));
        display.setMargin(new Insets(6, 12, 6, 12));
        display.addActionListener(e -> showCalendar());
        
        add(display, BorderLayout.CENTER);
        setPreferredSize(new Dimension(160, 36));
        refreshLabel();
    }

    public LocalDate getDate() { return date; }

    public void setDate(LocalDate date) {
        this.date = date;
        refreshLabel();
    }

    private void refreshLabel() {
        if (date == null) display.setText("ไม่กำหนดวันที่  ▼");
        else display.setText(date.format(FMT) + "  ▼");
    }

    private void showCalendar() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "เลือกวันที่", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        
        CalendarPanel panel = new CalendarPanel(date != null ? date : LocalDate.now(), dialog);
        panel.setBackground(Ui.BG_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Ui.ACCENT_BLUE, 1),
                new EmptyBorder(12, 12, 12, 12)));
                
        dialog.setContentPane(panel);
        dialog.pack();
        Point p = getLocationOnScreen();
        dialog.setLocation(p.x, p.y + getHeight() + 2);
        dialog.setVisible(true);
    }

    private class CalendarPanel extends JPanel {
        private YearMonth ym;
        private final JDialog dialog;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel days = new JPanel(new GridLayout(0, 7, 4, 4));

        CalendarPanel(LocalDate focus, JDialog dialog) {
            this.dialog = dialog;
            ym = YearMonth.from(focus);
            setLayout(new BorderLayout(0, 10));
            setBackground(Ui.BG_DARK);

            JButton prev = Ui.secondary("<");
            JButton next = Ui.secondary(">");
            prev.setMargin(new Insets(4, 10, 4, 10));
            next.setMargin(new Insets(4, 10, 4, 10));
            
            prev.addActionListener(e -> { ym = ym.minusMonths(1); rebuild(); });
            next.addActionListener(e -> { ym = ym.plusMonths(1); rebuild(); });
            
            monthLabel.setForeground(Ui.TEXT_LIGHT);
            monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 15f));

            JPanel nav = new JPanel(new BorderLayout(10, 0));
            nav.setBackground(Ui.BG_DARK);
            nav.add(prev, BorderLayout.WEST);
            nav.add(monthLabel, BorderLayout.CENTER);
            nav.add(next, BorderLayout.EAST);

            days.setBackground(Ui.BG_DARK);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            footer.setBackground(Ui.BG_DARK);
            
            JButton today = Ui.secondary("วันนี้");
            today.addActionListener(e -> pick(LocalDate.now()));
            footer.add(today);
            if (optional) {
                JButton clear = Ui.secondary("ไม่กำหนด");
                clear.addActionListener(e -> pick(null));
                footer.add(clear);
            }
            JButton close = Ui.secondary("ปิด");
            close.addActionListener(e -> dialog.dispose());
            footer.add(close);

            add(nav, BorderLayout.NORTH);
            add(days, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
            rebuild();
        }

        private void rebuild() {
            monthLabel.setText(TH_MONTHS[ym.getMonthValue() - 1] + "  " + ym.getYear());
            days.removeAll();
            for (String d : TH_DAYS) {
                JLabel h = new JLabel(d, SwingConstants.CENTER);
                h.setForeground(Ui.TEXT_MUTED);
                h.setFont(h.getFont().deriveFont(Font.BOLD, 12f));
                days.add(h);
            }
            LocalDate first = ym.atDay(1);
            int skip = first.getDayOfWeek().getValue() % 7;
            LocalDate cell = first.minusDays(skip);
            LocalDate today = LocalDate.now();
            
            for (int i = 0; i < 42; i++) {
                LocalDate d = cell;
                JButton b = new JButton(String.valueOf(d.getDayOfMonth()));
                b.setFocusPainted(false);
                b.setBorderPainted(false);
                // ขยายขนาดปุ่มให้กว้างขึ้น ป้องกันตัวเลขถูกตัดเป็น ...
                b.setPreferredSize(new Dimension(40, 36));
                b.setMargin(new Insets(2, 2, 2, 2));
                
                boolean inMonth = YearMonth.from(d).equals(ym);
                b.setEnabled(inMonth);
                
                if (inMonth && d.equals(date)) {
                    b.setBackground(Ui.ACCENT_BLUE);
                    b.setForeground(Color.WHITE);
                    b.setFont(b.getFont().deriveFont(Font.BOLD));
                } else if (inMonth && d.equals(today)) {
                    b.setBackground(new Color(50, 60, 80));
                    b.setForeground(Ui.ACCENT_BLUE);
                    b.setFont(b.getFont().deriveFont(Font.BOLD));
                } else {
                    b.setBackground(Ui.BG_CARD);
                    b.setForeground(inMonth ? Ui.TEXT_LIGHT : new Color(90, 90, 95));
                }
                
                b.addActionListener(e -> pick(d));
                days.add(b);
                cell = cell.plusDays(1);
            }
            days.revalidate();
            days.repaint();
            if (dialog.isVisible()) dialog.pack();
        }

        private void pick(LocalDate d) {
            setDate(d);
            dialog.dispose();
        }
    }
}