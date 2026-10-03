package com.payroll.DAO;

import com.payroll.domain.Employee;
import com.payroll.domain.LeaveBalance;
import com.payroll.domain.HR;
import com.payroll.domain.HR.LeaveStatus;
import com.payroll.subdomain.LeaveType;
import com.payroll.validation.SanitizationService;
import com.payroll.validation.ValidationException;
import com.payroll.validation.ValidationService;
import com.payroll.validation.OperationResult;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmployeeDAO {

    private static final Logger LOGGER = Logger.getLogger(EmployeeDAO.class.getName());
    private Connection connection;

    public EmployeeDAO(Connection connection) {
        this.connection = connection;
    }

    public List<LeaveType> getAllLeaveTypes(){
        List<LeaveType> leaveTypes = new ArrayList<>();
        if (connection != null) {
            String Query = "SELECT * FROM public.leave_types";
            try (PreparedStatement preparedStatement = connection.prepareStatement(Query);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()){
                    LeaveType leaveType = new LeaveType();
                    leaveType.setId(resultSet.getInt("id"));
                    leaveType.setLeaveType(resultSet.getString("leave_type"));
                    leaveTypes.add(leaveType);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving leave types", e);
            }
        }
        return leaveTypes;
    }

    public LeaveType getLeaveTypeById(int id){
        LeaveType leaveType = null;
        if (connection != null) {
            String Query = "SELECT * FROM public.leave_types WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(Query)) {
                preparedStatement.setInt(1, id);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        leaveType = new LeaveType();
                        leaveType.setId(resultSet.getInt("id"));
                        leaveType.setLeaveType(resultSet.getString("leave_type"));
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving leave type by ID: " + id, e);
            }
        }
        return leaveType;
    }

    public OperationResult saveLeaveBalance(LeaveBalance leaveBalance) {
        try {
            ValidationService.validateAndThrow(leaveBalance);

            if (leaveBalance.getTaken() > leaveBalance.getTotal()) {
                return OperationResult.validationFailure(
                    List.of("Taken days cannot exceed total days")
                );
            }

            if (leaveBalance.getAvailable() != (leaveBalance.getTotal() - leaveBalance.getTaken())) {
                return OperationResult.validationFailure(
                    List.of("Available days calculation is incorrect")
                );
            }

            if (connection != null) {
                String Query = "INSERT INTO public.employee_leave (employee_id, taken, available, total) VALUES(?, ?, ?, ?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(Query)) {
                    preparedStatement.setInt(1, leaveBalance.getEmpID());
                    preparedStatement.setInt(2, leaveBalance.getTaken());
                    preparedStatement.setInt(3, leaveBalance.getAvailable());
                    preparedStatement.setInt(4, leaveBalance.getTotal());

                    preparedStatement.executeUpdate();
                    LOGGER.log(Level.INFO, "Leave balance saved for employee: " + leaveBalance.getEmpID());
                    return OperationResult.success("Leave balance saved successfully");
                }
            }
            return OperationResult.failure("Database connection not available");
        } catch (ValidationException e) {
            LOGGER.log(Level.WARNING, "Validation failed for leave balance", e);
            return OperationResult.validationFailure(e.getErrors());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving leave balance", e);
            return OperationResult.failure("Failed to save leave balance. Please try again.");
        }
    }

    public OperationResult updateLeaveBalance(LeaveBalance leaveBalance) {
        try {
            ValidationService.validateAndThrow(leaveBalance);

            if (leaveBalance.getTaken() > leaveBalance.getTotal()) {
                return OperationResult.validationFailure(
                    List.of("Taken days cannot exceed total days")
                );
            }

            if (connection != null) {
                String Query = "INSERT INTO public.employee_leave (taken, available, total, employee_id) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (employee_id) DO UPDATE SET taken = EXCLUDED.taken, "
                        + "available = EXCLUDED.available, total = EXCLUDED.total";
                try (PreparedStatement preparedStatement = connection.prepareStatement(Query)) {
                    preparedStatement.setInt(1, leaveBalance.getTaken());
                    preparedStatement.setInt(2, leaveBalance.getAvailable());
                    preparedStatement.setInt(3, leaveBalance.getTotal());
                    preparedStatement.setInt(4, leaveBalance.getEmpID());

                    int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected == 0) {
                        return OperationResult.failure("Leave balance not found for employee");
                    }
                    LOGGER.log(Level.INFO, "Leave balance updated for employee: " + leaveBalance.getEmpID());
                    return OperationResult.success("Leave balance updated successfully");
                }
            }
            return OperationResult.failure("Database connection not available");
        } catch (ValidationException e) {
            LOGGER.log(Level.WARNING, "Validation failed for leave balance update", e);
            return OperationResult.validationFailure(e.getErrors());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating leave balance", e);
            return OperationResult.failure("Failed to update leave balance. Please try again.");
        }
    }

    public void deleteLeaveBalance(int empID) {
        if (connection != null) {
            String Query = "DELETE FROM public.employee_leave WHERE employee_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(Query)) {
                preparedStatement.setInt(1, empID);
                preparedStatement.executeUpdate();
                LOGGER.log(Level.INFO, "Leave balance deleted for employee: " + empID);
            } catch (SQLException e) {
                throw new DataAccessException("deleteLeaveBalance failed", e);
            }
        }
    }

    public LeaveBalance getLeaveBalance(int empID) {
        LeaveBalance leaveBalance = new LeaveBalance();
        if (connection != null) {
            String Query = "SELECT * FROM public.employee_leave WHERE employee_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(Query)) {
                preparedStatement.setInt(1, empID);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        leaveBalance.setEmpID(resultSet.getInt("employee_id"));
                        leaveBalance.setTaken(resultSet.getInt("taken"));
                        leaveBalance.setAvailable(resultSet.getInt("available"));
                        leaveBalance.setTotal(resultSet.getInt("total"));
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving leave balance", e);
            }
        }

        return leaveBalance;
    }

    public Employee timeIn(Employee attendanceDetails) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is not initialized");
            return attendanceDetails;
        }

        String query = "INSERT INTO public.employee_hours (employee_id, date, time_in) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            java.sql.Date sqlDate = java.sql.Date.valueOf(currentDate);
            java.sql.Time sqlTimeIn = java.sql.Time.valueOf(currentTime);

            preparedStatement.setInt(1, attendanceDetails.getEmpID());
            preparedStatement.setDate(2, sqlDate);
            preparedStatement.setTime(3, sqlTimeIn);
            int affectedRows = preparedStatement.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        attendanceDetails.setAttendanceId(generatedKeys.getInt(1));
                        LOGGER.log(Level.INFO, "Time-in recorded for employee: " + attendanceDetails.getEmpID());
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("timeIn failed", e);
        }

        return attendanceDetails;
    }

    public List<Employee> getAttendance(int empID, LocalDate from, LocalDate to) {
        String sql = "SELECT employee_id, date, time_in, time_out FROM public.employee_hours "
                + "WHERE employee_id = ? AND date BETWEEN ? AND ? ORDER BY date, time_in";
        List<Employee> days = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ps.setDate(2, java.sql.Date.valueOf(from));
            ps.setDate(3, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Employee day = new Employee();
                    day.setEmpID(rs.getInt("employee_id"));
                    day.setDate(rs.getDate("date"));
                    day.setTimeIn(rs.getObject("time_in", LocalTime.class));
                    day.setTimeOut(rs.getObject("time_out", LocalTime.class));
                    days.add(day);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("getAttendance failed", e);
        }
        return days;
    }

    public LocalDate latestAttendanceDate(int empID) {
        String sql = "SELECT max(date) FROM public.employee_hours WHERE employee_id = ? AND time_out IS NOT NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, empID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, LocalDate.class) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("latestAttendanceDate failed", e);
        }
    }

    public void recordTimeIn(int empID, LocalDate date, LocalTime time) {
        String sql = "INSERT INTO public.employee_hours (employee_id, date, time_in) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, empID);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setObject(3, time.withNano(0));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("recordTimeIn failed", e);
        }
    }

    public boolean recordTimeOut(int empID, LocalDate date, LocalTime time) {
        String sql = "UPDATE public.employee_hours SET time_out = ? "
                + "WHERE employee_id = ? AND date = ? AND time_in IS NOT NULL AND time_out IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, time.withNano(0));
            ps.setInt(2, empID);
            ps.setDate(3, java.sql.Date.valueOf(date));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("recordTimeOut failed", e);
        }
    }

    public boolean hasTimeInToday(int empID) {
        String query = "SELECT COUNT(*) FROM public.vw_employee_timein_today WHERE employee_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, empID);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking time-in status", e);
        }
        return false;
    }

    public boolean hasTimeOutToday(int empID) {
        String query = "SELECT COUNT(*) FROM public.vw_employee_hours_today WHERE employee_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, empID);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking time-out status", e);
        }
        return false;
    }

    public void autoFillLastUnclosedTimeOut(int empID) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is not initialized");
            return;
        }

        String call = "CALL auto_fill_last_unclosed_timeout(?)";

        try (CallableStatement callableStatement = connection.prepareCall(call)) {
            callableStatement.setInt(1, empID);
            callableStatement.execute();
            LOGGER.log(Level.INFO, "Auto-filled last missing time-out for employee: " + empID);
        } catch (SQLException e) {
            throw new DataAccessException("autoFillLastUnclosedTimeOut failed", e);
        }
    }

    public Employee timeOut(Employee attendanceDetails) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is not initialized");
            return attendanceDetails;
        }

        String call = "CALL record_employee_timeout(?)";

        try (CallableStatement callableStatement = connection.prepareCall(call)) {
            callableStatement.setInt(1, attendanceDetails.getEmpID());
            callableStatement.execute();
            LOGGER.log(Level.INFO, "Time-out recorded for employee: " + attendanceDetails.getEmpID());
        } catch (SQLException e) {
            throw new DataAccessException("timeOut failed", e);
        }

        return attendanceDetails;
    }

    public void deleteAttendanceRecords(int empID) {
        if (connection == null) {
            LOGGER.log(Level.SEVERE, "Database connection is not initialized");
            return;
        }

        String call = "CALL delete_attendance_records(?)";

        try (CallableStatement callableStatement = connection.prepareCall(call)) {
            callableStatement.setInt(1, empID);
            callableStatement.execute();
            LOGGER.log(Level.INFO, "Attendance records deleted for employee: " + empID);
        } catch (SQLException e) {
            throw new DataAccessException("deleteAttendanceRecords failed", e);
        }
    }

    public OperationResult addLeaveRequest(HR leaveDetails) {
        if (connection == null) {
            return OperationResult.failure("Database connection is not initialized");
        }

        try {

            leaveDetails.setSubject(SanitizationService.sanitizePlainText(leaveDetails.getSubject()));
            leaveDetails.setReason(SanitizationService.sanitizePlainText(leaveDetails.getReason()));

            if (leaveDetails.getDateTo().before(leaveDetails.getDateFrom())) {
                return OperationResult.validationFailure(
                    List.of("End date must be after start date")
                );
            }

            java.sql.Date dateFrom = leaveDetails.getDateFrom() != null
                    ? new java.sql.Date(leaveDetails.getDateFrom().getTime())
                    : null;
            java.sql.Date dateTo = leaveDetails.getDateTo() != null
                    ? new java.sql.Date(leaveDetails.getDateTo().getTime())
                    : null;
            Integer leaveTypeId = (leaveDetails.getLeaveType() != null)
                    ? leaveDetails.getLeaveType().getId()
                    : null;

            String call = "CALL add_leave_request(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (CallableStatement stmt = connection.prepareCall(call)) {
                stmt.setString(1, leaveDetails.getSubject());
                if (leaveTypeId != null) {
                    stmt.setInt(2, leaveTypeId);
                } else {
                    stmt.setNull(2, Types.INTEGER);
                }
                stmt.setDate(3, dateFrom);
                stmt.setDate(4, dateTo);
                stmt.setInt(5, leaveDetails.getTotalDays());
                stmt.setString(6, leaveDetails.getReason());
                stmt.setString(7, leaveDetails.getStatus().name());
                stmt.setInt(8, leaveDetails.getEmpID());
                if (leaveDetails.getApproverId() > 0) {
                    stmt.setInt(9, leaveDetails.getApproverId());
                } else {
                    stmt.setNull(9, Types.INTEGER);
                }
                stmt.registerOutParameter(10, Types.INTEGER);

                stmt.execute();

                int generatedId = stmt.getInt(10);
                if (generatedId > 0) {
                    leaveDetails.setLeaveId(generatedId);
                    LOGGER.log(Level.INFO, "Leave request created with ID: " + generatedId);
                    return OperationResult.success("Leave request created successfully");
                } else {
                    return OperationResult.failure("Leave request was inserted, but no ID was returned");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating leave request", e);
            return OperationResult.failure("Failed to create leave request. Please try again.");
        }
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : (int) value;
    }

    public record LeaveRow(int id, int employeeId, String employeeName, String subject, int typeId,
            String type, int totalDays, String reason, LocalDate dateFrom, LocalDate dateTo,
            String status, Integer approverId, String approverName) {
    }

    public List<LeaveRow> findLeaveRequests(Integer empID, String status) {
        String sql = "SELECT * FROM public.vw_leave_requests WHERE (? IS NULL OR employee_id = ?) "
                + "AND (? IS NULL OR status = ?) ORDER BY date_from DESC, id DESC";
        List<LeaveRow> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (empID == null) {
                ps.setNull(1, Types.INTEGER);
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(1, empID);
                ps.setInt(2, empID);
            }
            ps.setString(3, status);
            ps.setString(4, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new LeaveRow(rs.getInt("id"), rs.getInt("employee_id"), rs.getString("employee_name"),
                            rs.getString("subject"), rs.getInt("type"), rs.getString("leave_type"),
                            rs.getInt("total_days"), rs.getString("reason"),
                            rs.getObject("date_from", LocalDate.class), rs.getObject("date_to", LocalDate.class),
                            rs.getString("status"), nullableInt(rs, "approver_id"),
                            rs.getString("approver_name")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("findLeaveRequests failed", e);
        }
        return rows;
    }

    public LeaveRow findLeaveRequest(int leaveId) {
        String sql = "SELECT * FROM public.vw_leave_requests WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, leaveId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new LeaveRow(rs.getInt("id"), rs.getInt("employee_id"), rs.getString("employee_name"),
                        rs.getString("subject"), rs.getInt("type"), rs.getString("leave_type"),
                        rs.getInt("total_days"), rs.getString("reason"),
                        rs.getObject("date_from", LocalDate.class), rs.getObject("date_to", LocalDate.class),
                        rs.getString("status"), nullableInt(rs, "approver_id"),
                        rs.getString("approver_name"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("findLeaveRequest failed", e);
        }
    }

    public void deleteLeaveRequest(int id) {
        if (connection != null) {
            String call = "CALL delete_leave_request(?)";
            try (CallableStatement stmt = connection.prepareCall(call)) {
                stmt.setInt(1, id);
                stmt.execute();
                LOGGER.log(Level.INFO, "Leave request deleted: " + id);
            } catch (SQLException e) {
                throw new DataAccessException("deleteLeaveRequest failed", e);
            }
        }
    }

    public void deleteLeaveRequestbyEmpID(int empID) {
        if (connection != null) {
            String call = "CALL delete_leave_requests_by_emp_id(?)";
            try (CallableStatement stmt = connection.prepareCall(call)) {
                stmt.setInt(1, empID);
                stmt.execute();
                LOGGER.log(Level.INFO, "Leave requests deleted for employee: " + empID);
            } catch (SQLException e) {
                throw new DataAccessException("deleteLeaveRequestbyEmpID failed", e);
            }
        }
    }

    public List<HR> getLeavesByEmployee(int empID) {
        List<HR> allLeaves = new ArrayList<>();
        if (connection != null) {
            String Query = "SELECT * FROM vw_leave_requests WHERE employee_id = ? ORDER BY id ASC";
            try (PreparedStatement preparedStatement = connection.prepareStatement(Query)) {
                preparedStatement.setInt(1, empID);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        HR leaveDetails = toLeaveDetails(resultSet);
                        allLeaves.add(leaveDetails);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving leaves for employee", e);
            }
        }
        return allLeaves;
    }

    public List<HR> getAllLeaveRequestByStatus(LeaveStatus leaveStatus) {
        List<HR> allLeaveRequest = new ArrayList<>();
        if (connection != null) {
            String Query = "SELECT * FROM vw_leave_requests WHERE status = ? ORDER BY id ASC";
            try (PreparedStatement preparedStatement = connection.prepareStatement(Query)) {
                preparedStatement.setString(1, leaveStatus.name());

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        HR leaveDetails = toLeaveDetails(resultSet);
                        allLeaveRequest.add(leaveDetails);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving leave requests by status", e);
            }
        }
        return allLeaveRequest;
    }

    public void updateLeaveRequestStatus(LeaveStatus leaveStatus, int id, int approverId) {
        if (connection != null) {
            String call = "CALL update_leave_request_status(?, ?, ?)";
            try (CallableStatement stmt = connection.prepareCall(call)) {
                stmt.setString(1, leaveStatus.name());
                stmt.setInt(2, approverId);
                stmt.setInt(3, id);
                stmt.execute();
                LOGGER.log(Level.INFO, "Leave request " + id + " updated to " + leaveStatus.name());
            } catch (SQLException e) {
                throw new DataAccessException("updateLeaveRequestStatus failed", e);
            }
        }
    }

    private HR toLeaveDetails(ResultSet resultSet) throws SQLException {
        HR leaveDetails = new HR();
        leaveDetails.setLeaveId(resultSet.getInt("id"));
        leaveDetails.setEmpID(resultSet.getInt("employee_id"));
        leaveDetails.setSubject(resultSet.getString("subject"));
        leaveDetails.setDateFrom(resultSet.getDate("date_from"));
        leaveDetails.setDateTo(resultSet.getDate("date_to"));
        leaveDetails.setTotalDays(resultSet.getInt("total_days"));
        leaveDetails.setReason(resultSet.getString("reason"));
        leaveDetails.setStatus(LeaveStatus.valueOf(resultSet.getString("status")));
        leaveDetails.setApproverId(resultSet.getInt("approver_id"));

        int leaveTypeId = resultSet.getInt("type");
        if (leaveTypeId > 0) {
            LeaveType type = getLeaveTypeById(leaveTypeId);
            leaveDetails.setLeaveType(type);
        }

        return leaveDetails;
    }
}
