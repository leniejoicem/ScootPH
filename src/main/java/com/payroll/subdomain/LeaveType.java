package com.payroll.subdomain;

public class LeaveType {

    int id;
    String leaveType;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public String toString(){
        return this.leaveType;
    }
}
