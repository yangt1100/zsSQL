package com.zssql.common.handler;

/**
 * @description: 数据分组处理接口
 **/
public interface GroupHandler<T> {
    // 获取值
    T getValue();
}