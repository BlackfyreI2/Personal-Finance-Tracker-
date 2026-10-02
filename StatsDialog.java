package src;
import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StatsDialog = หน้าต่างสถิติ/กราฟของเดือนที่เลือก (เปิดจากเมนู "เครื่องมือ")
 */
public class StatsDialog extends JDialog {
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final DecimalFormat money0 = new DecimalFormat("#,##0");

    public static void show(Window owner, List<Transaction> all, YearMonth month, Map<String, Double> catBudgets) {
        new StatsDialog(owner, all, month, catBudgets).setVisible(true);
    }

    private StatsDialog(Window owner, List<Transaction> all, YearMonth month, Map<String, Double> catBudgets) {
        super(owner, "สถิติ", ModalityType.APPLICATION_MODAL);

        // รายการเฉพาะเดือนที่เลือก
        List<Transaction> items = new ArrayList<>();
        for (Transaction t : all) if (YearMonth.from(t.getDate()).equals(month)) items.add(t);

        // กราฟวงกลม + กราฟแท่ง
        Charts.PieChart pie = new Charts.PieChart();
        pie.setData(Stats.expenseByCategory(items));
        pie.setBorder(BorderFactory.createTitledBorder("สัดส่วนรายจ่ายตามหมวดหมู่ (" + month + ")"));

        List<String> labels = new ArrayList<>();
        double[] inc = new double[6];
        double[] exp = new double[6];
        for (int i = 0; i < 6; i++) {
            YearMonth ym = month.minusMonths(5 - i);
            labels.add(ym.toString());
            inc[i] = Stats.monthTotal(all, ym, true);
            exp[i] = Stats.monthTotal(all, ym, false);
        }
        Charts.BarChart bar = new Charts.BarChart();
        bar.setData(labels, inc, exp);
        bar.setBorder(BorderFactory.createTitledBorder("รายรับ-รายจ่ายย้อนหลัง 6 เดือน"));

        JPanel charts = new JPanel(new GridLayout(1, 2, 10, 0));
        charts.add(pie);
        charts.add(bar);

        // ข้อมูลเชิงลึก
        JLabel insights = new JLabel(buildInsights(items));
        insights.setVerticalAlignment(SwingConstants.TOP);
        insights.setBorder(BorderFactory.createTitledBorder("ข้อมูลเชิงลึก"));

        // งบตามหมวดหมู่
        JPanel meters = new JPanel(new GridLayout(0, 1, 0, 4));
        for (String cat : Transaction.EXPENSE_CATEGORIES) {
            double limit = catBudgets.getOrDefault(cat, 0.0);
            if (limit <= 0) continue;
            double spent = Stats.monthCategoryExpense(all, month, cat);
            int pct = (int) Math.round(spent / limit * 100);
            Charts.MeterBar m = new Charts.MeterBar();
            m.set(pct, cat + ": " + money0.format(spent) + " / " + money0.format(limit) + "  (" + pct + "%)", Charts.meterColor(pct));
            meters.add(m);
        }
        if (meters.getComponentCount() == 0) {
            meters.add(new JLabel("ยังไม่ได้ตั้งงบตามหมวด (เมนู เครื่องมือ > งบตามหมวด)"));
        }
        JScrollPane meterScroll = new JScrollPane(meters);
        meterScroll.setBorder(BorderFactory.createTitledBorder("งบตามหมวดหมู่"));

        JPanel south = new JPanel(new GridLayout(1, 2, 10, 0));
        south.setPreferredSize(new Dimension(100, 190));
        south.add(insights);
        south.add(meterScroll);

        JButton close = new JButton("ปิด");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(close);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        root.add(charts, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(root, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setSize(920, 640);
        setLocationRelativeTo(owner);
    }

    private String buildInsights(List<Transaction> items) {
        StringBuilder sb = new StringBuilder("<html><div style='padding:4px'>");
        if (items.isEmpty()) {
            sb.append("ยังไม่มีข้อมูลในเดือนนี้");
        } else {
            double income = Stats.sum(items, true);
            double expense = Stats.sum(items, false);
            Transaction big = Stats.biggestExpense(items);
            Map<String, Double> cats = Stats.expenseByCategory(items);
            sb.append("• รายจ่ายเฉลี่ยต่อวัน: <b>").append(money.format(Stats.avgExpensePerDay(items))).append("</b> บาท<br>");
            if (big != null) {
                sb.append("• รายจ่ายสูงสุด: <b>").append(escape(big.getName())).append("</b> ")
                  .append(money.format(big.getAmount())).append(" บาท<br>");
            }
            if (!cats.isEmpty()) {
                Map.Entry<String, Double> top = cats.entrySet().iterator().next();
                sb.append("• หมวดที่ใช้จ่ายมากที่สุด: <b>").append(escape(top.getKey())).append("</b> ")
                  .append(money.format(top.getValue())).append(" บาท<br>");
            }
            if (income > 0) {
                double rate = (income - expense) / income * 100;
                sb.append("• อัตราการออม: <b>").append(String.format("%.1f", rate)).append("%</b> ของรายรับ");
            } else {
                sb.append("• อัตราการออม: ยังไม่มีรายรับ");
            }
        }
        return sb.append("</div></html>").toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}