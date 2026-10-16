package src;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * TransactionDialog = หน้าต่างป๊อปอัปสำหรับ "แก้ไขรายการ"
 */
public class Transactiondialog extends JDialog {
    private Transaction result = null;

    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{Transaction.INCOME, Transaction.EXPENSE});
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JTextField nameField = new JTextField(18);
    private final JTextField amountField = new JTextField(10);
    private final DatePicker datePicker = new DatePicker();

    public static Transaction show(Window owner, Transaction t) {
        Transactiondialog d = new Transactiondialog(owner, t);
        d.setVisible(true);
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
        datePicker.setDate(t.getDate());

        JPanel form = Ui.formGrid(5,
                new JLabel("ประเภท:"), typeBox,
                new JLabel("หมวดหมู่:"), catBox,
                new JLabel("ชื่อรายการ:"), nameField,
                new JLabel("จำนวนเงิน:"), amountField,
                new JLabel("วันที่:"), datePicker);

        JButton ok = Ui.primary("บันทึก");
        JButton cancel = Ui.primary("ยกเลิก");
        ok.addActionListener(e -> onOk());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
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
        Object keep = catBox.getSelectedItem();
        catBox.removeAllItems();
        boolean income = Transaction.INCOME.equals(typeBox.getSelectedItem());
        for (String c : income ? Transaction.INCOME_CATEGORIES : Transaction.EXPENSE_CATEGORIES) catBox.addItem(c);
        if (keep != null) catBox.setSelectedItem(keep);
    }

    private void onOk() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { error("กรุณากรอกชื่อรายการ"); return; }
        Double amount = Ui.parseAmount(amountField.getText());
        if (amount == null) { error("จำนวนเงินต้องเป็นตัวเลขที่มากกว่า 0"); return; }
        LocalDate date = datePicker.getDate();
        if (date == null) { error("กรุณาเลือกวันที่"); return; }

        result = new Transaction((String) typeBox.getSelectedItem(), (String) catBox.getSelectedItem(), name, amount, date);
        dispose();
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
    }
}
