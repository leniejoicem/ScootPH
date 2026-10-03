package com.payroll.UI.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.DAO.ITDAO.AccountRow;
import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.DataTable;
import com.payroll.UI.kit.DataTable.Column;
import com.payroll.UI.kit.DataTable.Kind;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.Form;
import com.payroll.UI.kit.FormField;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ReportWindow;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.StatCard;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.theme.Theme;
import com.payroll.service.AccessService.AccountForm;
import com.payroll.subdomain.ComboItem;
import com.payroll.subdomain.UserRole;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class AccessPage extends Page {

    private record Data(List<AccountRow> rows, List<UserRole> roles) {
    }

    private final StatCard hr = new StatCard("HR", "—", "members");
    private final StatCard finance = new StatCard("Finance", "—", "members");
    private final StatCard it = new StatCard("IT", "—", "members");
    private final StatCard tfa = new StatCard("Two-factor", "—", "accounts protected");

    private final DataTable<AccountRow> accounts = new DataTable<>("Search by name, username or role",
            Column.of("ID", AccountRow::employeeId, Kind.NUMBER, 76),
            Column.text("Name", r -> r.lastName() + ", " + r.firstName(), 160),
            Column.text("Username", r -> r.username() == null ? null : "@" + r.username(), 120),
            Column.of("Role", r -> r.role() == null ? "No account" : r.role(), Kind.BADGE, 110),
            Column.of("2FA", r -> r.accountId() == null ? null : r.twoFactorEnabled() ? "Enabled" : "Disabled",
                    Kind.BADGE, 100));
    private final Form form = new Form(1);
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JComboBox<ComboItem> role = new JComboBox<>();
    private final FormField passwordField = new FormField("New password", password, "Leave blank to keep the current password");
    private final JButton save = Ui.primary("Save account");
    private final JButton resetTfa = Ui.danger("Reset 2FA");
    private final Card editor = new Card("Account", "Select an employee");
    private AccountRow selected;

    public AccessPage(AppContext app) {
        super(app);
        JButton print = Ui.ghost("Print role list");
        print.addActionListener(e -> ReportWindow.open(this, "Role masterlist", () -> app.services().reports().roleMasterlist(), print));
        add(new PageHeader("User access", "Who can sign in and what they can do", print));

        JPanel stats = Ui.transparent(new ResponsiveGrid(200, 4, 16, 16));
        stats.add(hr);
        stats.add(finance);
        stats.add(it);
        stats.add(tfa);
        add(stats);

        JPanel grid = Ui.transparent(new ResponsiveGrid(420, 2, 16, 16));
        accounts.visibleRows(14);
        grid.add(new Card("Accounts", null).content(accounts));

        password.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
        form.add("empUserName", new FormField("Username", username));
        form.add("empPassword", passwordField);
        form.add("userRole", new FormField("Role", role, "HR, Finance and IT must each keep at least one member"));
        editor.content(form);
        JPanel footer = Ui.transparent(new BorderLayout());
        footer.add(Ui.row(FlowLayout.LEFT, resetTfa), BorderLayout.WEST);
        footer.add(Ui.actions(save), BorderLayout.EAST);
        editor.footer(footer);
        grid.add(editor);
        add(grid);

        setEditable(false);
        accounts.onSelect(this::edit);
        save.addActionListener(e -> save());
        resetTfa.addActionListener(e -> resetTwoFactor());
    }

    @Override
    public void onShow() {
        reload(selected == null ? null : selected.employeeId());
    }

    private void reload(Integer select) {
        Async.run(this, () -> new Data(app.services().access().directory(), app.services().access().roles()), d -> {
            if (role.getItemCount() == 0) {
                role.addItem(new ComboItem(null, "Choose a role"));
                d.roles().forEach(r -> role.addItem(new ComboItem(r.getId(), r.getRole())));
            }
            accounts.setRows(d.rows());
            hr.setValue(String.valueOf(count(d.rows(), "HR")));
            finance.setValue(String.valueOf(count(d.rows(), "Finance")));
            it.setValue(String.valueOf(count(d.rows(), "IT")));
            long withAccount = d.rows().stream().filter(r -> r.accountId() != null).count();
            long protectedCount = d.rows().stream().filter(AccountRow::twoFactorEnabled).count();
            tfa.setValue(protectedCount + " / " + withAccount);
            if (select != null) {
                accounts.select(r -> r.employeeId() == select);
            }
        });
    }

    private static long count(List<AccountRow> rows, String role) {
        return rows.stream().filter(r -> role.equals(r.role())).count();
    }

    private void setEditable(boolean on) {
        username.setEnabled(on);
        password.setEnabled(on);
        role.setEnabled(on);
        save.setEnabled(on);
    }

    private void edit(AccountRow r) {
        selected = r;
        form.clearErrors();
        editor.setTitle(r.firstName() + " " + r.lastName());
        boolean hasAccount = r.accountId() != null;
        editor.setSubtitle(hasAccount ? "Employee #" + r.employeeId() : "No account yet. Set a username and password to create one.");
        username.setText(r.username() == null ? "" : r.username());
        password.setText("");
        passwordField.setHelp(hasAccount ? "Leave blank to keep the current password"
                : "8+ characters with upper and lower case, a number and a symbol");
        for (int i = 0; i < role.getItemCount(); i++) {
            Integer key = role.getItemAt(i).getKey();
            if (r.roleId() == null ? key == null : r.roleId().equals(key)) {
                role.setSelectedIndex(i);
            }
        }
        if (!hasAccount && role.getItemCount() > 1) {
            for (int i = 0; i < role.getItemCount(); i++) {
                if ("Employee".equals(role.getItemAt(i).getValue())) {
                    role.setSelectedIndex(i);
                }
            }
        }
        save.setText(hasAccount ? "Save account" : "Create account");
        resetTfa.setVisible(r.twoFactorEnabled());
        setEditable(true);
    }

    private void save() {
        if (selected == null) {
            return;
        }
        ComboItem r = (ComboItem) role.getSelectedItem();
        AccountForm f = new AccountForm(selected.employeeId(), username.getText(), password.getPassword(),
                r == null ? null : r.getKey());
        int id = selected.employeeId();
        form.clearErrors();
        Async.run(this, () -> {
            app.services().access().saveAccount(f);
            return Boolean.TRUE;
        }, ok -> {
            password.setText("");
            Feedback.success(this, "Account saved.");
            reload(id);
        }, form::showFailure, save);
    }

    private void resetTwoFactor() {
        if (selected == null) {
            return;
        }
        String name = selected.firstName() + " " + selected.lastName();
        if (!Feedback.confirmDestructive(this, "Reset two-factor authentication",
                "Turn off 2FA for " + name + "? They can sign in with just their password and set it up again.",
                "Reset 2FA")) {
            return;
        }
        int id = selected.employeeId();
        Async.run(this, () -> app.services().access().resetTwoFactor(id), () -> {
            Feedback.success(this, "Two-factor authentication was reset for " + name + ".");
            reload(id);
        }, resetTfa);
    }
}
