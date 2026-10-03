package com.payroll.domain;

import com.payroll.domain.HR.LeaveStatus;
import jakarta.validation.constraints.*;
import java.util.List;

public class LeaveBalance {

    @Min(value = 1, message = "Employee ID is required")
    private int empID;

    @Min(value = 0, message = "Total leave days cannot be negative")
    @Max(value = 365, message = "Total leave days cannot exceed 365")
    private int total = 25;

    @Min(value = 0, message = "Available leave days cannot be negative")
    @Max(value = 365, message = "Available leave days cannot exceed 365")
    private int available = 25;

    @Min(value = 0, message = "Taken leave days cannot be negative")
    @Max(value = 365, message = "Taken leave days cannot exceed 365")
    private int taken = 0;

    public int getEmpID() {
        return empID;
    }

    public void setEmpID(int empID) {
        this.empID = empID;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getTaken() {
        return taken;
    }

    public void setTaken(int taken) {
        this.taken = taken;
    }

    public void updateLeaveBalance(List<HR> leaveDetails){
        int totalDays = 0;
        for(HR l: leaveDetails){
            if (l.getStatus() != LeaveStatus.DECLINED) {
                totalDays += l.getTotalDays();
            }
        }
        setTaken(totalDays);
        int available = total - taken;
        setAvailable(available);
    }
}
