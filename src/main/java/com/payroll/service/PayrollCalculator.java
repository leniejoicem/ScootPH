package com.payroll.service;

import com.payroll.domain.Employee;
import com.payroll.domain.Person;
import com.payroll.domain.SalaryCalculation;
import java.time.YearMonth;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

public final class PayrollCalculator {

    private PayrollCalculator() {
    }

    public static Payslip compute(Person employee, String positionName, YearMonth period,
            List<Employee> attendance, DoubleUnaryOperator sssLookup) {
        long seconds = 0;
        int days = 0;
        for (Employee day : attendance) {
            long worked = day.getHoursWorked();
            if (worked > 0) {
                seconds += worked;
                days++;
            }
        }

        double basic = round(employee.getEmpBasicSalary());
        double rice = round(employee.getEmpRice());
        double phone = round(employee.getEmpPhone());
        double clothing = round(employee.getEmpClothing());
        double allowances = round(rice + phone + clothing);
        double gross = round(basic + allowances);

        double sss = round(sssLookup.applyAsDouble(basic));
        double philHealth = SalaryCalculation.calculatePhilHealthContribution(gross);
        double pagIbig = SalaryCalculation.calculatePagibigContribution(gross);
        double contributions = round(sss + philHealth + pagIbig);

        double taxable = round(Math.max(0, gross - contributions));
        double tax = SalaryCalculation.calculateWithholdingTax(taxable);
        double net = round(taxable - tax);

        return new Payslip(employee.getEmpID(), employee.getFormattedName(), positionName, period,
                days, seconds, round(employee.getEmpHourlyRate()), basic, rice, phone, clothing,
                allowances, gross, sss, philHealth, pagIbig, contributions, taxable, tax, net);
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
