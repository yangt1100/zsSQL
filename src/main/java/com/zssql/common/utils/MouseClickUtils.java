package com.zssql.common.utils;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class MouseClickUtils {

    public static boolean isLeftSingleClick(MouseEvent event) {
        if (null == event) {
            return false;
        }
        return event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1;
    }

    public static boolean isLeftDoubleClick(MouseEvent event) {
        if (null == event) {
            return false;
        }
        return event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2;
    }
}
