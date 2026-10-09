package src;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * SecurityDialog = หน้าต่างตั้งค่าความปลอดภัย (ตั้ง/เปลี่ยน/ปิดรหัสผ่าน และล็อกอัตโนมัติ)
 */
public class SecurityDialog extends JDialog {
    private static final int MIN_LENGTH = 4;

    private final JLabel status = new JLabel();
    private final JButton setBtn = new JButton("ตั้งรหัสผ่าน");
    private final JButton changeBtn = new JButton("เปลี่ยนรหัสผ่าน");
    private final JButton removeBtn = new JButton("ปิดการใช้รหัสผ่าน");
    private final JSpinner idleBox = new JSpinner(new SpinnerNumberModel(PasswordManager.getAutoLockMinutes(), 0, 240, 1));
    private final JButton saveIdleBtn = new JButton("บันทึก");

    public static void show(Window owner) {
        new SecurityDialog(owner).setVisible(true);
    }

    private SecurityDialog(Window owner) {
        super(owner, "ความปลอดภัย", ModalityType.APPLICATION_MODAL);

        status.setFont(status.getFont().deriveFont(Font.BOLD, 15f));

        JPanel pwPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        pwPanel.setBorder(BorderFactory.createTitledBorder("รหัสผ่าน"));
        pwPanel.add(setBtn);
        pwPanel.add(changeBtn);
        pwPanel.add(removeBtn);

        JPanel idlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        idlePanel.setBorder(BorderFactory.createTitledBorder("ล็อกอัตโนมัติ"));
        idlePanel.add(new JLabel("ล็อกเมื่อไม่ได้ใช้งานนาน (นาที, 0 = ปิด):"));
        idlePanel.add(idleBox);
        idlePanel.add(saveIdleBtn);

        JLabel note = new JLabel("<html><body style='width:430px'><b>ข้อควรรู้:</b> รหัสผ่านป้องกันการ \"เปิดโปรแกรม\" เท่านั้น "
                + "ไฟล์ข้อมูล (data.txt ฯลฯ) ยังไม่ถูกเข้ารหัส ผู้ที่เปิดไฟล์โดยตรงยังอ่านได้ "
                + "ถ้าต้องการความลับสูง ควรตั้งรหัสผ่านให้เครื่องคอมพิวเตอร์ด้วย "
                + "และเก็บ \"รหัสกู้คืน\" ไว้ในที่ปลอดภัย</html>");
        note.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JButton close = new JButton("ปิด");
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(close);

        JPanel center = new JPanel(new GridLayout(0, 1, 0, 6));
        center.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        center.add(status);
        center.add(pwPanel);
        center.add(idlePanel);

        setLayout(new BorderLayout(6, 6));
        add(center, BorderLayout.NORTH);
        add(note, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        setBtn.addActionListener(e -> { if (promptNewPassword(this)) refreshState(); });
        changeBtn.addActionListener(e -> onChange());
        removeBtn.addActionListener(e -> onRemove());
        saveIdleBtn.addActionListener(e -> {
            PasswordManager.setAutoLockMinutes((Integer) idleBox.getValue());
            JOptionPane.showMessageDialog(this, "บันทึกการล็อกอัตโนมัติแล้ว");
        });

        refreshState();
        pack();
        setLocationRelativeTo(owner);
    }

    private void refreshState() {
        boolean set = PasswordManager.isPasswordSet();
        status.setText(set ? "สถานะ: เปิดใช้รหัสผ่านอยู่" : "สถานะ: ยังไม่ได้ตั้งรหัสผ่าน");
        status.setForeground(set ? new Color(34, 139, 34) : new Color(220, 53, 69));
        setBtn.setEnabled(!set);
        changeBtn.setEnabled(set);
        removeBtn.setEnabled(set);
        idleBox.setEnabled(set);
        saveIdleBtn.setEnabled(set);
    }

    private void onChange() {
        if (!confirmCurrentPassword(this)) return;
        if (promptNewPassword(this)) refreshState();
    }

    private void onRemove() {
        if (!confirmCurrentPassword(this)) return;
        int ok = JOptionPane.showConfirmDialog(this, "ปิดการใช้รหัสผ่าน ใครก็เปิดโปรแกรมได้ทันที\nยืนยันหรือไม่?",
                "ปิดการใช้รหัสผ่าน", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            PasswordManager.removePassword();
            refreshState();
        }
    }

    // =====================================================
    //   ตัวช่วย (static) ใช้ร่วมกับ LockDialog ด้วย
    // =====================================================

    /** ให้ใส่รหัสผ่านปัจจุบันเพื่อยืนยัน คืน true ถ้าถูกต้อง */
    static boolean confirmCurrentPassword(Component parent) {
        JPasswordField f = new JPasswordField(16);
        int r = JOptionPane.showConfirmDialog(parent, new Object[]{"ใส่รหัสผ่านปัจจุบัน:", f},
                "ยืนยันรหัสผ่าน", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return false;
        char[] pw = f.getPassword();
        boolean ok = PasswordManager.verify(pw);
        Arrays.fill(pw, '\0');
        if (!ok) JOptionPane.showMessageDialog(parent, "รหัสผ่านไม่ถูกต้อง", "ยืนยันไม่สำเร็จ", JOptionPane.ERROR_MESSAGE);
        return ok;
    }

    /** ให้ตั้งรหัสผ่านใหม่ (พิมพ์ 2 ครั้ง) แล้วแสดงรหัสกู้คืน คืน true ถ้าตั้งสำเร็จ */
    static boolean promptNewPassword(Component parent) {
        while (true) {
            JPasswordField a = new JPasswordField(16);
            JPasswordField b = new JPasswordField(16);
            int r = JOptionPane.showConfirmDialog(parent,
                    new Object[]{"รหัสผ่านใหม่ (อย่างน้อย " + MIN_LENGTH + " ตัวอักษร):", a, "พิมพ์รหัสผ่านใหม่อีกครั้ง:", b},
                    "ตั้งรหัสผ่าน", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) return false;

            char[] p1 = a.getPassword(), p2 = b.getPassword();
            String problem = null;
            if (p1.length < MIN_LENGTH) problem = "รหัสผ่านสั้นเกินไป ต้องมีอย่างน้อย " + MIN_LENGTH + " ตัวอักษร";
            else if (!Arrays.equals(p1, p2)) problem = "รหัสผ่านทั้งสองช่องไม่ตรงกัน";

            if (problem != null) {
                Arrays.fill(p1, '\0');
                Arrays.fill(p2, '\0');
                JOptionPane.showMessageDialog(parent, problem, "ตั้งรหัสผ่านไม่สำเร็จ", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            String code = PasswordManager.setPassword(p1);
            Arrays.fill(p1, '\0');
            Arrays.fill(p2, '\0');
            showRecoveryCode(parent, code);
            return true;
        }
    }

    private static void showRecoveryCode(Component parent, String code) {
        JTextField field = new JTextField(code, 14);
        field.setEditable(false);
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        JOptionPane.showMessageDialog(parent, new Object[]{
                "<html><b>รหัสกู้คืนของคุณ</b> (ใช้เมื่อลืมรหัสผ่าน)</html>", field,
                "<html>กรุณาจดเก็บไว้ในที่ปลอดภัย <b>จะแสดงครั้งนี้ครั้งเดียว</b><br>ถ้าลืมทั้งรหัสผ่านและรหัสกู้คืน จะเข้าโปรแกรมไม่ได้</html>"},
                "ตั้งรหัสผ่านสำเร็จ", JOptionPane.INFORMATION_MESSAGE);
    }
}
