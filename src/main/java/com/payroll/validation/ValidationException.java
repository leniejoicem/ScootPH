package com.payroll.validation;

import java.util.List;

public class ValidationException extends Exception {

    private final List<String> errors;

    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getFormattedErrors() {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed";
        }

        StringBuilder sb = new StringBuilder("Validation errors:\n");
        for (int i = 0; i < errors.size(); i++) {
            sb.append((i + 1)).append(". ").append(errors.get(i)).append("\n");
        }

        return sb.toString().trim();
    }
}
