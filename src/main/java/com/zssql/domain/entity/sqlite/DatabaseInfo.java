package com.zssql.domain.entity.sqlite;

import lombok.Data;

import java.io.Serializable;

@Data
public class DatabaseInfo implements Serializable {
    private Integer id;
    private String databaseName;
    private Integer databaseType;
    private String databaseHost;
    private String databaseUser;
    private String databasePassword;
    private Integer databasePort;
    private String databaseScheme;
    private String createTime;

    public static String createSQL() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS db_info (");
        sql.append("id INTEGER PRIMARY KEY AUTOINCREMENT,");
        sql.append("databaseName TEXT,");
        sql.append("databaseType INTEGER,");
        sql.append("databaseHost TEXT,");
        sql.append("databaseUser TEXT,");
        sql.append("databasePassword TEXT,");
        sql.append("databasePort INTEGER,");
        sql.append("databaseScheme TEXT,");
        sql.append("createTime TEXT");
        sql.append(")");
        return sql.toString();
    }
}
