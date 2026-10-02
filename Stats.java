package src;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Stats = ฟังก์ชันคำนวณสถิติต่างๆ
 * (Backend: ไม่ยุ่งกับหน้าจอเลย รับลิสต์เข้ามาแล้วคืนผลลัพธ์)
 */
public class Stats {

    /** รวมยอดรายรับ (income=true) หรือรายจ่าย (income=false) */
    public static double sum(List<Transaction> list, boolean income) {
        double total = 0;
        for (Transaction t : list) {
            if (t.isIncome() == income) total += t.getAmount();
        }
        return total;
    }

    /** รวมยอดของเดือนที่กำหนด */
    public static double monthTotal(List<Transaction> list, YearMonth ym, boolean income) {
        double total = 0;
        for (Transaction t : list) {
            if (t.isIncome() == income && YearMonth.from(t.getDate()).equals(ym)) total += t.getAmount();
        }
        return total;
    }

    /** รายจ่ายของหมวดหมู่หนึ่ง ในเดือนที่กำหนด */
    public static double monthCategoryExpense(List<Transaction> list, YearMonth ym, String category) {
        double total = 0;
        for (Transaction t : list) {
            if (!t.isIncome() && t.getCategory().equals(category) && YearMonth.from(t.getDate()).equals(ym)) {
                total += t.getAmount();
            }
        }
        return total;
    }

    /** รายจ่ายแยกตามหมวดหมู่ เรียงจากมากไปน้อย */
    public static Map<String, Double> expenseByCategory(List<Transaction> list) {
        Map<String, Double> map = new HashMap<>();
        for (Transaction t : list) {
            if (!t.isIncome()) map.merge(t.getCategory(), t.getAmount(), Double::sum);
        }
        List<Map.Entry<String, Double>> entries = new ArrayList<>(map.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        Map<String, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : entries) sorted.put(e.getKey(), e.getValue());
        return sorted;
    }

    /** รายจ่ายที่มากที่สุด (ไม่มีให้คืน null) */
    public static Transaction biggestExpense(List<Transaction> list) {
        Transaction best = null;
        for (Transaction t : list) {
            if (!t.isIncome() && (best == null || t.getAmount() > best.getAmount())) best = t;
        }
        return best;
    }

    /** ค่าเฉลี่ยรายจ่ายต่อวัน (นับจากวันแรกถึงวันสุดท้ายที่มีรายจ่าย) */
    public static double avgExpensePerDay(List<Transaction> list) {
        LocalDate min = null, max = null;
        double total = 0;
        for (Transaction t : list) {
            if (t.isIncome()) continue;
            total += t.getAmount();
            if (min == null || t.getDate().isBefore(min)) min = t.getDate();
            if (max == null || t.getDate().isAfter(max)) max = t.getDate();
        }
        if (min == null) return 0;
        long days = ChronoUnit.DAYS.between(min, max) + 1;
        return total / days;
    }
}