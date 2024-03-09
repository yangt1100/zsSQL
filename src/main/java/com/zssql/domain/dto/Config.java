package com.zssql.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 配置项
 */
@Data
public class Config implements Serializable {
    // SQL执行后继续聚焦与SQL编辑器
    private Boolean focusSqlEditorAfterExecute = true;
    // 启动时恢复上一次窗口位置
    private Boolean restoreWindowPosAfterStart = true;
    // 启动时尝试恢复上一次会话
    private Boolean restoreConnAfterStart = true;
    // 关闭未保存的标签进行提示
    private Boolean tipSaveAfterCloseTab = true;
    // 自动执行光标所在SQL
    private Boolean autoExecuteCursorSql = true;
}
