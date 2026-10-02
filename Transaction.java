package src;
import java.time.LocalDate;
import java.util.List;

/**
 * Transaction = "หนึ่งรายการ" ของรายรับ/รายจ่าย
 * (Backend: โครงสร้างข้อมูล)
 */
public class Transaction {
    public static final String INCOME  = "รายรับ";
    public static final String EXPENSE = "รายจ่าย";

    // หมวดหมู่ที่ให้เลือก
    public static final List<String> INCOME_CATEGORIES =
            List.of("เงินเดือน", "โบนัส", "งานเสริม", "ลงทุน", "ของขวัญ", "อื่นๆ");
    public static final List<String> EXPENSE_CATEGORIES =
            List.of("อาหาร", "เดินทาง", "ที่พัก", "บิล/สาธารณูปโภค", "ช้อปปิ้ง", "บันเทิง", "สุขภาพ", "การศึกษา", "อื่นๆ");

    /** คืนรายชื่อหมวดหมู่ตามประเภท */
    public static List<String> categoriesFor(String type) {
        return INCOME.equals(type) ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
    }

    private final String type;
    private final String category;
    private final String name;
    private final double amount;
    private final LocalDate date;

    public Transaction(String type, String category, String name, double amount, LocalDate date) {
        this.type = type;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.date = date;
    }

    public String getType()       { return type; }
    public String getCategory()   { return category; }
    public String getName()       { return name; }
    public double getAmount()     { return amount; }
    public LocalDate getDate()    { return date; }
    public boolean isIncome()     { return type.equals(INCOME); }

    /** แปลงเป็นข้อความ 1 บรรทัดเพื่อเขียนลงไฟล์ (คั่นด้วย |) */
    public String toFileLine() {
        String safeName = name.replace("|", " ");
        return date + "|" + type + "|" + category + "|" + safeName + "|" + amount;
    }

    /** แปลงบรรทัดจากไฟล์กลับเป็น Transaction (บรรทัดเสียคืน null) รองรับไฟล์เก่าที่ยังไม่มีหมวดหมู่ด้วย */
    public static Transaction fromFileLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length >= 5) {   // รูปแบบใหม่: วันที่|ประเภท|หมวด|ชื่อ|จำนวน
                return new Transaction(p[1], p[2], p[3], Double.parseDouble(p[4]), LocalDate.parse(p[0]));
            } else if (p.length == 4) {   // รูปแบบเก่า: วันที่|ประเภท|ชื่อ|จำนวน
                return new Transaction(p[1], "อื่นๆ", p[2], Double.parseDouble(p[3]), LocalDate.parse(p[0]));
            }
        } catch (Exception e) {
            // ปล่อยผ่าน แล้วคืน null ด้านล่าง
        }
        return null;
    }
}