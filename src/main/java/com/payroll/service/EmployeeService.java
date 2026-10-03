package com.payroll.service;

import com.payroll.DAO.EmployeeDAO;
import com.payroll.DAO.HRDAO;
import com.payroll.DAO.ITDAO;
import com.payroll.domain.Employee;
import com.payroll.domain.IT;
import com.payroll.domain.LeaveBalance;
import com.payroll.domain.Person;
import com.payroll.subdomain.EmployeePosition;
import com.payroll.subdomain.EmployeeStatus;
import com.payroll.validation.AccountGroups;
import com.payroll.validation.DepartmentGroups;
import com.payroll.validation.OperationResult;
import com.payroll.validation.SanitizationService;
import com.payroll.validation.ValidationGroups;
import com.payroll.validation.ValidationService;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeService {

    public record EmployeeForm(
            Integer employeeId,
            String lastName,
            String firstName,
            LocalDate birthday,
            String address,
            String phone,
            String sss,
            String tin,
            String philHealth,
            String pagIbig,
            Integer statusId,
            Integer positionId,
            Integer supervisorId,
            String basicSalary,
            String hourlyRate,
            String semiMonthlyRate,
            String riceSubsidy,
            String phoneAllowance,
            String clothingAllowance,
            String username,
            char[] password) {
    }

    private final Connection connection;
    private final HRDAO employees;
    private final ITDAO accounts;
    private final EmployeeDAO records;

    public EmployeeService(Connection connection, HRDAO employees, ITDAO accounts, EmployeeDAO records) {
        this.connection = connection;
        this.employees = employees;
        this.accounts = accounts;
        this.records = records;
    }

    public List<Person> directory() {
        return employees.getEmployeeDirectory();
    }

    public Person get(int employeeId) {
        Person person = employees.getByEmpID(employeeId);
        if (person == null) {
            throw new ServiceException("Employee " + employeeId + " was not found.");
        }
        return person;
    }

    public IT account(int employeeId) {
        return accounts.getByEmpID(employeeId);
    }

    public List<EmployeePosition> positions() {
        return employees.getAllPosition();
    }

    public List<EmployeeStatus> statuses() {
        return employees.getAllStatuses();
    }

    public int signUp(EmployeeForm form) {
        return create(form, false);
    }

    public int add(EmployeeForm form) {
        return create(form, true);
    }

    private int create(EmployeeForm form, boolean byHr) {
        Map<String, String> errors = new LinkedHashMap<>();
        Person person = toPerson(form, byHr, errors);
        IT account = new IT();
        account.setEmpUserName(trim(form.username()));
        account.setEmpPassword(form.password() == null ? "" : new String(form.password()));

        errors.putAll(ValidationService.fieldErrors(person, ValidationGroups.Required.class,
                ValidationGroups.Format.class, DepartmentGroups.HR.class));
        errors.putAll(ValidationService.fieldErrors(account, AccountGroups.Required.class, AccountGroups.Format.class));
        moveAgeError(errors);
        if (!errors.containsKey("empUserName") && accounts.isUsernameTaken(account.getEmpUserName(), null)) {
            errors.put("empUserName", "This username is already taken");
        }
        if (!errors.isEmpty()) {
            throw ServiceException.invalid(errors);
        }
        if (employees.isDuplicateEmployee(person)) {
            throw new ServiceException("This employee already exists.");
        }

        return Transactions.inTransaction(connection, () -> {
            employees.addEmployeeDetails(person);
            if (person.getEmpID() <= 0) {
                throw new ServiceException("Could not create the employee record.");
            }
            accounts.saveUserAccount(account, person);
            LeaveBalance balance = new LeaveBalance();
            balance.setEmpID(person.getEmpID());
            OperationResult saved = records.saveLeaveBalance(balance);
            if (!saved.isSuccess()) {
                throw new ServiceException(saved.getFormattedErrors());
            }
            return person.getEmpID();
        });
    }

    public void update(EmployeeForm form) {
        if (form.employeeId() == null) {
            throw new ServiceException("Select an employee to update.");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        Person person = toPerson(form, true, errors);
        person.setEmpID(form.employeeId());
        errors.putAll(ValidationService.fieldErrors(person, ValidationGroups.Required.class,
                ValidationGroups.Format.class, DepartmentGroups.HR.class));

        String username = trim(form.username());
        boolean changePassword = form.password() != null && form.password().length > 0;
        IT account = new IT();
        account.setEmpUserName(username);
        account.setEmpPassword(changePassword ? new String(form.password()) : "Placeholder1!");
        errors.putAll(ValidationService.fieldErrors(account, AccountGroups.Required.class, AccountGroups.Format.class));
        moveAgeError(errors);
        if (!errors.containsKey("empUserName") && accounts.isUsernameTaken(username, form.employeeId())) {
            errors.put("empUserName", "This username is already taken");
        }
        if (form.supervisorId() != null && form.supervisorId().equals(form.employeeId())) {
            errors.put("empImmediateSupervisor", "An employee cannot supervise themself");
        }
        if (!errors.isEmpty()) {
            throw ServiceException.invalid(errors);
        }
        get(form.employeeId());

        Transactions.inTransaction(connection, () -> {
            employees.updateEmployeeDetails(person);
            if (accounts.getByEmpID(form.employeeId()) != null) {
                if (changePassword) {
                    account.setEmpID(form.employeeId());
                    accounts.updateEmployeeCredentials(account);
                } else {
                    accounts.updateUsername(form.employeeId(), username);
                }
            }
        });
    }

    public void delete(int employeeId, int actingEmployeeId) {
        if (employeeId == actingEmployeeId) {
            throw new ServiceException("You cannot delete your own record.");
        }
        Person person = get(employeeId);
        int team = employees.countSubordinates(employeeId);
        if (team > 0) {
            throw new ServiceException(person.getFormattedName() + " supervises " + team
                    + (team == 1 ? " employee" : " employees") + ". Assign them a new supervisor first.");
        }
        Transactions.inTransaction(connection, () -> {
            records.deleteLeaveBalance(employeeId);
            records.deleteLeaveRequestbyEmpID(employeeId);
            records.deleteAttendanceRecords(employeeId);
            accounts.deleteEmpAccount(employeeId);
            if (!employees.deleteEmployeeDetails(employeeId)) {
                throw new ServiceException("Could not delete the employee record.");
            }
        });
    }

    private Person toPerson(EmployeeForm form, boolean withCompensation, Map<String, String> errors) {
        Person person = new Employee();
        person.setLastName(SanitizationService.sanitizePlainText(trim(form.lastName())));
        person.setFirstName(SanitizationService.sanitizePlainText(trim(form.firstName())));
        person.setEmpBirthday(form.birthday() == null ? null : java.sql.Date.valueOf(form.birthday()));
        person.setEmpAddress(SanitizationService.sanitizePlainText(trim(form.address())));
        person.setEmpPhoneNumber(trim(form.phone()));
        person.setEmpSSS(trim(form.sss()));
        person.setEmpTIN(trim(form.tin()));
        person.setEmpPhilHealth(parseId(form.philHealth(), "empPhilHealth", "PhilHealth", errors));
        person.setEmpPagibig(parseId(form.pagIbig(), "empPagibig", "Pag-IBIG", errors));
        if (form.statusId() != null) {
            person.setEmpStatus(employees.getStatusById(form.statusId()));
        }
        if (form.positionId() != null) {
            person.setEmpPosition(employees.getPositionById(form.positionId()));
        }
        if (form.supervisorId() != null) {
            person.setEmpImmediateSupervisor(employees.getByEmpID(form.supervisorId(), false));
        }
        if (withCompensation) {
            person.setEmpBasicSalary(parseAmount(form.basicSalary(), "empBasicSalary", errors));
            person.setEmpHourlyRate(parseAmount(form.hourlyRate(), "empHourlyRate", errors));
            person.setEmpMonthlyRate(parseAmount(form.semiMonthlyRate(), "empMonthlyRate", errors));
            person.setEmpRice(parseAmount(form.riceSubsidy(), "empRice", errors));
            person.setEmpPhone(parseAmount(form.phoneAllowance(), "empPhone", errors));
            person.setEmpClothing(parseAmount(form.clothingAllowance(), "empClothing", errors));
        }
        return person;
    }

    private static void moveAgeError(Map<String, String> errors) {
        String age = errors.remove("adult");
        if (age != null) {
            errors.putIfAbsent("empBirthday", age);
        }
    }

    static double parseAmount(String text, String field, Map<String, String> errors) {
        String value = trim(text).replace(",", "").replace("₱", "").replace("PHP", "").trim();
        if (value.isEmpty()) {
            return 0;
        }
        try {
            double amount = Double.parseDouble(value);
            if (Double.isNaN(amount) || Double.isInfinite(amount)) {
                throw new NumberFormatException();
            }
            return amount;
        } catch (NumberFormatException e) {
            errors.put(field, "Enter an amount, e.g. 25000.00");
            return 0;
        }
    }

    static long parseId(String text, String field, String label, Map<String, String> errors) {
        String digits = trim(text).replace("-", "").replace(" ", "");
        if (digits.isEmpty()) {
            return 0;
        }
        if (!digits.matches("\\d{1,18}")) {
            errors.put(field, label + " must contain digits only");
            return 0;
        }
        return Long.parseLong(digits);
    }

    static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
