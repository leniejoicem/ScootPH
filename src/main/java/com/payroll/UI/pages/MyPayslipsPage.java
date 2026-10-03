package com.payroll.UI.pages;

import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.DataTable;
import com.payroll.UI.kit.MonthPicker;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ReportWindow;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.pages.parts.AttendanceTable;
import com.payroll.UI.pages.parts.PayslipCard;
import com.payroll.service.AttendanceService.Day;
import com.payroll.service.Payslip;
import com.payroll.service.ServiceException;
import java.time.YearMonth;
import javax.swing.JButton;
import javax.swing.JPanel;

public class MyPayslipsPage extends Page {

    private final MonthPicker month;
    private final PayslipCard payslip = new PayslipCard();
    private final DataTable<Day> attendance = AttendanceTable.create();
    private final JButton download = Ui.primary("Download payslip");
    private final JButton timecard = Ui.ghost("Export timecard");
    private Payslip current;

    public MyPayslipsPage(AppContext app) {
        super(app);
        YearMonth latest = app.services().payroll().latestPayableMonth();
        month = new MonthPicker(latest, YearMonth.of(2022, 1));
        month.putClientProperty("JComponent.minimumWidth", 180);
        add(new PageHeader("My payslips", "Pay is computed from your attendance once a month has ended",
                month, timecard, download));

        JPanel grid = Ui.transparent(new ResponsiveGrid(360, 2, 16, 16));
        grid.add(payslip);
        grid.add(new Card("Attendance", "Days used for this payslip").content(attendance));
        add(grid);

        month.addActionListener(e -> load());
        download.addActionListener(e -> {
            Payslip p = current;
            if (p != null) {
                ReportWindow.open(this, "Payslip " + p.period(), () -> {
                    int id = app.services().payroll().save(p);
                    return app.services().reports().payslip(id);
                }, download);
            }
        });
        timecard.addActionListener(e -> ReportWindow.open(this, "Timecard " + month.month(),
                () -> app.services().reports().timecard(app.employeeId(), month.month()), timecard));
    }

    private boolean initialised;

    @Override
    public void onShow() {
        if (!initialised) {
            initialised = true;
            Async.run(this, () -> app.services().attendance().latestMonth(app.employeeId()), latest -> {
                boolean changed = latest.filter(m -> !m.isAfter(month.month())).map(month::select).orElse(false);
                if (!changed) {
                    load();
                }
            });
            return;
        }
        load();
    }

    private void load() {
        YearMonth m = month.month();
        int me = app.employeeId();
        download.setEnabled(false);
        Async.run(this, () -> app.services().attendance().month(me, m), attendance::setRows);
        Async.run(this, () -> app.services().payroll().payslip(me, m), p -> {
            current = p;
            payslip.show(p);
            download.setEnabled(true);
        }, error -> {
            current = null;
            payslip.showMessage(error instanceof ServiceException ? error.getMessage()
                    : "Couldn't compute this payslip.");
        });
    }
}
