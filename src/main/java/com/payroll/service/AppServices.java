package com.payroll.service;

import com.payroll.DAO.EmployeeDAO;
import com.payroll.DAO.FinanceDAO;
import com.payroll.DAO.HRDAO;
import com.payroll.DAO.ITDAO;
import com.payroll.util.ResilientConnection;
import com.payroll.util.SchemaMigrator;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppServices implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(AppServices.class.getName());

    private final Connection connection;
    private final AuthService auth;
    private final EmployeeService employees;
    private final AttendanceService attendance;
    private final LeaveService leave;
    private final PayrollService payroll;
    private final AccessService access;
    private final ReportService reports;

    public AppServices(Connection connection, Clock clock) {
        this.connection = connection;
        HRDAO hrDao = new HRDAO(connection);
        ITDAO itDao = new ITDAO(connection);
        EmployeeDAO employeeDao = new EmployeeDAO(connection);
        FinanceDAO financeDao = new FinanceDAO(connection);
        this.auth = new AuthService(itDao, clock);
        this.employees = new EmployeeService(connection, hrDao, itDao, employeeDao);
        this.attendance = new AttendanceService(employeeDao, clock);
        this.leave = new LeaveService(connection, employeeDao, clock);
        this.payroll = new PayrollService(hrDao, financeDao, attendance, clock);
        this.access = new AccessService(connection, itDao);
        this.reports = new ReportService(connection);
    }

    public static AppServices connect() throws SQLException {
        Connection connection = ResilientConnection.open();
        try {
            SchemaMigrator.migrate(connection);
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return new AppServices(connection, Clock.systemDefaultZone());
    }

    public AuthService auth() {
        return auth;
    }

    public EmployeeService employees() {
        return employees;
    }

    public AttendanceService attendance() {
        return attendance;
    }

    public LeaveService leave() {
        return leave;
    }

    public PayrollService payroll() {
        return payroll;
    }

    public AccessService access() {
        return access;
    }

    public ReportService reports() {
        return reports;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Closing connection", e);
        }
    }
}
