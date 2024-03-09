package com.zssql.domain.dto;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.Data;
import org.controlsfx.control.spreadsheet.SpreadsheetView;

@Data
public class RowResultControlDto {
    private VBox rowVBox;
    private SpreadsheetView resultRowView;
    private Button startBtn;
    private Button preBtn;
    private Button nextBtn;
    private Button endBtn;
    private TextField lineEdit;

    // 当前展示的行
    private int selectIdx = 0;

    public RowResultControlDto(VBox rowVBox, SpreadsheetView resultRowView, Button startBtn, Button preBtn, Button nextBtn, Button endBtn, TextField lineEdit) {
        this.rowVBox = rowVBox;
        this.resultRowView = resultRowView;
        this.startBtn = startBtn;
        this.preBtn = preBtn;
        this.nextBtn = nextBtn;
        this.endBtn = endBtn;
        this.lineEdit = lineEdit;
    }
}
