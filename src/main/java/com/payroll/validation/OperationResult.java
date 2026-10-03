package com.payroll.validation;

import java.util.ArrayList;
import java.util.List;

public class OperationResult {

    private boolean success;
    private String message;
    private List<String> errors;

    public OperationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.errors = new ArrayList<>();
    }

    public OperationResult(boolean success, String message, List<String> errors) {
        this.success = success;
        this.message = message;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public static OperationResult success(String message) {
        return new OperationResult(true, message);
    }

    public static OperationResult failure(String message) {
        return new OperationResult(false, message);
    }

    public static OperationResult validationFailure(List<String> errors) {
        return new OperationResult(false, "Validation failed", errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getFormattedErrors() {
        if (errors.isEmpty()) {
            return message;
        }

        StringBuilder sb = new StringBuilder(message).append(":\n");
        for (int i = 0; i < errors.size(); i++) {
            sb.append((i + 1)).append(". ").append(errors.get(i)).append("\n");
        }
        return sb.toString().trim();
    }
}
