package com.zssql.common.exception;


/**
 * Created by wangzhiyuan on 2020/2/24
 */
public enum ErrorEnum {
    SYSTEM_ERROR("系统异常"),
    PARAM_NULL_ERROR("参数为空"),
    PARAM_ERROR("参数错误：%s"),
    ERROR_WITH_REASON("%s"),
    DATA_NOT_FOUND("没有找到数据"),


    ;

    private String code;
    private String msg;

    ErrorEnum(String msg) {
        this.code = ApplicationException.DEFAULT_ERROR_CODE;
        this.msg = msg;
    }

    ErrorEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.msg;
    }

    public ApplicationException newCustomException(String code, String msg, Object... args) {
        return new BusinessException(code, msg, args);
    }

    public ApplicationException newCustomException(String code, String msg, Throwable cause, Object... args) {
        return new BusinessException(code, msg, args, cause);
    }
}
