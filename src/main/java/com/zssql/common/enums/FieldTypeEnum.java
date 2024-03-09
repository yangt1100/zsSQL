package com.zssql.common.enums;

import lombok.Getter;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 指标字段类型枚举
 */
public enum FieldTypeEnum {
    INTEGER(1, "Integer", "整数", 0),
    DECIMAL(2, "Decimal",  "小数", 0.0),
    STRING(3, "String",  "字符串", ""),
    DATE(4, "DateTime", "日期", "2023-09-18 12:00:00"),
    ;

    // 日期表达式模式
    private static final Pattern DATE_EXPRESSION_PATTERN = Pattern.compile("\\$\\[time\\((.*?)\\)\\]");

    @Getter
    private Integer type;
    @Getter
    private String desc;
    @Getter
    private String name;
    @Getter
    private Object defaultValue;

    FieldTypeEnum(Integer type, String desc, String name, Object defaultValue){
        this.type = type;
        this.desc = desc;
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public static FieldTypeEnum getEnumByType(Integer code){
        if (code == null){
            return null;
        }
        return Arrays.stream(FieldTypeEnum.values()).filter(b-> Objects.equals(b.getType(), code)).findFirst().orElse(null);
    }

    public static String getDescByType(Integer code){
        if (code == null){
            return null;
        }
        FieldTypeEnum enumByType = getEnumByType(code);
        return null == enumByType? null : enumByType.getDesc();
    }

    @Override
    public String toString() {
        return desc;
    }
}