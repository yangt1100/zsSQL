package com.zssql.common.handler;

import java.math.BigDecimal;

/**
 * @description: BigDecimal汇总处理器
 **/
public interface BigDeciamFlatSumHandler<T> {
    // 处理器
    void value(T obj, Collector<BigDecimal> out);
}