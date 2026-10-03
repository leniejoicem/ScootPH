package com.payroll.UI.auth;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.DateField;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.Form;
import com.payroll.UI.kit.FormField;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.theme.Theme;
import com.payroll.domain.IT;
import com.payroll.service.AppServices;
import com.payroll.service.AuthService;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ForgotPasswordDialog extends JDialog {

    private final AppServices services;
    private final CardLayout steps = new CardLayout();
    private final JPanel stepPanel = new JPanel(steps);
    private final JLabel stepLabel = Ui.caption("Step 1 of 2");

    private final JTextField username = new JTextField();
    private final DateField birthday = new DateField();
    private final JTextField phone = new JTextField();
    private final JTextField tin = new JTextField();
    private final Form identity = new Form(1);
    private final JButton continueButton = Ui.primary("Continue");

    private final JTextField code = new JTextField();
    private final Form codeForm = new Form(1);
    private final JButton verifyCode = Ui.primary("Verify code");

    private final JPasswordField password = new JPasswordField();
    private final JPasswordField confirm = new JPasswordField();
    private final Form passwordForm = new Form(1);
    private final JButton reset = Ui.primary("Reset password");

    private IT account;
    private boolean done;

    public ForgotPasswordDialog(Component parent, AppServices services) {
        super(SwingUtilities.getWindowAncestor(parent), "Reset password", ModalityType.APPLICATION_MODAL);
        this.services = services;
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.SURFACE);
        root.setBorder(Ui.padding(28));
        JPanel head = Ui.transparent(new BorderLayout(0, 4));
        head.add(stepLabel, BorderLayout.NORTH);
        head.add(Ui.label("Reset your password", Font.BOLD, 22, Theme.TEXT), BorderLayout.CENTER);
        root.add(head, BorderLayout.NORTH);

        stepPanel.setOpaque(false);
        stepPanel.add(identityStep(), "identity");
        stepPanel.add(codeStep(), "code");
        stepPanel.add(passwordStep(), "password");
        root.add(stepPanel, BorderLayout.CENTER);

        setContentPane(root);
        setMinimumSize(new Dimension(420, 520));
        setSize(460, 600);
        setLocationRelativeTo(parent);
        getRootPane().setDefaultButton(continueButton);
    }

    private JPanel identityStep() {
        phone.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "As recorded by HR");
        tin.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "123-456-789");
        identity.add("username", new FormField("Username", username));
        identity.add("birthday", new FormField("Birthday", birthday));
        identity.add("phone", new FormField("Phone number", phone));
        identity.add("tin", new FormField("TIN", tin));
        continueButton.addActionListener(e -> {
            identity.clearErrors();
            Async.run(this, () -> services.auth().verifyIdentity(username.getText(), birthday.getDate(),
                    phone.getText(), tin.getText()), a -> {
                account = a;
                if (AuthService.isTwoFactorEnabled(a)) {
                    stepLabel.setText("Step 2 of 3");
                    steps.show(stepPanel, "code");
                    getRootPane().setDefaultButton(verifyCode);
                } else {
                    showPasswordStep("Step 2 of 2");
                }
            }, null, continueButton);
        });
        return Ui.stack(12, Ui.paragraph("First, confirm it's you with the details HR has on file."), identity,
                Ui.actions(continueButton));
    }

    private JPanel codeStep() {
        code.setFont(Theme.font(Font.BOLD, 20));
        code.setHorizontalAlignment(SwingConstants.CENTER);
        codeForm.add("code", new FormField("Authentication code", code, "Your account uses two-factor authentication"));
        verifyCode.addActionListener(e -> {
            if (services.auth().verifyResetCode(account, code.getText())) {
                showPasswordStep("Step 3 of 3");
            } else {
                codeForm.showErrors(Map.of("code", "That code didn't work. Enter the newest code."));
            }
        });
        return Ui.stack(12, Ui.paragraph("Enter the 6-digit code from your authenticator app."), codeForm,
                Ui.actions(verifyCode));
    }

    private JPanel passwordStep() {
        password.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        confirm.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        passwordForm.add("password", new FormField("New password", password,
                "8+ characters with upper and lower case, a number and a symbol"));
        passwordForm.add("confirm", new FormField("Confirm new password", confirm));
        reset.addActionListener(e -> {
            passwordForm.clearErrors();
            char[] p = password.getPassword();
            char[] c = confirm.getPassword();
            Async.run(this, () -> {
                services.auth().resetPassword(account, p, c);
                return Boolean.TRUE;
            }, ok -> {
                done = true;
                dispose();
            }, passwordForm::showFailure, reset);
        });
        return Ui.stack(12, Ui.paragraph("Choose a new password you haven't used before."), passwordForm,
                Ui.actions(reset));
    }

    private void showPasswordStep(String label) {
        stepLabel.setText(label);
        steps.show(stepPanel, "password");
        getRootPane().setDefaultButton(reset);
        password.requestFocusInWindow();
    }

    public static void open(Component parent, AppServices services) {
        ForgotPasswordDialog d = new ForgotPasswordDialog(parent, services);
        d.setVisible(true);
        if (d.done) {
            Feedback.success(parent, "Password reset. Sign in with your new password.");
        }
    }
}
