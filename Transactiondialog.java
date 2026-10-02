package src;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * TransactionDialog = หน้าต่างป๊อปอัปสำหรับ "แก้ไขรายการ"
 */
public class Transactiondialog extends JDialog {
    private Transaction result = null;   // ถ้ากดยกเลิกจะเป็น null

    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{Transaction.INCOME, Transaction.EXPENSE});
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JTextField nameField = new JTextField(18);
    private final JTextField amountField = new JTextField(10);
    private final JTextField dateField = new JTextField(10);

    /** เปิดหน้าต่าง รอจนผู้ใช้ปิด แล้วคืนรายการที่แก้แล้ว (หรือ null ถ้ายกเลิก) */
    public static Transaction show(Window owner, Transaction t) {
        Transactiondialog d = new Transactiondialog(owner, t);
        d.setVisible(true);   // เป็น modal: โค้ดจะหยุดรอตรงนี้จนหน้าต่างถูกปิด
        return d.result;
    }

    private Transactiondialog(Window owner, Transaction t) {
        super(owner, "แก้ไขรายการ", ModalityType.APPLICATION_MODAL);

        typeBox.setSelectedItem(t.getType());
        fillCategories();
        catBox.setSelectedItem(t.getCategory());
        typeBox.addActionListener(e -> fillCategories());
        nameField.setText(t.getName());
        amountField.setText(String.valueOf(t.getAmount()));
        dateField.setText(t.getDate().toString());

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        form.add(new JLabel("ประเภท:"));              form.add(typeBox);
        form.add(new JLabel("หมวดหมู่:"));            form.add(catBox);
        form.add(new JLabel("ชื่อรายการ:"));          form.add(nameField);
        form.add(new JLabel("จำนวนเงิน:"));           form.add(amountField);
        form.add(new JLabel("วันที่ (ปปปป-ดด-วว):")); form.add(dateField);

        JButton ok = new JButton("บันทึก");
        JButton cancel = new JButton("ยกเลิก");
        ok.addActionListener(e -> onOk());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    private void fillCategories() {
        catBox.removeAllItems();
        boolean income = Transaction.INCOME.equals(typeBox.getSelectedItem());
        for (String c : income ? Transaction.INCOME_CATEGORIES : Transaction.EXPENSE_CATEGORIES) catBox.addItem(c);
    }

    private void onOk() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { error("กรุณากรอกชื่อรายการ"); return; }

        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) { error("จำนวนเงินต้องเป็นตัวเลข"); return; }
        if (amount <= 0) { error("จำนวนเงินต้องมากกว่า 0"); return; }

        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException e) { error("รูปแบบวันที่ไม่ถูกต้อง เช่น 2026-10-02"); return; }

        result = new Transaction((String) typeBox.getSelectedItem(), (String) catBox.getSelectedItem(), name, amount, date);
        dispose();
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
    }
}