package com.zssql.controller;

import com.zssql.HelloController;
import com.zssql.common.utils.MouseClickUtils;
import com.zssql.domain.dto.Config;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

public class ConfigController {
    @FXML
    private CheckBox autoExecuteCursorSqlCheckBox;
    @FXML
    private CheckBox focusSqlEditorAfterExecuteCheckBox;
    @FXML
    private CheckBox restoreConnAfterStartCheckBox;
    @FXML
    private CheckBox restoreWindowPosAfterStartCheckBox;
    @FXML
    private CheckBox tipSaveAfterCloseTabCheckBox;

    @FXML
    private Button cancelBtn;

    @Setter
    private HelloController parent;

    public void init(Config config) {
        focusSqlEditorAfterExecuteCheckBox.setSelected(BooleanUtils.isTrue(config.getFocusSqlEditorAfterExecute()));
        restoreWindowPosAfterStartCheckBox.setSelected(BooleanUtils.isTrue(config.getRestoreWindowPosAfterStart()));
        restoreConnAfterStartCheckBox.setSelected(BooleanUtils.isTrue(config.getRestoreConnAfterStart()));
        tipSaveAfterCloseTabCheckBox.setSelected(BooleanUtils.isTrue(config.getTipSaveAfterCloseTab()));
        autoExecuteCursorSqlCheckBox.setSelected(BooleanUtils.isTrue(config.getAutoExecuteCursorSql()));
    }

    @FXML
    void onClose(MouseEvent event) {
        if (!MouseClickUtils.isLeftSingleClick(event)) {
            return;
        }
        close();
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

    @FXML
    void onSave(MouseEvent event) {
        Config config = new Config();
        config.setFocusSqlEditorAfterExecute(focusSqlEditorAfterExecuteCheckBox.isSelected());
        config.setRestoreWindowPosAfterStart(restoreWindowPosAfterStartCheckBox.isSelected());
        config.setRestoreConnAfterStart(restoreConnAfterStartCheckBox.isSelected());
        config.setTipSaveAfterCloseTab(tipSaveAfterCloseTabCheckBox.isSelected());
        config.setAutoExecuteCursorSql(autoExecuteCursorSqlCheckBox.isSelected());
        parent.saveConfig(config);
        close();
    }
}
