package com.payroll.UI.pages;

import com.payroll.DAO.EmployeeDAO.LeaveRow;
import com.payroll.UI.AppContext;
import com.payroll.UI.kit.Async;
import com.payroll.UI.kit.Card;
import com.payroll.UI.kit.DataTable;
import com.payroll.UI.kit.DataTable.Column;
import com.payroll.UI.kit.DataTable.Kind;
import com.payroll.UI.kit.Feedback;
import com.payroll.UI.kit.PageHeader;
import com.payroll.UI.kit.ResponsiveGrid;
import com.payroll.UI.kit.Ui;
import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class LeaveApprovalsPage extends Page {

    private final DataTable<LeaveRow> pending = new DataTable<>("Search by employee, subject or type",
            Column.text("Employee", LeaveRow::employeeName, 160),
            Column.text("Subject", LeaveRow::subject, 140),
            Column.text("Type", LeaveRow::type, 110),
            Column.of("From", LeaveRow::dateFrom, Kind.DATE, 100),
            Column.of("To", LeaveRow::dateTo, Kind.DATE, 100),
            Column.of("Days", LeaveRow::totalDays, Kind.NUMBER, 50));
    private final PageHeader header = new PageHeader("Leave approvals", "Requests waiting for a decision");
    private final JPanel details = Ui.transparent(new ResponsiveGrid(160, 2, 16, 12));
    private final JTextArea reason = Ui.paragraph("");
    private final JButton approve = Ui.primary("Approve");
    private final JButton decline = Ui.danger("Decline");
    private final Card detailCard = new Card("Request details", "Select a request to review it");

    public LeaveApprovalsPage(AppContext app) {
        super(app);
        add(header);
        JPanel grid = Ui.transparent(new ResponsiveGrid(420, 2, 16, 16));
        pending.visibleRows(12);
        pending.setEmptyMessage("No pending requests", "New leave requests will be listed here.");
        grid.add(new Card("Pending", null).content(pending));

        JPanel body = Ui.transparent(new BorderLayout(0, 16));
        body.add(details, BorderLayout.NORTH);
        JPanel reasonBox = Ui.transparent(new BorderLayout(0, 4));
        reasonBox.add(Ui.caption("Reason"), BorderLayout.NORTH);
        reasonBox.add(reason, BorderLayout.CENTER);
        body.add(reasonBox, BorderLayout.CENTER);
        detailCard.content(body);
        detailCard.footer(Ui.actions(decline, approve));
        grid.add(detailCard);
        add(grid);

        pending.onSelect(this::showDetails);
        approve.addActionListener(e -> decide(true));
        decline.addActionListener(e -> decide(false));
        showDetails(null);
    }

    @Override
    public void onShow() {
        Async.run(this, () -> app.services().leave().pendingFor(app.employeeId()), rows -> {
            pending.setRows(rows);
            header.setSubtitle(rows.isEmpty() ? "No requests are waiting"
                    : rows.size() + (rows.size() == 1 ? " request is" : " requests are") + " waiting for a decision");
            if (pending.selected().isEmpty()) {
                showDetails(null);
            }
        });
    }

    private void showDetails(LeaveRow r) {
        details.removeAll();
        boolean has = r != null;
        approve.setEnabled(has);
        decline.setEnabled(has);
        if (!has) {
            detailCard.setSubtitle("Select a request to review it");
            reason.setText("");
            details.revalidate();
            return;
        }
        detailCard.setSubtitle(r.employeeName() + " · " + r.type());
        details.add(Ui.detail("Subject", r.subject()), ResponsiveGrid.FULL);
        details.add(Ui.detail("From", Ui.date(r.dateFrom())));
        details.add(Ui.detail("To", Ui.date(r.dateTo())));
        details.add(Ui.detail("Working days", String.valueOf(r.totalDays())));
        details.add(Ui.detail("Employee ID", "#" + r.employeeId()));
        reason.setText(r.reason());
        Async.run(this, () -> app.services().leave().balance(r.employeeId()), b -> {
            details.add(Ui.detail("Balance left", b.available() + " of " + b.total() + " days (this request included)"),
                    ResponsiveGrid.FULL);
            details.revalidate();
            details.repaint();
        });
        details.revalidate();
        details.repaint();
    }

    private void decide(boolean approved) {
        pending.selected().ifPresent(r -> {
            if (!approved && !Feedback.confirmDestructive(this, "Decline request",
                    "Decline " + r.employeeName() + "'s request “" + r.subject() + "”?", "Decline")) {
                return;
            }
            Async.run(this, () -> {
                if (approved) {
                    app.services().leave().approve(app.employeeId(), r.id());
                } else {
                    app.services().leave().decline(app.employeeId(), r.id());
                }
            }, () -> {
                Feedback.success(this, (approved ? "Approved " : "Declined ") + r.employeeName() + "'s request.");
                pending.clearSelection();
                showDetails(null);
                onShow();
            }, approve, decline);
        });
    }
}
