package com.payroll.validation;

import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

public class Password {

    private static final int WORK_FACTOR = 12;

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid BCrypt hash format: " + e.getMessage());
            return false;
        }
    }

    public static List<String> strengthErrors(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            errors.add("Password is required.");
            return errors;
        }
        if (password.length() < 8)
            errors.add("Must be at least 8 characters long.");
        if (!password.matches(".*[A-Z].*"))
            errors.add("Must contain at least one uppercase letter.");
        if (!password.matches(".*[a-z].*"))
            errors.add("Must contain at least one lowercase letter.");
        if (!password.matches(".*\\d.*"))
            errors.add("Must contain at least one digit.");
        if (!password.matches(".*[!@#$%^&*()\\-+=<>?].*"))
            errors.add("Must contain at least one special character (e.g., !@#$%^&*).");

        return errors;
    }
}
