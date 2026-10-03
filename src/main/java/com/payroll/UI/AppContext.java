package com.payroll.UI;

import com.payroll.domain.IT;
import com.payroll.domain.Person;
import com.payroll.security.AccessControl;
import com.payroll.security.Permission;
import com.payroll.security.Role;
import com.payroll.service.AppServices;
import java.util.function.Consumer;

public final class AppContext {

    private final AppServices services;
    private final IT account;
    private Consumer<String> navigator = page -> { };

    public AppContext(AppServices services, IT account) {
        this.services = services;
        this.account = account;
    }

    public AppServices services() {
        return services;
    }

    public IT account() {
        return account;
    }

    public int employeeId() {
        return account.getEmpID();
    }

    public Person person() {
        return account.getEmpDetails();
    }

    public String displayName() {
        Person p = person();
        return p != null ? p.getFormattedName() : account.getEmpUserName();
    }

    public String firstName() {
        Person p = person();
        return p != null && p.getFirstName() != null ? p.getFirstName().trim() : account.getEmpUserName();
    }

    public Role role() {
        return AccessControl.roleOf(account);
    }

    public boolean can(Permission permission) {
        return AccessControl.hasPermission(account, permission);
    }

    public void navigate(String page) {
        navigator.accept(page);
    }

    void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator;
    }

    public void refreshPerson() {
        account.setEmpDetails(services.employees().get(account.getEmpID()));
    }
}
