package com.payroll.domain;

import java.util.Date;
import com.payroll.subdomain.EmployeePosition;
import com.payroll.subdomain.EmployeeStatus;
import com.payroll.domain.Finance;

public class Finance extends Person {

    private int payrollId;
    private Date payrollPeriodStart;
    private Date payrollPeriodEnd;
    private double payNumberOfHoursWorked;
    private double payComputedSalary;
    private double payTotalContributions;
    private double payNetPay;
    private int payEmpId;
    private double payTotalAllowance;
    private double paySssContri;
    private double payPhealthContri;
    private double payPagibigContri;
    private double payWithholdingTax;
    private double payHourlyRate;
    private String payFullName;
    private String payPosition;
    private double payRiceAllowance;
    private double payPhoneAllowance;
    private double payClothingAllowance;

    public Finance() {
        super(0, "", "", "", null, "", "", "", 0, 0, null, null, null,0,0,0,0,0,0);
    }

    public Finance(int empID, String lastName, String firstName, String empAddress, Date empBirthday,
                   String empPhoneNumber, String empSSS, String empTIN, long empPhilHealth,
                   long empPagibig, Person empImmediateSupervisor, EmployeeStatus empStatus,
                   EmployeePosition empPosition, double empBasicSalary, double empRice,
                   double empPhone, double empClothing, double empMonthlyRate, double empHourlyRate) {
        super(empID, lastName, firstName, empAddress, empBirthday, empPhoneNumber, empSSS,
              empTIN, empPhilHealth, empPagibig, empImmediateSupervisor, empStatus, empPosition,
              empBasicSalary,empRice,empPhone,empClothing, empMonthlyRate,empHourlyRate);
    }

    @Override
    public int getEmpID() {
        return empID;
    }

    @Override
    public void setEmpID(int empID) {
        this.empID = empID;
    }

        public int getPayrollId() {
        return payrollId;
    }

    public Date getPayrollPeriodStart() {
        return payrollPeriodStart;
    }

    public Date getPayrollPeriodEnd() {
        return payrollPeriodEnd;
    }

    public double getPayNumberOfHoursWorked() {
        return payNumberOfHoursWorked;
    }

    public double getPayComputedSalary() {
        return payComputedSalary;
    }

    public double getPayTotalContributions() {
        return payTotalContributions;
    }

    public double getPayNetPay() {
        return payNetPay;
    }

    public int getPayEmpId() {
        return payEmpId;
    }

    public double getPayTotalAllowance() {
        return payTotalAllowance;
    }

    public double getPaySssContri() {
        return paySssContri;
    }

    public double getPayPhealthContri() {
        return payPhealthContri;
    }

    public double getPayPagibigContri() {
        return payPagibigContri;
    }

    public double getPayWithholdingTax() {
        return payWithholdingTax;
    }

    public double getPayHourlyRate() {
        return payHourlyRate;
    }

    public String getPayFullName() {
        return payFullName;
    }

    public String getPayPosition() {
        return payPosition;
    }

    public double getPayRiceAllowance() {
        return payRiceAllowance;
    }

    public double getPayPhoneAllowance() {
        return payPhoneAllowance;
    }

    public double getPayClothingAllowance() {
        return payClothingAllowance;
    }

        public void setPayrollId(int payrollId) {
        this.payrollId = payrollId;
    }

    public void setPayrollPeriodStart(Date payrollPeriodStart) {
        this.payrollPeriodStart = payrollPeriodStart;
    }

    public void setPayrollPeriodEnd(Date payrollPeriodEnd) {
        this.payrollPeriodEnd = payrollPeriodEnd;
    }

    public void setPayNumberOfHoursWorked(double payNumberOfHoursWorked) {
        this.payNumberOfHoursWorked = payNumberOfHoursWorked;
    }

    public void setPayComputedSalary(double payComputedSalary) {
        this.payComputedSalary = payComputedSalary;
    }

    public void setPayTotalContributions(double payTotalContributions) {
        this.payTotalContributions = payTotalContributions;
    }

    public void setPayNetPay(double payNetPay) {
        this.payNetPay = payNetPay;
    }

    public void setPayEmpId(int payEmpId) {
        this.payEmpId = payEmpId;
    }

    public void setPayTotalAllowance(double payTotalAllowance) {
        this.payTotalAllowance = payTotalAllowance;
    }

    public void setPaySssContri(double paySssContri) {
        this.paySssContri = paySssContri;
    }

    public void setPayPhealthContri(double payPhealthContri) {
        this.payPhealthContri = payPhealthContri;
    }

    public void setPayPagibigContri(double payPagibigContri) {
        this.payPagibigContri = payPagibigContri;
    }

    public void setPayWithholdingTax(double payWithholdingTax) {
        this.payWithholdingTax = payWithholdingTax;
    }

    public void setPayHourlyRate(double payHourlyRate) {
        this.payHourlyRate = payHourlyRate;
    }

    public void setPayFullName(String payFullName) {
        this.payFullName = payFullName;
    }

    public void setPayPosition(String payPosition) {
        this.payPosition = payPosition;
    }

    public void setPayRiceAllowance(double payRiceAllowance) {
        this.payRiceAllowance = payRiceAllowance;
    }

    public void setPayPhoneAllowance(double payPhoneAllowance) {
        this.payPhoneAllowance = payPhoneAllowance;
    }

    public void setPayClothingAllowance(double payClothingAllowance) {
        this.payClothingAllowance = payClothingAllowance;
    }
}
