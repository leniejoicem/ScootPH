package com.payroll.UI.pages.parts;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.UI.kit.DateField;
import com.payroll.UI.kit.Form;
import com.payroll.UI.kit.FormField;
import com.payroll.domain.IT;
import com.payroll.domain.Person;
import com.payroll.service.EmployeeService.EmployeeForm;
import com.payroll.subdomain.ComboItem;
import com.payroll.subdomain.EmployeePosition;
import com.payroll.subdomain.EmployeeStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class EmployeeEditor extends Form {

    private final boolean withCompensation;
    private Integer employeeId;

    public final JTextField lastName = new JTextField();
    public final JTextField firstName = new JTextField();
    public final DateField birthday = new DateField();
    public final JTextField phone = new JTextField();
    public final JTextField address = new JTextField();
    public final JComboBox<ComboItem> position = new JComboBox<>();
    public final JComboBox<ComboItem> status = new JComboBox<>();
    public final JComboBox<ComboItem> supervisor = new JComboBox<>();
    public final JTextField sss = new JTextField();
    public final JTextField tin = new JTextField();
    public final JTextField philHealth = new JTextField();
    public final JTextField pagIbig = new JTextField();
    public final JTextField basicSalary = new JTextField();
    public final JTextField hourlyRate = new JTextField();
    public final JTextField semiMonthly = new JTextField();
    public final JTextField rice = new JTextField();
    public final JTextField phoneAllowance = new JTextField();
    public final JTextField clothing = new JTextField();
    public final JTextField username = new JTextField();
    public final JPasswordField password = new JPasswordField();
    private final FormField passwordField;

    public EmployeeEditor(boolean withCompensation, int columns) {
        super(columns);
        this.withCompensation = withCompensation;
        birthday.setMaxDate(LocalDate.now().minusYears(18));
        placeholder(phone, "0917 123 4567");
        placeholder(sss, "12-3456789-0");
        placeholder(tin, "123-456-789");
        placeholder(philHealth, "12 digits");
        placeholder(pagIbig, "12 digits");
        password.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");

        section("Personal");
        add("lastName", new FormField("Last name", lastName));
        add("firstName", new FormField("First name", firstName));
        add("empBirthday", new FormField("Birthday", birthday, "Must be 18 or older"));
        add("empPhoneNumber", new FormField("Phone number", phone));
        add("empAddress", new FormField("Address", address), 2);

        section("Employment");
        add("empPosition", new FormField("Position", position));
        add("empStatus", new FormField("Status", status));
        add("empImmediateSupervisor", new FormField("Immediate supervisor", supervisor));

        section("Government IDs");
        add("empSSS", new FormField("SSS", sss));
        add("empTIN", new FormField("TIN", tin));
        add("empPhilHealth", new FormField("PhilHealth", philHealth));
        add("empPagibig", new FormField("Pag-IBIG", pagIbig));

        if (withCompensation) {
            section("Compensation (monthly, ₱)");
            add("empBasicSalary", new FormField("Basic salary", basicSalary));
            add("empHourlyRate", new FormField("Hourly rate", hourlyRate));
            add("empMonthlyRate", new FormField("Semi-monthly rate", semiMonthly));
            add("empRice", new FormField("Rice subsidy", rice));
            add("empPhone", new FormField("Phone allowance", phoneAllowance));
            add("empClothing", new FormField("Clothing allowance", clothing));
        }

        section("Sign-in");
        add("empUserName", new FormField("Username", username, "3 to 50 characters"));
        passwordField = add("empPassword", new FormField("Password", password,
                "8+ characters with upper and lower case, a number and a symbol"));
    }

    private static void placeholder(JTextField field, String text) {
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, text);
    }

    public void setChoices(List<EmployeePosition> positions, List<EmployeeStatus> statuses, List<Person> people) {
        ComboItem p = (ComboItem) position.getSelectedItem();
        ComboItem s = (ComboItem) status.getSelectedItem();
        ComboItem v = (ComboItem) supervisor.getSelectedItem();
        position.removeAllItems();
        position.addItem(new ComboItem(null, "Choose a position"));
        positions.forEach(x -> position.addItem(new ComboItem(x.getId(), x.getPosition())));
        status.removeAllItems();
        status.addItem(new ComboItem(null, "Choose a status"));
        statuses.forEach(x -> status.addItem(new ComboItem(x.getId(), x.getStatus())));
        supervisor.removeAllItems();
        supervisor.addItem(new ComboItem(null, "Choose a supervisor"));
        people.stream()
                .sorted((a, b) -> a.getFormattedName().compareToIgnoreCase(b.getFormattedName()))
                .forEach(x -> supervisor.addItem(new ComboItem(x.getEmpID(), x.getFormattedName())));
        if (p != null) {
            position.setSelectedItem(p);
        }
        if (s != null) {
            status.setSelectedItem(s);
        }
        if (v != null) {
            supervisor.setSelectedItem(v);
        }
    }

    public boolean isEditing() {
        return employeeId != null;
    }

    public Integer employeeId() {
        return employeeId;
    }

    public void clear() {
        employeeId = null;
        for (JTextField f : new JTextField[]{lastName, firstName, phone, address, sss, tin, philHealth, pagIbig,
                basicSalary, hourlyRate, semiMonthly, rice, phoneAllowance, clothing, username}) {
            f.setText("");
        }
        password.setText("");
        birthday.setDate(null);
        selectKey(position, null);
        selectKey(status, null);
        selectKey(supervisor, null);
        passwordField.setError(null);
        clearErrors();
    }

    public void load(Person p, IT account) {
        clear();
        employeeId = p.getEmpID();
        lastName.setText(nz(p.getLastName()));
        firstName.setText(nz(p.getFirstName()));
        birthday.setDate(p.getEmpBirthday() == null ? null : p.getEmpBirthday() instanceof java.sql.Date d
                ? d.toLocalDate() : p.getEmpBirthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        phone.setText(nz(p.getEmpPhoneNumber()));
        address.setText(nz(p.getEmpAddress()));
        selectKey(position, p.getEmpPosition() == null ? null : p.getEmpPosition().getId());
        selectKey(status, p.getEmpStatus() == null ? null : p.getEmpStatus().getId());
        selectKey(supervisor, p.getEmpImmediateSupervisor() == null ? null : p.getEmpImmediateSupervisor().getEmpID());
        sss.setText(nz(p.getEmpSSS()));
        tin.setText(nz(p.getEmpTIN()));
        philHealth.setText(p.getEmpPhilHealth() == 0 ? "" : String.valueOf(p.getEmpPhilHealth()));
        pagIbig.setText(p.getEmpPagibig() == 0 ? "" : String.valueOf(p.getEmpPagibig()));
        basicSalary.setText(amount(p.getEmpBasicSalary()));
        hourlyRate.setText(amount(p.getEmpHourlyRate()));
        semiMonthly.setText(amount(p.getEmpMonthlyRate()));
        rice.setText(amount(p.getEmpRice()));
        phoneAllowance.setText(amount(p.getEmpPhone()));
        clothing.setText(amount(p.getEmpClothing()));
        username.setText(account == null ? "" : nz(account.getEmpUserName()));
        password.setText("");
        passwordField.setError(null);
    }

    public void setPasswordHint(String hint) {
        passwordField.setError(null);
        passwordField.setHelp(hint);
    }

    public EmployeeForm toForm() {
        return new EmployeeForm(employeeId, lastName.getText(), firstName.getText(), birthday.getDate(),
                address.getText(), phone.getText(), sss.getText(), tin.getText(), philHealth.getText(),
                pagIbig.getText(), key(status), key(position), key(supervisor),
                withCompensation ? basicSalary.getText() : "", withCompensation ? hourlyRate.getText() : "",
                withCompensation ? semiMonthly.getText() : "", withCompensation ? rice.getText() : "",
                withCompensation ? phoneAllowance.getText() : "", withCompensation ? clothing.getText() : "",
                username.getText(), password.getPassword());
    }

    private static Integer key(JComboBox<ComboItem> box) {
        ComboItem item = (ComboItem) box.getSelectedItem();
        return item == null ? null : item.getKey();
    }

    private static void selectKey(JComboBox<ComboItem> box, Integer key) {
        for (int i = 0; i < box.getItemCount(); i++) {
            Integer k = box.getItemAt(i).getKey();
            if (key == null ? k == null : key.equals(k)) {
                box.setSelectedIndex(i);
                return;
            }
        }
        if (box.getItemCount() > 0) {
            box.setSelectedIndex(0);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String amount(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }
}
