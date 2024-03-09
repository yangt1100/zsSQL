package com.zssql.domain.dto;

import java.io.Serializable;

/**
 * @description: 错误实体
 **/
public class ErrorDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String errorMessage;

    public ErrorDto(){

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ErrorDto(String errorMessage){
        this.errorMessage = errorMessage;
    }

    public static void setErrorInfo(ErrorDto errorDto, String errorMessage){
        if (null != errorDto){
            errorDto.setErrorMessage(errorMessage);
        }
    }
}
