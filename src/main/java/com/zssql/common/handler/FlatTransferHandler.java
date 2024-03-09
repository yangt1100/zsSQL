package com.zssql.common.handler;

/**
 * @description: 数据转换处理接口
 **/
public interface FlatTransferHandler<E, T> {
    // 转换
    void transfer(E source, Collector<T> out);
}