package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * LockDialog = หน้าจอล็อก ต้องใส่รหัสผ่านถึงจะเข้าโปรแกรมได้
 *  - ใส่ผิดครบ 3 ครั้ง จะต้องรอก่อนลองใหม่ (5, 10, 20, 40, 60 วินาที...)
 *  - ลืมรหัสผ่าน: ใช้ "รหัสกู้คืน" ที่ได้รับตอนตั้งรหัสผ่าน
 */
public class LockDialog extends JDialog {
    private boolean unlocked = false;
    private int failures = 0;
    private javax.swing.Timer cooldown;

    private final JPasswordField pw = new JPasswordField(16);
    private final JLabel msg = new JLabel(" ", SwingConstants.CENTER);
    private final JButton unlockBtn = new JButton("ปลดล็อก");
    private final JButton forgotBtn = new JButton("ลืมรหัสผ่าน?");
    private final JButton exitBtn = new JButton("ออกจากโปรแกรม");

    /** แสดงหน้าล็อกและรอจนปลดล็อกสำเร็จ (true) หรือผู้ใช้ปิดหน้าต่าง/กดออก (false) */
    public static boolean unlock(Window owner) {
        LockDialog d = new LockDialog(owner);
        d.setVisible(true);
        return d.unlocked;
    }

    private LockDialog(Window owner) {
        super(owner, "โปรแกรมถูกล็อก", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);

        JLabel title = new JLabel("โปรแกรมบันทึกรายรับ-รายจ่าย", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel sub = new JLabel("กรุณาใส่รหัสผ่านเพื่อเข้าใช้งาน", SwingConstants.CENTER);
        msg.setForeground(new Color(220, 53, 69));

        JPanel body = new JPanel(new GridLayout(0, 1, 0, 10));
        body.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        body.add(title);
        body.add(sub);
        body.add(pw);
        body.add(msg);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        buttons.add(unlockBtn);
        buttons.add(forgotBtn);
        buttons.add(exitBtn);

        setLayout(new BorderLayout());
        add(body, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(unlockBtn);

        unlockBtn.addActionListener(e -> tryUnlock());
        forgotBtn.addActionListener(e -> recover());
        exitBtn.addActionListener(e -> dispose());   // ปิดหน้านี้ = ไม่ปลดล็อก (ผู้เรียกจะออกจากโปรแกรม)

        pack();
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { pw.requestFocusInWindow(); }
        });
    }

    private void tryUnlock() {
        char[] input = pw.getPassword();
        boolean ok = PasswordManager.verify(input);
        java.util.Arrays.fill(input, '\0');   // ล้างรหัสผ่านออกจากหน่วยความจำ
        if (ok) {
            unlocked = true;
            dispose();
        } else {
            onFail();
        }
    }

    private void onFail() {
        failures++;
        pw.setText("");
        if (failures >= 3) {
            int secs = Math.min(60, 5 * (1 << Math.min(failures - 3, 4)));
            startCooldown(secs);
        } else {
            msg.setText("รหัสผ่านไม่ถูกต้อง (ครั้งที่ " + failures + ")");
            pw.requestFocusInWindow();
        }
    }

    /** ล็อกปุ่มชั่วคราวหลังใส่ผิดหลายครั้ง กันการเดารหัสผ่านรัวๆ */
    private void startCooldown(int secs) {
        setBusy(true);
        final int[] left = {secs};
        msg.setText("ใส่ผิดหลายครั้ง กรุณารอ " + left[0] + " วินาที");
        cooldown = new javax.swing.Timer(1000, e -> {
            left[0]--;
            if (left[0] <= 0) {
                cooldown.stop();
                setBusy(false);
                msg.setText("ลองใหม่ได้แล้ว (ผิดมาแล้ว " + failures + " ครั้ง)");
                pw.requestFocusInWindow();
            } else {
                msg.setText("ใส่ผิดหลายครั้ง กรุณารอ " + left[0] + " วินาที");
            }
        });
        cooldown.start();
    }

    private void setBusy(boolean busy) {
        pw.setEnabled(!busy);
        unlockBtn.setEnabled(!busy);
        forgotBtn.setEnabled(!busy);
    }

    /** ลืมรหัสผ่าน: ใส่รหัสกู้คืน แล้วเลือกตั้งรหัสใหม่หรือปิดการใช้รหัสผ่าน */
    private void recover() {
        String code = JOptionPane.showInputDialog(this,
                "ใส่รหัสกู้คืน (รูปแบบ XXXX-XXXX-XXXX ที่ได้รับตอนตั้งรหัสผ่าน):", "กู้คืนรหัสผ่าน", JOptionPane.QUESTION_MESSAGE);
        if (code == null) return;
        if (!PasswordManager.verifyRecoveryCode(code)) {
            JOptionPane.showMessageDialog(this, "รหัสกู้คืนไม่ถูกต้อง", "กู้คืนไม่สำเร็จ", JOptionPane.ERROR_MESSAGE);
            onFail();   // นับเป็นการใส่ผิดด้วย
            return;
        }
        Object[] options = {"ตั้งรหัสผ่านใหม่", "ปิดการใช้รหัสผ่าน"};
        int choice = JOptionPane.showOptionDialog(this, "รหัสกู้คืนถูกต้อง ต้องการทำอะไรต่อ?", "กู้คืนรหัสผ่าน",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == 0) {
            if (SecurityDialog.promptNewPassword(this)) { unlocked = true; dispose(); }
        } else if (choice == 1) {
            PasswordManager.removePassword();
            unlocked = true;
            dispose();
        }
    }

    @Override
    public void dispose() {
        if (cooldown != null) cooldown.stop();
        super.dispose();
    }
}
