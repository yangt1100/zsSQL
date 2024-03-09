package com.zssql.common.handler;

/**
 * @description: 本地缓存处理接口
 **/
public interface LocalCacher<E, T> {
    // 获取值
    T getValue(E code);
}