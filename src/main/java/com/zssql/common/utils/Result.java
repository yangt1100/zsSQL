package com.zssql.common.utils;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final transient String DEFAULT_SUCCESS_CODE = "0";
    public static final transient String DEFAULT_ERROR_CODE = "-1";
    @ApiModelProperty("0:成功;其他:失败错误码")
    private String code;
    @ApiModelProperty("返回的结果")
    private T data;
    @ApiModelProperty("错误信息，给开发者使用。（可选）")
    private String message;
    @ApiModelProperty("提示信息，终端用户使用。（可选）")
    private String info;
    @ApiModelProperty("请求id")
    private String requestId;

    public Result(T data) {
        this.data = data;
        this.code = "0";
        this.message = "success";
    }

    public Result() {
        this.code = "-1";
        this.message = "failure";
    }

    public boolean isSuccess() {
        return "0".equals(this.code);
    }

    public static Result success() {
        return new Result((Object)null);
    }

    public static Result success(Object data) {
        return new Result(data);
    }

    public static Result DefaultFailure(String msg) {
        Result result = new Result();
        result.setCode("-1");
        result.setMessage(msg);
        return result;
    }

    public String getCode() {
        return this.code;
    }

    public T getData() {
        return this.data;
    }

    public String getMessage() {
        return this.message;
    }

    public String getInfo() {
        return this.info;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public Result<T> setCode(final String code) {
        this.code = code;
        return this;
    }

    public Result<T> setData(final T data) {
        this.data = data;
        return this;
    }

    public Result<T> setMessage(final String message) {
        this.message = message;
        return this;
    }

    public Result<T> setInfo(final String info) {
        this.info = info;
        return this;
    }

    public Result<T> setRequestId(final String requestId) {
        this.requestId = requestId;
        return this;
    }

    public String toString() {
        return "Result(code=" + this.getCode() + ", data=" + this.getData() + ", message=" + this.getMessage() + ", info=" + this.getInfo() + ", requestId=" + this.getRequestId() + ")";
    }
}
