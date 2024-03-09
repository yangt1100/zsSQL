package com.zssql.common.utils;

import com.zssql.common.exception.BusinessException;
import com.zssql.common.exception.ErrorEnum;

public class ExceptionUtil {
    public static BusinessException exp(ErrorEnum errorEnum) {
        return new BusinessException(errorEnum.getCode(), errorEnum.getMessage());
    }

    public static BusinessException exp(ErrorEnum errorEnum, String... desc) {
        String errMsg = errorEnum.getMessage();
        String destDesc = String.format(errMsg, desc);

        return new BusinessException(errorEnum.getCode(), destDesc);
    }

    public static BusinessException exp(String... desc) {
        String errMsg = ErrorEnum.ERROR_WITH_REASON.getMessage();
        String destDesc = String.format(errMsg, desc);

        return new BusinessException(ErrorEnum.ERROR_WITH_REASON.getCode(), destDesc);
    }

    public static void throwE(ErrorEnum errorEnum) throws BusinessException {
        throw exp(errorEnum);
    }

    public static void throwE(ErrorEnum errorEnum, String... desc) throws BusinessException {
        throw exp(errorEnum, desc);
    }

    public static void throwE(String desc) throws BusinessException {
        throw exp(ErrorEnum.ERROR_WITH_REASON, desc);
    }

    /**
     * 匹配条件则抛出异常
     * @param expression
     * @param ErrorEnum
     * @throws BusinessException
     */
    public static void matchThrow(boolean expression, ErrorEnum ErrorEnum) throws BusinessException {
        if (expression){
            throw exp(ErrorEnum);
        }
    }

    public static void matchThrow(boolean expression, ErrorEnum ErrorEnum, String... desc) throws BusinessException {
        if (expression){
            throw exp(ErrorEnum, desc);
        }
    }

    public static void matchThrow(boolean expression, String desc) throws BusinessException {
        matchThrow(expression, ErrorEnum.ERROR_WITH_REASON, desc);
    }

    public static void ifNullThrow(Object obj, ErrorEnum errorEnum) throws BusinessException {
        matchThrow(null == obj, errorEnum);
    }

    public static void ifNullThrow(Object obj, ErrorEnum errorEnum, String... desc) throws BusinessException {
        matchThrow(null == obj, errorEnum, desc);
    }

    public static void ifNullThrow(Object obj, String desc) throws BusinessException {
        matchThrow(null == obj, desc);
    }

    /**
     * 不匹配条件则抛出异常
     * @param expression
     * @param ErrorEnum
     * @throws BusinessException
     */
    public static void notMatchThrow(boolean expression, ErrorEnum ErrorEnum) throws BusinessException {
        if (!expression){
            throw exp(ErrorEnum);
        }
    }

    public static void notMatchThrow(boolean expression, ErrorEnum ErrorEnum, String... desc) throws BusinessException {
        if (!expression){
            throw exp(ErrorEnum, desc);
        }
    }

    public static void notMatchThrow(boolean expression, String... desc) throws BusinessException {
        notMatchThrow(expression, ErrorEnum.ERROR_WITH_REASON, desc);
    }

    public static void notNullThrow(Object obj, ErrorEnum errorEnum) throws BusinessException {
        notMatchThrow(null == obj, errorEnum);
    }

    public static void notNullThrow(Object obj, ErrorEnum errorEnum, String... desc) throws BusinessException {
        notMatchThrow(null == obj, errorEnum, desc);
    }

    public static void notNullThrow(Object obj, String desc) throws BusinessException {
        notMatchThrow(null == obj, desc);
    }
}
