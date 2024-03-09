package com.zssql.common.handler;

import java.util.List;

/**
 * @description: 导出数据处理器
 **/
public interface ExportDataHandler<T> {
    /**
     * 获取导出数据, 返回空则认为结束
     * @return
     */
    List<T> getExportData();
}
