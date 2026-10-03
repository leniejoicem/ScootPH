package com.payroll.service;

import com.payroll.DAO.FinanceDAO;
import com.payroll.DAO.HRDAO;
import com.payroll.domain.Employee;
import com.payroll.domain.Finance;
import com.payroll.domain.Person;
import com.payroll.validation.DepartmentGroups;
import com.payroll.validation.ValidationService;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PayrollService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    public record CompensationForm(int employeeId, String sss, String tin, String philHealth, String pagIbig,
            String basicSalary, String hourlyRate, String semiMonthlyRate, String riceSubsidy,
            String phoneAllowance, String clothingAllowance) {
    }

    private final HRDAO employees;
    private final FinanceDAO finance;
    private final AttendanceService attendance;
    private final Clock clock;

    public PayrollService(HRDAO employees, FinanceDAO finance, AttendanceService attendance) {
        this(employees, finance, attendance, Clock.systemDefaultZone());
    }

    public PayrollService(HRDAO employees, FinanceDAO finance, AttendanceService attendance, Clock clock) {
        this.employees = employees;
        this.finance = finance;
        this.attendance = attendance;
        this.clock = clock;
    }

    public YearMonth latestPayableMonth() {
        return YearMonth.now(clock).minusMonths(1);
    }

    public Payslip payslip(int employeeId, YearMonth period) {
        if (!period.isBefore(YearMonth.now(clock))) {
            throw new ServiceException("Payroll for " + period.format(MONTH)
                    + " will be available after the month ends.");
        }
        Person employee = employees.getByEmpID(employeeId);
        if (employee == null) {
            throw new ServiceException("Employee " + employeeId + " was not found.");
        }
        List<Employee> days = attendance.completedDays(employeeId, period);
        if (days.isEmpty()) {
            throw new ServiceException("No attendance was recorded for " + employee.getFormattedName()
                    + " in " + period.format(MONTH) + ".");
        }
        String position = employee.getEmpPosition() != null ? employee.getEmpPosition().getPosition() : "";
        return PayrollCalculator.compute(employee, position, period, days, finance::calculateSssContribution);
    }

    public int save(Payslip payslip) {
        int existing = finance.findSavedPayslip(payslip.employeeId(), payslip.period().atDay(1), payslip.netPay());
        if (existing > 0) {
            return existing;
        }
        Finance saved = finance.savePayrollReport(payslip.toPayrollRecord());
        if (saved == null || saved.getPayrollId() <= 0) {
            throw new ServiceException("Could not save the payslip. Please try again.");
        }
        return saved.getPayrollId();
    }

    public List<Person> compensationDirectory() {
        return employees.getEmployeeDirectory();
    }

    public void updateCompensation(CompensationForm form) {
        Person person = employees.getByEmpID(form.employeeId());
        if (person == null) {
            throw new ServiceException("Employee " + form.employeeId() + " was not found.");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        person.setEmpSSS(EmployeeService.trim(form.sss()));
        person.setEmpTIN(EmployeeService.trim(form.tin()));
        person.setEmpPhilHealth(EmployeeService.parseId(form.philHealth(), "empPhilHealth", "PhilHealth", errors));
        person.setEmpPagibig(EmployeeService.parseId(form.pagIbig(), "empPagibig", "Pag-IBIG", errors));
        person.setEmpBasicSalary(EmployeeService.parseAmount(form.basicSalary(), "empBasicSalary", errors));
        person.setEmpHourlyRate(EmployeeService.parseAmount(form.hourlyRate(), "empHourlyRate", errors));
        person.setEmpMonthlyRate(EmployeeService.parseAmount(form.semiMonthlyRate(), "empMonthlyRate", errors));
        person.setEmpRice(EmployeeService.parseAmount(form.riceSubsidy(), "empRice", errors));
        person.setEmpPhone(EmployeeService.parseAmount(form.phoneAllowance(), "empPhone", errors));
        person.setEmpClothing(EmployeeService.parseAmount(form.clothingAllowance(), "empClothing", errors));
        ValidationService.fieldErrors(person, DepartmentGroups.Finance.class).forEach(errors::putIfAbsent);
        if (!errors.isEmpty()) {
            throw ServiceException.invalid(errors);
        }
        finance.updatePayrollDetails(person);
    }

    public static String monthLabel(YearMonth month) {
        return month.format(MONTH);
    }
}
