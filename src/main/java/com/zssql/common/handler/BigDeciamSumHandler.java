package com.zssql.common.handler;

import java.math.BigDecimal;

/**
 * @description: BigDecimal汇总处理器
 **/
public interface BigDeciamSumHandler<T> {
    // 处理器
    BigDecimal value(T obj);
}