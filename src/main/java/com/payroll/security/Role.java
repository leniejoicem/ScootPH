package com.payroll.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum Role {
    EMPLOYEE("Employee"),
    HR("HR",
            Permission.MANAGE_EMPLOYEES,
            Permission.VIEW_EMPLOYEE_ATTENDANCE,
            Permission.APPROVE_LEAVE),
    FINANCE("Finance",
            Permission.VIEW_EMPLOYEE_PAYROLL,
            Permission.MANAGE_PAYROLL),
    IT("IT",
            Permission.MANAGE_USER_ROLES);

    private static final Set<Permission> BASE_PERMISSIONS = EnumSet.of(
            Permission.VIEW_OWN_PROFILE,
            Permission.VIEW_OWN_PAYROLL,
            Permission.REQUEST_LEAVE);

    private final String displayName;
    private final Set<Permission> extraPermissions;

    Role(String displayName, Permission... extraPermissions) {
        this.displayName = displayName;
        this.extraPermissions = extraPermissions.length == 0
                ? EnumSet.noneOf(Permission.class)
                : EnumSet.of(extraPermissions[0], extraPermissions);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<Permission> getPermissions() {
        EnumSet<Permission> permissions = EnumSet.copyOf(BASE_PERMISSIONS);
        permissions.addAll(extraPermissions);
        return Collections.unmodifiableSet(permissions);
    }

    public boolean has(Permission permission) {
        return BASE_PERMISSIONS.contains(permission) || extraPermissions.contains(permission);
    }

    public static Role fromName(String name) {
        if (name != null) {
            String normalized = name.trim().toUpperCase(Locale.ROOT);
            for (Role role : values()) {
                if (role.name().equals(normalized)) {
                    return role;
                }
            }
        }
        return EMPLOYEE;
    }
}
