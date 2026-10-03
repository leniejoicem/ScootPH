package com.payroll.UI.pages;

import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.DataTable;
import com.payroll.UI.kit.DataTable.Column;
import com.payroll.UI.kit.DataTable.Kind;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ReportWindow;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.pages.parts.EmployeeEditor;
import com.payroll.domain.IT;
import com.payroll.domain.Person;
import com.payroll.subdomain.EmployeePosition;
import com.payroll.subdomain.EmployeeStatus;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;

public class EmployeesPage extends Page {

    private record Choices(List<EmployeePosition> positions, List<EmployeeStatus> statuses, List<Person> people) {
    }

    private final DataTable<Person> directory = new DataTable<>("Search by name, ID, position or status",
            Column.of("ID", Person::getEmpID, Kind.NUMBER, 76),
            Column.text("Name", p -> p.getLastName() + ", " + p.getFirstName(), 150),
            Column.text("Position", p -> p.getEmpPosition() == null ? null : p.getEmpPosition().getPosition(), 150),
            Column.of("Status", p -> p.getEmpStatus() == null ? null : p.getEmpStatus().getStatus(), Kind.BADGE, 124));
    private final EmployeeEditor editor = new EmployeeEditor(true, 2);
    private final Card editorCard = new Card("Add employee", "Fill in the new employee's details");
    private final JButton save = Ui.primary("Add employee");
    private final JButton cancel = Ui.secondary("Clear");
    private final JButton delete = Ui.danger("Delete");
    private final JButton printProfile = Ui.ghost("Print profile");
    private final JButton newEmployee = Ui.primary("New employee");

    public EmployeesPage(AppContext app) {
        super(app);
        JButton masterlist = Ui.ghost("Print masterlist");
        masterlist.addActionListener(e -> ReportWindow.open(this, "Employee masterlist",
                () -> app.services().reports().employeeMasterlist(), masterlist));
        newEmployee.addActionListener(e -> startNew());
        add(new PageHeader("Employees", "Add, update and remove employee records", masterlist, newEmployee));

        JPanel grid = Ui.transparent(new ResponsiveGrid(440, 2, 16, 16));
        Card list = new Card("Directory", null);
        directory.visibleRows(14);
        directory.setEmptyMessage("No employees", "Add your first employee with “New employee”.");
        list.content(directory);
        grid.add(list);

        JPanel left = Ui.row(FlowLayout.LEFT, delete, printProfile);
        JPanel right = Ui.actions(cancel, save);
        JPanel footer = Ui.transparent(new BorderLayout());
        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        editorCard.content(editor);
        editorCard.footer(footer);
        grid.add(editorCard);
        add(grid);

        directory.onSelect(this::edit);
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> startNew());
        delete.addActionListener(e -> deleteSelected());
        printProfile.addActionListener(e -> {
            Integer id = editor.employeeId();
            if (id != null) {
                ReportWindow.open(this, "Employee profile", () -> app.services().reports().employeeProfile(id), printProfile);
            }
        });
        startNew();
    }

    @Override
    public void onShow() {
        reload(null);
    }

    private void reload(Integer selectId) {
        Async.run(this, () -> new Choices(app.services().employees().positions(),
                app.services().employees().statuses(), app.services().employees().directory()), c -> {
            editor.setChoices(c.positions(), c.statuses(), c.people());
            directory.setRows(c.people());
            if (selectId != null) {
                directory.select(p -> p.getEmpID() == selectId);
            }
        });
    }

    private void startNew() {
        directory.clearSelection();
        editor.clear();
        editor.setPasswordHint("8+ characters with upper and lower case, a number and a symbol");
        editorCard.setTitle("Add employee");
        editorCard.setSubtitle("Fill in the new employee's details");
        save.setText("Add employee");
        delete.setVisible(false);
        printProfile.setVisible(false);
    }

    private void edit(Person summary) {
        int id = summary.getEmpID();
        Async.run(this, () -> {
            Person p = app.services().employees().get(id);
            IT account = app.services().employees().account(id);
            return new Object[]{p, account};
        }, loaded -> {
            editor.load((Person) loaded[0], (IT) loaded[1]);
            editor.setPasswordHint("Leave blank to keep the current password");
            editorCard.setTitle("Edit " + summary.getFirstName() + " " + summary.getLastName());
            editorCard.setSubtitle("Employee #" + id);
            save.setText("Save changes");
            delete.setVisible(id != app.employeeId());
            printProfile.setVisible(true);
        });
    }

    private void save() {
        editor.clearErrors();
        var form = editor.toForm();
        boolean editing = editor.isEditing();
        Async.run(this, () -> {
            if (editing) {
                app.services().employees().update(form);
                if (form.employeeId() == app.employeeId()) {
                    app.refreshPerson();
                }
                return form.employeeId();
            }
            return app.services().employees().add(form);
        }, id -> {
            Feedback.success(this, editing ? "Changes saved." : "Employee added. They can now sign in.");
            reload(id);
            if (!editing) {
                startNew();
            } else {
                editor.password.setText("");
            }
        }, editor::showFailure, save);
    }

    private void deleteSelected() {
        Integer id = editor.employeeId();
        if (id == null) {
            return;
        }
        String name = editor.firstName.getText() + " " + editor.lastName.getText();
        if (!Feedback.confirmDestructive(this, "Delete employee",
                "Delete " + name + "? Their account, attendance and leave records will be removed. "
                + "This can't be undone.", "Delete")) {
            return;
        }
        Async.run(this, () -> app.services().employees().delete(id, app.employeeId()), () -> {
            Feedback.success(this, name + " was deleted.");
            startNew();
            reload(null);
        }, delete);
    }
}
