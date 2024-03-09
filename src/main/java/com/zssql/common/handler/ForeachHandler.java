package com.zssql.common.handler;

/**
 * @description: 遍历处理器
 **/
public interface ForeachHandler<T> {
    // 单个元素处理逻辑
    void handler(T val);
}