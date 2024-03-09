package com.zssql.domain.dto;

import com.zssql.common.enums.ResultViewType;
import com.zssql.common.utils.CommonUtil;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebView;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class SqlControlDto implements Serializable {
    // SQL编辑器
    private WebView sqlCodeView;
    // SQL结果标签容器
    private TabPane resultTabPane;
    // 每个结果标签页对应最近一次执行日志
    private Map<Tab, ExecLogDto> execLogMap = new HashMap<>();
    // 每个结果标签页对应的结果视图类型
    private Map<Tab, ResultViewType> resultViewTypeMap = new HashMap<>();
    // sql文件路径
    private String sqlFilePath;
    // sql是否已变更
    private boolean modified;
    // 明细页签
    private Tab detailTab;
    // 表明细页签WebView
    private WebView detailView;
    // 日志页签
    private Tab logTab;
    // 日志文本框
    private TextArea logArea;

    public void disable() {
        CommonUtil.disableControls(sqlCodeView, resultTabPane);
    }

    public void enable() {
        CommonUtil.enableControls(sqlCodeView, resultTabPane);
    }

    /**
     * 清除结果页签
     */
    public void removeAllResultTab() {
        resultTabPane.getTabs().removeIf(t -> {
            if (t != detailTab && t != logTab) {
                execLogMap.remove(t);
                resultViewTypeMap.remove(t);
                return true;
            }
            return false;
        });
    }
}
