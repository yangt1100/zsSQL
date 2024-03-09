package com.zssql.controller;

import com.zssql.common.intf.IndexHandler;
import com.zssql.common.utils.CommonUtil;
import com.zssql.common.utils.MessageAlert;
import com.zssql.common.utils.MouseClickUtils;
import com.zssql.domain.dto.TableColumnDto;
import com.zssql.domain.dto.ddl.ColumnRow;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class IndexSelectViewController implements Initializable {
    @FXML
    private Button cancelBtn;
    @FXML
    private Button moveDownBtn;
    @FXML
    private Button moveUpBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private Button selectBtn;
    @FXML
    private TableColumn<ColumnRow, String> sourceColColumn;
    @FXML
    private TableColumn<ColumnRow, String> sourceTypeColumn;
    @FXML
    private TableView<ColumnRow> sourceView;
    @FXML
    private TableColumn<ColumnRow, String> targetColColumn;
    @FXML
    private TableColumn<ColumnRow, String> targetTypeColumn;
    @FXML
    private TableView<ColumnRow> targetView;
    @FXML
    private Button unSelectBtn;

    @Setter
    private List<ColumnRow> sourceCols;
    @Setter
    private List<ColumnRow> targetCols;
    @Setter
    private IndexHandler indexHandler;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sourceColColumn.setCellValueFactory(new PropertyValueFactory<>("columnName"));
        sourceTypeColumn.setCellValueFactory(new PropertyValueFactory<>("columnType"));
        targetColColumn.setCellValueFactory(new PropertyValueFactory<>("columnName"));
        targetTypeColumn.setCellValueFactory(new PropertyValueFactory<>("columnType"));
        saveBtn.setGraphic(new ImageView(new Image("ok.png")));
        cancelBtn.setGraphic(new ImageView(new Image("cancel.png")));
        selectBtn.setGraphic(new ImageView(new Image("right.png")));
        unSelectBtn.setGraphic(new ImageView(new Image("left.png")));
        moveUpBtn.setGraphic(new ImageView(new Image("up.png")));
        moveDownBtn.setGraphic(new ImageView(new Image("down.png")));

        sourceView.setPlaceholder(new Label(""));
        sourceView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        targetView.setPlaceholder(new Label("未选择索引列"));

        // 禁用操作按钮
        CommonUtil.disableControls(selectBtn, unSelectBtn, moveUpBtn, moveDownBtn);
        // 源视图选中事件
        sourceView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            ObservableList<ColumnRow> selectedItems = sourceView.getSelectionModel().getSelectedItems();
            if (CollectionUtils.isEmpty(selectedItems)) {
                CommonUtil.disableControls(selectBtn);
            } else {
                CommonUtil.enableControls(selectBtn);
            }
        });
        // 目标视图选中事件
        targetView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            int idx = targetView.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                CommonUtil.disableControls(unSelectBtn, moveUpBtn, moveDownBtn);
            } else {
                CommonUtil.enableControls(unSelectBtn, moveUpBtn, moveDownBtn);
                if (0 == idx) {
                    CommonUtil.disableControls(moveUpBtn);
                }
                if (idx == targetView.getItems().size() - 1) {
                    CommonUtil.disableControls(moveDownBtn);
                }
            }
        });
        targetView.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!BooleanUtils.isTrue(newValue) && !moveUpBtn.isFocused() && !moveDownBtn.isFocused()) {
                CommonUtil.disableControls(moveUpBtn, moveDownBtn);
            } else if (BooleanUtils.isTrue(newValue)) {
                int idx = targetView.getSelectionModel().getSelectedIndex();
                if (idx < 0) {
                    CommonUtil.disableControls(unSelectBtn, moveUpBtn, moveDownBtn);
                } else {
                    CommonUtil.enableControls(unSelectBtn, moveUpBtn, moveDownBtn);
                    if (0 == idx) {
                        CommonUtil.disableControls(moveUpBtn);
                    }
                    if (idx == targetView.getItems().size() - 1) {
                        CommonUtil.disableControls(moveDownBtn);
                    }
                }
            }
        });
        sourceView.setOnMouseClicked(event -> {
            if (MouseClickUtils.isLeftDoubleClick(event)) {
                selectIndex();
            }
        });

        CommonUtil.buttonBind(selectBtn, this::selectIndex);
        CommonUtil.buttonBind(unSelectBtn, () -> {
            ObservableList<ColumnRow> selectedItems = targetView.getSelectionModel().getSelectedItems();
            if (CollectionUtils.isEmpty(selectedItems)) {
                return;
            }
            sourceView.getItems().addAll(selectedItems);
            targetView.getItems().removeAll(selectedItems);
            targetView.getSelectionModel().clearSelection();
        });
        CommonUtil.buttonBind(moveUpBtn, () -> {
            int idx = targetView.getSelectionModel().getSelectedIndex();
            if (idx <= 0) {
                return;
            }
            ColumnRow currItem = targetView.getItems().get(idx);
            targetView.getItems().set(idx, targetView.getItems().get(idx - 1));
            targetView.getItems().set(idx - 1, currItem);
            targetView.getSelectionModel().select(idx - 1);
            targetView.requestFocus();
        });
        CommonUtil.buttonBind(moveDownBtn, () -> {
            int idx = targetView.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx >= targetView.getItems().size() - 1) {
                return;
            }
            ColumnRow currItem = targetView.getItems().get(idx);
            targetView.getItems().set(idx, targetView.getItems().get(idx + 1));
            targetView.getItems().set(idx + 1, currItem);
            targetView.getSelectionModel().select(idx + 1);
            targetView.requestFocus();
        });
        CommonUtil.buttonBind(cancelBtn, this::close);
        CommonUtil.buttonBind(saveBtn, () -> {
            if (CollectionUtils.isEmpty(targetView.getItems())) {
                if (!MessageAlert.confirm("未选择任何列，确认继续?")) {
                    return;
                }
            }
            indexHandler.submit(targetView.getItems());
            close();
        });
    }

    private void selectIndex() {
        ObservableList<ColumnRow> selectedItems = sourceView.getSelectionModel().getSelectedItems();
        if (CollectionUtils.isEmpty(selectedItems)) {
            return;
        }
        targetView.getItems().addAll(selectedItems);
        sourceView.getItems().removeAll(selectedItems);
        sourceView.getSelectionModel().clearSelection();
    }

    public void init() {
        sourceView.getItems().addAll(sourceCols);
        targetView.getItems().addAll(targetCols);
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            close();
        }
    }

    private void close() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}
