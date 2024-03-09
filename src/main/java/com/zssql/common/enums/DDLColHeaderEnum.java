package com.zssql.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DDL列视图表头列枚举
 */
@Getter
public enum DDLColHeaderEnum {
    COL_NAME("列名", 0),
    COL_TYPE("数据类型", 1),
    COL_LENGTH("长度", 2),
    COL_DEFVAL("默认", 3),
    COL_PRIMARY("主键?", 4),
    COL_NOTNULL("非空?", 5),
    COL_UNSIGNED("Unsigned", 6),
    COL_AUTOINCR("自增?", 7),
    COL_AUTOUPDATE("更新", 8),
    COL_COMMENT("注释", 9),
    ;

    // 列表头名称
    private String hName;
    // 列表头索引
    private int colIdx;

    DDLColHeaderEnum(String hName, int colIdx) {
        this.hName = hName;
        this.colIdx = colIdx;
    }

    public static List<String> headerNames() {
        return Arrays.stream(values()).map(DDLColHeaderEnum::getHName).collect(Collectors.toList());
    }
}
