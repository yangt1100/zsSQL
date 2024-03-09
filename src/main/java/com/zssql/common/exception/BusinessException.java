package com.zssql.common.exception;

public class BusinessException extends ApplicationException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(String code, String message) {
        super(code, message);
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }

    public BusinessException(String message, Object[] args) {
        super(message, args);
    }

    public BusinessException(String message, Object[] args, Throwable cause) {
        super(message, args, cause);
    }

    public BusinessException(String code, String message, Object[] args) {
        super(code, message, args);
    }

    public BusinessException(String code, String message, Object[] args, Throwable cause) {
        super(code, message, args, cause);
    }
}
