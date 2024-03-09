package com.zssql.common.intf;

import com.zssql.common.enums.ExportType;

import java.util.Set;

public interface ExportHandler {
    void export(ExportType type, Set<String> fields);
}
