package com.payroll.DAO;

public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message + (cause != null && cause.getMessage() != null ? ": " + cause.getMessage() : ""), cause);
    }
}
