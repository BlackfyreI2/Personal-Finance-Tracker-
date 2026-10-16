package src;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * RecurringDialog = หน้าต่างจัดการ "รายการประจำ" (รองรับ Dark Theme สมบูรณ์)
 */
public class Recurringdialog extends JDialog {
    private final List<Recurring> list;
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final DecimalFormat plain = new DecimalFormat("0.##");

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

    public static void show(Window owner, List<Recurring> list) {
        new Recurringdialog(owner, list).setVisible(true);
    }

    private Recurringdialog(Window owner, List<Recurring> list) {
        super(owner, "รายการประจำ", ModalityType.APPLICATION_MODAL);
        this.list = list;

        getContentPane().setBackground(Ui.BG_DARK);

        fillCategories(typeBox, catBox);
        typeBox.addActionListener(e -> fillCategories(typeBox, catBox));

        JPanel formInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        formInner.setOpaque(false);
        
        JLabel l1 = new JLabel("ประเภท:"); l1.setForeground(Ui.TEXT_LIGHT);
        JLabel l2 = new JLabel("หมวดหมู่:"); l2.setForeground(Ui.TEXT_LIGHT);
        JLabel l3 = new JLabel("ชื่อ:"); l3.setForeground(Ui.TEXT_LIGHT);
        JLabel l4 = new JLabel("จำนวนเงิน:"); l4.setForeground(Ui.TEXT_LIGHT);
        JLabel l5 = new JLabel("ทุกวันที่:"); l5.setForeground(Ui.TEXT_LIGHT);

        formInner.add(l1);     formInner.add(typeBox);
        formInner.add(l2);   formInner.add(catBox);
        formInner.add(l3);       formInner.add(nameField);
        formInner.add(l4);  formInner.add(amountField);
        formInner.add(l5);  formInner.add(dayBox);
        
        JButton addBtn = Ui.primary("เพิ่ม");
        addBtn.addActionListener(e -> onAdd());
        formInner.add(addBtn);
        JPanel form = Ui.titled("เพิ่มรายการประจำ", formInner);

        Ui.styleTable(table);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onEdit();
            }
        });
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(800, 240));
        scroll.getViewport().setBackground(Ui.BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER_COLOR));

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Ui.BG_DARK);
        center.setBorder(new EmptyBorder(0, 8, 0, 8));
        center.add(Ui.hint("ดับเบิลคลิกหรือกด \"แก้ไข\" เพื่อเปลี่ยนรายละเอียด "
                + "โปรแกรมจะเพิ่มรายการเข้าประวัติเมื่อถึงวันที่กำหนด (รวมเดือนที่ไม่ได้เปิดโปรแกรม)"), BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        JButton editBtn = Ui.secondary("แก้ไข");
        JButton delBtn = Ui.secondary("ลบที่เลือก");
        JButton closeBtn = Ui.secondary("ปิด");
        editBtn.addActionListener(e -> onEdit());
        delBtn.addActionListener(e -> onDelete());
        closeBtn.addActionListener(e -> dispose());
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setBackground(Ui.BG_DARK);
        buttons.add(editBtn);
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

    private static void fillCategories(JComboBox<String> typeBox, JComboBox<String> catBox) {
        Object keep = catBox.getSelectedItem();
        catBox.removeAllItems();
        for (String c : Transaction.categoriesFor((String) typeBox.getSelectedItem())) catBox.addItem(c);
        if (keep != null) catBox.setSelectedItem(keep);
    }

    private void reloadTable() {
        model.setRowCount(0);
        for (Recurring r : list) {
            model.addRow(new Object[]{r.getType(), r.getCategory(), r.getName(),
                    money.format(r.getAmount()), r.getDay(), r.getStartMonth()});
        }
    }

    private Recurring selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : list.get(row);
    }

    private void onAdd() {
        String name = nameField.getText().trim();
        Double amount = Ui.parseAmount(amountField.getText());
        if (name.isEmpty()) { error("กรุณากรอกชื่อรายการ"); return; }
        if (amount == null) { error("จำนวนเงินต้องเป็นตัวเลขที่มากกว่า 0"); return; }

        list.add(Recurring.create((String) typeBox.getSelectedItem(), (String) catBox.getSelectedItem(),
                name, amount, (Integer) dayBox.getValue(), LocalDate.now()));
        Filemanager.saveRecurring(list);
        nameField.setText("");
        amountField.setText("");
        reloadTable();
    }

    private void onEdit() {
        Recurring r = selected();
        if (r == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกรายการที่ต้องการแก้ไขก่อน"); return; }

        JComboBox<String> type = new JComboBox<>(new String[]{Transaction.EXPENSE, Transaction.INCOME});
        type.setSelectedItem(r.getType());
        JComboBox<String> cat = new JComboBox<>();
        fillCategories(type, cat);
        cat.setSelectedItem(r.getCategory());
        type.addActionListener(e -> fillCategories(type, cat));

        JTextField name = new JTextField(r.getName(), 16);
        name.setBackground(Ui.BG_DARK);
        name.setForeground(Ui.TEXT_LIGHT);
        name.setCaretColor(Color.WHITE);

        JTextField amount = new JTextField(plain.format(r.getAmount()), 10);
        amount.setBackground(Ui.BG_DARK);
        amount.setForeground(Ui.TEXT_LIGHT);
        amount.setCaretColor(Color.WHITE);

        JSpinner day = new JSpinner(new SpinnerNumberModel(r.getDay(), 1, 31, 1));

        JLabel l1 = new JLabel("ประเภท:"); l1.setForeground(Ui.TEXT_LIGHT);
        JLabel l2 = new JLabel("หมวดหมู่:"); l2.setForeground(Ui.TEXT_LIGHT);
        JLabel l3 = new JLabel("ชื่อรายการ:"); l3.setForeground(Ui.TEXT_LIGHT);
        JLabel l4 = new JLabel("จำนวนเงิน:"); l4.setForeground(Ui.TEXT_LIGHT);
        JLabel l5 = new JLabel("ทุกวันที่:"); l5.setForeground(Ui.TEXT_LIGHT);

        JPanel form = Ui.formGrid(5, l1, type, l2, cat, l3, name, l4, amount, l5, day);
        form.setOpaque(true);
        form.setBackground(Ui.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        int ok = JOptionPane.showConfirmDialog(this, form, "แก้ไขรายการประจำ",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        String n = name.getText().trim();
        Double v = Ui.parseAmount(amount.getText());
        if (n.isEmpty()) { error("กรุณากรอกชื่อรายการ"); return; }
        if (v == null) { error("จำนวนเงินต้องเป็นตัวเลขที่มากกว่า 0"); return; }

        int i = list.indexOf(r);
        list.set(i, r.withDetails((String) type.getSelectedItem(), (String) cat.getSelectedItem(),
                n, v, (Integer) day.getValue()));
        Filemanager.saveRecurring(list);
        reloadTable();
    }

    private void onDelete() {
        Recurring r = selected();
        if (r == null) { JOptionPane.showMessageDialog(this, "กรุณาเลือกรายการที่ต้องการลบก่อน"); return; }
        int ok = JOptionPane.showConfirmDialog(this, "ลบรายการประจำ \"" + r.getName() + "\" ใช่หรือไม่?",
                "ยืนยันการลบ", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            list.remove(r);
            Filemanager.saveRecurring(list);
            reloadTable();
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
    }
}
