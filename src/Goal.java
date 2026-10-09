package src;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Goal = "เป้าหมายการออม" เช่น เที่ยวญี่ปุ่น 50,000 บาท ภายในธันวาคม
 * (Backend) เก็บยอดที่ออมได้แล้ว และคำนวณว่าต้องออมเดือนละเท่าไร
 */
public class Goal {
    private final String name;
    private final double target;
    private double saved;
    private final LocalDate deadline;    // null = ไม่กำหนดวันที่
    private final LocalDate created;

    public Goal(String name, double target, double saved, LocalDate deadline, LocalDate created) {
        this.name = name;
        this.target = target;
        this.saved = saved;
        this.deadline = deadline;
        this.created = created;
    }

    public String getName()        { return name; }
    public double getTarget()      { return target; }
    public double getSaved()       { return saved; }
    public LocalDate getDeadline() { return deadline; }
    public LocalDate getCreated()  { return created; }

    public boolean isDone()        { return saved >= target; }
    public double remaining()      { return Math.max(0, target - saved); }
    public int percent()           { return (int) Math.round(saved / target * 100); }

    public void deposit(double amount)  { saved += amount; }

    /** ถอนเงินออก (ไม่ให้ต่ำกว่า 0) */
    public void withdraw(double amount) { saved = Math.max(0, saved - amount); }

    /** จำนวนเดือนที่เหลือ นับรวมเดือนนี้ถึงเดือนที่ครบกำหนด (0 = เลยกำหนดแล้ว, -1 = ไม่กำหนดวันที่) */
    public int monthsLeft(LocalDate today) {
        if (deadline == null) return -1;
        if (deadline.isBefore(today)) return 0;
        return (int) ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)) + 1;
    }

    /** ต้องออมเดือนละเท่าไรถึงจะทัน (0 = ไม่ต้อง/ไม่กำหนดวันที่) */
    public double monthlyNeeded(LocalDate today) {
        if (deadline == null || isDone()) return 0;
        int m = monthsLeft(today);
        return m <= 0 ? remaining() : remaining() / m;
    }

    public String toFileLine() {
        return name.replace("|", " ") + "|" + target + "|" + saved + "|"
                + (deadline == null ? "" : deadline.toString()) + "|" + created;
    }

    public static Goal fromFileLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length < 5) return null;
            LocalDate dl = p[3].isEmpty() ? null : LocalDate.parse(p[3]);
            return new Goal(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]), dl, LocalDate.parse(p[4]));
        } catch (Exception e) {
            return null;
        }
    }
}
