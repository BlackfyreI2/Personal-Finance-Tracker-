package src;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/**
 * PasswordManager = จัดการรหัสผ่านของโปรแกรม (Backend)
 *
 * หลักการสำคัญ: "ไม่เก็บรหัสผ่านจริง" ไว้ในไฟล์ เก็บเฉพาะ hash
 * - ใช้ PBKDF2 (วนคำนวณ 120,000 รอบ) + salt สุ่ม ทำให้เดารหัสผ่านย้อนกลับได้ยากมาก
 * - มี "รหัสกู้คืน" สำหรับกรณีลืมรหัสผ่าน (เก็บเป็น hash เช่นกัน)
 * ข้อมูลทั้งหมดอยู่ในไฟล์ security.txt
 */
public class PasswordManager {
    private static final Path FILE = Paths.get("security.txt");
    private static final int ITERATIONS = 120_000;
    private static final String RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";   // ตัดตัวที่สับสน (0/O, 1/I)
    private static final SecureRandom RANDOM = new SecureRandom();

    // ---------- สถานะ ----------
    public static boolean isPasswordSet() {
        return load().getProperty("hash") != null;
    }

    /** ตั้ง/เปลี่ยนรหัสผ่าน แล้วคืน "รหัสกู้คืน" ใหม่ (ต้องแสดงให้ผู้ใช้จดไว้ จะไม่แสดงซ้ำอีก) */
    public static String setPassword(char[] password) {
        Properties p = load();
        byte[] salt = randomBytes(16);
        p.setProperty("salt", b64(salt));
        p.setProperty("hash", b64(hash(password, salt)));

        String code = generateRecoveryCode();
        byte[] recSalt = randomBytes(16);
        p.setProperty("recSalt", b64(recSalt));
        p.setProperty("recHash", b64(hash(normalize(code).toCharArray(), recSalt)));
        store(p);
        return code;
    }

    /** ตรวจรหัสผ่าน */
    public static boolean verify(char[] password) {
        Properties p = load();
        String salt = p.getProperty("salt"), hash = p.getProperty("hash");
        if (salt == null || hash == null) return false;
        byte[] actual = hash(password, unb64(salt));
        return MessageDigest.isEqual(actual, unb64(hash));   // เทียบแบบเวลาคงที่ กัน timing attack
    }

    /** ตรวจรหัสกู้คืน (พิมพ์ตัวเล็ก/ใส่ขีดหรือไม่ใส่ก็ได้) */
    public static boolean verifyRecoveryCode(String code) {
        Properties p = load();
        String salt = p.getProperty("recSalt"), hash = p.getProperty("recHash");
        if (salt == null || hash == null) return false;
        byte[] actual = hash(normalize(code).toCharArray(), unb64(salt));
        return MessageDigest.isEqual(actual, unb64(hash));
    }

    /** ปิดการใช้รหัสผ่าน */
    public static void removePassword() {
        Properties p = load();
        p.remove("salt");
        p.remove("hash");
        p.remove("recSalt");
        p.remove("recHash");
        store(p);
    }

    // ---------- ล็อกอัตโนมัติ (นาทีที่ไม่มีการใช้งาน, 0 = ปิด) ----------
    public static int getAutoLockMinutes() {
        try {
            return Integer.parseInt(load().getProperty("autoLockMinutes", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void setAutoLockMinutes(int minutes) {
        Properties p = load();
        p.setProperty("autoLockMinutes", String.valueOf(Math.max(0, minutes)));
        store(p);
    }

    // ---------- ภายใน ----------
    private static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("ระบบไม่รองรับการเข้ารหัส", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String generateRecoveryCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(RECOVERY_ALPHABET.charAt(RANDOM.nextInt(RECOVERY_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String normalize(String code) {
        return code.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    private static String b64(byte[] b)   { return Base64.getEncoder().encodeToString(b); }
    private static byte[] unb64(String s) { return Base64.getDecoder().decode(s); }

    private static Properties load() {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (IOException e) {
                System.out.println("อ่านไฟล์ความปลอดภัยไม่สำเร็จ: " + e.getMessage());
            }
        }
        return p;
    }

    private static void store(Properties p) {
        try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            p.store(w, "security settings - do not edit");
        } catch (IOException e) {
            System.out.println("บันทึกไฟล์ความปลอดภัยไม่สำเร็จ: " + e.getMessage());
        }
    }
}
