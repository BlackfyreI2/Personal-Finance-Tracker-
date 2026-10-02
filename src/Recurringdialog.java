package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * RecurringDialog = หน้าต่างจัดการ "รายการประจำ" (เพิ่ม/ลบ)
 */
public class Recurringdialog extends JDialog {
    private final List<Recurring> list;
    private final DecimalFormat money = new DecimalFormat("#,##0.00");

    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{Transaction.EXPENSE, Transaction.INCOME});
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JTextField nameField = new JTextField(12);
    private final JTextField amountField = new JTextField(8);
    private final JSpinner dayBox = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ประเภท", "หมวดหมู่", "ชื่อรายการ", "จำนวนเงิน", "ทุกวันที่", "เริ่มเดือน"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    /** เปิดหน้าต่างและรอจนปิด (list จะถูกแก้ไขและบันทึกลงไฟล์ให้เอง) */
    public static void show(Window owner, List<Recurring> list) {
        new Recurringdialog(owner, list).setVisible(true);
    }

    private Recurringdialog(Window owner, List<Recurring> list) {
        super(owner, "รายการประจำ", ModalityType.APPLICATION_MODAL);
        this.list = list;

        fillCategories();
        typeBox.addActionListener(e -> fillCategories());

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("เพิ่มรายการประจำ"));
        form.add(new JLabel("ประเภท:"));     form.add(typeBox);
        form.add(new JLabel("หมวดหมู่:"));   form.add(catBox);
        form.add(new JLabel("ชื่อ:"));       form.add(nameField);
        form.add(new JLabel("จำนวนเงิน:"));  form.add(amountField);
        form.add(new JLabel("ทุกวันที่:"));  form.add(dayBox);
        JButton addBtn = new JButton("เพิ่ม");
        addBtn.addActionListener(e -> onAdd());
        form.add(addBtn);

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(760, 220));

        JLabel hint = new JLabel("<html><body style='width:700px'>โปรแกรมจะเพิ่มรายการเข้าประวัติอัตโนมัติเมื่อถึงวันที่กำหนด (ตรวจทุกครั้งที่เปิดโปรแกรม "
                + "ถ้าไม่ได้เปิดหลายเดือนจะเพิ่มย้อนหลังให้ครบ) ถ้าตั้งวันที่ 29-31 แล้วเดือนนั้นมีวันไม่ถึง จะใช้วันสุดท้ายของเดือน</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JButton delBtn = new JButton("ลบที่เลือก");
        JButton closeBtn = new JButton("ปิด");
        delBtn.addActionListener(e -> onDelete());
        closeBtn.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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

    private void fillCategories() {
        catBox.removeAllItems();
        for (String c : Transaction.categoriesFor((String) typeBox.getSelectedItem())) catBox.addItem(c);
    }

    private void reloadTable() {
        model.setRowCount(0);
        for (Recurring r : list) {
            model.addRow(new Object[]{r.getType(), r.getCategory(), r.getName(),
                    money.format(r.getAmount()), r.getDay(), r.getStartMonth()});
        }
    }

    private void onAdd() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { error("กรุณากรอกชื่อรายการ"); return; }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) { error("จำนวนเงินต้องเป็นตัวเลข"); return; }
        if (amount <= 0) { error("จำนวนเงินต้องมากกว่า 0"); return; }

        list.add(Recurring.create((String) typeBox.getSelectedItem(), (String) catBox.getSelectedItem(),
                name, amount, (Integer) dayBox.getValue(), LocalDate.now()));
        Filemanager.saveRecurring(list);
        nameField.setText("");
        amountField.setText("");
        reloadTable();
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "กรุณาเลือกรายการที่ต้องการลบก่อน"); return; }
        list.remove(row);
        Filemanager.saveRecurring(list);
        reloadTable();
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
    }
}