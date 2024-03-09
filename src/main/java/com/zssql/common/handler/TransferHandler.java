package com.zssql.common.handler;

/**
 * @description: 数据转换处理接口
 **/
public interface TransferHandler<E, T> {
    // 转换
    T transfer(E source);
}