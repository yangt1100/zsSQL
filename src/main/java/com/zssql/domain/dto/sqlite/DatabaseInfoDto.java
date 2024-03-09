package com.zssql.domain.dto.sqlite;

import com.zssql.domain.entity.sqlite.DatabaseInfo;
import lombok.Data;

@Data
public class DatabaseInfoDto extends DatabaseInfo {
    private String databaseTypeDesc;
}
