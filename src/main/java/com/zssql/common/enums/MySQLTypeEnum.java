package com.zssql.common.enums;

import com.zssql.common.utils.CommonUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum MySQLTypeEnum {
    DECIMAL("decimal", Types.DECIMAL, true, true, true, 65L, 65L, SQLCreateLengthType.EITHER, false),
    TINYINT("tinyint", Types.TINYINT, true, true, true, 3L, 3L, SQLCreateLengthType.ONLY, false),
    BOOLEAN("boolean", Types.BOOLEAN, false, false, false,  3L, 3L, SQLCreateLengthType.NONE, false),
    SMALLINT("smallint", Types.SMALLINT, true, true, true, 5L, 5L, SQLCreateLengthType.ONLY, false),
    INT("int", Types.INTEGER, true, true, true, 10L, 10L, SQLCreateLengthType.ONLY, false),
    FLOAT("float", Types.REAL, true, true, true, 12L, 12L, SQLCreateLengthType.BOTH, false),
    DOUBLE("double", Types.DOUBLE, true, true, true, 22L, 22L, SQLCreateLengthType.BOTH, false),
    TIMESTAMP("timestamp", Types.TIMESTAMP, false, false, false,  26L, 26L, SQLCreateLengthType.NONE, false),
    BIGINT("bigint", Types.BIGINT, true, true, true, 19L, 20L, SQLCreateLengthType.ONLY, false),
    MEDIUMINT("mediumint", Types.INTEGER, true, true, true, 7L, 8L, SQLCreateLengthType.ONLY, false),
    DATE("date", Types.DATE, false, false, false, 10L, 10L, SQLCreateLengthType.NONE, false),
    TIME("time", Types.TIME, false, false, false, 16L, 16L, SQLCreateLengthType.NONE, false),
    DATETIME("datetime", Types.TIMESTAMP, false, false, false, 26L, 26L, SQLCreateLengthType.NONE, false),
    YEAR("year", Types.DATE, false, false, false, 4L, 4L, SQLCreateLengthType.NONE, false),
    VARCHAR("varchar", Types.VARCHAR, false, false, false, 65535L, 65535L, SQLCreateLengthType.ONLY, false),
    VARBINARY("varbinary", Types.VARBINARY, false, false, false, 65535L, 65535L, SQLCreateLengthType.ONLY, false),
    BIT("bit", Types.BIT, true, false, false, 1L, 1L, SQLCreateLengthType.NONE, false),
    JSON("json", Types.LONGVARCHAR, false, false, false, 1073741824L, 1073741824L, SQLCreateLengthType.NONE, true),
    ENUM("enum", Types.CHAR, false, false, false, 65535L, 65535L, SQLCreateLengthType.LIST, false),
    SET("set", Types.CHAR, false, false, false, 64L, 64L, SQLCreateLengthType.LIST, false),
    TINYBLOB("tinyblob", Types.VARBINARY, false, false, false, 255L, 255L, SQLCreateLengthType.NONE, false),
    TINYTEXT("tinytext", Types.VARCHAR, false, false, false, 255L, 255L, SQLCreateLengthType.NONE, false),
    MEDIUMBLOB("mediumblob", Types.LONGVARBINARY, false, false, false, 16777215L, 16777215L, SQLCreateLengthType.NONE, true),
    MEDIUMTEXT("mediumtext", Types.LONGVARCHAR, false, false, false, 16777215L, 16777215L, SQLCreateLengthType.NONE, true),
    LONGBLOB("longblob", Types.LONGVARBINARY, false, false, false, 4294967295L, 4294967295L, SQLCreateLengthType.NONE, true),
    LONGTEXT("longtext", Types.LONGVARCHAR, false, false, false, 4294967295L, 4294967295L, SQLCreateLengthType.NONE, true),
    BLOB("blob", Types.LONGVARBINARY, false, false, false, 65535L, 65535L, SQLCreateLengthType.ONLY, true),
    TEXT("text", Types.LONGVARCHAR, false, false, false, 65535L, 65535L, SQLCreateLengthType.ONLY, true),
    CHAR("char", Types.CHAR, false, false, false, 255L, 255L, SQLCreateLengthType.ONLY, false),
    BINARY("binary", Types.BINARY, false, false, false, 255L, 255L, SQLCreateLengthType.ONLY, false),
    ;

    // 类型名称
    private String typeName;
    // SQL类型：{@link java.sql.Types}
    private int sqlType;
    // 是否数字类型
    private boolean isDecimal;
    // 是否可填充0
    private boolean canZeroFill;
    // 是否可以为unsigned类型
    private boolean canUnsigned;
    /**
     * 类型精度：
     * 1、对于数字类型代表最大精度
     * 2、对于字符类型代表最大长度
     * 3、对于时间类型代表其转换后字符的长度
     * 4、对于二进制类型代表最大字节数
     */
    private long precision;
    // unsigned类型精度
    private long uprecision;
    // 字段创建类型
    private SQLCreateLengthType lengthType;
    // 是否大字段
    private boolean isBlob;
    // 对应值编辑类型
    private ValEditType editType;

    MySQLTypeEnum(String typeName, int sqlType, boolean isDecimal, boolean canZeroFill, boolean canUnsigned,
                  long precision, long uprecision, SQLCreateLengthType lengthType, boolean isBlob) {
        this.typeName = typeName;
        this.sqlType = sqlType;
        this.isDecimal = isDecimal;
        this.canZeroFill = canZeroFill;
        this.canUnsigned = canUnsigned;
        this.precision = precision;
        this.uprecision = uprecision;
        this.lengthType = lengthType;
        this.isBlob = isBlob;
    }

    public static List<String> typeNames() {
        List<String> ts = CommonUtil.datasTransfer(Arrays.stream(values()).collect(Collectors.toList()), MySQLTypeEnum::getTypeName);
        Collections.sort(ts);
        return ts;
    }

    public static MySQLTypeEnum getByTypeName(String typeName) {
        if (StringUtils.isBlank(typeName)) {
            return null;
        }

        // 去除"UNSIGNED"标识
        if (StringUtils.endsWithIgnoreCase(typeName, "UNSIGNED")) {
            typeName = StringUtils.left(typeName, typeName.length() - "UNSIGNED".length());
            typeName = StringUtils.trim(typeName);
        }

        for (MySQLTypeEnum value : values()) {
            if (StringUtils.equalsIgnoreCase(value.getTypeName(), typeName)) {
                return value;
            }
        }
        return null;
    }

    public static ValEditType getValEditType(String typeName) {
        MySQLTypeEnum sqlType = getByTypeName(typeName);
        if (null == sqlType) {
            return ValEditType.UNKNOWN;
        }
        return sqlType.getValEditType(StringUtils.endsWithIgnoreCase(typeName, "UNSIGNED"));
    }

    public static SQLCreateLengthType getLengthTypeByTypeName(String typeName) {
        MySQLTypeEnum typeEnum = getByTypeName(typeName);
        if (null == typeEnum) {
            return SQLCreateLengthType.NONE;
        }
        return typeEnum.getLengthType();
    }

    /**
     * 获取对应SQL类型对应值编辑的类型
     * @param isUnsigned 是否unsigned
     * @return
     */
    public ValEditType getValEditType(boolean isUnsigned) {
        if (isBlob) {
            // 大字段无需额外判断
            return ValEditType.BLOB;
        }
        switch (this) {
            case DECIMAL, FLOAT, DOUBLE:
                // 小数格式
                if (isUnsigned) {
                    return ValEditType.U_DECIMAL;
                }
                return ValEditType.DECIMAL;
            case TINYINT, SMALLINT, INT, BIGINT, MEDIUMINT, YEAR, BOOLEAN, BIT:
                // 整数格式
                if (isUnsigned) {
                    return ValEditType.U_INTEGER;
                }
                return ValEditType.INTEGER;
            case TIMESTAMP, DATETIME:
                // 日期时间格式
                return ValEditType.DATETIME;
            case DATE:
                return ValEditType.DATE;
            case TIME:
                return ValEditType.TIME;
            case ENUM:
                return ValEditType.ENUM;
            case SET:
                return ValEditType.SET;
            default:
                return ValEditType.STRING;
        }
    }
}
