package com.zssql.common.enums;

public enum SQLCreateLengthType {
    // 无需指定长度
    NONE,
    // 仅需要指定整数
    ONLY,
    // 需指定整数和小数
    BOTH,
    // 整数或小数均可
    EITHER,
    // 列表
    LIST
}
