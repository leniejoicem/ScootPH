package com.payroll.service;

import com.payroll.domain.Finance;
import java.time.YearMonth;

public record Payslip(
        int employeeId,
        String employeeName,
        String position,
        YearMonth period,
        int daysWorked,
        long secondsWorked,
        double hourlyRate,
        double basicSalary,
        double riceSubsidy,
        double phoneAllowance,
        double clothingAllowance,
        double totalAllowance,
        double grossPay,
        double sss,
        double philHealth,
        double pagIbig,
        double totalContributions,
        double taxableIncome,
        double withholdingTax,
        double netPay) {

    public double hoursWorked() {
        return secondsWorked / 3600.0;
    }

    public String hoursWorkedLabel() {
        return String.format("%d:%02d", secondsWorked / 3600, (secondsWorked % 3600) / 60);
    }

    public Finance toPayrollRecord() {
        Finance record = new Finance();
        record.setPayEmpId(employeeId);
        record.setPayrollPeriodStart(java.sql.Date.valueOf(period.atDay(1)));
        record.setPayrollPeriodEnd(java.sql.Date.valueOf(period.atEndOfMonth()));
        record.setPayFullName(employeeName);
        record.setPayPosition(position);
        record.setPayHourlyRate(hourlyRate);
        record.setPayNumberOfHoursWorked(Math.round(hoursWorked() * 100.0) / 100.0);
        record.setPayComputedSalary(basicSalary);
        record.setPayRiceAllowance(riceSubsidy);
        record.setPayPhoneAllowance(phoneAllowance);
        record.setPayClothingAllowance(clothingAllowance);
        record.setPayTotalAllowance(totalAllowance);
        record.setPaySssContri(sss);
        record.setPayPhealthContri(philHealth);
        record.setPayPagibigContri(pagIbig);
        record.setPayTotalContributions(totalContributions);
        record.setPayWithholdingTax(withholdingTax);
        record.setPayNetPay(netPay);
        return record;
    }
}
