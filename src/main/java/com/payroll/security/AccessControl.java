package com.payroll.security;

import com.payroll.domain.IT;
import java.util.Arrays;

public final class AccessControl {

    private AccessControl() {
    }

    public static Role roleOf(IT account) {
        if (account == null || account.getUserRole() == null) {
            return Role.EMPLOYEE;
        }
        return Role.fromName(account.getUserRole().getRole());
    }

    public static boolean hasPermission(IT account, Permission permission) {
        return account != null && roleOf(account).has(permission);
    }

    public static boolean hasAnyPermission(IT account, Permission... permissions) {
        return Arrays.stream(permissions).anyMatch(permission -> hasPermission(account, permission));
    }

    public static void requireAny(IT account, Permission... permissions) {
        if (!hasAnyPermission(account, permissions)) {
            throw new SecurityException("Access denied for role " + roleOf(account)
                    + ": requires one of " + Arrays.toString(permissions));
        }
    }
}
