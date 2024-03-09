package com.zssql.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DDL索引视图表头列枚举
 */
@Getter
public enum DDLIndexHeaderEnum {
    IDX_NAME("索引名", 0),
    IDX_COL("索引列", 1),
    IDX_TYPE("索引类型", 2),
    ;

    // 列表头名称
    private String hName;
    // 列表头索引
    private int colIdx;

    DDLIndexHeaderEnum(String hName, int colIdx) {
        this.hName = hName;
        this.colIdx = colIdx;
    }

    public static List<String> headerNames() {
        return Arrays.stream(values()).map(DDLIndexHeaderEnum::getHName).collect(Collectors.toList());
    }
}
