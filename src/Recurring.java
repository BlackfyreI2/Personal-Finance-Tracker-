package src;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Recurring = "รายการประจำ" เช่น ค่าเช่าบ้านทุกวันที่ 1, เงินเดือนทุกวันที่ 25
 * (Backend) โปรแกรมจะสร้าง Transaction ให้อัตโนมัติเมื่อถึงวันที่กำหนด
 */
public class Recurring {
    private final String type;
    private final String category;
    private final String name;
    private final double amount;
    private final int day;                 // ทุกวันที่เท่าไรของเดือน (1-31)
    private final YearMonth startMonth;    // เดือนแรกที่เริ่มทำงาน
    private YearMonth lastApplied;         // เดือนล่าสุดที่เพิ่มรายการไปแล้ว (null = ยังไม่เคย)

    public Recurring(String type, String category, String name, double amount,
                     int day, YearMonth startMonth, YearMonth lastApplied) {
        this.type = type;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.day = day;
        this.startMonth = startMonth;
        this.lastApplied = lastApplied;
    }

    /** สร้างรายการประจำใหม่ โดยครั้งแรกจะเป็น "วันที่ถัดไปที่ยังมาไม่ถึง/ตรงกับวันนี้" */
    public static Recurring create(String type, String category, String name, double amount, int day, LocalDate today) {
        YearMonth thisMonth = YearMonth.from(today);
        int dayThisMonth = Math.min(day, thisMonth.lengthOfMonth());
        YearMonth start = dayThisMonth >= today.getDayOfMonth() ? thisMonth : thisMonth.plusMonths(1);
        return new Recurring(type, category, name, amount, day, start, null);
    }

    public String getType()          { return type; }
    public String getCategory()      { return category; }
    public String getName()          { return name; }
    public double getAmount()        { return amount; }
    public int getDay()              { return day; }
    public YearMonth getStartMonth() { return startMonth; }

    /**
     * สร้างรายการที่ "ถึงกำหนดแล้ว" ทั้งหมด (รวมเดือนที่ผ่านมาตอนไม่ได้เปิดโปรแกรมด้วย)
     * ถ้าวันที่เกินจำนวนวันของเดือน (เช่น 31 ในเดือน ก.พ.) จะใช้วันสุดท้ายของเดือนแทน
     */
    public List<Transaction> applyDue(LocalDate today) {
        List<Transaction> out = new ArrayList<>();
        YearMonth ym = (lastApplied == null) ? startMonth : lastApplied.plusMonths(1);
        YearMonth current = YearMonth.from(today);
        while (!ym.isAfter(current)) {
            LocalDate d = ym.atDay(Math.min(day, ym.lengthOfMonth()));
            if (d.isAfter(today)) break;   // ยังไม่ถึงวัน
            out.add(new Transaction(type, category, name, amount, d));
            lastApplied = ym;
            ym = ym.plusMonths(1);
        }
        return out;
    }

    public String toFileLine() {
        return type + "|" + category + "|" + name.replace("|", " ") + "|" + amount + "|" + day
                + "|" + startMonth + "|" + (lastApplied == null ? "" : lastApplied.toString());
    }

    public static Recurring fromFileLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length < 7) return null;
            YearMonth last = p[6].isEmpty() ? null : YearMonth.parse(p[6]);
            return new Recurring(p[0], p[1], p[2], Double.parseDouble(p[3]),
                    Integer.parseInt(p[4]), YearMonth.parse(p[5]), last);
        } catch (Exception e) {
            return null;
        }
    }
}