package com.zssql.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RowValueDto implements Serializable {
    private Integer idx;
    private String column;
    private Object value;
    private String comment;
}
