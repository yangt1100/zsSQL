package com.zssql.domain.dto.snapshot;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统连接快照
 */
@Data
public class ConnectSnapshotDto implements Serializable {
    // 本地连接名
    private String databaseName;
    // SQL页签信息
    private List<SqlSnapshotDto> sqlTabs;

    public void addSql(String sqlTabName, String sql, String sqlFilePath, boolean modified) {
        if (null == sqlTabs) {
            sqlTabs = new ArrayList<>();
        }
        SqlSnapshotDto sqlSnapshotDto = new SqlSnapshotDto(sqlTabName, sqlFilePath, modified, sql);
        sqlTabs.add(sqlSnapshotDto);
    }


    @Data
    public static class SqlSnapshotDto implements Serializable {
        // SQL页签文本
        private String sqlTabName;
        // 对应的SQL文件路径
        private String sqlFilePath;
        // sql是否已变更
        private boolean modified;
        // sql内容
        private String sql;

        public SqlSnapshotDto(String sqlTabName, String sqlFilePath, boolean modified, String sql) {
            this.sqlTabName = sqlTabName;
            this.sqlFilePath = sqlFilePath;
            this.modified = modified;
            this.sql = sql;
        }
    }
}
