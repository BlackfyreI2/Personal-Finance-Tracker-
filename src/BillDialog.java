package src;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * BillDialog = หน้าต่างจัดการ "บิลล่วงหน้า" (เพิ่ม / จ่ายแล้ว / ลบ)
 */
public class BillDialog extends JDialog {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final List<Bill> bills;
    private final List<Transaction> transactions;
    private final List<Bill> sorted = new ArrayList<>();   // ลำดับเดียวกับแถวในตาราง (เรียงตามวันครบกำหนด)
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

    /** เปิดหน้าต่างและรอจนปิด (bills และ transactions จะถูกแก้ไขและบันทึกลงไฟล์ให้เอง) */
    public static void show(Window owner, List<Bill> bills, List<Transaction> transactions) {
        new BillDialog(owner, bills, transactions).setVisible(true);
    }

    private BillDialog(Window owner, List<Bill> bills, List<Transaction> transactions) {
        super(owner, "บิลล่วงหน้า", ModalityType.APPLICATION_MODAL);
        this.bills = bills;
        this.transactions = transactions;

        for (String c : Transaction.EXPENSE_CATEGORIES) catBox.addItem(c);
        catBox.setSelectedItem("บิล/สาธารณูปโภค");

        // --- ฟอร์มเพิ่มบิล ---
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("เพิ่มบิล"));
        form.add(new JLabel("ชื่อบิล:"));        form.add(nameField);
        form.add(new JLabel("หมวดหมู่:"));       form.add(catBox);
        form.add(new JLabel("ยอด:"));            form.add(amountField);
        form.add(new JLabel("ครบกำหนดทุกวันที่:")); form.add(dueDayBox);
        form.add(new JLabel("เตือนล่วงหน้า (วัน):")); form.add(remindBox);
        JButton addBtn = new JButton("เพิ่ม");
        addBtn.addActionListener(e -> onAdd());
        amountField.addActionListener(e -> onAdd());
        form.add(addBtn);

        // --- ตาราง ---
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(820, 240));

        JLabel hint = new JLabel("<html><body style='width:760px'>เมื่อกด \"จ่ายแล้ว\" โปรแกรมจะบันทึกเป็นรายจ่ายวันนี้ให้ "
                + "และเลื่อนบิลไปรอบเดือนถัดไปอัตโนมัติ (ใส่ยอดที่จ่ายจริงได้ เช่น ค่าไฟที่ยอดไม่เท่ากันทุกเดือน) "
                + "ถ้าตั้งวันที่ 29-31 แล้วเดือนนั้นมีวันไม่ถึง จะใช้วันสุดท้ายของเดือน</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // --- ปุ่ม ---
        JButton payBtn = new JButton("จ่ายแล้ว");
        JButton delBtn = new JButton("ลบบิล");
        JButton closeBtn = new JButton("ปิด");
        payBtn.addActionListener(e -> onPay());
        delBtn.addActionListener(e -> onDelete());
        closeBtn.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(payBtn);
        buttons.add(delBtn);
        buttons.add(closeBtn);

        JPanel center = new JPanel(new BorderLayout());
        center.add(hint, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        setLayout(new BorderLayout(6, 6));
        add(form, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        reloadTable();
        pack();
        setLocationRelativeTo(owner);
    }

    /** แสดงสถานะสีส้ม/แดงเมื่อใกล้หรือเลยกำหนด */
    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            if (!sel && row < sorted.size()) {
                int level = sorted.get(row).level(LocalDate.now());
                c.setForeground(level == 2 ? new Color(220, 53, 69) : level == 1 ? new Color(230, 126, 34) : t.getForeground());
            }
            return c;
        }
    }

    private void reloadTable() {
        sorted.clear();
        sorted.addAll(bills);
        sorted.sort(Comparator.comparing(Bill::nextDueDate));   // ใกล้ครบกำหนดที่สุดอยู่บน

        LocalDate today = LocalDate.now();
        model.setRowCount(0);
        for (Bill b : sorted) {
            model.addRow(new Object[]{b.getName(), b.getCategory(), money.format(b.getAmount()),
                    b.nextDueDate().format(DATE_FMT), b.statusText(today), b.getRemindDays() + " วัน"});
        }
    }

    private void onAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { error("กรุณากรอกชื่อบิล"); return; }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) { error("ยอดต้องเป็นตัวเลข"); return; }
        if (amount <= 0) { error("ยอดต้องมากกว่า 0"); return; }

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

    private void onPay() {
        Bill b = selected();
        if (b == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกบิลที่จ่ายแล้วก่อน"); return; }

        String input = JOptionPane.showInputDialog(this,
                "ยอดที่จ่ายจริงของ \"" + b.getName() + "\" (บาท):", plain.format(b.getAmount()));
        if (input == null) return;
        double paid;
        try {
            paid = Double.parseDouble(input.trim().replace(",", ""));
        } catch (NumberFormatException e) { error("ยอดต้องเป็นตัวเลข"); return; }
        if (paid <= 0) { error("ยอดต้องมากกว่า 0"); return; }

        transactions.add(b.markPaid(LocalDate.now(), paid));   // บันทึกเป็นรายจ่าย + เลื่อนไปรอบถัดไป
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
