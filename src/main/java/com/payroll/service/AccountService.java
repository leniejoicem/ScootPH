package com.payroll.service;

import com.payroll.DAO.ITDAO;
import com.payroll.domain.IT;
import com.payroll.validation.OperationResult;
import com.payroll.validation.Password;
import java.util.List;

public class AccountService {

    private final ITDAO accountDao;

    public AccountService(ITDAO accountDao) {
        this.accountDao = accountDao;
    }

    public OperationResult changePassword(IT account, String currentPassword, String newPassword, String confirmPassword) {
        if (account == null) {
            return OperationResult.failure("No signed-in account.");
        }
        if (!Password.verifyPassword(currentPassword, account.getEmpPassword())) {
            return OperationResult.failure("Current password is incorrect.");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            return OperationResult.failure("New password and confirmation do not match.");
        }
        if (newPassword.equals(currentPassword)) {
            return OperationResult.failure("New password must be different from the current password.");
        }
        List<String> weaknesses = Password.strengthErrors(newPassword);
        if (!weaknesses.isEmpty()) {
            return OperationResult.validationFailure(weaknesses);
        }
        if (accountDao == null || !accountDao.updatePassword(account.getAccountID(), newPassword)) {
            return OperationResult.failure("Failed to update password. Please try again.");
        }
        account.setEmpPassword(Password.hashPassword(newPassword));
        return OperationResult.success("Password changed successfully!");
    }
}
