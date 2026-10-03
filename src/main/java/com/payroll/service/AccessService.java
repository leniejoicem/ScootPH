package com.payroll.service;

import com.payroll.DAO.ITDAO;
import com.payroll.DAO.ITDAO.AccountRow;
import com.payroll.domain.IT;
import com.payroll.domain.Person;
import com.payroll.subdomain.UserRole;
import com.payroll.validation.Password;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccessService {

    public record AccountForm(int employeeId, String username, char[] newPassword, Integer roleId) {
    }

    private final Connection connection;
    private final ITDAO accounts;

    public AccessService(Connection connection, ITDAO accounts) {
        this.connection = connection;
        this.accounts = accounts;
    }

    public List<AccountRow> directory() {
        return accounts.getAccountDirectory();
    }

    public List<UserRole> roles() {
        return accounts.getAllUserRole();
    }

    public void saveAccount(AccountForm form) {
        String username = form.username() == null ? "" : form.username().trim();
        String password = form.newPassword() == null ? "" : new String(form.newPassword());
        IT existing = accounts.getByEmpID(form.employeeId());

        Map<String, String> errors = new LinkedHashMap<>();
        if (username.length() < 3 || username.length() > 50) {
            errors.put("empUserName", "Username must be 3 to 50 characters");
        } else if (accounts.isUsernameTaken(username, form.employeeId())) {
            errors.put("empUserName", "This username is already taken");
        }
        if (form.roleId() == null) {
            errors.put("userRole", "Choose a role");
        }
        if (existing == null && password.isEmpty()) {
            errors.put("empPassword", "Set a password for the new account");
        } else if (!password.isEmpty()) {
            List<String> weaknesses = Password.strengthErrors(password);
            if (!weaknesses.isEmpty()) {
                errors.put("empPassword", String.join(" ", weaknesses));
            }
        }
        if (!errors.isEmpty()) {
            throw ServiceException.invalid(errors);
        }
        UserRole role = accounts.getByRolesId(form.roleId());
        if (role == null) {
            throw ServiceException.invalid(Map.of("userRole", "Choose a role"));
        }

        if (existing != null && (existing.getUserRole() == null || existing.getUserRole().getId() != role.getId())) {
            List<String> missing = accounts.getMissingRolesAfterUpdate(form.employeeId(), role.getRole());
            if (!missing.isEmpty()) {
                throw new ServiceException("This change would leave nobody in " + String.join(", ", missing)
                        + ". Assign someone else to " + (missing.size() == 1 ? "that role" : "those roles") + " first.");
            }
        }

        Transactions.inTransaction(connection, () -> {
            IT account = new IT();
            account.setEmpID(form.employeeId());
            account.setEmpUserName(username);
            account.setUserRole(role);
            if (existing == null) {
                Person person = new com.payroll.domain.Employee();
                person.setEmpID(form.employeeId());
                account.setEmpPassword(password);
                accounts.saveUserAccount(account, person);
                account.setEmpPassword(accounts.getByEmpID(form.employeeId()).getEmpPassword());
            } else {
                account.setEmpPassword(password.isEmpty() ? existing.getEmpPassword() : Password.hashPassword(password));
            }
            accounts.updateEmployeeAccountWithRole(account);
            return null;
        });
    }

    public void resetTwoFactor(int employeeId) {
        IT existing = accounts.getByEmpID(employeeId);
        if (existing == null) {
            throw new ServiceException("This employee has no account.");
        }
        accounts.updateTfaSecret(existing.getAccountID(), null);
    }
}
