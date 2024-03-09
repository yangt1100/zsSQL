package com.zssql.domain.dto;

import com.zssql.domain.dto.sqlite.DatabaseInfoDto;
import com.zssql.domain.vo.DatasouceTableVO;
import lombok.Data;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.List;

/**
 * 连接信息实体
 */
@Data
public class ConnectInfoDto implements Serializable {
    // 数据库信息
    private DatabaseInfoDto dbInfoDto;
    // 表结构
    private List<DatasouceTableVO> tables;
    // 数据库连接源
    private DataSource dataSource;

    public String getConnectName() {
        StringBuilder name = new StringBuilder();
        name.append("数据库:").append(dbInfoDto.getDatabaseName());
        return name.toString();
    }
}
