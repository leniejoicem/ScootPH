package com.payroll.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public ServiceException(String message) {
        this(message, Map.of(), null);
    }

    public ServiceException(String message, Throwable cause) {
        this(message, Map.of(), cause);
    }

    public ServiceException(String message, Map<String, String> fieldErrors) {
        this(message, fieldErrors, null);
    }

    private ServiceException(String message, Map<String, String> fieldErrors, Throwable cause) {
        super(message, cause);
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public static ServiceException invalid(Map<String, String> fieldErrors) {
        return new ServiceException(fieldErrors.size() == 1
                ? fieldErrors.values().iterator().next()
                : "Please correct the highlighted fields.", fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
