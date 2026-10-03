package com.payroll.UI.pages;

import com.formdev.flatlaf.FlatClientProperties;
import com.payroll.DAO.EmployeeDAO.LeaveRow;
import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.DataTable;
import com.payroll.UI.kit.DataTable.Column;
import com.payroll.UI.kit.DataTable.Kind;
import com.payroll.UI.kit.DateField;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.Form;
import com.payroll.UI.kit.FormField;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.StatCard;
import com.payroll.UI.kit.Ui;
import com.payroll.UI.theme.Theme;
import com.payroll.service.LeaveService;
import com.payroll.service.LeaveService.LeaveForm;
import com.payroll.subdomain.ComboItem;
import com.payroll.subdomain.LeaveType;
import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class LeavePage extends Page {

    private final StatCard available = new StatCard("Available", "—", "working days");
    private final StatCard used = new StatCard("Used or pending", "—", "working days");
    private final StatCard total = new StatCard("Yearly allowance", "—", "working days");

    private final JComboBox<ComboItem> type = new JComboBox<>();
    private final JTextField subject = new JTextField();
    private final DateField from = new DateField();
    private final DateField to = new DateField();
    private final JTextArea reason = new JTextArea(4, 20);
    private final JLabel days = Ui.muted("Pick your dates to see how many working days this uses.");
    private final Form form = new Form(2);
    private final JButton submit = Ui.primary("Submit request");

    private final DataTable<LeaveRow> history = new DataTable<>("Search requests",
            Column.text("Subject", LeaveRow::subject, 140),
            Column.text("Type", LeaveRow::type, 110),
            Column.of("From", LeaveRow::dateFrom, Kind.DATE, 100),
            Column.of("To", LeaveRow::dateTo, Kind.DATE, 100),
            Column.of("Days", LeaveRow::totalDays, Kind.NUMBER, 50),
            Column.of("Status", LeaveRow::status, Kind.BADGE, 110));
    private final JButton withdraw = Ui.danger("Withdraw request");

    public LeavePage(AppContext app) {
        super(app);
        add(new PageHeader("Leave", "Request time off and track your requests"));

        JPanel stats = Ui.transparent(new ResponsiveGrid(220, 3, 16, 16));
        stats.add(available);
        stats.add(used);
        stats.add(total);
        add(stats);

        JPanel grid = Ui.transparent(new ResponsiveGrid(380, 2, 16, 16));
        grid.add(requestCard());
        Card historyCard = new Card("My requests", "Pending requests can be withdrawn", withdraw);
        historyCard.content(history);
        grid.add(historyCard);
        add(grid);

        history.setEmptyMessage("No requests yet", "Your leave requests will appear here.");
        withdraw.setEnabled(false);
        history.onSelect(r -> withdraw.setEnabled("PENDING".equals(r.status())));
        withdraw.addActionListener(e -> withdrawSelected());
        submit.addActionListener(e -> submit());
        from.addChangeListener(this::updateDays);
        to.addChangeListener(this::updateDays);
    }

    private Card requestCard() {
        subject.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. Family trip");
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        JScrollPane reasonScroll = new JScrollPane(reason);
        from.disableWeekends();
        to.disableWeekends();
        from.setMinDate(LocalDate.now());
        to.setMinDate(LocalDate.now());

        form.add("leaveType", new FormField("Leave type", type));
        form.add("subject", new FormField("Subject", subject, "5 to 100 characters"));
        form.add("dateFrom", new FormField("From", from));
        form.add("dateTo", new FormField("To", to));
        form.add("reason", new FormField("Reason", reasonScroll, "10 to 500 characters"), 2);

        JPanel footer = Ui.transparent(new BorderLayout(12, 0));
        footer.add(days, BorderLayout.CENTER);
        footer.add(Ui.actions(submit), BorderLayout.EAST);
        return new Card("Request leave", "Weekdays only; weekends are not counted").content(Ui.stack(12, form, footer));
    }

    @Override
    public void onShow() {
        if (type.getItemCount() == 0) {
            Async.run(this, () -> app.services().leave().leaveTypes(), this::fillTypes);
        }
        reload();
    }

    private void fillTypes(List<LeaveType> types) {
        type.removeAllItems();
        type.addItem(new ComboItem(null, "Choose a type"));
        for (LeaveType t : types) {
            type.addItem(new ComboItem(t.getId(), t.getLeaveType()));
        }
    }

    private void reload() {
        int me = app.employeeId();
        Async.run(this, () -> app.services().leave().balance(me), this::showBalance);
        Async.run(this, () -> app.services().leave().history(me), rows -> {
            history.setRows(rows);
            withdraw.setEnabled(history.selected().map(r -> "PENDING".equals(r.status())).orElse(false));
        });
    }

    private void showBalance(LeaveService.Balance b) {
        available.setValue(String.valueOf(b.available()));
        used.setValue(String.valueOf(b.used()));
        total.setValue(String.valueOf(b.total()));
    }

    private void updateDays() {
        LocalDate f = from.getDate();
        LocalDate t = to.getDate();
        if (f != null && t == null) {
            to.setDate(f);
            return;
        }
        if (f == null || t == null || t.isBefore(f)) {
            days.setText("Pick your dates to see how many working days this uses.");
            return;
        }
        int n = LeaveService.workingDays(f, t);
        days.setText("Uses " + n + " working day" + (n == 1 ? "" : "s") + ".");
    }

    private void submit() {
        form.clearErrors();
        ComboItem t = (ComboItem) type.getSelectedItem();
        LeaveForm request = new LeaveForm(t == null ? null : t.getKey(), subject.getText(), from.getDate(),
                to.getDate(), reason.getText());
        Async.run(this, () -> app.services().leave().apply(app.employeeId(), request), id -> {
            subject.setText("");
            reason.setText("");
            from.setDate(null);
            to.setDate(null);
            type.setSelectedIndex(0);
            Feedback.success(this, "Leave request submitted. You'll see the decision here.");
            reload();
        }, form::showFailure, submit);
    }

    private void withdrawSelected() {
        history.selected().ifPresent(r -> {
            if (!Feedback.confirmDestructive(this, "Withdraw request",
                    "Withdraw “" + r.subject() + "” (" + Ui.date(r.dateFrom()) + ")?", "Withdraw")) {
                return;
            }
            Async.run(this, () -> app.services().leave().withdraw(app.employeeId(), r.id()), () -> {
                Feedback.success(this, "Request withdrawn. The days are available again.");
                reload();
            }, withdraw);
        });
    }
}
