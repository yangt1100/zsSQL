package com.zssql.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExecLogDto implements Serializable {
    // 提示信息
    private String tip;
    // 执行耗时(单位：ms)
    private Long timeMill;
    // 记录条数
    private Integer recordCnt;

    public ExecLogDto(String tip, Long timeMill, Integer recordCnt) {
        this.tip = tip;
        this.timeMill = timeMill;
        this.recordCnt = recordCnt;
    }

    public String timeTip() {
        if (null == timeMill) {
            return null;
        }
        if (timeMill < 1000) {
            return "执行: " + timeMill + " ms";
        } else {
            return "执行: " + String.format("%.4f", timeMill / 1000.0) + " s";
        }
    }

    public String recordCntTip() {
        if (null == recordCnt) {
            return null;
        }
        return "记录数: " + recordCnt;
    }
}
