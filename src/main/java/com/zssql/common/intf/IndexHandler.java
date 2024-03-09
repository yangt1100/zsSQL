package com.zssql.common.intf;

import com.zssql.domain.dto.ddl.ColumnRow;

import java.util.List;

public interface IndexHandler {
    void submit(List<ColumnRow> idxCols);
}
