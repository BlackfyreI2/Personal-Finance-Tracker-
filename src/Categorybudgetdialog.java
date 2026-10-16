package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CategoryBudgetDialog = หน้าต่างตั้ง "งบรายเดือนแยกตามหมวดหมู่"
 */
public class Categorybudgetdialog extends JDialog {
    private Map<String, Double> result = null;
    private final Map<String, JTextField> fields = new LinkedHashMap<>();

    public static Map<String, Double> show(Window owner, Map<String, Double> current, List<Transaction> all) {
        Categorybudgetdialog d = new Categorybudgetdialog(owner, current, all);
        d.setVisible(true);
        return d.result;
    }

    private Categorybudgetdialog(Window owner, Map<String, Double> current, List<Transaction> all) {
        super(owner, "งบรายเดือนแยกตามหมวดหมู่", ModalityType.APPLICATION_MODAL);
        DecimalFormat money = new DecimalFormat("#,##0.00");
        YearMonth now = YearMonth.now();

        JPanel grid = new JPanel(new GridLayout(Transaction.EXPENSE_CATEGORIES.size() + 1, 3, 12, 8));
        grid.setBorder(new EmptyBorder(4, 4, 4, 4));
        JLabel h1 = new JLabel("หมวดหมู่");
        JLabel h2 = new JLabel("ใช้ไปเดือนนี้");
        JLabel h3 = new JLabel("งบ (บาท, 0 = ไม่กำหนด)");
        h1.setFont(h1.getFont().deriveFont(Font.BOLD));
        h2.setFont(h2.getFont().deriveFont(Font.BOLD));
        h3.setFont(h3.getFont().deriveFont(Font.BOLD));
        grid.add(h1);
        grid.add(h2);
        grid.add(h3);
        for (String cat : Transaction.EXPENSE_CATEGORIES) {
            JTextField f = new JTextField(10);
            double limit = current.getOrDefault(cat, 0.0);
            f.setText(limit > 0 ? String.valueOf(limit) : "0");
            fields.put(cat, f);
            grid.add(new JLabel(cat));
            grid.add(new JLabel(money.format(Stats.monthCategoryExpense(all, now, cat))));
            grid.add(f);
        }

        JPanel body = new JPanel(new BorderLayout());
        body.setBorder(new EmptyBorder(12, 16, 8, 16));
        body.add(Ui.hint("แก้ตัวเลขในช่องขวาสุดแล้วกดบันทึก — ไม่ต้องลบแล้วเพิ่มใหม่"), BorderLayout.NORTH);
        body.add(grid, BorderLayout.CENTER);

        JButton ok = Ui.primary("บันทึก");
        JButton cancel = Ui.primary("ยกเลิก");
        ok.addActionListener(e -> onOk());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(ok);
        buttons.add(cancel);

        setLayout(new BorderLayout());
        add(body, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    private void onOk() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Map.Entry<String, JTextField> e : fields.entrySet()) {
            try {
                double v = Double.parseDouble(e.getValue().getText().trim().replace(",", ""));
                if (v < 0) throw new NumberFormatException();
                if (v > 0) map.put(e.getKey(), v);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "งบของหมวด \"" + e.getKey() + "\" ต้องเป็นตัวเลขที่ไม่ติดลบ",
                        "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        result = map;
        dispose();
    }
}
