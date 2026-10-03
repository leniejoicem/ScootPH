package com.payroll.UI.pages;

import com.formdev.flatlaf.FlatClientProperties;
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
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.Ui;
import com.payroll.domain.Person;
import com.payroll.service.PayrollService.CompensationForm;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CompensationPage extends Page {

    private final DataTable<Person> directory = new DataTable<>("Search by name or ID",
            Column.of("ID", Person::getEmpID, Kind.NUMBER, 76),
            Column.text("Name", p -> p.getLastName() + ", " + p.getFirstName(), 170),
            Column.of("Basic salary", Person::getEmpBasicSalary, Kind.MONEY, 120),
            Column.of("Allowances", p -> p.getEmpRice() + p.getEmpPhone() + p.getEmpClothing(), Kind.MONEY, 110),
            Column.of("Hourly", Person::getEmpHourlyRate, Kind.MONEY, 90));
    private final Form form = new Form(2);
    private final JTextField sss = new JTextField();
    private final JTextField tin = new JTextField();
    private final JTextField philHealth = new JTextField();
    private final JTextField pagIbig = new JTextField();
    private final JTextField basic = new JTextField();
    private final JTextField hourly = new JTextField();
    private final JTextField semiMonthly = new JTextField();
    private final JTextField rice = new JTextField();
    private final JTextField phone = new JTextField();
    private final JTextField clothing = new JTextField();
    private final JButton save = Ui.primary("Save changes");
    private final Card editor = new Card("Compensation", "Select an employee to edit");
    private Integer employeeId;

    public CompensationPage(AppContext app) {
        super(app);
        add(new PageHeader("Compensation", "Salaries, allowances and government IDs used for payroll"));
        JPanel grid = Ui.transparent(new ResponsiveGrid(420, 2, 16, 16));
        directory.visibleRows(14);
        grid.add(new Card("Employees", null).content(directory));

        sss.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "12-3456789-0");
        tin.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "123-456-789");
        form.section("Government IDs");
        form.add("empSSS", new FormField("SSS", sss));
        form.add("empTIN", new FormField("TIN", tin));
        form.add("empPhilHealth", new FormField("PhilHealth", philHealth, "12 digits"));
        form.add("empPagibig", new FormField("Pag-IBIG", pagIbig, "12 digits"));
        form.section("Monthly pay (₱)");
        form.add("empBasicSalary", new FormField("Basic salary", basic, "At least ₱20,000"));
        form.add("empHourlyRate", new FormField("Hourly rate", hourly, "At least ₱70"));
        form.add("empMonthlyRate", new FormField("Semi-monthly rate", semiMonthly));
        form.add("empRice", new FormField("Rice subsidy", rice, "At least ₱1,500"));
        form.add("empPhone", new FormField("Phone allowance", phone, "At least ₱500"));
        form.add("empClothing", new FormField("Clothing allowance", clothing, "At least ₱500"));
        editor.content(form);
        editor.footer(Ui.actions(save));
        grid.add(editor);
        add(grid);

        setEditable(false);
        directory.onSelect(this::edit);
        save.addActionListener(e -> save());
    }

    @Override
    public void onShow() {
        reload(employeeId);
    }

    private void reload(Integer select) {
        Async.run(this, () -> app.services().payroll().compensationDirectory(), people -> {
            directory.setRows(people);
            if (select != null) {
                directory.select(p -> p.getEmpID() == select);
            }
        });
    }

    private void setEditable(boolean on) {
        for (FormField f : form.fields().values()) {
            f.input().setEnabled(on);
        }
        save.setEnabled(on);
    }

    private void edit(Person p) {
        employeeId = p.getEmpID();
        form.clearErrors();
        editor.setTitle(p.getFirstName() + " " + p.getLastName());
        editor.setSubtitle("Employee #" + p.getEmpID());
        sss.setText(nz(p.getEmpSSS()));
        tin.setText(nz(p.getEmpTIN()));
        philHealth.setText(p.getEmpPhilHealth() == 0 ? "" : String.valueOf(p.getEmpPhilHealth()));
        pagIbig.setText(p.getEmpPagibig() == 0 ? "" : String.valueOf(p.getEmpPagibig()));
        basic.setText(amount(p.getEmpBasicSalary()));
        hourly.setText(amount(p.getEmpHourlyRate()));
        semiMonthly.setText(amount(p.getEmpMonthlyRate()));
        rice.setText(amount(p.getEmpRice()));
        phone.setText(amount(p.getEmpPhone()));
        clothing.setText(amount(p.getEmpClothing()));
        setEditable(true);
    }

    private void save() {
        if (employeeId == null) {
            return;
        }
        CompensationForm f = new CompensationForm(employeeId, sss.getText(), tin.getText(), philHealth.getText(),
                pagIbig.getText(), basic.getText(), hourly.getText(), semiMonthly.getText(), rice.getText(),
                phone.getText(), clothing.getText());
        int id = employeeId;
        form.clearErrors();
        Async.run(this, () -> {
            app.services().payroll().updateCompensation(f);
            if (id == app.employeeId()) {
                app.refreshPerson();
            }
            return Boolean.TRUE;
        }, ok -> {
            Feedback.success(this, "Compensation saved.");
            reload(id);
        }, form::showFailure, save);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String amount(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }
}
