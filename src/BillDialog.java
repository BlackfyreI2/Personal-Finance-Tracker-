package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BillDialog = หน้าต่างจัดการ "บิลล่วงหน้า" (รองรับ Dark Theme สมบูรณ์)
 */
public class BillDialog extends JDialog {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<Bill> bills;
    private final List<Transaction> transactions;
    private final List<Bill> sorted = new ArrayList<>();
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final DecimalFormat plain = new DecimalFormat("0.##");

    private final JTextField nameField = new JTextField(12);
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JTextField amountField = new JTextField(8);
    private final JSpinner dueDayBox = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
    private final JSpinner remindBox = new JSpinner(new SpinnerNumberModel(3, 0, 30, 1));
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ชื่อบิล", "หมวดหมู่", "ยอดปกติ", "ครบกำหนด", "สถานะ", "เตือนล่วงหน้า"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public static void show(Window owner, List<Bill> bills, List<Transaction> transactions) {
        new BillDialog(owner, bills, transactions).setVisible(true);
    }

    private BillDialog(Window owner, List<Bill> bills, List<Transaction> transactions) {
        super(owner, "บิลล่วงหน้า", ModalityType.APPLICATION_MODAL);
        this.bills = bills;
        this.transactions = transactions;
        
        getContentPane().setBackground(Ui.BG_DARK);

        for (String c : Transaction.EXPENSE_CATEGORIES) catBox.addItem(c);
        catBox.setSelectedItem("บิล/สาธารณูปโภค");

        JPanel formInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        formInner.setOpaque(false);
        
        JLabel l1 = new JLabel("ชื่อบิล:"); l1.setForeground(Ui.TEXT_LIGHT);
        JLabel l2 = new JLabel("หมวดหมู่:"); l2.setForeground(Ui.TEXT_LIGHT);
        JLabel l3 = new JLabel("ยอด:"); l3.setForeground(Ui.TEXT_LIGHT);
        JLabel l4 = new JLabel("ครบกำหนดทุกวันที่:"); l4.setForeground(Ui.TEXT_LIGHT);
        JLabel l5 = new JLabel("เตือนล่วงหน้า (วัน):"); l5.setForeground(Ui.TEXT_LIGHT);

        formInner.add(l1);        formInner.add(nameField);
        formInner.add(l2);       formInner.add(catBox);
        formInner.add(l3);            formInner.add(amountField);
        formInner.add(l4); formInner.add(dueDayBox);
        formInner.add(l5); formInner.add(remindBox);
        
        JButton addBtn = Ui.primary("เพิ่มบิล");
        addBtn.addActionListener(e -> onAdd());
        amountField.addActionListener(e -> onAdd());
        formInner.add(addBtn);
        JPanel form = Ui.titled("เพิ่มบิลใหม่", formInner);

        Ui.styleTable(table);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEdit();
            }
        });
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(860, 260));
        scroll.getViewport().setBackground(Ui.BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER_COLOR));

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Ui.BG_DARK);
        center.setBorder(new EmptyBorder(0, 8, 0, 8));
        center.add(Ui.hint("ดับเบิลคลิกแถวหรือกด \"แก้ไข\" เพื่อเปลี่ยนชื่อ ยอด หรือวันครบกำหนด "
                + "กด \"จ่ายแล้ว\" จะบันทึกเป็นรายจ่ายวันนี้แล้วเลื่อนไปรอบถัดไป "
                + "ถ้าตั้งวันที่ 29–31 แล้วเดือนนั้นวันไม่ถึง จะใช้วันสุดท้ายของเดือน"), BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        JButton editBtn = Ui.secondary("แก้ไข");
        JButton payBtn = Ui.primary("จ่ายแล้ว");
        JButton delBtn = Ui.secondary("ลบบิล");
        JButton closeBtn = Ui.secondary("ปิด");
        editBtn.addActionListener(e -> onEdit());
        payBtn.addActionListener(e -> onPay());
        delBtn.addActionListener(e -> onDelete());
        closeBtn.addActionListener(e -> dispose());
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setBackground(Ui.BG_DARK);
        buttons.add(editBtn);
        buttons.add(payBtn);
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

    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            if (!sel && row < sorted.size()) {
                int level = sorted.get(row).level(LocalDate.now());
                c.setForeground(level == 2 ? new Color(255, 100, 100)
                        : level == 1 ? new Color(250, 179, 135) : t.getForeground());
            }
            return c;
        }
    }

    private void reloadTable() {
        sorted.clear();
        sorted.addAll(bills);
        sorted.sort(Comparator.comparing(Bill::nextDueDate));

        LocalDate today = LocalDate.now();
        model.setRowCount(0);
        for (Bill b : sorted) {
            model.addRow(new Object[]{b.getName(), b.getCategory(), money.format(b.getAmount()),
                    b.nextDueDate().format(DATE_FMT), b.statusText(today), b.getRemindDays() + " วัน"});
        }
    }

    private void onAdd() {
        String name = nameField.getText().trim();
        Double amount = Ui.parseAmount(amountField.getText());
        if (name.isEmpty()) { error("กรุณากรอกชื่อบิล"); return; }
        if (amount == null) { error("ยอดต้องเป็นตัวเลขที่มากกว่า 0"); return; }

        bills.add(Bill.create(name, (String) catBox.getSelectedItem(), amount,
                (Integer) dueDayBox.getValue(), (Integer) remindBox.getValue(), LocalDate.now()));
        Filemanager.saveBills(bills);
        nameField.setText("");
        amountField.setText("");
        reloadTable();
    }

    private Bill selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : sorted.get(row);
    }

    private void onEdit() {
        Bill b = selected();
        if (b == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกบิลที่ต้องการแก้ไขก่อน"); return; }

        JTextField name = new JTextField(b.getName(), 16);
        name.setBackground(Ui.BG_DARK);
        name.setForeground(Ui.TEXT_LIGHT);
        name.setCaretColor(Color.WHITE);

        JComboBox<String> cat = new JComboBox<>();
        for (String c : Transaction.EXPENSE_CATEGORIES) cat.addItem(c);
        cat.setSelectedItem(b.getCategory());

        JTextField amount = new JTextField(plain.format(b.getAmount()), 10);
        amount.setBackground(Ui.BG_DARK);
        amount.setForeground(Ui.TEXT_LIGHT);
        amount.setCaretColor(Color.WHITE);

        JSpinner due = new JSpinner(new SpinnerNumberModel(b.getDueDay(), 1, 31, 1));
        JSpinner remind = new JSpinner(new SpinnerNumberModel(b.getRemindDays(), 0, 30, 1));

        JLabel l1 = new JLabel("ชื่อบิล:"); l1.setForeground(Ui.TEXT_LIGHT);
        JLabel l2 = new JLabel("หมวดหมู่:"); l2.setForeground(Ui.TEXT_LIGHT);
        JLabel l3 = new JLabel("ยอดปกติ:"); l3.setForeground(Ui.TEXT_LIGHT);
        JLabel l4 = new JLabel("ครบกำหนดทุกวันที่:"); l4.setForeground(Ui.TEXT_LIGHT);
        JLabel l5 = new JLabel("เตือนล่วงหน้า (วัน):"); l5.setForeground(Ui.TEXT_LIGHT);

        JPanel form = Ui.formGrid(5, l1, name, l2, cat, l3, amount, l4, due, l5, remind);
        form.setOpaque(true);
        form.setBackground(Ui.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        int ok = JOptionPane.showConfirmDialog(this, form, "แก้ไขบิล",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String n = name.getText().trim();
        Double v = Ui.parseAmount(amount.getText());
        if (n.isEmpty()) { error("กรุณากรอกชื่อบิล"); return; }
        if (v == null) { error("ยอดต้องเป็นตัวเลขที่มากกว่า 0"); return; }

        int i = bills.indexOf(b);
        bills.set(i, b.withDetails(n, (String) cat.getSelectedItem(), v,
                (Integer) due.getValue(), (Integer) remind.getValue()));
        Filemanager.saveBills(bills);
        reloadTable();
    }

    private void onPay() {
        Bill b = selected();
        if (b == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกบิลที่จ่ายแล้วก่อน"); return; }

        String input = JOptionPane.showInputDialog(this,
                "ยอดที่จ่ายจริงของ \"" + b.getName() + "\" (บาท):", plain.format(b.getAmount()));
        if (input == null) return;
        Double paid = Ui.parseAmount(input);
        if (paid == null) { error("ยอดต้องเป็นตัวเลขที่มากกว่า 0"); return; }

        transactions.add(b.markPaid(LocalDate.now(), paid));
        Filemanager.save(transactions);
        Filemanager.saveBills(bills);
        reloadTable();
    }

    private void onDelete() {
        Bill b = selected();
        if (b == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกบิลที่ต้องการลบก่อน"); return; }
        int ok = JOptionPane.showConfirmDialog(this, "ลบบิล \"" + b.getName() + "\" ใช่หรือไม่?\n(รายจ่ายที่เคยบันทึกไว้จะไม่ถูกลบ)",
                "ยืนยันการลบ", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            bills.remove(b);
            Filemanager.saveBills(bills);
            reloadTable();
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
    }
}
