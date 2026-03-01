package com.cmbchina.datadirect.caliber.application.exception;

public class BusinessConflictException extends RuntimeException {

    private final String code;

    public BusinessConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
