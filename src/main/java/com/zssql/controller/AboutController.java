package com.zssql.controller;

import com.zssql.common.config.SystemConfig;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class AboutController implements Initializable {
    public Label versionLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        versionLabel.setText("版本号: " + SystemConfig.VERSION);
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            close();
        }
    }

    private void close() {
        Stage stage = (Stage) versionLabel.getScene().getWindow();
        stage.close();
    }

    public void onMouseClicked(MouseEvent mouseEvent) {
        close();
    }
}
