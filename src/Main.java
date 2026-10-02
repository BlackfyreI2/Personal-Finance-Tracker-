package src;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Main = หน้าจอ GUI (Frontend) + จุดเริ่มต้นโปรแกรม
 */
public class Main extends JFrame {

    private static final String ALL = "ทั้งหมด";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // โหมดมืดหรือไม่ (static เพราะใช้ร่วมกันทั้งโปรแกรม)
    private static boolean dark = false;
    
    // โทนสีใหม่: นุ่มนวล ดูเป็นแอปพลิเคชันการเงินสมัยใหม่
    private static Color incomeColor()  { return dark ? new Color(80, 220, 120) : new Color(34, 139, 34); } 
    private static Color expenseColor() { return dark ? new Color(255, 100, 100) : new Color(220, 53, 69); } 
    private static Color balanceColor() { return dark ? new Color(100, 160, 255) : new Color(0, 102, 204); } 
    private static Color warningColor() { return dark ? new Color(250, 179, 135) : new Color(243, 156, 18); } 

    // ---------- ข้อมูล ----------
    private List<Transaction> transactions;
    private List<Recurring> recurring;
    private Map<String, Double> categoryBudgets;
    private double budget;
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final DecimalFormat money0 = new DecimalFormat("#,##0");

    // ---------- ฟอร์มเพิ่มรายการ ----------
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{Transaction.INCOME, Transaction.EXPENSE});
    private final JComboBox<String> catBox = new JComboBox<>();
    private final JTextField nameField = new JTextField(14);
    private final JTextField amountField = new JTextField(8);

    // ---------- ตัวกรอง/ค้นหา ----------
    private final JComboBox<String> filterType = new JComboBox<>(new String[]{ALL, Transaction.INCOME, Transaction.EXPENSE});
    private final JComboBox<String> filterMonth = new JComboBox<>();
    private final JTextField searchField = new JTextField(12);
    private boolean updatingFilters = false;

    // ---------- ตาราง ----------
    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[]{"วันที่", "ประเภท", "หมวดหมู่", "ชื่อรายการ", "จำนวนเงิน (บาท)"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
                @Override public Class<?> getColumnClass(int c) {
                    if (c == 0) return LocalDate.class;
                    if (c == 4) return Double.class;
                    return String.class;
                }
            };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);

    // ---------- สรุปยอด ----------
    private final JLabel incomeLabel = new JLabel();
    private final JLabel expenseLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
    private final JLabel countLabel = new JLabel();
    private final JLabel budgetLabel = new JLabel("งบเดือนนี้:");
    private final Charts.MeterBar budgetBar = new Charts.MeterBar();

    // ---------- สถิติ ----------
    private final Charts.PieChart pieChart = new Charts.PieChart();
    private final Charts.BarChart barChart = new Charts.BarChart();
    private final JLabel insightLabel = new JLabel();
    private final JPanel catBudgetPanel = new JPanel(new GridLayout(0, 1, 0, 4));

    // ---------- ปุ่มธีม ----------
    private final JCheckBox darkBox = new JCheckBox("โหมดมืด");

    public Main() {
        super("โปรแกรมบันทึกรายรับ-รายจ่าย");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 740);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // อ่านข้อมูลจากไฟล์ตอนเปิดโปรแกรม
        transactions = Filemanager.load();
        recurring = Filemanager.loadRecurring();
        categoryBudgets = Filemanager.loadCategoryBudgets();
        budget = Filemanager.loadBudget();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("รายการ", buildListTab());
        tabs.addTab("สถิติ", buildStatsTab());
        add(buildHeader(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        // สร้างรายการประจำที่ถึงกำหนด
        int added = applyRecurring();
        refreshAll();
        if (added > 0) {
            SwingUtilities.invokeLater(() -> showInfo("เพิ่มรายการประจำอัตโนมัติ " + added + " รายการ"));
        }
    }

    // =====================================================
    //                    สร้างหน้าจอ
    // =====================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton recurringBtn = new JButton("รายการประจำ");
        JButton catBudgetBtn = new JButton("งบตามหมวด");
        recurringBtn.addActionListener(e -> openRecurring());
        catBudgetBtn.addActionListener(e -> openCategoryBudgets());

        darkBox.setSelected(dark);
        darkBox.addActionListener(e -> switchTheme(darkBox.isSelected()));
        header.add(recurringBtn);
        header.add(catBudgetBtn);
        header.add(darkBox);
        return header;
    }

    private JPanel buildListTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        // --- ฟอร์มเพิ่มรายการ ---
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("เพิ่มรายการ (วันที่ใช้วันนี้อัตโนมัติ)"));
        fillCategories(catBox, typeBox);
        typeBox.addActionListener(e -> fillCategories(catBox, typeBox));
        catBox.setPrototypeDisplayValue("บิล/สาธารณูปโภค");
        filterType.setPrototypeDisplayValue("ทั้งหมด  ");
        JButton addBtn = new JButton("เพิ่ม");
        addBtn.addActionListener(e -> addTransaction());
        amountField.addActionListener(e -> addTransaction());
        form.add(new JLabel("ประเภท:"));     form.add(typeBox);
        form.add(new JLabel("หมวดหมู่:"));   form.add(catBox);
        form.add(new JLabel("ชื่อรายการ:")); form.add(nameField);
        form.add(new JLabel("จำนวนเงิน:"));  form.add(amountField);
        form.add(addBtn);

        // --- ตัวกรอง/ค้นหา ---
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filter.setBorder(BorderFactory.createTitledBorder("ค้นหา / กรอง"));
        filter.add(new JLabel("ค้นหา:"));  filter.add(searchField);
        filter.add(new JLabel("ประเภท:")); filter.add(filterType);
        filter.add(new JLabel("เดือน:"));  filter.add(filterMonth);
        JButton clearBtn = new JButton("ล้างตัวกรอง");
        clearBtn.addActionListener(e -> clearFilters());
        filter.add(clearBtn);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });
        filterType.addActionListener(e -> applyFilter());
        filterMonth.addActionListener(e -> applyFilter());

        JPanel north = new JPanel(new GridLayout(2, 1, 0, 4));
        north.add(form);
        north.add(filter);
        tab.add(north, BorderLayout.NORTH);

        // --- ตาราง ---
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(260);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        installRenderers();
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });
        tab.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- ปุ่มด้านล่างตาราง ---
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        JButton editBtn = new JButton("แก้ไขที่เลือก");
        JButton delBtn = new JButton("ลบที่เลือก");
        JButton importBtn = new JButton("นำเข้า CSV");
        JButton exportBtn = new JButton("ส่งออก CSV");
        editBtn.addActionListener(e -> editSelected());
        delBtn.addActionListener(e -> deleteSelected());
        importBtn.addActionListener(e -> importCsv());
        exportBtn.addActionListener(e -> exportCsv());
        actions.add(new JLabel("เคล็ดลับ: คลิกหัวตารางเพื่อเรียงลำดับ / ดับเบิลคลิกที่แถวเพื่อแก้ไข"));
        actions.add(editBtn);
        actions.add(delBtn);
        actions.add(importBtn);
        actions.add(exportBtn);
        tab.add(actions, BorderLayout.SOUTH);
        return tab;
    }

    private void installRenderers() {
        RowRenderer renderer = new RowRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Double.class, renderer);
    }

    void switchTheme(boolean darkMode) {
        Filemanager.saveDarkMode(darkMode);
        applyTheme(darkMode);
        installRenderers();
        darkBox.setSelected(darkMode);
        updateSummary();
        updateStats();
        table.repaint();
    }

    private JPanel buildStatsTab() {
        JPanel tab = new JPanel(new BorderLayout(8, 8));
        tab.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        pieChart.setBorder(BorderFactory.createTitledBorder("สัดส่วนรายจ่ายตามหมวดหมู่ (ตามตัวกรอง)"));
        barChart.setBorder(BorderFactory.createTitledBorder("รายรับ-รายจ่ายย้อนหลัง 6 เดือน"));
        JPanel charts = new JPanel(new GridLayout(1, 2, 8, 0));
        charts.add(pieChart);
        charts.add(barChart);

        insightLabel.setBorder(BorderFactory.createTitledBorder("ข้อมูลเชิงลึก (ตามตัวกรอง)"));
        insightLabel.setVerticalAlignment(SwingConstants.TOP);

        JScrollPane catScroll = new JScrollPane(catBudgetPanel);
        catScroll.setBorder(BorderFactory.createTitledBorder("งบตามหมวดหมู่ (เดือนนี้)"));

        JPanel south = new JPanel(new GridLayout(1, 2, 8, 0));
        south.setPreferredSize(new Dimension(100, 190));
        south.add(insightLabel);
        south.add(catScroll);

        tab.add(charts, BorderLayout.CENTER);
        tab.add(south, BorderLayout.SOUTH);
        return tab;
    }

    private JPanel buildBottomPanel() {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 6));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 8, 8, 8),
                BorderFactory.createTitledBorder("สรุปยอด (Real-time ตามตัวกรองที่เลือก)")));

        Font big = new Font("Tahoma", Font.BOLD, 14);
        JPanel row1 = new JPanel(new GridLayout(1, 4, 10, 0));
        for (JLabel l : new JLabel[]{incomeLabel, expenseLabel, balanceLabel, countLabel}) {
            l.setFont(big);
            row1.add(l);
        }

        JButton budgetBtn = new JButton("ตั้งงบรวมรายเดือน");
        budgetBtn.addActionListener(e -> setBudget());
        JPanel row2 = new JPanel(new BorderLayout(8, 0));
        row2.add(budgetLabel, BorderLayout.WEST);
        row2.add(budgetBar, BorderLayout.CENTER);
        row2.add(budgetBtn, BorderLayout.EAST);

        p.add(row1);
        p.add(row2);
        return p;
    }

    private class RowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            if (value instanceof Double) value = money.format((Double) value);
            else if (value instanceof LocalDate) value = ((LocalDate) value).format(DATE_FMT);

            Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
            setHorizontalAlignment(col == 4 ? RIGHT : LEFT);
            if (selected) {
                c.setForeground(t.getSelectionForeground());
            } else {
                boolean income = Transaction.INCOME.equals(t.getValueAt(row, 1));
                c.setForeground(income ? incomeColor() : expenseColor());
            }
            return c;
        }
    }

    // =====================================================
    //                    การทำงานหลัก
    // =====================================================

    private static void fillCategories(JComboBox<String> catBox, JComboBox<String> typeBox) {
        catBox.removeAllItems();
        for (String c : Transaction.categoriesFor((String) typeBox.getSelectedItem())) catBox.addItem(c);
    }

    private void addTransaction() {
        String name = nameField.getText().trim();
        String amountText = amountField.getText().trim().replace(",", "");
        if (name.isEmpty() || amountText.isEmpty()) {
            showError("กรุณากรอกชื่อรายการและจำนวนเงิน");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException ex) {
            showError("จำนวนเงินต้องเป็นตัวเลข");
            return;
        }
        if (amount <= 0) {
            showError("จำนวนเงินต้องมากกว่า 0");
            return;
        }

        String type = (String) typeBox.getSelectedItem();
        String category = (String) catBox.getSelectedItem();
        transactions.add(new Transaction(type, category, name, amount, LocalDate.now()));
        Filemanager.save(transactions);

        nameField.setText("");
        amountField.setText("");
        nameField.requestFocus();
        refreshAll();

        if (type.equals(Transaction.EXPENSE)) checkBudgetAlerts(category, amount);
    }

    private void checkBudgetAlerts(String category, double justAdded) {
        YearMonth now = YearMonth.now();
        if (budget > 0) {
            double after = Stats.monthTotal(transactions, now, false);
            alertIfCrossed("งบรวมเดือนนี้", after - justAdded, after, budget);
        }
        double limit = categoryBudgets.getOrDefault(category, 0.0);
        if (limit > 0) {
            double after = Stats.monthCategoryExpense(transactions, now, category);
            alertIfCrossed("งบหมวด \"" + category + "\"", after - justAdded, after, limit);
        }
    }

    private void alertIfCrossed(String title, double before, double after, double limit) {
        if (before <= limit && after > limit) {
            JOptionPane.showMessageDialog(this,
                    title + " เกินแล้ว!\nใช้ไป " + money.format(after) + " จากงบ " + money.format(limit),
                    "เตือนงบประมาณ", JOptionPane.WARNING_MESSAGE);
        } else if (before < limit * 0.8 && after >= limit * 0.8 && after <= limit) {
            JOptionPane.showMessageDialog(this,
                    title + " ใช้ไปเกิน 80% แล้ว\nใช้ไป " + money.format(after) + " จากงบ " + money.format(limit),
                    "เตือนงบประมาณ", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private int selectedIndex() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    private void editSelected() {
        int idx = selectedIndex();
        if (idx < 0) { showInfo("กรุณาเลือกรายการที่ต้องการแก้ไขก่อน"); return; }
        Transaction edited = Transactiondialog.show(this, transactions.get(idx));
        if (edited != null) {
            transactions.set(idx, edited);
            Filemanager.save(transactions);
            refreshAll();
        }
    }

    private void deleteSelected() {
        int idx = selectedIndex();
        if (idx < 0) { showInfo("กรุณาเลือกรายการที่ต้องการลบก่อน"); return; }
        int ok = JOptionPane.showConfirmDialog(this, "ต้องการลบรายการนี้ใช่หรือไม่?",
                "ยืนยันการลบ", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            transactions.remove(idx);
            Filemanager.save(transactions);
            refreshAll();
        }
    }

    private void setBudget() {
        String input = JOptionPane.showInputDialog(this,
                "ตั้งงบรายจ่ายรวมต่อเดือน (บาท)\nใส่ 0 เพื่อปิดการใช้งบ", budget > 0 ? String.valueOf(budget) : "");
        if (input == null) return;
        try {
            double v = Double.parseDouble(input.trim().replace(",", ""));
            if (v < 0) throw new NumberFormatException();
            budget = v;
            Filemanager.saveBudget(budget);
            updateSummary();
        } catch (NumberFormatException e) {
            showError("กรุณากรอกตัวเลขที่ไม่ติดลบ");
        }
    }

    private void openRecurring() {
        Recurringdialog.show(this, recurring);
        int added = applyRecurring();
        if (added > 0) {
            refreshAll();
            showInfo("เพิ่มรายการประจำอัตโนมัติ " + added + " รายการ");
        }
    }

    private int applyRecurring() {
        int count = 0;
        LocalDate today = LocalDate.now();
        for (Recurring r : recurring) {
            List<Transaction> due = r.applyDue(today);
            transactions.addAll(due);
            count += due.size();
        }
        if (count > 0) {
            Filemanager.save(transactions);
            Filemanager.saveRecurring(recurring);
        }
        return count;
    }

    private void openCategoryBudgets() {
        Map<String, Double> updated = Categorybudgetdialog.show(this, categoryBudgets, transactions);
        if (updated != null) {
            categoryBudgets = updated;
            Filemanager.saveCategoryBudgets(categoryBudgets);
            updateStats();
        }
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("ไฟล์ CSV", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Filemanager.ImportResult result;
        try {
            result = Filemanager.importCsv(chooser.getSelectedFile());
        } catch (IOException e) {
            showError("อ่านไฟล์ไม่สำเร็จ: " + e.getMessage());
            return;
        }

        Set<String> existing = new HashSet<>();
        for (Transaction t : transactions) existing.add(t.toFileLine());
        List<Transaction> fresh = new ArrayList<>();
        int duplicates = 0;
        for (Transaction t : result.items) {
            if (existing.contains(t.toFileLine())) duplicates++;
            else fresh.add(t);
        }

        String summary = "อ่านได้ " + result.items.size() + " รายการ\n"
                + "ซ้ำกับที่มีอยู่ (จะข้าม): " + duplicates + "\n"
                + "ข้อมูลไม่ถูกต้อง (จะข้าม): " + result.invalid;
        if (fresh.isEmpty()) {
            showInfo(summary + "\n\nไม่มีรายการใหม่ให้นำเข้า");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, summary + "\n\nนำเข้า " + fresh.size() + " รายการใหม่เลยหรือไม่?",
                "ยืนยันการนำเข้า", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            transactions.addAll(fresh);
            Filemanager.save(transactions);
            refreshAll();
            showInfo("นำเข้าเรียบร้อย " + fresh.size() + " รายการ");
        }
    }

    private void exportCsv() {
        List<Transaction> v = visible();
        if (v.isEmpty()) { showInfo("ไม่มีข้อมูลให้ส่งออก"); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("รายรับรายจ่าย.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getPath() + ".csv");
            if (Filemanager.exportCsv(v, f)) showInfo("ส่งออก " + v.size() + " รายการเรียบร้อย\n" + f.getAbsolutePath());
            else showError("ส่งออกไม่สำเร็จ");
        }
    }

    private void refreshAll() {
        tableModel.setRowCount(0);
        for (Transaction t : transactions) {
            tableModel.addRow(new Object[]{t.getDate(), t.getType(), t.getCategory(), t.getName(), t.getAmount()});
        }
        rebuildMonthFilter();
        applyFilter();
    }

    private void rebuildMonthFilter() {
        Object selected = filterMonth.getSelectedItem();
        TreeSet<String> months = new TreeSet<>(Collections.reverseOrder());
        for (Transaction t : transactions) months.add(YearMonth.from(t.getDate()).toString());

        updatingFilters = true;
        filterMonth.removeAllItems();
        filterMonth.addItem(ALL);
        for (String m : months) filterMonth.addItem(m);
        filterMonth.setSelectedItem(selected != null && (selected.equals(ALL) || months.contains(selected)) ? selected : ALL);
        updatingFilters = false;
    }

    private void clearFilters() {
        updatingFilters = true;
        searchField.setText("");
        filterType.setSelectedItem(ALL);
        filterMonth.setSelectedItem(ALL);
        updatingFilters = false;
        applyFilter();
    }

    private void applyFilter() {
        if (updatingFilters) return;
        final String type = (String) filterType.getSelectedItem();
        final String month = (String) filterMonth.getSelectedItem();
        final String query = searchField.getText().trim().toLowerCase();

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                if (!ALL.equals(type) && !type.equals(e.getStringValue(1))) return false;
                if (month != null && !ALL.equals(month) && !e.getValue(0).toString().startsWith(month)) return false;
                if (!query.isEmpty()) {
                    String text = (e.getStringValue(2) + " " + e.getStringValue(3)).toLowerCase();
                    if (!text.contains(query)) return false;
                }
                return true;
            }
        });
        updateSummary();
        updateStats();
    }

    private List<Transaction> visible() {
        List<Transaction> v = new ArrayList<>();
        for (int i = 0; i < table.getRowCount(); i++) {
            v.add(transactions.get(table.convertRowIndexToModel(i)));
        }
        return v;
    }

    private void updateSummary() {
        List<Transaction> v = visible();
        double income = Stats.sum(v, true);
        double expense = Stats.sum(v, false);
        double balance = income - expense;
        incomeLabel.setText("รายรับรวม: " + money.format(income));
        expenseLabel.setText("รายจ่ายรวม: " + money.format(expense));
        balanceLabel.setText("คงเหลือ: " + money.format(balance));
        incomeLabel.setForeground(incomeColor());
        expenseLabel.setForeground(expenseColor());
        balanceLabel.setForeground(balance >= 0 ? balanceColor() : expenseColor());
        countLabel.setText("จำนวน: " + v.size() + " รายการ");

        double monthExpense = Stats.monthTotal(transactions, YearMonth.now(), false);
        if (budget > 0) {
            int pct = (int) Math.round(monthExpense / budget * 100);
            budgetBar.set(pct, money.format(monthExpense) + " / " + money.format(budget) + "  (" + pct + "%)", meterColor(pct));
        } else {
            budgetBar.set(0, "ยังไม่ได้ตั้งงบรวม", incomeColor());
        }
    }

    private Color meterColor(int pct) {
        return pct >= 100 ? expenseColor() : pct >= 80 ? warningColor() : incomeColor();
    }

    private void updateStats() {
        List<Transaction> v = visible();
        pieChart.setData(Stats.expenseByCategory(v));

        List<String> labels = new ArrayList<>();
        double[] inc = new double[6];
        double[] exp = new double[6];
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 6; i++) {
            YearMonth ym = now.minusMonths(5 - i);
            labels.add(ym.toString());
            inc[i] = Stats.monthTotal(transactions, ym, true);
            exp[i] = Stats.monthTotal(transactions, ym, false);
        }
        barChart.setData(labels, inc, exp);

        catBudgetPanel.removeAll();
        for (String cat : Transaction.EXPENSE_CATEGORIES) {
            double limit = categoryBudgets.getOrDefault(cat, 0.0);
            if (limit <= 0) continue;
            double spent = Stats.monthCategoryExpense(transactions, now, cat);
            int pct = (int) Math.round(spent / limit * 100);
            Charts.MeterBar bar = new Charts.MeterBar();
            bar.set(pct, cat + ": " + money0.format(spent) + " / " + money0.format(limit) + "  (" + pct + "%)", meterColor(pct));
            catBudgetPanel.add(bar);
        }
        if (catBudgetPanel.getComponentCount() == 0) {
            catBudgetPanel.add(new JLabel("ยังไม่ได้ตั้งงบตามหมวด (กดปุ่ม \"งบตามหมวด\" ด้านบน)"));
        }
        catBudgetPanel.revalidate();
        catBudgetPanel.repaint();

        double income = Stats.sum(v, true);
        double expense = Stats.sum(v, false);
        StringBuilder sb = new StringBuilder("<html><div style='padding:4px'>");
        if (v.isEmpty()) {
            sb.append("ยังไม่มีข้อมูล");
        } else {
            Transaction big = Stats.biggestExpense(v);
            Map<String, Double> cats = Stats.expenseByCategory(v);
            sb.append("• รายจ่ายเฉลี่ยต่อวัน: <b>").append(money.format(Stats.avgExpensePerDay(v))).append("</b> บาท<br>");
            if (big != null) {
                sb.append("• รายจ่ายสูงสุด: <b>").append(big.getName()).append("</b> ")
                  .append(money.format(big.getAmount())).append(" บาท (").append(big.getDate().format(DATE_FMT)).append(")<br>");
            }
            if (!cats.isEmpty()) {
                Map.Entry<String, Double> top = cats.entrySet().iterator().next();
                sb.append("• หมวดที่ใช้จ่ายมากที่สุด: <b>").append(top.getKey()).append("</b> ")
                  .append(money.format(top.getValue())).append(" บาท<br>");
            }
            if (income > 0) {
                double rate = (income - expense) / income * 100;
                sb.append("• อัตราการออม: <b>").append(String.format("%.1f", rate)).append("%</b> ของรายรับ");
                if (rate < 0) sb.append(" (ใช้จ่ายมากกว่ารายรับ!)");
            } else {
                sb.append("• อัตราการออม: ยังไม่มีรายรับ");
            }
        }
        sb.append("</div></html>");
        insightLabel.setText(sb.toString());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "ข้อมูลไม่ถูกต้อง", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    // =====================================================
    //             ธีม (สว่าง/มืด) และจุดเริ่มต้นโปรแกรม
    // =====================================================

    /** เปลี่ยนธีมของทั้งโปรแกรม */
    public static void applyTheme(boolean darkMode) {
        dark = darkMode;
        Charts.dark = darkMode; 
        try {
            UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
            
            // 1. บังคับเปลี่ยนฟอนต์ทุกชิ้นส่วนด้วยการวนลูปทับ 100%
            FontUIResource myFont = new FontUIResource("Tahoma", Font.PLAIN, 14);
            UIManager.put("defaultFont", myFont);
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();
            defaults.put("defaultFont", myFont);
            
            // วนลูปตรวจสอบทุก Key ที่อยู่ในระบบ หากอันไหนเป็น Font ให้ทับด้วย myFont ทันที
            for (Enumeration<Object> keys = defaults.keys(); keys.hasMoreElements();) {
                Object key = keys.nextElement();
                if (defaults.get(key) instanceof Font) {
                    defaults.put(key, myFont);
                }
            }
            for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements();) {
                Object key = keys.nextElement();
                if (UIManager.get(key) instanceof Font) {
                    UIManager.put(key, myFont);
                }
            }
            
            // 2. ตั้งค่าชุดสีใหม่ให้เป็นสไตล์ Clean & Minimal
            Color bgColor = darkMode ? new Color(30, 30, 30) : new Color(245, 245, 250); 
            Color panelColor = darkMode ? new Color(45, 45, 45) : Color.WHITE; 
            Color borderColor = darkMode ? new Color(70, 70, 70) : new Color(210, 215, 220); 
            Color textColor = darkMode ? new Color(230, 230, 230) : new Color(40, 40, 40); 
            Color selectBgColor = darkMode ? new Color(75, 110, 175) : new Color(108, 142, 231); 
            
            defaults.put("control", bgColor);
            defaults.put("nimbusLightBackground", panelColor);
            defaults.put("info", panelColor);
            defaults.put("nimbusBase", borderColor);
            defaults.put("text", textColor);
            defaults.put("nimbusSelectionBackground", selectBgColor);
            defaults.put("nimbusSelectedText", Color.WHITE);
            defaults.put("nimbusFocus", selectBgColor);
            
        } catch (Exception ignored) { }
        
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
        }
    }

    public static void main(String[] args) {
        applyTheme(Filemanager.loadDarkMode()); 
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}