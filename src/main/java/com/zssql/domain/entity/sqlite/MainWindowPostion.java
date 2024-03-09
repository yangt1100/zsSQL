package com.zssql.domain.entity.sqlite;

import lombok.Data;

import java.io.Serializable;

@Data
public class MainWindowPostion implements Serializable {
    private Integer id;
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private Integer fullScreen;

    public static String createSQL() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS window_pos (");
        sql.append("id INTEGER PRIMARY KEY,");
        sql.append("x REAL,");
        sql.append("y REAL,");
        sql.append("width REAL,");
        sql.append("height REAL,");
        sql.append("fullScreen INTEGER");
        sql.append(")");
        return sql.toString();
    }
}
