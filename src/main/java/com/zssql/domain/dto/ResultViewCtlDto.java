package com.zssql.domain.dto;

import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.web.WebView;
import lombok.Data;
import org.controlsfx.control.spreadsheet.SpreadsheetView;

import java.io.Serializable;

@Data
public class ResultViewCtlDto implements Serializable {
    // 对应页签
    private Tab connectTab;
    private Tab sqlTab;
    private Tab resultTab;

    // 网格视图表格
    private SpreadsheetView tableView;
    // 行视图表格
    private SpreadsheetView rowView;
    // HTML视图
    private WebView htmlView;
    // SQL内容文本框
    private TextField sqlArea;

    // 工具栏控件
    private Button exportSelectBtn;
    private Button exportAllBtn;
    private ToggleButton tableViewBtn;
    private ToggleButton htmlViewBtn;
    private ToggleButton rowViewBtn;
    private Button leftBtn;
    private TextField firstLineEdit;
    private Button rightBtn;
    private TextField rowCntEdit;

    // 原始SQL语句
    private String sql;

    // 行视图控件实体
    private RowResultControlDto rowControlDto;
}
