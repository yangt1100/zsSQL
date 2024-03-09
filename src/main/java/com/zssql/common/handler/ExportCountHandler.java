package com.zssql.common.handler;

/**
 * @description: 导出数据数量统计处理器
 **/
public interface ExportCountHandler<T> {
    /**
     * 统计导出数据总量
     * @return
     */
    int countData();
}
