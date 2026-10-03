package com.payroll.service;

import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

public class ReportService {

    public enum Report {
        PAYSLIP("report/Payslip.jrxml"),
        TIMECARD("report/Timecard.jrxml"),
        EMPLOYEES("report/EmployeeReport.jrxml"),
        EMPLOYEE_PROFILE("report/EmployeeProfileReport.jrxml"),
        ROLES("report/RoleReport.jrxml");

        final String resource;

        Report(String resource) {
            this.resource = resource;
        }
    }

    private static final Map<Report, JasperReport> COMPILED = new ConcurrentHashMap<>();

    private final Connection connection;

    public ReportService(Connection connection) {
        this.connection = connection;
    }

    public JasperPrint payslip(int payrollId) {
        return fill(Report.PAYSLIP, Map.of("payroll_id", payrollId));
    }

    public JasperPrint timecard(int employeeId, YearMonth month) {
        return fill(Report.TIMECARD, Map.of("employee_id", employeeId,
                "month", month.getMonthValue(), "year", month.getYear()));
    }

    public JasperPrint employeeMasterlist() {
        return fill(Report.EMPLOYEES, Map.of());
    }

    public JasperPrint employeeProfile(int employeeId) {
        return fill(Report.EMPLOYEE_PROFILE, Map.of("employee_id", employeeId));
    }

    public JasperPrint roleMasterlist() {
        return fill(Report.ROLES, Map.of());
    }

    JasperPrint fill(Report report, Map<String, Object> values) {
        try {
            Map<String, Object> parameters = new HashMap<>(values);
            URL logo = getClass().getClassLoader().getResource("report/scootph-logo.png");
            parameters.put("LogoPath", logo);
            JasperPrint print = JasperFillManager.fillReport(compiled(report), parameters, connection);
            if (print.getPages().isEmpty()) {
                throw new ServiceException("There is no data for this report.");
            }
            return print;
        } catch (JRException e) {
            throw new ServiceException("Could not create the report: " + e.getMessage(), e);
        }
    }

    static JasperReport compiled(Report report) throws JRException {
        JasperReport cached = COMPILED.get(report);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = ReportService.class.getClassLoader().getResourceAsStream(report.resource)) {
            if (in == null) {
                throw new JRException("Missing report design " + report.resource);
            }
            JasperReport compiled = JasperCompileManager.compileReport(in);
            COMPILED.put(report, compiled);
            return compiled;
        } catch (java.io.IOException e) {
            throw new JRException(e);
        }
    }
}
