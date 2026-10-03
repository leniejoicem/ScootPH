package com.payroll.service;

import com.payroll.DAO.EmployeeDAO;
import com.payroll.DAO.EmployeeDAO.LeaveRow;
import com.payroll.domain.HR;
import com.payroll.domain.HR.LeaveStatus;
import com.payroll.domain.LeaveBalance;
import com.payroll.subdomain.LeaveType;
import com.payroll.validation.OperationResult;
import java.sql.Connection;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LeaveService {

    public record LeaveForm(Integer typeId, String subject, LocalDate from, LocalDate to, String reason) {
    }

    public record Balance(int total, int used, int available) {
    }

    private final Connection connection;
    private final EmployeeDAO records;
    private final Clock clock;

    public LeaveService(Connection connection, EmployeeDAO records) {
        this(connection, records, Clock.systemDefaultZone());
    }

    public LeaveService(Connection connection, EmployeeDAO records, Clock clock) {
        this.connection = connection;
        this.records = records;
        this.clock = clock;
    }

    public List<LeaveType> leaveTypes() {
        return records.getAllLeaveTypes();
    }

    public List<LeaveRow> history(int employeeId) {
        return records.findLeaveRequests(employeeId, null);
    }

    public List<LeaveRow> pendingFor(int approverId) {
        return records.findLeaveRequests(null, LeaveStatus.PENDING.name()).stream()
                .filter(r -> r.employeeId() != approverId)
                .toList();
    }

    public Balance balance(int employeeId) {
        LeaveBalance stored = records.getLeaveBalance(employeeId);
        int total = stored.getTotal() > 0 ? stored.getTotal() : 25;
        int used = usedDays(employeeId);
        return new Balance(total, used, Math.max(0, total - used));
    }

    private int usedDays(int employeeId) {
        return records.findLeaveRequests(employeeId, null).stream()
                .filter(r -> !LeaveStatus.DECLINED.name().equals(r.status()))
                .mapToInt(LeaveRow::totalDays)
                .sum();
    }

    public int apply(int employeeId, LeaveForm form) {
        LocalDate today = LocalDate.now(clock);
        Map<String, String> errors = new LinkedHashMap<>();
        String subject = form.subject() == null ? "" : form.subject().trim();
        String reason = form.reason() == null ? "" : form.reason().trim();

        if (form.typeId() == null) {
            errors.put("leaveType", "Choose a leave type");
        }
        if (subject.length() < 5 || subject.length() > 100) {
            errors.put("subject", "Subject must be 5 to 100 characters");
        }
        if (reason.length() < 10 || reason.length() > 500) {
            errors.put("reason", "Reason must be 10 to 500 characters");
        }
        if (form.from() == null) {
            errors.put("dateFrom", "Choose a start date");
        } else if (form.from().isBefore(today)) {
            errors.put("dateFrom", "Start date can't be in the past");
        } else if (isWeekend(form.from())) {
            errors.put("dateFrom", "Leave must start on a weekday");
        }
        if (form.to() == null) {
            errors.put("dateTo", "Choose an end date");
        } else if (form.from() != null && form.to().isBefore(form.from())) {
            errors.put("dateTo", "End date can't be before the start date");
        } else if (isWeekend(form.to())) {
            errors.put("dateTo", "Leave must end on a weekday");
        }
        if (!errors.isEmpty()) {
            throw ServiceException.invalid(errors);
        }

        int days = workingDays(form.from(), form.to());
        Balance balance = balance(employeeId);
        if (days > balance.available()) {
            throw new ServiceException("This request needs " + days + " working day" + (days == 1 ? "" : "s")
                    + " but you have " + balance.available() + " available.");
        }
        for (LeaveRow existing : records.findLeaveRequests(employeeId, null)) {
            boolean active = !LeaveStatus.DECLINED.name().equals(existing.status());
            if (active && !existing.dateTo().isBefore(form.from()) && !existing.dateFrom().isAfter(form.to())) {
                throw new ServiceException("You already have leave from " + existing.dateFrom() + " to "
                        + existing.dateTo() + " (" + existing.status().toLowerCase() + ").");
            }
        }

        HR request = new HR();
        request.setEmpID(employeeId);
        request.setSubject(subject);
        request.setReason(reason);
        request.setDateFrom(java.sql.Date.valueOf(form.from()));
        request.setDateTo(java.sql.Date.valueOf(form.to()));
        request.setTotalDays(days);
        request.setStatus(LeaveStatus.PENDING);
        LeaveType type = records.getLeaveTypeById(form.typeId());
        if (type == null) {
            throw ServiceException.invalid(Map.of("leaveType", "Choose a leave type"));
        }
        request.setLeaveType(type);

        return Transactions.inTransaction(connection, () -> {
            OperationResult result = records.addLeaveRequest(request);
            if (!result.isSuccess()) {
                throw new ServiceException(result.getFormattedErrors());
            }
            saveBalance(employeeId);
            return request.getLeaveId();
        });
    }

    public void withdraw(int employeeId, int leaveId) {
        LeaveRow row = records.findLeaveRequest(leaveId);
        if (row == null || row.employeeId() != employeeId) {
            throw new ServiceException("Leave request not found.");
        }
        if (!LeaveStatus.PENDING.name().equals(row.status())) {
            throw new ServiceException("Only pending requests can be withdrawn. This one is "
                    + row.status().toLowerCase() + ".");
        }
        Transactions.inTransaction(connection, () -> {
            records.deleteLeaveRequest(leaveId);
            saveBalance(employeeId);
        });
    }

    public void approve(int approverId, int leaveId) {
        decide(approverId, leaveId, LeaveStatus.APPROVED);
    }

    public void decline(int approverId, int leaveId) {
        decide(approverId, leaveId, LeaveStatus.DECLINED);
    }

    private void decide(int approverId, int leaveId, LeaveStatus decision) {
        LeaveRow row = records.findLeaveRequest(leaveId);
        if (row == null) {
            throw new ServiceException("Leave request not found.");
        }
        if (row.employeeId() == approverId) {
            throw new ServiceException("You can't approve or decline your own leave request.");
        }
        if (!LeaveStatus.PENDING.name().equals(row.status())) {
            throw new ServiceException("This request was already " + row.status().toLowerCase() + ".");
        }
        Transactions.inTransaction(connection, () -> {
            records.updateLeaveRequestStatus(decision, leaveId, approverId);
            saveBalance(row.employeeId());
        });
    }

    private void saveBalance(int employeeId) {
        Balance balance = balance(employeeId);
        LeaveBalance stored = new LeaveBalance();
        stored.setEmpID(employeeId);
        stored.setTotal(balance.total());
        stored.setTaken(Math.min(balance.used(), balance.total()));
        stored.setAvailable(balance.available());
        OperationResult result = records.updateLeaveBalance(stored);
        if (!result.isSuccess()) {
            throw new ServiceException(result.getFormattedErrors());
        }
    }

    static boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    public static int workingDays(LocalDate from, LocalDate to) {
        int days = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (!isWeekend(d)) {
                days++;
            }
        }
        return days;
    }
}
