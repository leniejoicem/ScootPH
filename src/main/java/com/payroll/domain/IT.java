package com.payroll.domain;
import com.payroll.subdomain.EmployeePosition;
import com.payroll.subdomain.EmployeeStatus;
import com.payroll.subdomain.UserRole;
import com.payroll.validation.AccountGroups;
import com.payroll.validation.DepartmentGroups;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.AssertTrue;
import com.payroll.validation.ValidationGroups;

import java.util.Date;

public class IT extends Person{
    private int accountID;
    @NotBlank(message = "Username is required", groups = AccountGroups.Required.class)
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters", groups = AccountGroups.Format.class)
    private String empUserName;

    @NotBlank(message = "Password is required", groups = AccountGroups.Required.class)
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-+=<>?]).{8,}$",
        message = "Password must include uppercase, lowercase, number, special character and should be 8 characters",
        groups = AccountGroups.Format.class
    )
    private String empPassword;
    @Valid
    private Person empDetails;
    @Valid
    @NotNull(message = "User role is required", groups = DepartmentGroups.IT.class)
    private UserRole userRole;
    @Valid
    private HR leaveDetail;

    private String tfa;

    public IT() {
        super(0, "", "", "", null, "", "", "", 0, 0, null, null, null,0,0,0,0,0,0);
    }

    public IT(int empID, String lastName, String firstName, String empAddress, Date empBirthday,
              String empPhoneNumber, String empSSS, String empTIN, long empPhilHealth,
              long empPagibig, Person empImmediateSupervisor, EmployeeStatus empStatus,
              EmployeePosition empPosition, double empBasicSalary, double empRice,
              double empPhone, double empClothing, double empMonthlyRate, double empHourlyRate,
              int accountID, String empUserName, String empPassword,
              Person empDetails, Finance payrollDetails, UserRole userRole, HR leaveDetail) {
        super(empID, lastName, firstName, empAddress, empBirthday, empPhoneNumber, empSSS,
              empTIN, empPhilHealth, empPagibig, empImmediateSupervisor, empStatus, empPosition,
              empBasicSalary,empRice,empPhone,empClothing, empMonthlyRate,empHourlyRate);

        this.accountID = accountID;
        this.empUserName = empUserName;
        this.empPassword = empPassword;
        this.empDetails = empDetails;
        this.userRole = userRole;
        this.leaveDetail = leaveDetail;
    }

    public int getAccountID() {
        return accountID; }

    public void setAccountID(int accountID) {
        this.accountID = accountID; }

    public String getEmpUserName() {
        return empUserName; }

    public void setEmpUserName(String empUserName) {
        this.empUserName = empUserName; }

    public String getEmpPassword() {
        return empPassword; }

    public void setEmpPassword(String empPassword) {
        this.empPassword = empPassword; }

    public Person getEmpDetails() {
        return empDetails; }

    public void setEmpDetails(Person empDetails) {
        this.empDetails = empDetails; }

    public UserRole getUserRole() {
        return userRole; }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole; }

    public HR getLeaveDetail() {
        return leaveDetail; }

    public void setLeaveDetail(HR leaveDetail) {
        this.leaveDetail = leaveDetail; }

    public String getTfa() {
        return tfa;
    }

    public void setTfa(String tfa) {
        this.tfa = tfa;
    }

    @AssertTrue(message = "Password must not be the same as the username")
    public boolean isPasswordDifferentFromUsername() {
        if (empUserName == null || empPassword == null) return true;
        return !empPassword.equalsIgnoreCase(empUserName);
    }
}
