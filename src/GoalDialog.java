package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GoalDialog = หน้าต่าง "เป้าหมายการออม" (รองรับ Dark Theme สมบูรณ์ รวมถึงป๊อปอัปแก้ไข)
 */
public class GoalDialog extends JDialog {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<Goal> goals;
    private final double totalBalance;
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final DecimalFormat money0 = new DecimalFormat("#,##0");
    private final DecimalFormat plain = new DecimalFormat("0.##");

    private final JTextField nameField = new JTextField(14);
    private final JTextField targetField = new JTextField(9);
    private final DatePicker deadlinePicker = new DatePicker(true);
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
        deadlinePicker.setDate(null);
        
        getContentPane().setBackground(Ui.BG_DARK);

        JPanel formInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        formInner.setOpaque(false);
        
        JLabel lblName = new JLabel("ชื่อเป้าหมาย:"); lblName.setForeground(Ui.TEXT_LIGHT);
        JLabel lblTarget = new JLabel("ยอดเป้าหมาย:"); lblTarget.setForeground(Ui.TEXT_LIGHT);
        JLabel lblDate = new JLabel("ภายในวันที่:"); lblDate.setForeground(Ui.TEXT_LIGHT);

        formInner.add(lblName); formInner.add(nameField);
        formInner.add(lblTarget);  formInner.add(targetField);
        formInner.add(lblDate);   formInner.add(deadlinePicker);
        
        JButton addBtn = Ui.primary("เพิ่มเป้าหมาย");
        addBtn.addActionListener(e -> onAdd());
        formInner.add(addBtn);
        JPanel form = Ui.titled("สร้างเป้าหมายใหม่", formInner);

        Ui.styleTable(table);
        table.setRowHeight(32);
        table.getColumnModel().getColumn(3).setCellRenderer(new ProgressRenderer());
        table.getColumnModel().getColumn(3).setPreferredWidth(220);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{1, 2, 4, 6}) table.getColumnModel().getColumn(c).setCellRenderer(right);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEdit();
            }
        });
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(920, 250));
        scroll.getViewport().setBackground(Ui.BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER_COLOR));

        summary.setBorder(new EmptyBorder(8, 12, 8, 12));
        summary.setForeground(Ui.TEXT_LIGHT);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Ui.BG_DARK);
        center.setBorder(new EmptyBorder(0, 8, 0, 8));
        center.add(summary, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        JButton editBtn = Ui.secondary("แก้ไข");
        JButton depositBtn = Ui.primary("ฝากเงิน");
        JButton withdrawBtn = Ui.primary("ถอนเงิน");
        JButton delBtn = Ui.secondary("ลบเป้าหมาย");
        JButton closeBtn = Ui.secondary("ปิด");
        editBtn.addActionListener(e -> onEdit());
        depositBtn.addActionListener(e -> onMove(true));
        withdrawBtn.addActionListener(e -> onMove(false));
        delBtn.addActionListener(e -> onDelete());
        closeBtn.addActionListener(e -> dispose());
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setBackground(Ui.BG_DARK);
        buttons.add(editBtn);
        buttons.add(depositBtn);
        buttons.add(withdrawBtn);
        buttons.add(delBtn);
        buttons.add(closeBtn);

        setLayout(new BorderLayout(6, 6));
        add(form, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        reloadTable();
        pack();
        setLocationRelativeTo(owner);
    }

    private class ProgressRenderer implements TableCellRenderer {
        private final Charts.MeterBar bar = new Charts.MeterBar();
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Goal g = goals.get(row);
            int pct = g.percent();
            bar.set(pct, pct + "%", g.isDone() ? new Color(46, 204, 113) : Ui.ACCENT_BLUE);
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
                + "ใช้ได้อิสระ <b>" + money.format(free) + "</b><br>"
                + "<span style='color:#A0A0A5; font-size:10px;'>ดับเบิลคลิกแถวหรือกดแก้ไข เพื่อเปลี่ยนชื่อ ยอดเป้าหมาย หรือวันกำหนด</span></html>");
    }

    private void onAdd() {
        String name = nameField.getText().trim();
        Double target = Ui.parseAmount(targetField.getText());
        if (name.isEmpty()) { error("กรุณากรอกชื่อเป้าหมาย"); return; }
        if (target == null) { error("ยอดเป้าหมายต้องเป็นตัวเลขที่มากกว่า 0"); return; }

        LocalDate deadline = deadlinePicker.getDate();
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            error("วันที่ต้องไม่ใช่วันที่ผ่านมาแล้ว");
            return;
        }

        goals.add(new Goal(name, target, 0, deadline, LocalDate.now()));
        Filemanager.saveGoals(goals);
        nameField.setText("");
        targetField.setText("");
        deadlinePicker.setDate(null);
        reloadTable();
    }

    private Goal selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : goals.get(row);
    }

    private void onEdit() {
        Goal g = selected();
        if (g == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกเป้าหมายที่ต้องการแก้ไขก่อน"); return; }

        JTextField name = new JTextField(g.getName(), 16);
        name.setBackground(Ui.BG_DARK);
        name.setForeground(Ui.TEXT_LIGHT);
        name.setCaretColor(Color.WHITE);

        JTextField target = new JTextField(plain.format(g.getTarget()), 10);
        target.setBackground(Ui.BG_DARK);
        target.setForeground(Ui.TEXT_LIGHT);
        target.setCaretColor(Color.WHITE);

        DatePicker deadline = new DatePicker(true);
        deadline.setDate(g.getDeadline());

        JLabel l1 = new JLabel("ชื่อเป้าหมาย:"); l1.setForeground(Ui.TEXT_LIGHT);
        JLabel l2 = new JLabel("ยอดเป้าหมาย:"); l2.setForeground(Ui.TEXT_LIGHT);
        JLabel l3 = new JLabel("ภายในวันที่:"); l3.setForeground(Ui.TEXT_LIGHT);

        // สร้าง JPanel สำหรับฟอร์มแก้ไข และปรับพื้นหลังให้เป็น Dark Theme กลมกลืน
        JPanel form = Ui.formGrid(3, l1, name, l2, target, l3, deadline);
        form.setOpaque(true);
        form.setBackground(Ui.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ปรับแต่งปุ่ม OK / Cancel ใน JOptionPane ให้เข้ากับธีมมืด
        UIManager.put("OptionPane.background", Ui.BG_DARK);
        UIManager.put("Panel.background", Ui.BG_DARK);
        UIManager.put("OptionPane.messageForeground", Ui.TEXT_LIGHT);

        int ok = JOptionPane.showConfirmDialog(this, form, "แก้ไขเป้าหมาย",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String n = name.getText().trim();
        Double v = Ui.parseAmount(target.getText());
        if (n.isEmpty()) { error("กรุณากรอกชื่อเป้าหมาย"); return; }
        if (v == null) { error("ยอดเป้าหมายต้องเป็นตัวเลขที่มากกว่า 0"); return; }

        int i = goals.indexOf(g);
        goals.set(i, g.withDetails(n, v, deadline.getDate()));
        Filemanager.saveGoals(goals);
        reloadTable();
    }

    private void onMove(boolean deposit) {
        Goal g = selected();
        if (g == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกเป้าหมายก่อน"); return; }

        String input = JOptionPane.showInputDialog(this,
                (deposit ? "ฝากเข้า" : "ถอนจาก") + " \"" + g.getName() + "\" (บาท):");
        if (input == null) return;
        Double amount = Ui.parseAmount(input);
        if (amount == null) { error("จำนวนเงินต้องเป็นตัวเลขที่มากกว่า 0"); return; }

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
