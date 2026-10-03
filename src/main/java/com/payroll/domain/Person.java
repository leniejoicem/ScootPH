package com.payroll.domain;

import com.payroll.subdomain.EmployeePosition;
import com.payroll.subdomain.EmployeeStatus;
import com.payroll.validation.DepartmentGroups;
import jakarta.validation.constraints.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.payroll.validation.ValidationGroups;
import java.util.Calendar;

public abstract class Person {

    int empID;

    @NotBlank(message = "Last name is required", groups = ValidationGroups.Required.class)
    @Size(max = 50, message = "Last name must be at most 50 characters", groups = ValidationGroups.Format.class)
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ'\\-\\s.]+$", message = "Last name contains invalid characters", groups = ValidationGroups.Format.class)
    private String lastName;

    @NotBlank(message = "First name is required", groups = ValidationGroups.Required.class)
    @Size(max = 50, message = "First name must be at most 50 characters", groups = ValidationGroups.Format.class)
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ'\\-\\s.]+$", message = "First name contains invalid characters", groups = ValidationGroups.Format.class)
    private String firstName;

    @NotNull(message = "Birthday is required", groups = ValidationGroups.Required.class)
    @Past(message = "Birthday must be in the past", groups = ValidationGroups.Format.class)
    private Date empBirthday;

    @AssertTrue(message = "Employee must be at least 18 years old", groups = ValidationGroups.Format.class)
    public boolean isAdult() {
        if (empBirthday == null) return true;
        Calendar today = Calendar.getInstance();
        Calendar dob   = Calendar.getInstance();
        dob.setTime(empBirthday);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--;
        return age >= 18;
    }

    @NotBlank(message = "Address is required", groups = ValidationGroups.Required.class)
    @Size(max = 200, message = "Address cannot exceed 200 characters", groups = ValidationGroups.Format.class)
    private String empAddress;

    @NotBlank(message = "Phone number is required", groups = ValidationGroups.Required.class)
    @Pattern(
        regexp = "^[0-9+\\-\\s()]{10,20}$",
        message = "Invalid phone number format",
        groups = ValidationGroups.Format.class
    )
    private String empPhoneNumber;

    @NotNull(message = "Employee status is required", groups = ValidationGroups.Required.class)
    private EmployeeStatus empStatus;

    @NotNull(message = "Employee position is required", groups = ValidationGroups.Required.class)
    private EmployeePosition empPosition;

    @NotNull(message = "Immediate supervisor is required", groups = ValidationGroups.Required.class)
    private Person empImmediateSupervisor;

    @NotBlank(
    message = "SSS number is required",
    groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class }
    )
    @Pattern(
        regexp = "^\\d{2}-\\d{7}-\\d{1}$",
        message = "SSS format must be XX-XXXXXXX-X",
        groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class }
    )
    private String empSSS;

    @NotBlank(
    message = "TIN is required",
    groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class }
    )
    @Pattern(
        regexp = "^\\d{3}-\\d{3}-\\d{3}(-\\d{3})?$",
        message = "TIN format must be XXX-XXX-XXX",
        groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class }
    )
    private String empTIN;

    @NotNull(
    message = "PhilHealth number is required",
    groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class }
    )
    @Digits(integer = 12, fraction = 0, message = "PhilHealth must be 12 digits", groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class })
    @Min(value = 100_000_000_000L, message = "PhilHealth must be 12 digits", groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class })
    @Max(value = 999_999_999_999L, message = "PhilHealth must be 12 digits", groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class })
    private Long empPhilHealth;

    @NotNull(
    message = "Pag-IBIG number is required",
    groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class }
    )
    @Digits(integer = 12, fraction = 0, message = "Pag-IBIG must be 12 digits", groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class })
    @Min(value = 100_000_000_000L, message = "Pag-IBIG must be 12 digits", groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class })
    @Max(value = 999_999_999_999L, message = "Pag-IBIG must be 12 digits", groups = { DepartmentGroups.HR.class, DepartmentGroups.Finance.class })
    private Long empPagibig;

    @PositiveOrZero(
        message = "Basic salary cannot be negative",
        groups = {ValidationGroups.Format.class, DepartmentGroups.HR.class}
    )
    @DecimalMin(
        value = "20000.0",
        message = "Basic salary must be at least PHP 20,000",
        groups = {DepartmentGroups.Finance.class}
    )
    private double empBasicSalary;

    @PositiveOrZero(
        message = "Rice allowance cannot be negative",
        groups = {ValidationGroups.Format.class, DepartmentGroups.HR.class}
    )
    @DecimalMin(
        value = "1500.0",
        message = "Rice allowance must be at least PHP 1,500",
        groups = {DepartmentGroups.Finance.class}
    )
    private double empRice;

    @PositiveOrZero(
        message = "Phone allowance cannot be negative",
        groups = {ValidationGroups.Format.class, DepartmentGroups.HR.class}
    )
    @DecimalMin(
        value = "500.0",
        message = "Phone allowance must be at least PHP 500",
        groups = {DepartmentGroups.Finance.class}
    )
    private double empPhone;

    @PositiveOrZero(
        message = "Clothing allowance cannot be negative",
        groups = {ValidationGroups.Format.class, DepartmentGroups.HR.class}
    )
    @DecimalMin(
        value = "500.0",
        message = "Clothing allowance must be at least PHP 500",
        groups = {DepartmentGroups.Finance.class}
    )
    private double empClothing;

    @PositiveOrZero(
        message = "Monthly rate cannot be negative",
        groups = {ValidationGroups.Format.class, DepartmentGroups.HR.class}
    )
    @DecimalMin(
        value = "1000.0",
        message = "Semi-monthly rate must be at least PHP 1,000",
        groups = {DepartmentGroups.Finance.class}
    )
    private double empMonthlyRate;

    @PositiveOrZero(
        message = "Hourly rate cannot be negative",
        groups = {ValidationGroups.Format.class, DepartmentGroups.HR.class}
    )
    @DecimalMin(
        value = "70.0",
        message = "Hourly rate must be at least PHP 70",
        groups = {DepartmentGroups.Finance.class}
    )
    private double empHourlyRate;

    public Person(int empID, String lastName, String firstName, String empAddress, Date empBirthday,
                  String empPhoneNumber, String empSSS, String empTIN, long empPhilHealth,
                  long empPagibig, Person empImmediateSupervisor, EmployeeStatus empStatus,
                  EmployeePosition empPosition, double empBasicSalary, double empRice,
                  double empPhone, double empClothing, double empMonthlyRate, double empHourlyRate) {
        this.empID = empID;
        this.lastName = lastName;
        this.firstName = firstName;
        this.empAddress = empAddress;
        this.empBirthday = empBirthday;
        this.empPhoneNumber = empPhoneNumber;
        this.empSSS = empSSS;
        this.empTIN = empTIN;
        this.empPhilHealth = empPhilHealth;
        this.empPagibig = empPagibig;
        this.empImmediateSupervisor = empImmediateSupervisor;
        this.empStatus = empStatus;
        this.empPosition = empPosition;
        this.empBasicSalary = empBasicSalary;
        this.empRice = empRice;
        this.empPhone = empPhone;
        this.empClothing = empClothing;
        this.empMonthlyRate = empMonthlyRate;
        this.empHourlyRate = empHourlyRate;
    }

    public int getEmpID() {
        return empID;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getEmpAddress() {
        return empAddress;
    }

    public Date getEmpBirthday() {
        return empBirthday;
    }

    public String getEmpPhoneNumber() {
        return empPhoneNumber;
    }

    public String getEmpSSS() {
        return empSSS;
    }

    public String getEmpTIN() {
        return empTIN;
    }

    public long getEmpPhilHealth() {
        return empPhilHealth;
    }

    public long getEmpPagibig() {
        return empPagibig;
    }

    public Person getEmpImmediateSupervisor() {
        return empImmediateSupervisor;
    }

    public EmployeeStatus getEmpStatus() {
        return empStatus;
    }

    public EmployeePosition getEmpPosition() {
        return empPosition;
    }

    public void setEmpID(int empID) {
        this.empID = empID;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setEmpAddress(String empAddress) {
        this.empAddress = empAddress;
    }

    public void setEmpBirthday(Date empBirthday) {
        this.empBirthday = empBirthday;
    }

    public void setEmpPhoneNumber(String empPhoneNumber) {
        this.empPhoneNumber = empPhoneNumber;
    }

    public void setEmpSSS(String empSSS) {
        this.empSSS = empSSS;
    }

    public void setEmpTIN(String empTIN) {
        this.empTIN = empTIN;
    }

    public void setEmpPhilHealth(long empPhilHealth) {
        this.empPhilHealth = empPhilHealth;
    }

    public void setEmpPagibig(long empPagibig) {
        this.empPagibig = empPagibig;
    }

    public void setEmpImmediateSupervisor(Person empImmediateSupervisor) {
        this.empImmediateSupervisor = empImmediateSupervisor;
    }

    public void setEmpStatus(EmployeeStatus empStatus) {
        this.empStatus = empStatus;
    }

    public void setEmpPosition(EmployeePosition empPosition) {
        this.empPosition = empPosition;
    }

    public double getEmpBasicSalary() {
        return empBasicSalary;
    }

    public double getEmpRice() {
        return empRice;
    }

    public double getEmpPhone() {
        return empPhone;
    }

    public double getEmpClothing() {
        return empClothing;
    }

    public double getEmpMonthlyRate() {
        return empMonthlyRate;
    }

    public double getEmpHourlyRate() {
        return empHourlyRate;
    }

    public void setEmpBasicSalary(double empBasicSalary) {
        this.empBasicSalary = empBasicSalary;
    }

    public void setEmpRice(double empRice) {
        this.empRice = empRice;
    }

    public void setEmpPhone(double empPhone) {
        this.empPhone = empPhone;
    }

    public void setEmpClothing(double empClothing) {
        this.empClothing = empClothing;
    }

    public void setEmpMonthlyRate(double empMonthlyRate) {
        this.empMonthlyRate = empMonthlyRate;
    }

    public void setEmpHourlyRate(double empHourlyRate) {
        this.empHourlyRate = empHourlyRate;
    }

    public String getFormattedName() {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        return (first + " " + last).trim();
    }

    public String getFormattedBirthday(){
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return this.empBirthday != null ? formatter.format(this.empBirthday) : null;
    }
}
