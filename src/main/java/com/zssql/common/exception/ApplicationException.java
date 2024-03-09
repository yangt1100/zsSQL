package com.zssql.common.exception;

public class ApplicationException extends RuntimeException {
    public static final transient String DEFAULT_ERROR_CODE = "500";
    protected String code = "500";
    protected Object[] args;

    public ApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ApplicationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationException(String message, Object[] args) {
        super(message);
        this.args = args;
    }

    public ApplicationException(String message, Object[] args, Throwable cause) {
        super(message, cause);
        this.args = args;
    }

    public ApplicationException(String code, String message, Object[] args) {
        super(message);
        this.code = code;
        this.args = args;
    }

    public ApplicationException(String code, String message, Object[] args, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.args = args;
    }

    public String getCode() {
        return this.code;
    }

    public Object[] getArgs() {
        return this.args;
    }
}
