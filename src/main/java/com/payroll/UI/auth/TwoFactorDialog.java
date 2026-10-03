package com.payroll.UI.auth;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.FormField;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.theme.Theme;
import com.payroll.domain.IT;
import com.payroll.service.AuthService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class TwoFactorDialog extends JDialog {

    private final JTextField code = new JTextField();
    private final FormField codeField = new FormField("6-digit code", code, "From your authenticator app");
    private final JButton verify = Ui.primary("Verify and turn on");
    private boolean enabled;

    public TwoFactorDialog(Component parent, AuthService auth, IT account) {
        super(SwingUtilities.getWindowAncestor(parent), "Two-factor authentication", ModalityType.APPLICATION_MODAL);
        AuthService.TwoFactorEnrollment enrollment = auth.beginTwoFactorEnrollment(account);

        JPanel root = Ui.transparent(new BorderLayout(0, 16));
        root.setBorder(Ui.padding(24));
        root.setOpaque(true);
        root.setBackground(Theme.SURFACE);

        JPanel head = Ui.transparent(new BorderLayout(0, 4));
        head.add(Ui.heading("Protect your account"), BorderLayout.NORTH);
        JLabel intro = Ui.muted("<html><body style='width:310px'>Scan this QR code with Google "
                + "Authenticator, Microsoft Authenticator or a similar app, then enter the code it shows.</body></html>");
        head.add(intro, BorderLayout.CENTER);
        root.add(head, BorderLayout.NORTH);

        JLabel qr = new JLabel(qrIcon(enrollment.otpAuthUrl(), 200));
        qr.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel middle = Ui.transparent(new BorderLayout(0, 8));
        middle.add(qr, BorderLayout.NORTH);
        JTextField key = new JTextField(group(enrollment.secret()));
        key.setEditable(false);
        key.setHorizontalAlignment(SwingConstants.CENTER);
        key.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        key.setForeground(Theme.TEXT);
        key.setBackground(Theme.NEUTRAL_SOFT);
        key.getAccessibleContext().setAccessibleName("Manual setup key");
        JButton copy = Ui.link("Copy key");
        copy.addActionListener(e -> {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(enrollment.secret()), null);
            Feedback.success(this, "Key copied");
        });
        JPanel keyRow = Ui.transparent(new BorderLayout(0, 6));
        keyRow.add(Ui.centered(Ui.caption("Can't scan? Enter this key manually:")), BorderLayout.NORTH);
        keyRow.add(key, BorderLayout.CENTER);
        keyRow.add(Ui.row(java.awt.FlowLayout.CENTER, copy), BorderLayout.SOUTH);
        middle.add(keyRow, BorderLayout.CENTER);

        code.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "123 456");
        code.setFont(Theme.font(Font.BOLD, 18));
        code.setHorizontalAlignment(SwingConstants.CENTER);
        middle.add(codeField, BorderLayout.SOUTH);
        root.add(middle, BorderLayout.CENTER);

        JButton cancel = Ui.secondary("Cancel");
        cancel.addActionListener(e -> dispose());
        root.add(Ui.actions(cancel, verify), BorderLayout.SOUTH);

        verify.addActionListener(e -> {
            codeField.setError(null);
            Async.run(this, () -> {
                auth.confirmTwoFactorEnrollment(account, enrollment, code.getText());
                return Boolean.TRUE;
            }, ok -> {
                enabled = true;
                dispose();
            }, error -> {
                if (error instanceof com.payroll.service.ServiceException se && se.getFieldErrors().containsKey("code")) {
                    codeField.setError("That code didn't match. Try the newest code.");
                    code.selectAll();
                    code.requestFocusInWindow();
                } else {
                    Feedback.failure(this, error);
                }
            }, verify);
        });
        getRootPane().setDefaultButton(verify);
        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    public boolean wasEnabled() {
        return enabled;
    }

    static String group(String secret) {
        return secret.replaceAll("(.{4})(?!$)", "$1 ");
    }

    static ImageIcon qrIcon(String content, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            return new ImageIcon(MatrixToImageWriter.toBufferedImage(matrix));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean open(Component parent, AuthService auth, IT account) {
        TwoFactorDialog d = new TwoFactorDialog(parent, auth, account);
        d.setVisible(true);
        return d.wasEnabled();
    }
}
