package src;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FileManager = จัดการอ่าน/เขียนไฟล์ทั้งหมด
 *   data.txt        = รายการรายรับ-รายจ่าย
 *   budget.txt      = งบรวมต่อเดือน
 *   budgets.txt     = งบแยกตามหมวดหมู่
 *   recurring.txt   = รายการประจำ
 *   settings.txt    = การตั้งค่า (ธีม)
 */
public class Filemanager {
    private static final Path DATA_FILE      = Paths.get("data.txt");
    private static final Path BUDGET_FILE    = Paths.get("budget.txt");
    private static final Path CAT_BUDGETS    = Paths.get("budgets.txt");
    private static final Path RECURRING_FILE = Paths.get("recurring.txt");
    private static final Path SETTINGS_FILE  = Paths.get("settings.txt");

    // ---------- รายการ ----------
    public static List<Transaction> load() {
        List<Transaction> list = new ArrayList<>();
        for (String line : readLines(DATA_FILE)) {
            Transaction t = Transaction.fromFileLine(line);
            if (t != null) list.add(t);
        }
        return list;
    }

    public static void save(List<Transaction> list) {
        List<String> lines = new ArrayList<>();
        for (Transaction t : list) lines.add(t.toFileLine());
        writeLines(DATA_FILE, lines);
    }

    // ---------- งบรวม ----------
    public static double loadBudget() {
        try {
            if (Files.exists(BUDGET_FILE)) {
                return Double.parseDouble(Files.readString(BUDGET_FILE, StandardCharsets.UTF_8).trim());
            }
        } catch (Exception e) { /* ไฟล์เสีย → ถือว่ายังไม่ตั้งงบ */ }
        return 0;
    }

    public static void saveBudget(double budget) {
        try {
            Files.writeString(BUDGET_FILE, String.valueOf(budget), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("บันทึกงบไม่สำเร็จ: " + e.getMessage());
        }
    }

    // ---------- งบแยกหมวด (รูปแบบบรรทัด: หมวด|จำนวน) ----------
    public static Map<String, Double> loadCategoryBudgets() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (String line : readLines(CAT_BUDGETS)) {
            try {
                String[] p = line.split("\\|", -1);
                double v = Double.parseDouble(p[1]);
                if (v > 0) map.put(p[0], v);
            } catch (Exception e) { /* ข้ามบรรทัดเสีย */ }
        }
        return map;
    }

    public static void saveCategoryBudgets(Map<String, Double> map) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Double> e : map.entrySet()) lines.add(e.getKey() + "|" + e.getValue());
        writeLines(CAT_BUDGETS, lines);
    }

    // ---------- รายการประจำ ----------
    public static List<Recurring> loadRecurring() {
        List<Recurring> list = new ArrayList<>();
        for (String line : readLines(RECURRING_FILE)) {
            Recurring r = Recurring.fromFileLine(line);
            if (r != null) list.add(r);
        }
        return list;
    }

    public static void saveRecurring(List<Recurring> list) {
        List<String> lines = new ArrayList<>();
        for (Recurring r : list) lines.add(r.toFileLine());
        writeLines(RECURRING_FILE, lines);
    }

    // ---------- ธีม ----------
    public static boolean loadDarkMode() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                return Files.readString(SETTINGS_FILE, StandardCharsets.UTF_8).trim().equals("dark");
            }
        } catch (IOException e) { /* ใช้ค่าเริ่มต้น */ }
        return false;
    }

    public static void saveDarkMode(boolean dark) {
        try {
            Files.writeString(SETTINGS_FILE, dark ? "dark" : "light", StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("บันทึกการตั้งค่าไม่สำเร็จ: " + e.getMessage());
        }
    }

    // ---------- ส่งออก CSV ----------
    public static boolean exportCsv(List<Transaction> list, File file) {
        StringBuilder sb = new StringBuilder("\uFEFF");   // BOM ให้ Excel รู้ว่าเป็น UTF-8
        sb.append("วันที่,ประเภท,หมวดหมู่,ชื่อรายการ,จำนวนเงิน\n");
        for (Transaction t : list) {
            sb.append(t.getDate()).append(',')
              .append(t.getType()).append(',')
              .append(quote(t.getCategory())).append(',')
              .append(quote(t.getName())).append(',')
              .append(t.getAmount()).append('\n');
        }
        try {
            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ---------- นำเข้า CSV ----------
    /** ผลลัพธ์การนำเข้า: รายการที่อ่านได้ + จำนวนบรรทัดที่อ่านไม่ได้ */
    public static class ImportResult {
        public final List<Transaction> items = new ArrayList<>();
        public int invalid = 0;
    }

    /**
     * อ่านไฟล์ CSV รูปแบบ: วันที่,ประเภท,หมวดหมู่,ชื่อรายการ,จำนวนเงิน
     * (รองรับ 4 คอลัมน์ที่ไม่มีหมวดหมู่, วันที่แบบ 2026-10-02 หรือ 2/10/2026 รวมถึงปี พ.ศ.)
     */
    public static ImportResult importCsv(File file) throws IOException {
        ImportResult result = new ImportResult();
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        boolean firstLine = true;
        for (String line : lines) {
            line = line.replace("\uFEFF", "");
            if (line.trim().isEmpty()) continue;
            List<String> cols = parseCsvLine(line);

            // บรรทัดแรกที่คอลัมน์แรกไม่ใช่วันที่ = หัวตาราง ข้ามไป
            if (firstLine) {
                firstLine = false;
                if (parseDate(cols.get(0)) == null) continue;
            }
            Transaction t = parseRow(cols);
            if (t == null) result.invalid++;
            else result.items.add(t);
        }
        return result;
    }

    private static Transaction parseRow(List<String> c) {
        if (c.size() < 4) return null;
        LocalDate date = parseDate(c.get(0));
        String type = normalizeType(c.get(1));
        if (date == null || type == null) return null;

        String category, name, amountText;
        if (c.size() >= 5) { category = c.get(2).trim(); name = c.get(3).trim(); amountText = c.get(4); }
        else               { category = "อื่นๆ";          name = c.get(2).trim(); amountText = c.get(3); }

        double amount;
        try {
            amount = Double.parseDouble(amountText.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0 || name.isEmpty()) return null;
        if (!Transaction.categoriesFor(type).contains(category)) category = "อื่นๆ";
        return new Transaction(type, category, name, amount, date);
    }

    private static String normalizeType(String s) {
        s = s.trim().toLowerCase();
        if (s.equals("รายรับ") || s.equals("income") || s.equals("in") || s.equals("+")) return Transaction.INCOME;
        if (s.equals("รายจ่าย") || s.equals("expense") || s.equals("out") || s.equals("-")) return Transaction.EXPENSE;
        return null;
    }

    private static LocalDate parseDate(String s) {
        s = s.trim();
        try {
            if (s.contains("/")) {                       // วัน/เดือน/ปี
                String[] p = s.split("/");
                int year = Integer.parseInt(p[2].trim());
                if (year > 2400) year -= 543;            // แปลงปี พ.ศ. เป็น ค.ศ.
                return LocalDate.of(year, Integer.parseInt(p[1].trim()), Integer.parseInt(p[0].trim()));
            }
            return LocalDate.parse(s);                   // ปปปป-ดด-วว
        } catch (Exception e) {
            return null;
        }
    }

    /** แยกบรรทัด CSV โดยรองรับข้อความในเครื่องหมายคำพูดที่มีคอมม่า */
    static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(ch);
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(ch);
        }
        out.add(cur.toString());
        return out;
    }

    // ---------- ตัวช่วย ----------
    private static List<String> readLines(Path path) {
        try {
            if (Files.exists(path)) return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("อ่านไฟล์ไม่สำเร็จ: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private static void writeLines(Path path, List<String> lines) {
        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("บันทึกไฟล์ไม่สำเร็จ: " + e.getMessage());
        }
    }

    private static String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}