package src;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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
import java.util.*;
import java.util.List;

public class Main extends JFrame {

    private static final String ALL = "ทั้งหมด";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] TH_MONTHS = {
            "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
            "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    };

    // ข้อมูลระบบ
    private List<Transaction> transactions;
    private List<Recurring> recurring;
    private Map<String, Double> categoryBudgets;
    private double budget;
    private List<Bill> bills;
    private List<Goal> goals;
    private final DecimalFormat money = new DecimalFormat("#,##0.00");

    // ระบบความปลอดภัยและการเตือน
    private final Set<String> notifiedBills = new HashSet<>();
    private boolean passwordEnabled;
    private int autoLockMinutes;
    private long lastActivity = System.currentTimeMillis();
    private boolean locked = false;

    // UI Layout
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContent = new JPanel(cardLayout);
    private JButton activeMenuBtn = null;

    // Dashboard Cards
    private final JLabel incomeLabel = new JLabel();
    private final JLabel expenseLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
    private final JLabel countLabel = new JLabel();
    private final Charts.PieChart pieChart = new Charts.PieChart();
    private final Charts.BarChart barChart = new Charts.BarChart();

    // Transactions View
    private final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"วันที่", "ประเภท", "หมวดหมู่", "ชื่อรายการ", "จำนวนเงิน (บาท)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) {
            if (c == 0) return LocalDate.class;
            if (c == 4) return Double.class;
            return String.class;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    
    // เก็บ Mapping ระหว่างชื่อเดือนกับค่ารหัส "YYYY-MM"
    private final Map<String, String> monthKeyMap = new LinkedHashMap<>();
    private final JComboBox<String> filterMonth = new JComboBox<>();

    public Main() {
        super("Finance Tracker - โปรแกรมบันทึกรายรับ-รายจ่าย");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 720));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Ui.BG_DARK);

        transactions = Filemanager.load();
        recurring = Filemanager.loadRecurring();
        categoryBudgets = Filemanager.loadCategoryBudgets();
        budget = Filemanager.loadBudget();
        bills = Filemanager.loadBills();
        goals = Filemanager.loadGoals();

        add(buildSidebar(), BorderLayout.WEST);
        
        mainContent.setBackground(Ui.BG_DARK);
        mainContent.add(buildDashboard(), "Dashboard");
        mainContent.add(buildTransactionsView(), "Transactions");
        add(mainContent, BorderLayout.CENTER);

        Charts.dark = true; 
        applyRecurring();
        refreshAll();
        startServices();
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(20, 20, 22));
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));

        JLabel appTitle = new JLabel("Finance Tracker");
        appTitle.setForeground(Ui.ACCENT_BLUE);
        appTitle.setFont(appTitle.getFont().deriveFont(Font.BOLD, 20f));
        appTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("บันทึกการเงินส่วนบุคคล");
        subtitle.setForeground(Ui.TEXT_MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(12f));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(appTitle);
        sidebar.add(subtitle);
        sidebar.add(Box.createVerticalStrut(40));

        JButton btnDash = createMenuButton("  หน้าหลัก (แดชบอร์ด)", "Dashboard");
        JButton btnTrans = createMenuButton("  ประวัติรายการ", "Transactions");
        activeMenuBtn = btnDash;
        setMenuActive(btnDash, true);
        
        sidebar.add(btnDash);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnTrans);
        sidebar.add(Box.createVerticalStrut(10));
        
        JButton btnBills = createMenuButton("  บิลและแจ้งเตือน", null);
        btnBills.addActionListener(e -> { BillDialog.show(this, bills, transactions); refreshAll(); });
        sidebar.add(btnBills);
        sidebar.add(Box.createVerticalStrut(10));

        JButton btnGoals = createMenuButton("  เป้าหมายการออม", null);
        btnGoals.addActionListener(e -> GoalDialog.show(this, goals, getBalance()));
        sidebar.add(btnGoals);
        sidebar.add(Box.createVerticalStrut(10));
        
        JButton btnRecur = createMenuButton("  รายการประจำ", null);
        btnRecur.addActionListener(e -> { Recurringdialog.show(this, recurring); applyRecurring(); refreshAll(); });
        sidebar.add(btnRecur);

        sidebar.add(Box.createVerticalGlue());

        JButton btnSettings = createMenuButton("  ตั้งค่าและความปลอดภัย", null);
        btnSettings.addActionListener(e -> { SecurityDialog.show(this); refreshSecuritySettings(); });
        sidebar.add(btnSettings);

        return sidebar;
    }

    private JButton createMenuButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(200, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(new Color(30, 30, 35));
        btn.setForeground(Ui.TEXT_LIGHT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (cardName != null) {
            btn.addActionListener(e -> {
                cardLayout.show(mainContent, cardName);
                if (activeMenuBtn != null) setMenuActive(activeMenuBtn, false);
                activeMenuBtn = btn;
                setMenuActive(btn, true);
            });
        }
        return btn;
    }

    private void setMenuActive(JButton btn, boolean active) {
        btn.setBackground(active ? Ui.ACCENT_BLUE : new Color(30, 30, 35));
        btn.setForeground(active ? Color.WHITE : Ui.TEXT_LIGHT);
    }

    private JPanel buildDashboard() {
        JPanel dash = new JPanel(new BorderLayout(20, 20));
        dash.setBackground(Ui.BG_DARK);
        dash.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        
        JPanel topMetrics = new JPanel(new GridLayout(1, 4, 15, 0));
        topMetrics.setOpaque(false);
        topMetrics.add(Ui.metricCard("รายรับเดือนนี้", incomeLabel));
        topMetrics.add(Ui.metricCard("รายจ่ายเดือนนี้", expenseLabel));
        topMetrics.add(Ui.metricCard("คงเหลือเดือนนี้", balanceLabel));
        topMetrics.add(Ui.metricCard("จำนวนรายการ", countLabel));

        // ย้ายปุ่ม "จัดการงบประมาณ" กลับมาไว้ในเมนูด่วนตรงนี้
        JPanel quickAccess = Ui.titledCard("เมนูด่วน", Ui.toolbar(
            createActionButton("+ เพิ่มรายรับ", true, this::addQuickIncome),
            createActionButton("- เพิ่มรายจ่าย", false, this::addQuickExpense),
            createActionButton("จัดการงบประมาณ", true, () -> Categorybudgetdialog.show(this, categoryBudgets, transactions)),
            createActionButton("ล็อกโปรแกรม", false, this::lockNow)
        ));

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        chartsPanel.setOpaque(false);
        pieChart.setPreferredSize(new Dimension(0, 300));
        barChart.setPreferredSize(new Dimension(0, 300));
        chartsPanel.add(Ui.titledCard("สัดส่วนรายจ่ายเดือนนี้", pieChart));
        chartsPanel.add(Ui.titledCard("แนวโน้ม 6 เดือนย้อนหลัง", barChart));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 20));
        centerPanel.setOpaque(false);
        centerPanel.add(quickAccess, BorderLayout.NORTH);
        centerPanel.add(chartsPanel, BorderLayout.CENTER);

        dash.add(topMetrics, BorderLayout.NORTH);
        dash.add(centerPanel, BorderLayout.CENTER);
        return dash;
    }

    private JPanel buildTransactionsView() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(Ui.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        
        JLabel lblFilter = new JLabel("เลือกเดือน:");
        lblFilter.setForeground(Ui.TEXT_LIGHT);
        lblFilter.setFont(lblFilter.getFont().deriveFont(Font.BOLD, 13f));
        
        toolbar.add(lblFilter);
        toolbar.add(filterMonth);
        filterMonth.addActionListener(e -> applyFilter());

        JPanel actionArea = Ui.toolbar(
            createActionButton("แก้ไขรายการ", true, this::editSelected),
            createActionButton("ลบรายการ", false, this::deleteSelected),
            createActionButton("นำเข้า CSV", true, this::importCsv),
            createActionButton("ส่งออก CSV", true, this::exportCsv)
        );

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setOpaque(false);
        topWrapper.add(toolbar, BorderLayout.WEST);
        topWrapper.add(actionArea, BorderLayout.EAST);

        Ui.styleTable(table);
        table.setRowSorter(sorter);
        installRenderers();
        
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Ui.BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(Ui.BORDER_COLOR));

        panel.add(topWrapper, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JButton createActionButton(String text, boolean isPrimary, Runnable action) {
        JButton btn = isPrimary ? Ui.primary(text) : Ui.secondary(text);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void installRenderers() {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                if (value instanceof Double) value = money.format((Double) value);
                else if (value instanceof LocalDate) value = ((LocalDate) value).format(DATE_FMT);

                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                setHorizontalAlignment(col == 4 ? RIGHT : LEFT);
                
                if (!selected) {
                    c.setBackground(Ui.BG_CARD);
                    boolean income = Transaction.INCOME.equals(t.getValueAt(row, 1));
                    c.setForeground(income ? new Color(80, 220, 120) : new Color(255, 100, 100));
                }
                return c;
            }
        });
    }

    private void startServices() {
        refreshSecuritySettings();
        SwingUtilities.invokeLater(this::checkBills);

        new javax.swing.Timer(30 * 60 * 1000, e -> checkBills()).start();

        Toolkit.getDefaultToolkit().addAWTEventListener(
                ev -> lastActivity = System.currentTimeMillis(),
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        new javax.swing.Timer(5000, e -> {
            if (passwordEnabled && autoLockMinutes > 0 && !locked
                    && System.currentTimeMillis() - lastActivity > autoLockMinutes * 60_000L) {
                lockNow();
            }
        }).start();

        getRootPane().registerKeyboardAction(e -> lockNow(), KeyStroke.getKeyStroke("control L"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void checkBills() {
        if (locked) return;
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        for (Bill b : bills) {
            if (!b.needsReminder(today)) continue;
            if (!notifiedBills.add(b.getName() + "|" + b.nextDueDate())) continue;
            sb.append("• ").append(b.getName()).append("  ").append(money.format(b.getAmount())).append(" บาท  —  ")
              .append(b.statusText(today)).append(" (").append(b.nextDueDate().format(DATE_FMT)).append(")\n");
        }
        if (sb.length() == 0) return;

        Object[] options = {"เปิดหน้าบิล", "ปิด"};
        int choice = JOptionPane.showOptionDialog(this, "มีบิลใกล้ครบกำหนด/เลยกำหนด:\n\n" + sb,
                "แจ้งเตือนบิล", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
        if (choice == 0) { BillDialog.show(this, bills, transactions); refreshAll(); }
    }

    private void refreshSecuritySettings() {
        passwordEnabled = PasswordManager.isPasswordSet();
        autoLockMinutes = PasswordManager.getAutoLockMinutes();
        lastActivity = System.currentTimeMillis();
    }

    private void lockNow() {
        if (locked) return;
        if (!passwordEnabled) {
            int r = JOptionPane.showConfirmDialog(this, "ยังไม่ได้ตั้งรหัสผ่าน ต้องการตั้งตอนนี้เลยหรือไม่?",
                    "ล็อกโปรแกรม", JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) { SecurityDialog.show(this); refreshSecuritySettings(); }
            return;
        }
        locked = true;
        for (Window w : getOwnedWindows()) w.dispose();
        setVisible(false);

        if (!LockDialog.unlock(null)) System.exit(0);

        locked = false;
        refreshSecuritySettings();
        setVisible(true);
        toFront();
    }

    private void addQuickIncome() { promptTransaction(Transaction.INCOME); }
    private void addQuickExpense() { promptTransaction(Transaction.EXPENSE); }

    private void promptTransaction(String type) {
        Transaction dummy = new Transaction(type, type.equals(Transaction.INCOME) ? "เงินเดือน" : "อาหาร", "", 0, LocalDate.now());
        Transaction added = Transactiondialog.show(this, dummy);
        if (added != null) {
            transactions.add(added);
            Filemanager.save(transactions);
            refreshAll();
        }
    }

    private int selectedIndex() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    private void editSelected() {
        int idx = selectedIndex();
        if (idx < 0) { JOptionPane.showMessageDialog(this, "กรุณาเลือกรายการที่ต้องการแก้ไขก่อน"); return; }
        Transaction edited = Transactiondialog.show(this, transactions.get(idx));
        if (edited != null) {
            transactions.set(idx, edited);
            Filemanager.save(transactions);
            refreshAll();
        }
    }

    private void deleteSelected() {
        int idx = selectedIndex();
        if (idx < 0) { JOptionPane.showMessageDialog(this, "กรุณาเลือกรายการที่ต้องการลบก่อน"); return; }
        int ok = JOptionPane.showConfirmDialog(this, "ต้องการลบรายการนี้ใช่หรือไม่?", "ยืนยันการลบ", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            transactions.remove(idx);
            Filemanager.save(transactions);
            refreshAll();
        }
    }
    
    private void applyRecurring() {
        LocalDate today = LocalDate.now();
        boolean changed = false;
        for (Recurring r : recurring) {
            List<Transaction> due = r.applyDue(today);
            if (!due.isEmpty()) {
                transactions.addAll(due);
                changed = true;
            }
        }
        if (changed) {
            Filemanager.save(transactions);
            Filemanager.saveRecurring(recurring);
        }
    }

    private double getBalance() {
        return Stats.sum(transactions, true) - Stats.sum(transactions, false);
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
        Object selectedLabel = filterMonth.getSelectedItem();
        monthKeyMap.clear();
        monthKeyMap.put(ALL, ALL);

        YearMonth current = YearMonth.now();
        for (int i = -24; i <= 12; i++) {
            YearMonth ym = current.plusMonths(i);
            String label = TH_MONTHS[ym.getMonthValue() - 1] + " " + (ym.getYear() + 543);
            monthKeyMap.put(label, ym.toString());
        }

        filterMonth.removeAllItems();
        for (String label : monthKeyMap.keySet()) {
            filterMonth.addItem(label);
        }

        if (selectedLabel != null && monthKeyMap.containsKey(selectedLabel.toString())) {
            filterMonth.setSelectedItem(selectedLabel);
        } else {
            filterMonth.setSelectedItem(TH_MONTHS[current.getMonthValue() - 1] + " " + (current.getYear() + 543));
        }
    }

    private void applyFilter() {
        String selectedLabel = (String) filterMonth.getSelectedItem();
        final String monthKey = monthKeyMap.getOrDefault(selectedLabel, ALL);

        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                if (monthKey != null && !ALL.equals(monthKey) && !e.getValue(0).toString().startsWith(monthKey)) return false;
                return true;
            }
        });
        updateDashboard();
    }

    private void updateDashboard() {
        List<Transaction> v = new ArrayList<>();
        for (int i = 0; i < table.getRowCount(); i++) v.add(transactions.get(table.convertRowIndexToModel(i)));
        
        double inc = Stats.sum(v, true);
        double exp = Stats.sum(v, false);
        incomeLabel.setText("฿ " + money.format(inc));
        expenseLabel.setText("฿ " + money.format(exp));
        balanceLabel.setText("฿ " + money.format(inc - exp));
        countLabel.setText(v.size() + " รายการ");
        
        pieChart.setData(Stats.expenseByCategory(v));
        
        List<String> labels = new ArrayList<>();
        double[] incArr = new double[6];
        double[] expArr = new double[6];
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 6; i++) {
            YearMonth ym = now.minusMonths(5 - i);
            labels.add(ym.toString());
            incArr[i] = Stats.monthTotal(transactions, ym, true);
            expArr[i] = Stats.monthTotal(transactions, ym, false);
        }
        barChart.setData(labels, incArr, expArr);
        
        repaint();
    }
    
    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("ไฟล์ CSV", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Filemanager.ImportResult result;
        try {
            result = Filemanager.importCsv(chooser.getSelectedFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "อ่านไฟล์ไม่สำเร็จ: " + e.getMessage(), "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, summary + "\n\nไม่มีรายการใหม่ให้นำเข้า");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, summary + "\n\nนำเข้า " + fresh.size() + " รายการใหม่เลยหรือไม่?",
                "ยืนยันการนำเข้า", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            transactions.addAll(fresh);
            Filemanager.save(transactions);
            refreshAll();
            JOptionPane.showMessageDialog(this, "นำเข้าเรียบร้อย " + fresh.size() + " รายการ");
        }
    }

    private void exportCsv() {
        List<Transaction> v = new ArrayList<>();
        for (int i = 0; i < table.getRowCount(); i++) v.add(transactions.get(table.convertRowIndexToModel(i)));
        
        if (v.isEmpty()) { JOptionPane.showMessageDialog(this, "ไม่มีข้อมูลให้ส่งออก"); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("รายรับรายจ่าย.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getPath() + ".csv");
            if (Filemanager.exportCsv(v, f)) JOptionPane.showMessageDialog(this, "ส่งออก " + v.size() + " รายการเรียบร้อย\n" + f.getAbsolutePath());
            else JOptionPane.showMessageDialog(this, "ส่งออกไม่สำเร็จ", "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            
            java.awt.Font myFont = new java.awt.Font("Tahoma", java.awt.Font.PLAIN, 14);
            javax.swing.plaf.FontUIResource fontRes = new javax.swing.plaf.FontUIResource(myFont);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if (UIManager.get(key) instanceof javax.swing.plaf.FontUIResource || UIManager.get(key) instanceof java.awt.Font) {
                    UIManager.put(key, fontRes);
                }
            }
        } catch (Exception e) {}
        
        SwingUtilities.invokeLater(() -> {
            if (PasswordManager.isPasswordSet() && !LockDialog.unlock(null)) {
                System.exit(0);
            }
            new Main().setVisible(true);
        });
    }
}
