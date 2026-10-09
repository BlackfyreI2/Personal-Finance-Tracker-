package src;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * GoalDialog = หน้าต่าง "เป้าหมายการออม" (สร้างเป้าหมาย / ฝากเงิน / ถอนเงิน / ลบ)
 *
 * แนวคิด: เงินในเป้าหมายคือการ "แบ่งส่วน" ของยอดคงเหลือที่มีอยู่ ไม่ใช่รายจ่าย
 * จึงไม่ไปแก้ประวัติรายรับ-รายจ่าย (ยอดคงเหลือรวมจึงไม่เพี้ยน และไม่นับซ้ำ)
 */
public class GoalDialog extends JDialog {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<Goal> goals;
    private final double totalBalance;      // ยอดคงเหลือรวมทั้งหมด (รายรับ - รายจ่าย)
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final DecimalFormat money0 = new DecimalFormat("#,##0");

    private final JTextField nameField = new JTextField(14);
    private final JTextField targetField = new JTextField(9);
    private final JTextField deadlineField = new JTextField(9);
    private final JLabel summary = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"เป้าหมาย", "เก็บแล้ว", "เป้าหมาย", "ความคืบหน้า", "เหลืออีก", "กำหนด", "ต้องออม/เดือน"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public static void show(Window owner, List<Goal> goals, double totalBalance) {
        new GoalDialog(owner, goals, totalBalance).setVisible(true);
    }

    private GoalDialog(Window owner, List<Goal> goals, double totalBalance) {
        super(owner, "เป้าหมายการออม", ModalityType.APPLICATION_MODAL);
        this.goals = goals;
        this.totalBalance = totalBalance;

        // --- ฟอร์มสร้างเป้าหมาย ---
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("สร้างเป้าหมายใหม่"));
        form.add(new JLabel("ชื่อเป้าหมาย:"));    form.add(nameField);
        form.add(new JLabel("ยอดเป้าหมาย:"));     form.add(targetField);
        form.add(new JLabel("ภายในวันที่ (ปปปป-ดด-วว, เว้นว่างได้):")); form.add(deadlineField);
        JButton addBtn = new JButton("เพิ่มเป้าหมาย");
        addBtn.addActionListener(e -> onAdd());
        form.add(addBtn);

        // --- ตาราง ---
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(3).setCellRenderer(new ProgressRenderer());
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{1, 2, 4, 6}) table.getColumnModel().getColumn(c).setCellRenderer(right);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(900, 230));

        summary.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        // --- ปุ่ม ---
        JButton depositBtn = new JButton("ฝากเงิน");
        JButton withdrawBtn = new JButton("ถอนเงิน");
        JButton delBtn = new JButton("ลบเป้าหมาย");
        JButton closeBtn = new JButton("ปิด");
        depositBtn.addActionListener(e -> onMove(true));
        withdrawBtn.addActionListener(e -> onMove(false));
        delBtn.addActionListener(e -> onDelete());
        closeBtn.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(depositBtn);
        buttons.add(withdrawBtn);
        buttons.add(delBtn);
        buttons.add(closeBtn);

        JPanel center = new JPanel(new BorderLayout());
        center.add(summary, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        setLayout(new BorderLayout(6, 6));
        add(form, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        reloadTable();
        pack();
        setLocationRelativeTo(owner);
    }

    /** วาดแถบความคืบหน้าในช่อง (ใช้ MeterBar ที่วาดเองใน Charts) */
    private class ProgressRenderer implements TableCellRenderer {
        private final Charts.MeterBar bar = new Charts.MeterBar();
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Goal g = goals.get(row);
            int pct = g.percent();
            bar.set(pct, pct + "%", g.isDone() ? new Color(46, 204, 113) : new Color(52, 152, 219));
            return bar;
        }
    }

    private void reloadTable() {
        LocalDate today = LocalDate.now();
        model.setRowCount(0);
        double allocated = 0;
        for (Goal g : goals) {
            allocated += g.getSaved();
            String deadline = g.getDeadline() == null ? "-" : g.getDeadline().format(DATE_FMT);
            String monthly = g.isDone() ? "สำเร็จแล้ว" : g.getDeadline() == null ? "-" : money.format(g.monthlyNeeded(today));
            if (!g.isDone() && g.getDeadline() != null && g.monthsLeft(today) == 0) deadline += " (เลยกำหนด)";
            model.addRow(new Object[]{g.getName(), money.format(g.getSaved()), money.format(g.getTarget()),
                    g, money.format(g.remaining()), deadline, monthly});
        }
        double free = totalBalance - allocated;
        summary.setText("<html>ยอดคงเหลือรวม <b>" + money.format(totalBalance) + "</b> บาท &nbsp;|&nbsp; "
                + "แบ่งไว้ในเป้าหมาย <b>" + money.format(allocated) + "</b> &nbsp;|&nbsp; "
                + "ใช้ได้อิสระ <b>" + money.format(free) + "</b></html>");
    }

    private void onAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { error("กรุณากรอกชื่อเป้าหมาย"); return; }
        double target;
        try {
            target = Double.parseDouble(targetField.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) { error("ยอดเป้าหมายต้องเป็นตัวเลข"); return; }
        if (target <= 0) { error("ยอดเป้าหมายต้องมากกว่า 0"); return; }

        LocalDate deadline = null;
        String dl = deadlineField.getText().trim();
        if (!dl.isEmpty()) {
            try {
                deadline = LocalDate.parse(dl);
            } catch (DateTimeParseException e) { error("รูปแบบวันที่ไม่ถูกต้อง เช่น 2026-12-31"); return; }
            if (deadline.isBefore(LocalDate.now())) { error("วันที่ต้องไม่ใช่วันที่ผ่านมาแล้ว"); return; }
        }

        goals.add(new Goal(name, target, 0, deadline, LocalDate.now()));
        Filemanager.saveGoals(goals);
        nameField.setText("");
        targetField.setText("");
        deadlineField.setText("");
        reloadTable();
    }

    private Goal selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : goals.get(row);
    }

    /** ฝากเงิน (deposit=true) หรือถอนเงิน (deposit=false) */
    private void onMove(boolean deposit) {
        Goal g = selected();
        if (g == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกเป้าหมายก่อน"); return; }

        String input = JOptionPane.showInputDialog(this,
                (deposit ? "ฝากเข้า" : "ถอนจาก") + " \"" + g.getName() + "\" (บาท):");
        if (input == null) return;
        double amount;
        try {
            amount = Double.parseDouble(input.trim().replace(",", ""));
        } catch (NumberFormatException e) { error("จำนวนเงินต้องเป็นตัวเลข"); return; }
        if (amount <= 0) { error("จำนวนเงินต้องมากกว่า 0"); return; }

        if (deposit) {
            double allocated = 0;
            for (Goal x : goals) allocated += x.getSaved();
            if (amount > totalBalance - allocated) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "ยอดคงเหลือที่ใช้ได้อิสระมีแค่ " + money.format(totalBalance - allocated)
                                + " บาท\nยังต้องการฝากต่อหรือไม่?", "ยอดคงเหลือไม่พอ", JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) return;
            }
            boolean wasDone = g.isDone();
            g.deposit(amount);
            Filemanager.saveGoals(goals);
            reloadTable();
            if (!wasDone && g.isDone()) {
                JOptionPane.showMessageDialog(this, "ยินดีด้วย! ถึงเป้าหมาย \"" + g.getName() + "\" แล้ว ("
                        + money0.format(g.getTarget()) + " บาท)", "สำเร็จ", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            if (amount > g.getSaved()) { error("ยอดที่ถอนมากกว่าที่เก็บไว้ (" + money.format(g.getSaved()) + " บาท)"); return; }
            g.withdraw(amount);
            Filemanager.saveGoals(goals);
            reloadTable();
        }
    }

    private void onDelete() {
        Goal g = selected();
        if (g == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกเป้าหมายที่ต้องการลบก่อน"); return; }
        int ok = JOptionPane.showConfirmDialog(this,
                "ลบเป้าหมาย \"" + g.getName() + "\" ใช่หรือไม่?\n(เงิน " + money.format(g.getSaved())
                        + " บาท จะกลับเป็นยอดคงเหลือที่ใช้ได้อิสระ)", "ยืนยันการลบ", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            goals.remove(g);
            Filemanager.saveGoals(goals);
            reloadTable();
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
    }
}
