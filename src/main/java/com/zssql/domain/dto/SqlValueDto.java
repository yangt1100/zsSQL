package com.zssql.domain.dto;

import com.zssql.common.enums.SQLType;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 指标结果对象
 */
@Data
public class SqlValueDto implements Serializable {
    // SQL类型
    private SQLType type = SQLType.QUERY;
    // 执行时间(单位：毫秒)
    private Long executeMill;
    /*********************查询类型返回值*******************************/
    // 列数量
    private Integer columnCount;
    // 列名
    private List<String> cloumnNames = new ArrayList<>();
    // 列标签
    private List<String> columnLabels = new ArrayList<>();
    // 列所属标签
    private List<String> columnBelongTables = new ArrayList<>();
    // 每列注释
    private List<String> columnComments = new ArrayList<>();
    // 列类型(映射后)
    private List<Integer> columnTypes = new ArrayList<>();
    // SQL类型
    private List<Integer> sqlTypes = new ArrayList<>();
    private List<String> sqlTypeNames = new ArrayList<>();
    // 值
    private List<List<Object>> vals = new ArrayList<>();

    /*********************执行类型返回值*******************************/
    // 影响行数
    private Integer effectRow;
}
