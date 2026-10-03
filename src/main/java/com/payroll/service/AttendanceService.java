package com.payroll.service;

import com.payroll.DAO.EmployeeDAO;
import com.payroll.domain.Employee;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AttendanceService {

    public static final LocalTime TIME_IN_OPENS = LocalTime.of(8, 0);
    public static final LocalTime TIME_IN_CLOSES = LocalTime.of(16, 0);
    public static final LocalTime TIME_OUT_OPENS = LocalTime.of(9, 0);
    public static final LocalTime TIME_OUT_CLOSES = LocalTime.of(17, 0);

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a");

    public record Today(LocalDate date, LocalTime timeIn, LocalTime timeOut) {
        public boolean canTimeIn() {
            return timeIn == null;
        }

        public boolean canTimeOut() {
            return timeIn != null && timeOut == null;
        }

        public boolean isComplete() {
            return timeIn != null && timeOut != null;
        }
    }

    public record Day(LocalDate date, LocalTime timeIn, LocalTime timeOut, long secondsWorked) {
        public boolean isComplete() {
            return timeIn != null && timeOut != null && !isPlaceholder(timeIn, timeOut);
        }

        public String hoursLabel() {
            return isComplete() ? String.format("%d:%02d", secondsWorked / 3600, (secondsWorked % 3600) / 60) : "—";
        }

        public String status() {
            if (timeIn == null || isPlaceholder(timeIn, timeOut)) {
                return "Absent";
            }
            return timeOut == null ? "No time-out" : "Present";
        }
    }

    static boolean isPlaceholder(LocalTime in, LocalTime out) {
        return LocalTime.MIDNIGHT.equals(in) && LocalTime.MIDNIGHT.equals(out);
    }

    private final EmployeeDAO records;
    private final Clock clock;

    public AttendanceService(EmployeeDAO records) {
        this(records, Clock.systemDefaultZone());
    }

    public AttendanceService(EmployeeDAO records, Clock clock) {
        this.records = records;
        this.clock = clock;
    }

    public Today today(int employeeId) {
        LocalDate date = LocalDate.now(clock);
        LocalTime in = null;
        LocalTime out = null;
        for (Employee day : records.getAttendance(employeeId, date, date)) {
            if (in == null && day.getTimeIn() != null) {
                in = day.getTimeIn();
            }
            if (day.getTimeOut() != null) {
                out = day.getTimeOut();
            }
        }
        return new Today(date, in, out);
    }

    public Today timeIn(int employeeId) {
        LocalTime now = LocalTime.now(clock).withNano(0);
        if (now.isBefore(TIME_IN_OPENS) || now.isAfter(TIME_IN_CLOSES)) {
            throw new ServiceException("Time-in is available from " + TIME_IN_OPENS.format(CLOCK)
                    + " to " + TIME_IN_CLOSES.format(CLOCK) + ".");
        }
        Today today = today(employeeId);
        if (!today.canTimeIn()) {
            throw new ServiceException("You already timed in today at " + today.timeIn().format(CLOCK) + ".");
        }
        records.autoFillLastUnclosedTimeOut(employeeId);
        records.recordTimeIn(employeeId, today.date(), now);
        return today(employeeId);
    }

    public Today timeOut(int employeeId) {
        LocalTime now = LocalTime.now(clock).withNano(0);
        if (now.isBefore(TIME_OUT_OPENS) || now.isAfter(TIME_OUT_CLOSES)) {
            throw new ServiceException("Time-out is available from " + TIME_OUT_OPENS.format(CLOCK)
                    + " to " + TIME_OUT_CLOSES.format(CLOCK) + ".");
        }
        Today today = today(employeeId);
        if (today.timeIn() == null) {
            throw new ServiceException("You haven't timed in today.");
        }
        if (today.timeOut() != null) {
            throw new ServiceException("You already timed out today at " + today.timeOut().format(CLOCK) + ".");
        }
        if (!records.recordTimeOut(employeeId, today.date(), now)) {
            throw new ServiceException("Could not record your time-out. Please try again.");
        }
        return today(employeeId);
    }

    public java.util.Optional<YearMonth> latestMonth(int employeeId) {
        LocalDate latest = records.latestAttendanceDate(employeeId);
        return java.util.Optional.ofNullable(latest).map(YearMonth::from);
    }

    public List<Day> month(int employeeId, YearMonth month) {
        List<Day> days = new ArrayList<>();
        for (Employee row : records.getAttendance(employeeId, month.atDay(1), month.atEndOfMonth())) {
            LocalDate date = ((java.sql.Date) row.getDate()).toLocalDate();
            days.add(new Day(date, row.getTimeIn(), row.getTimeOut(), row.getHoursWorked()));
        }
        return days;
    }

    public List<Employee> completedDays(int employeeId, YearMonth month) {
        List<Employee> complete = new ArrayList<>();
        for (Employee row : records.getAttendance(employeeId, month.atDay(1), month.atEndOfMonth())) {
            if (row.getTimeIn() != null && row.getTimeOut() != null && !isPlaceholder(row.getTimeIn(), row.getTimeOut())) {
                complete.add(row);
            }
        }
        return complete;
    }
}
