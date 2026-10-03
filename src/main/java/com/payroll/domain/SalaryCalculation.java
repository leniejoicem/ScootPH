package com.payroll.domain;
import com.payroll.util.*;
import com.payroll.domain.IT;
import com.payroll.domain.Person;
import com.payroll.domain.Employee;
import java.sql.Connection;
import java.util.List;

public class SalaryCalculation {
    private Connection connection;
    public SalaryCalculation(Connection connection){
        this.connection = connection;
    }

    public static double calculatePhilHealthContribution(double empSalary) {
        double rawContribution = empSalary * 0.015;

        double roundedContribution = Math.round(rawContribution * 100.0) / 100.0;

        return roundedContribution;
    }

    public static double calculatePagibigContribution(double empSalary)  {
        if (empSalary <= 0) {
            return 0.0;
        }
        double rate = empSalary <= 1500 ? 0.01 : 0.02;
        return Math.min(round2(empSalary * rate), 100.0);
    }

    public static double calculateWithholdingTax(double taxableIncome) {
        double tax;
        if (taxableIncome <= 20833) {
            tax = 0;
        } else if (taxableIncome <= 33333) {
            tax = (taxableIncome - 20833) * 0.20;
        } else if (taxableIncome <= 66667) {
            tax = 2500 + (taxableIncome - 33333) * 0.25;
        } else if (taxableIncome <= 166667) {
            tax = 10833.33 + (taxableIncome - 66667) * 0.30;
        } else if (taxableIncome <= 666667) {
            tax = 40833.33 + (taxableIncome - 166667) * 0.32;
        } else {
            tax = 200833.33 + (taxableIncome - 666667) * 0.35;
        }
        return round2(tax);
    }

    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static double getTotalAllowance(Person empDetails){
        double total = 0;
        if (empDetails != null) {
            total += empDetails.getEmpRice();
            total += empDetails.getEmpPhone();
            total += empDetails.getEmpClothing();
        }
        return total;
     }

    public static double getTotalHoursWorked(List<Employee> empHours){
        double totalHoursWorked = 0;
        for (Employee employeeHours: empHours){
            totalHoursWorked += employeeHours.getHoursWorked();
        }
        return totalHoursWorked/3600;
    }

    public static String getFormattedTotalHoursWorked(List<Employee> empHours){
        long totalHoursWorked = 0;
        for (Employee employeeHours: empHours){
            totalHoursWorked += employeeHours.getHoursWorked();
        }
        return String.format("%d:%02d", totalHoursWorked / 3600, (totalHoursWorked % 3600) / 60);
    }

    public static double getBasicSalary(List<Employee> empHours, IT empAccount) {
        return empAccount.getEmpDetails().getEmpBasicSalary();
    }

    public static double getComputedSalary(List<Employee> empHours, IT empAccount) {
        double totalHoursWorked = SalaryCalculation.getTotalHoursWorked(empHours);
        double hourlyRate = empAccount.getEmpDetails().getEmpHourlyRate();
        return totalHoursWorked * hourlyRate;
    }

    public static double getGrossSalary(List<Employee> empHours,IT empAccount){
        double basicSalary = getBasicSalary(empHours, empAccount);
        return basicSalary + getTotalAllowance(empAccount.getEmpDetails());
    }

    public static double getTotalDeductions(double empSalary, double sssContri){
        double philhealthContri = calculatePhilHealthContribution(empSalary);
        double pagibigContri = calculatePagibigContribution(empSalary);
        return philhealthContri + sssContri + pagibigContri;
    }

    public static double getTaxableIncome(double empSalary, double sssContri){
        double totalDeductions = getTotalDeductions(empSalary, sssContri);
        return empSalary - totalDeductions;
    }

    public static double getNetPay(double empSalary, double sssContri) {
        double taxableIncome = getTaxableIncome(empSalary, sssContri);
        return taxableIncome - calculateWithholdingTax(taxableIncome);
    }
}
