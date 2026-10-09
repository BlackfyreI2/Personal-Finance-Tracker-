package src;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Bill = "บิลที่ต้องจ่ายทุกเดือน" เช่น ค่าไฟ ค่าอินเทอร์เน็ต บัตรเครดิต
 * (Backend) คำนวณว่าบิลครบกำหนดเมื่อไร และควรแจ้งเตือนหรือยัง
 */
public class Bill {
    private final String name;
    private final String category;
    private final double amount;
    private final int dueDay;              // ครบกำหนดทุกวันที่เท่าไรของเดือน (1-31)
    private final int remindDays;          // เตือนล่วงหน้ากี่วัน
    private final YearMonth startMonth;    // รอบแรกของบิล
    private YearMonth lastPaid;            // รอบล่าสุดที่จ่ายแล้ว (null = ยังไม่เคยจ่าย)

    public Bill(String name, String category, double amount, int dueDay, int remindDays,
                YearMonth startMonth, YearMonth lastPaid) {
        this.name = name;
        this.category = category;
        this.amount = amount;
        this.dueDay = dueDay;
        this.remindDays = remindDays;
        this.startMonth = startMonth;
        this.lastPaid = lastPaid;
    }

    /** สร้างบิลใหม่ รอบแรกคือ "วันครบกำหนดถัดไปที่ยังไม่ผ่านไป" */
    public static Bill create(String name, String category, double amount, int dueDay, int remindDays, LocalDate today) {
        YearMonth thisMonth = YearMonth.from(today);
        int dayThisMonth = Math.min(dueDay, thisMonth.lengthOfMonth());
        YearMonth start = dayThisMonth >= today.getDayOfMonth() ? thisMonth : thisMonth.plusMonths(1);
        return new Bill(name, category, amount, dueDay, remindDays, start, null);
    }

    public String getName()       { return name; }
    public String getCategory()   { return category; }
    public double getAmount()     { return amount; }
    public int getDueDay()        { return dueDay; }
    public int getRemindDays()    { return remindDays; }

    /** รอบบิลที่กำลังรอจ่ายอยู่ */
    public YearMonth currentCycle() {
        return lastPaid == null ? startMonth : lastPaid.plusMonths(1);
    }

    /** วันครบกำหนดของรอบปัจจุบัน (ถ้าวันที่เกินวันในเดือน เช่น 31 ใน ก.พ. ใช้วันสุดท้ายของเดือน) */
    public LocalDate nextDueDate() {
        YearMonth c = currentCycle();
        return c.atDay(Math.min(dueDay, c.lengthOfMonth()));
    }

    /** อีกกี่วันจะครบกำหนด (ติดลบ = เลยกำหนดแล้ว) */
    public long daysUntilDue(LocalDate today) {
        return ChronoUnit.DAYS.between(today, nextDueDate());
    }

    /** ควรแจ้งเตือนหรือไม่ = เหลือเวลาไม่เกินจำนวนวันที่ตั้งไว้ (รวมเลยกำหนดแล้ว) */
    public boolean needsReminder(LocalDate today) {
        return daysUntilDue(today) <= remindDays;
    }

    /** ระดับความเร่งด่วน: 2 = เลยกำหนด/วันนี้, 1 = ใกล้ถึง, 0 = ยังอีกนาน */
    public int level(LocalDate today) {
        long d = daysUntilDue(today);
        if (d <= 0) return 2;
        if (d <= remindDays) return 1;
        return 0;
    }

    public String statusText(LocalDate today) {
        long d = daysUntilDue(today);
        if (d < 0) return "เลยกำหนด " + (-d) + " วัน";
        if (d == 0) return "ครบกำหนดวันนี้";
        if (d <= remindDays) return "อีก " + d + " วัน";
        return "ยังไม่ถึงกำหนด (อีก " + d + " วัน)";
    }

    /**
     * จ่ายบิลรอบนี้แล้ว: คืนรายจ่ายที่ต้องบันทึก และเลื่อนไปรอบถัดไป
     * (ยอดที่จ่ายจริงอาจไม่เท่ายอดปกติ เช่น ค่าไฟ จึงรับ paidAmount)
     */
    public Transaction markPaid(LocalDate today, double paidAmount) {
        Transaction t = new Transaction(Transaction.EXPENSE, category, name, paidAmount, today);
        lastPaid = currentCycle();
        return t;
    }

    public String toFileLine() {
        return name.replace("|", " ") + "|" + category + "|" + amount + "|" + dueDay + "|" + remindDays
                + "|" + startMonth + "|" + (lastPaid == null ? "" : lastPaid.toString());
    }

    public static Bill fromFileLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length < 7) return null;
            YearMonth last = p[6].isEmpty() ? null : YearMonth.parse(p[6]);
            return new Bill(p[0], p[1], Double.parseDouble(p[2]), Integer.parseInt(p[3]),
                    Integer.parseInt(p[4]), YearMonth.parse(p[5]), last);
        } catch (Exception e) {
            return null;
        }
    }
}
