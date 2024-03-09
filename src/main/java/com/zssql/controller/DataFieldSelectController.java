package com.zssql.controller;

import com.zssql.common.utils.CommonUtil;
import com.zssql.common.utils.MessageAlert;
import com.zssql.common.utils.MouseClickUtils;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DataFieldSelectController {
    public ListView<String> fieldView;

    @Setter
    private List<String> allFields;
    @Setter
    private TextField fieldEdit;
    @Setter
    private DataQueryResultController parent;

    public void init() {
        fieldView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fieldView.getItems().clear();
        fieldView.getItems().addAll(allFields);

        if (StringUtils.isNotBlank(fieldEdit.getText())) {
            List<String> selectedItems = CommonUtil.split(fieldEdit.getText().trim(), ",");
            List<Integer> selectedIdx = new ArrayList<>();
            for (int i = 0; i < allFields.size(); i++) {
                if (selectedItems.contains(allFields.get(i))) {
                    selectedIdx.add(i);
                }
            }
            if (CollectionUtils.isNotEmpty(selectedIdx) && selectedIdx.size() == 1) {
                fieldView.getSelectionModel().selectIndices(selectedIdx.get(0));
            } else {
                int[] ss = new int[selectedIdx.size() - 1];
                for (int i = 0; i < ss.length; i++) {
                    ss[i] = selectedIdx.get(i+1);
                }
                fieldView.getSelectionModel().selectIndices(selectedIdx.get(0), ss);
            }
        }
    }

    public void onSave(MouseEvent mouseEvent) {
        if (!MouseClickUtils.isLeftSingleClick(mouseEvent)) {
            return;
        }

        List<String> selectedItems = fieldView.getSelectionModel().getSelectedItems();
        if (CollectionUtils.isEmpty(selectedItems)) {
            MessageAlert.information("请选择需要展示的字段");
            return;
        }
        fieldEdit.setText(CommonUtil.join(selectedItems, ","));
        parent.refreshResult();
        close();
    }

    public void onCloseDialog(MouseEvent mouseEvent) {
        if (!MouseClickUtils.isLeftSingleClick(mouseEvent)) {
            return;
        }
        close();
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            // ESC键关闭窗口
            close();
        }
    }

    private void close() {
        Stage stage = (Stage) fieldView.getScene().getWindow();
        stage.close();
    }
}
