package com.zssql.controller;

import com.zssql.common.enums.ExportType;
import com.zssql.common.intf.ExportHandler;
import com.zssql.common.utils.CommonUtil;
import com.zssql.common.utils.MessageAlert;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

public class ExportSelectViewController implements Initializable {
    public Button exportBtn;
    public RadioButton sqlRadioBtn;
    @FXML
    private Button cancelBtn;
    @FXML
    private RadioButton csvRadioBtn;
    @FXML
    private RadioButton htmlRadioBtn;
    @FXML
    private RadioButton excelRadioBtn;
    @FXML
    private ToggleGroup exportType;
    @FXML
    private ListView<String> fieldListView;

    @Setter
    private ExportHandler exportHandler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        CommonUtil.buttonBind(cancelBtn, this::close);
        CommonUtil.buttonBind(exportBtn, this::export);
        excelRadioBtn.setSelected(true);
        fieldListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    public void init(List<String> fields) {
        fieldListView.getItems().addAll(fields);
        fieldListView.getSelectionModel().selectAll();
    }

    private void close() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            close();
        }
    }

    private void export() {
        ExportType type = null;
        if (excelRadioBtn.isSelected()) {
            type = ExportType.EXCEL;
        } else if (csvRadioBtn.isSelected()) {
            type = ExportType.CSV;
        } else if (htmlRadioBtn.isSelected()) {
            type = ExportType.HTML;
        } else if (sqlRadioBtn.isSelected()) {
            type = ExportType.SQL;
        }
        if (null == type) {
            MessageAlert.information("请先选择导出格式");
            return;
        }

        List<String> selectFields = fieldListView.getSelectionModel().getSelectedItems();
        if (CollectionUtils.isEmpty(selectFields)) {
            MessageAlert.information("请选择需要导出的字段");
            return;
        }

        close();
        exportHandler.export(type, new HashSet<>(selectFields));
    }

}
