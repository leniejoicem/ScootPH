package com.payroll.UI.pages;

import com.payroll.DAO.EmployeeDAO.LeaveRow;
import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Badge;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.StatCard;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.theme.Theme;
import com.payroll.security.Permission;
import com.payroll.service.AttendanceService;
import com.payroll.service.LeaveService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class HomePage extends Page {

    private final PageHeader header;
    private final StatCard leaveAvailable;
    private final StatCard leaveUsed;
    private final StatCard hoursThisMonth;
    private final StatCard roleStat;
    private final JLabel todayStatus = Ui.label(" ", Font.BOLD, 18, Theme.TEXT);
    private final JLabel todayDetail = Ui.muted(" ");
    private final JButton timeIn = Ui.primary("Time in");
    private final JButton timeOut = Ui.secondary("Time out");
    private final JPanel recentLeave = Ui.transparent(new java.awt.GridLayout(0, 1, 0, 8));
    private final Card approvals;
    private final JPanel approvalList = Ui.transparent(new java.awt.GridLayout(0, 1, 0, 8));

    public HomePage(AppContext app) {
        super(app);
        header = new PageHeader(greeting(LocalTime.now()) + ", " + app.firstName(), Ui.date(LocalDate.now()));
        add(header);

        JPanel stats = Ui.transparent(new ResponsiveGrid(220, 4, 16, 16));
        leaveAvailable = new StatCard("Leave available", "—", "working days");
        leaveUsed = new StatCard("Leave used or pending", "—", "this year");
        hoursThisMonth = new StatCard("Hours this month", "—", YearMonth.now().getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH));
        roleStat = roleStatCard();
        stats.add(leaveAvailable);
        stats.add(leaveUsed);
        stats.add(hoursThisMonth);
        if (roleStat != null) {
            stats.add(roleStat);
        }
        add(stats);

        JPanel grid = Ui.transparent(new ResponsiveGrid(340, 2, 16, 16));
        grid.add(todayCard());
        JButton viewAll = Ui.link("View all");
        viewAll.addActionListener(e -> app.navigate("Leave"));
        Card leave = new Card("Recent leave requests", "Your latest requests and their status", viewAll);
        leave.content(recentLeave);
        grid.add(leave);
        JButton review = Ui.link("Review");
        review.addActionListener(e -> app.navigate("Leave Approvals"));
        approvals = new Card("Waiting for your approval", "Pending leave requests from your team", review);
        approvals.content(approvalList);
        approvals.setVisible(app.can(Permission.APPROVE_LEAVE));
        grid.add(approvals, ResponsiveGrid.FULL);
        add(grid);

        timeIn.addActionListener(e -> Async.run(this, () -> app.services().attendance().timeIn(app.employeeId()),
                today -> {
                    showToday(today);
                    Feedback.success(this, "Time-in recorded at " + Ui.time(today.timeIn()) + ".");
                }, null, timeIn));
        timeOut.addActionListener(e -> Async.run(this, () -> app.services().attendance().timeOut(app.employeeId()),
                today -> {
                    showToday(today);
                    Feedback.success(this, "Time-out recorded at " + Ui.time(today.timeOut()) + ".");
                    loadHours();
                }, null, timeOut));
    }

    static String greeting(LocalTime now) {
        if (now.getHour() < 12) {
            return "Good morning";
        }
        return now.getHour() < 18 ? "Good afternoon" : "Good evening";
    }

    private StatCard roleStatCard() {
        if (app.can(Permission.APPROVE_LEAVE)) {
            return new StatCard("Pending approvals", "—", "leave requests");
        }
        if (app.can(Permission.MANAGE_PAYROLL)) {
            return new StatCard("Next payroll", com.payroll.service.PayrollService.monthLabel(app.services().payroll().latestPayableMonth()),
                    "ready to process");
        }
        if (app.can(Permission.MANAGE_USER_ROLES)) {
            return new StatCard("Two-factor adoption", "—", "of accounts");
        }
        return null;
    }

    private Card todayCard() {
        Card card = new Card("Today", "Record your attendance");
        JPanel body = Ui.transparent(new BorderLayout(0, 12));
        JPanel text = Ui.transparent(new BorderLayout(0, 4));
        text.add(todayStatus, BorderLayout.NORTH);
        text.add(todayDetail, BorderLayout.CENTER);
        body.add(text, BorderLayout.NORTH);
        body.add(Ui.row(FlowLayout.LEFT, timeIn, timeOut), BorderLayout.CENTER);
        body.add(Ui.caption("Time-in 8:00 AM – 4:00 PM · Time-out 9:00 AM – 5:00 PM"), BorderLayout.SOUTH);
        return card.content(body);
    }

    @Override
    public void onShow() {
        header.setTitle(greeting(LocalTime.now()) + ", " + app.firstName());
        header.setSubtitle(Ui.date(LocalDate.now()));
        int me = app.employeeId();
        Async.run(this, () -> app.services().attendance().today(me), this::showToday);
        Async.run(this, () -> app.services().leave().balance(me), this::showBalance);
        Async.run(this, () -> app.services().leave().history(me), this::showRecent);
        loadHours();
        if (app.can(Permission.APPROVE_LEAVE)) {
            Async.run(this, () -> app.services().leave().pendingFor(me), this::showApprovals);
        } else if (app.can(Permission.MANAGE_USER_ROLES)) {
            Async.run(this, () -> app.services().access().directory(), rows -> {
                long withAccount = rows.stream().filter(r -> r.accountId() != null).count();
                long enabled = rows.stream().filter(r -> r.twoFactorEnabled()).count();
                roleStat.setValue(withAccount == 0 ? "0%" : Math.round(100.0 * enabled / withAccount) + "%");
                roleStat.setCaption(enabled + " of " + withAccount + " accounts");
            });
        }
    }

    private void loadHours() {
        Async.run(this, () -> app.services().attendance().month(app.employeeId(), YearMonth.now()), days -> {
            long seconds = days.stream().mapToLong(AttendanceService.Day::secondsWorked).sum();
            hoursThisMonth.setValue(String.format("%d:%02d", seconds / 3600, (seconds % 3600) / 60));
            hoursThisMonth.setCaption(days.stream().filter(AttendanceService.Day::isComplete).count() + " days recorded");
        });
    }

    void showToday(AttendanceService.Today today) {
        if (today.timeIn() == null) {
            todayStatus.setText("You haven't timed in yet");
            todayDetail.setText("Your day starts when you time in.");
        } else if (today.timeOut() == null) {
            todayStatus.setText("Timed in at " + Ui.time(today.timeIn()));
            todayDetail.setText("Don't forget to time out before 5:00 PM.");
        } else {
            todayStatus.setText("Done for today");
            todayDetail.setText(Ui.time(today.timeIn()) + " – " + Ui.time(today.timeOut()));
        }
        timeIn.setEnabled(today.canTimeIn());
        timeOut.setEnabled(today.canTimeOut());
    }

    private void showBalance(LeaveService.Balance b) {
        leaveAvailable.setValue(String.valueOf(b.available()));
        leaveAvailable.setCaption("of " + b.total() + " working days");
        leaveUsed.setValue(String.valueOf(b.used()));
    }

    private void showRecent(List<LeaveRow> rows) {
        recentLeave.removeAll();
        if (rows.isEmpty()) {
            recentLeave.add(Ui.muted("No leave requests yet."));
        }
        rows.stream().limit(4).forEach(r -> recentLeave.add(leaveRow(r, r.type())));
        recentLeave.revalidate();
        recentLeave.repaint();
    }

    private void showApprovals(List<LeaveRow> rows) {
        if (roleStat != null) {
            roleStat.setValue(String.valueOf(rows.size()));
            roleStat.setCaption(rows.size() == 1 ? "request to review" : "requests to review");
        }
        approvalList.removeAll();
        if (rows.isEmpty()) {
            approvalList.add(Ui.muted("No requests are waiting."));
        }
        rows.stream().limit(5).forEach(r -> approvalList.add(leaveRow(r, r.employeeName())));
        approvalList.revalidate();
        approvalList.repaint();
    }

    private static JPanel leaveRow(LeaveRow r, String title) {
        JPanel row = Ui.transparent(new BorderLayout(12, 0));
        JPanel text = Ui.transparent(new BorderLayout(0, 2));
        JLabel t = Ui.text(title + " · " + r.subject());
        t.setFont(Theme.font(Font.BOLD, 13));
        text.add(t, BorderLayout.NORTH);
        text.add(Ui.caption(Ui.date(r.dateFrom()) + (r.dateTo().equals(r.dateFrom()) ? "" : " – " + Ui.date(r.dateTo()))
                + " · " + r.totalDays() + (r.totalDays() == 1 ? " day" : " days")), BorderLayout.CENTER);
        row.add(text, BorderLayout.CENTER);
        JPanel badge = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        badge.add(new Badge(r.status()));
        row.add(badge, BorderLayout.EAST);
        return row;
    }
}
