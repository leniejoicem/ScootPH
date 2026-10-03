package com.payroll.UI.pages;

import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.DataTable;
import com.payroll.UI.kit.MonthPicker;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ReportWindow;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.StatCard;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.pages.parts.AttendanceTable;
import com.payroll.UI.pages.parts.EmployeePicker;
import com.payroll.UI.theme.Theme;
import com.payroll.service.AttendanceService.Day;
import java.time.YearMonth;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;

public class AttendancePage extends Page {

    private final EmployeePicker employee = new EmployeePicker();
    private final MonthPicker month = new MonthPicker(YearMonth.now(), YearMonth.of(2022, 1));
    private final DataTable<Day> table = AttendanceTable.create();
    private final StatCard present = new StatCard("Days present", "—", " ");
    private final StatCard hours = new StatCard("Hours worked", "—", "after lunch breaks");
    private final StatCard missing = new StatCard("Missing time-outs", "—", " ");
    private final JButton export = Ui.ghost("Export timecard");

    public AttendancePage(AppContext app) {
        super(app);
        add(new PageHeader("Attendance", "Review time records for any employee", export));

        JPanel filters = Ui.transparent(new ResponsiveGrid(240, 2, 16, 8));
        filters.add(new com.payroll.UI.kit.FormField("Employee", employee));
        filters.add(new com.payroll.UI.kit.FormField("Month", month));
        add(new Card().content(filters));

        JPanel stats = Ui.transparent(new ResponsiveGrid(220, 3, 16, 16));
        stats.add(present);
        stats.add(hours);
        stats.add(missing);
        add(stats);

        table.visibleRows(14);
        add(new Card("Time records", null).content(table));

        employee.addActionListener(e -> jumpToLatestMonth());
        month.addActionListener(e -> load());
        export.addActionListener(e -> {
            Integer id = employee.employeeId();
            if (id != null) {
                ReportWindow.open(this, "Timecard", () -> app.services().reports().timecard(id, month.month()), export);
            }
        });
    }

    @Override
    public void onShow() {
        if (employee.getItemCount() == 0) {
            Async.run(this, () -> app.services().employees().directory(), people -> {
                employee.setPeople(people);
                employee.selectEmployee(app.employeeId());
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
            YearMonth target = latest.orElse(YearMonth.now());
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
        Async.run(this, () -> app.services().attendance().month(id, m), this::show);
    }

    private void show(List<Day> days) {
        table.setRows(days);
        long complete = days.stream().filter(Day::isComplete).count();
        long seconds = days.stream().mapToLong(Day::secondsWorked).sum();
        present.setValue(String.valueOf(complete));
        present.setCaption(days.size() + " records");
        hours.setValue(String.format("%d:%02d", seconds / 3600, (seconds % 3600) / 60));
        missing.setValue(String.valueOf(days.stream().filter(d -> d.timeIn() != null && d.timeOut() == null).count()));
        export.setEnabled(!days.isEmpty());
    }
}
