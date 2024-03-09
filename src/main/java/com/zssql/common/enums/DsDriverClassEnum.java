package com.zssql.common.enums;


import com.zssql.common.utils.CommonUtil;
import com.zssql.domain.entity.sqlite.DatabaseInfo;

/**
 * 数据源JDBC驱动类枚举
 */
public enum DsDriverClassEnum {
    MYSQL(1, "MySQL", "com.mysql.cj.jdbc.Driver", "?"),
    TIDB(2, "TiDB", "com.mysql.cj.jdbc.Driver", "?"),
    // ORACLE(3, "Oracle", "oracle.jdbc.OracleDriver", "?"),
    STARROCKS(4, "StarRocks", "com.mysql.cj.jdbc.Driver", "?"),
    ;
    // 数据库类型
    private Integer dsType;
    // 数据库名称
    private String dbName;
    // 驱动类全路径
    private String driverClass;
    // SQL参数占位符
    private String placeHolder;

    DsDriverClassEnum(Integer dsType, String dbName, String driverClass, String placeHolder) {
        this.dsType = dsType;
        this.dbName = dbName;
        this.driverClass = driverClass;
        this.placeHolder = placeHolder;
    }

    public Integer getDsType() {
        return dsType;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getDbName() {
        return dbName;
    }

    public String getPlaceHolder() {
        return placeHolder;
    }

    @Override
    public String toString() {
        return dbName;
    }

    public static DsDriverClassEnum getDriverClassEnum(Integer type) {
        if (null == type) {
            return null;
        }
        for (DsDriverClassEnum val : values()) {
            if (CommonUtil.isObjectEquals(val.getDsType(), type)) {
                return val;
            }
        }
        return null;
    }

    public static String getDriverClass(Integer type) {
        DsDriverClassEnum driverClassEnum = getDriverClassEnum(type);
        return null == driverClassEnum? null : driverClassEnum.getDriverClass();
    }

    public static String getDbName(Integer type) {
        DsDriverClassEnum driverClassEnum = getDriverClassEnum(type);
        return null == driverClassEnum? null : driverClassEnum.getDbName();
    }

    public static String getPlaceHolder(Integer type) {
        DsDriverClassEnum driverClassEnum = getDriverClassEnum(type);
        return null == driverClassEnum? null : driverClassEnum.getPlaceHolder();
    }

    public String getJDBCUrl(DatabaseInfo databaseInfo) {
        if (null == databaseInfo) {
            return null;
        }

        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://");
        url.append(databaseInfo.getDatabaseHost()).append(":");
        url.append(databaseInfo.getDatabasePort()).append("/");
        url.append(databaseInfo.getDatabaseScheme());
        url.append("?useUnicode=true&useSSL=true&characterEncoding=utf8&allowMultiQueries=true&autoReconnect=true&autoReconnectForPools=true&serverTimezone=Asia/Shanghai&zeroDateTimeBehavior=convertToNull");
        return url.toString();
    }
}
