package com.payroll.UI.pages;

import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.DataTable;
import com.payroll.UI.kit.FormField;
import com.payroll.UI.kit.MonthPicker;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ReportWindow;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.pages.parts.AttendanceTable;
import com.payroll.UI.pages.parts.EmployeePicker;
import com.payroll.UI.pages.parts.PayslipCard;
import com.payroll.service.AttendanceService.Day;
import com.payroll.service.Payslip;
import com.payroll.service.ServiceException;
import java.time.YearMonth;
import javax.swing.JButton;
import javax.swing.JPanel;

public class PayrollPage extends Page {

    private final EmployeePicker employee = new EmployeePicker();
    private final MonthPicker month;
    private final PayslipCard payslip = new PayslipCard();
    private final DataTable<Day> attendance = AttendanceTable.create();
    private final JButton generate = Ui.primary("Generate payslip");
    private final JButton timecard = Ui.ghost("Export timecard");
    private Payslip current;

    public PayrollPage(AppContext app) {
        super(app);
        month = new MonthPicker(app.services().payroll().latestPayableMonth(), YearMonth.of(2022, 1));
        add(new PageHeader("Payroll", "Pay is computed from attendance for months that have ended",
                timecard, generate));

        JPanel filters = Ui.transparent(new ResponsiveGrid(240, 2, 16, 8));
        filters.add(new FormField("Employee", employee));
        filters.add(new FormField("Pay period", month));
        add(new Card().content(filters));

        JPanel grid = Ui.transparent(new ResponsiveGrid(360, 2, 16, 16));
        grid.add(payslip);
        grid.add(new Card("Attendance", "Days used for this payslip").content(attendance));
        add(grid);

        employee.addActionListener(e -> jumpToLatestMonth());
        month.addActionListener(e -> load());
        generate.addActionListener(e -> {
            Payslip p = current;
            if (p != null) {
                ReportWindow.open(this, "Payslip " + p.employeeName() + " " + p.period(), () -> {
                    int id = app.services().payroll().save(p);
                    return app.services().reports().payslip(id);
                }, generate);
            }
        });
        timecard.addActionListener(e -> {
            Integer id = employee.employeeId();
            if (id != null) {
                ReportWindow.open(this, "Timecard", () -> app.services().reports().timecard(id, month.month()), timecard);
            }
        });
    }

    @Override
    public void onShow() {
        if (employee.getItemCount() == 0) {
            Async.run(this, () -> app.services().employees().directory(), people -> {
                employee.setPeople(people);
                jumpToLatestMonth();
            });
        } else {
            load();
        }
    }

    private void jumpToLatestMonth() {
        Integer id = employee.employeeId();
        if (id == null) {
            return;
        }
        Async.run(this, () -> app.services().attendance().latestMonth(id), latest -> {
            YearMonth target = latest.filter(m -> m.isBefore(app.services().payroll().latestPayableMonth().plusMonths(1)))
                    .orElse(app.services().payroll().latestPayableMonth());
            if (!month.select(target)) {
                load();
            }
        });
    }

    private void load() {
        Integer id = employee.employeeId();
        YearMonth m = month.month();
        if (id == null || m == null) {
            return;
        }
        generate.setEnabled(false);
        Async.run(this, () -> app.services().attendance().month(id, m), attendance::setRows);
        Async.run(this, () -> app.services().payroll().payslip(id, m), p -> {
            current = p;
            payslip.show(p);
            generate.setEnabled(true);
        }, error -> {
            current = null;
            payslip.showMessage(error instanceof ServiceException ? error.getMessage() : "Couldn't compute this payslip.");
        });
    }
}
