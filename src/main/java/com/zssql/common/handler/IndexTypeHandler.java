package com.zssql.common.handler;

import com.zssql.domain.dto.ddl.IndexRow;

public interface IndexTypeHandler {
    void handler(IndexRow indexRow);
}
