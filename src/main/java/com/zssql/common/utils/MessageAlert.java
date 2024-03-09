package com.zssql.common.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class MessageAlert {

    public static void information(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void warning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> ob = alert.showAndWait();
        ButtonType bt = ob.isPresent()? ob.get() :ButtonType.CANCEL;
        return bt == ButtonType.OK;
    }

    public static ButtonType confirmSave(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.setTitle("提示");
        alert.setHeaderText(header);
        alert.setContentText(message);
        Optional<ButtonType> ob = alert.showAndWait();
        return ob.isPresent()? ob.get() :ButtonType.CANCEL;
    }

    public static ButtonType allSave = new ButtonType("都保存");
    public static ButtonType allNotSave = new ButtonType("都不保存");
    public static ButtonType confirmConnSqlSave(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, null, ButtonType.YES, ButtonType.NO, allSave, allNotSave, ButtonType.CANCEL);
        alert.setTitle("提示");
        alert.setHeaderText(header);
        alert.setContentText(message);
        Optional<ButtonType> ob = alert.showAndWait();
        return ob.isPresent()? ob.get() : ButtonType.CANCEL;
    }
}
