package com.payroll.UI.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.UI.AppContext;
import com.payroll.UI.auth.TwoFactorDialog;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Avatar;
import com.payroll.UI.kit.Badge;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.Form;
import com.payroll.UI.kit.FormField;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ReportWindow;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.theme.Theme;
import com.payroll.domain.Person;
import com.payroll.service.AuthService;
import com.payroll.service.ServiceException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class ProfilePage extends Page {

    private final JPanel summary = Ui.transparent(new BorderLayout(16, 0));
    private final JPanel personal = Ui.transparent(new ResponsiveGrid(180, 2, 16, 14));
    private final JPanel ids = Ui.transparent(new ResponsiveGrid(180, 2, 16, 14));
    private final JPanel pay = Ui.transparent(new ResponsiveGrid(180, 2, 16, 14));
    private final JPasswordField current = new JPasswordField();
    private final JPasswordField password = new JPasswordField();
    private final JPasswordField confirm = new JPasswordField();
    private final Form passwordForm = new Form(1);
    private final JButton changePassword = Ui.primary("Update password");
    private final Badge twoFactorStatus = new Badge("Disabled");
    private final JLabel twoFactorText = Ui.muted(" ");
    private final JButton twoFactorButton = Ui.secondary("Set up");

    public ProfilePage(AppContext app) {
        super(app);
        JButton print = Ui.ghost("Print profile");
        print.addActionListener(e -> ReportWindow.open(this, "Employee profile",
                () -> app.services().reports().employeeProfile(app.employeeId()), print));
        add(new PageHeader("My profile", "Your employment details and account security", print));

        Card summaryCard = new Card();
        summaryCard.content(summary);
        add(summaryCard);

        JPanel grid = Ui.transparent(new ResponsiveGrid(340, 2, 16, 16));
        grid.add(new Card("Personal details").content(personal));
        grid.add(new Card("Government IDs").content(ids));
        grid.add(new Card("Compensation", "Monthly amounts").content(pay));
        grid.add(securityCard());
        add(grid);

        changePassword.addActionListener(e -> changePassword());
        twoFactorButton.addActionListener(e -> {
            if (TwoFactorDialog.open(this, app.services().auth(), app.account())) {
                showTwoFactor();
                Feedback.success(this, "Two-factor authentication is on. You'll need a code each time you sign in.");
            }
        });
    }

    private Card securityCard() {
        passwordForm.add("current", new FormField("Current password", current));
        passwordForm.add("password", new FormField("New password", password,
                "8+ characters with upper and lower case, a number and a symbol"));
        passwordForm.add("confirm", new FormField("Confirm new password", confirm));
        for (JPasswordField f : new JPasswordField[]{current, password, confirm}) {
            f.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        }

        JPanel twoFactor = Ui.transparent(new BorderLayout(12, 4));
        JPanel tfText = Ui.transparent(new BorderLayout(0, 2));
        JPanel title = Ui.row(FlowLayout.LEFT, Ui.heading("Two-factor authentication"), twoFactorStatus);
        tfText.add(title, BorderLayout.NORTH);
        tfText.add(twoFactorText, BorderLayout.CENTER);
        twoFactor.add(tfText, BorderLayout.CENTER);
        JPanel tfButton = Ui.transparent(new BorderLayout());
        tfButton.add(twoFactorButton, BorderLayout.NORTH);
        twoFactor.add(tfButton, BorderLayout.EAST);

        JPanel separator = new JPanel();
        separator.setBackground(Theme.BORDER);
        separator.setPreferredSize(new java.awt.Dimension(1, 1));

        Card card = new Card("Security", "Password and sign-in protection");
        card.content(Ui.stack(16, passwordForm, Ui.actions(changePassword), separator, twoFactor));
        return card;
    }

    @Override
    public void onShow() {
        Async.run(this, () -> {
            app.refreshPerson();
            return app.person();
        }, this::render);
        showTwoFactor();
    }

    private void showTwoFactor() {
        boolean on = AuthService.isTwoFactorEnabled(app.account());
        twoFactorStatus.setValue(on ? "Enabled" : "Disabled");
        twoFactorText.setText(on ? "A code from your authenticator app is required at sign-in."
                : "Add a second step at sign-in with an authenticator app.");
        twoFactorButton.setText(on ? "Set up again" : "Set up");
    }

    void render(Person p) {
        summary.removeAll();
        summary.add(new Avatar(app.displayName(), 64), BorderLayout.WEST);
        JPanel who = Ui.transparent(new BorderLayout(0, 4));
        who.add(Ui.label(app.displayName(), Font.BOLD, 20, Theme.TEXT), BorderLayout.NORTH);
        String position = p.getEmpPosition() != null ? p.getEmpPosition().getPosition() : "";
        JPanel line = Ui.row(FlowLayout.LEFT, Ui.muted(position),
                new Badge(p.getEmpStatus() != null ? p.getEmpStatus().getStatus() : ""),
                new Badge(app.role().getDisplayName()));
        who.add(line, BorderLayout.CENTER);
        who.add(Ui.caption("Employee #" + p.getEmpID() + " · @" + app.account().getEmpUserName()), BorderLayout.SOUTH);
        summary.add(who, BorderLayout.CENTER);

        personal.removeAll();
        personal.add(Ui.detail("Birthday", Ui.date(p.getEmpBirthday())));
        personal.add(Ui.detail("Phone", p.getEmpPhoneNumber()));
        personal.add(Ui.detail("Address", p.getEmpAddress()), ResponsiveGrid.FULL);
        personal.add(Ui.detail("Immediate supervisor",
                p.getEmpImmediateSupervisor() != null ? p.getEmpImmediateSupervisor().getFormattedName() : null));

        ids.removeAll();
        ids.add(Ui.detail("SSS", p.getEmpSSS()));
        ids.add(Ui.detail("TIN", p.getEmpTIN()));
        ids.add(Ui.detail("PhilHealth", p.getEmpPhilHealth() == 0 ? null : String.valueOf(p.getEmpPhilHealth())));
        ids.add(Ui.detail("Pag-IBIG", p.getEmpPagibig() == 0 ? null : String.valueOf(p.getEmpPagibig())));

        pay.removeAll();
        pay.add(Ui.detail("Basic salary", Ui.peso(p.getEmpBasicSalary())));
        pay.add(Ui.detail("Hourly rate", Ui.peso(p.getEmpHourlyRate())));
        pay.add(Ui.detail("Semi-monthly rate", Ui.peso(p.getEmpMonthlyRate())));
        pay.add(Ui.detail("Rice subsidy", Ui.peso(p.getEmpRice())));
        pay.add(Ui.detail("Phone allowance", Ui.peso(p.getEmpPhone())));
        pay.add(Ui.detail("Clothing allowance", Ui.peso(p.getEmpClothing())));
        revalidate();
        repaint();
    }

    private void changePassword() {
        passwordForm.clearErrors();
        char[] c = current.getPassword();
        char[] n = password.getPassword();
        char[] k = confirm.getPassword();
        if (c.length == 0) {
            passwordForm.showErrors(Map.of("current", "Enter your current password"));
            return;
        }
        Async.run(this, () -> {
            app.services().auth().changePassword(app.account(), c, n, k);
            return Boolean.TRUE;
        }, ok -> {
            current.setText("");
            password.setText("");
            confirm.setText("");
            Feedback.success(this, "Your password was changed.");
        }, error -> {
            if (error instanceof ServiceException) {
                passwordForm.showFailure(error);
            } else {
                Feedback.failure(this, error);
            }
        }, changePassword);
    }
}
