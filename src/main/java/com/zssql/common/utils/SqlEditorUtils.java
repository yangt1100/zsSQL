package com.zssql.common.utils;

import cn.hutool.core.lang.Pair;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;

public class SqlEditorUtils {

    public static String getAllSqlCode(WebView sqlCodeView) {
        return (String) sqlCodeView.getEngine().executeScript("editor.getValue();");
    }

    public static String getSelectSqlCode(WebView sqlCodeView) {
        return (String) sqlCodeView.getEngine().executeScript("editor.getSelection();");
    }

    public static void setSqlCode(WebView sqlCodeView, String val) {
        if (null == val) {
            val = "";
        }

        val = StringUtils.replace(val, "\n", "\\n");
        val = StringUtils.replace(val, "\r", "\\r");
        val = StringUtils.replace(val, "\"", "\\\"");

        sqlCodeView.getEngine().executeScript(String.format("editor.setValue(\"%s\");", val));
    }

    public static Pair<Integer, Integer> getCursor(WebView sqlCodeView, boolean isCursorStart) {
        JSObject obj = (JSObject) sqlCodeView.getEngine().executeScript(String.format("editor.getCursor(%s);", isCursorStart? "true" : "false"));
        if (null == obj) {
            return new Pair<>(0, 0);
        }
        return new Pair<>(Integer.valueOf(obj.getMember("line").toString()), Integer.valueOf(obj.getMember("ch").toString()));
    }

    public static void insertSqlCode(WebView sqlCodeView, String val) {
        if (StringUtils.isBlank(val)) {
            return;
        }
        val = StringUtils.replace(val, "\n", "\\n");
        val = StringUtils.replace(val, "\r", "\\r");
        val = StringUtils.replace(val, "\"", "\\\"");
        sqlCodeView.getEngine().executeScript(String.format("var cursor = editor.getCursor(); editor.replaceRange(\"%s\", cursor, cursor);", val));
    }

    public static void replaceSelect(WebView sqlCodeView, String val) {
        val = StringUtils.replace(val, "\n", "\\n");
        val = StringUtils.replace(val, "\r", "\\r");
        val = StringUtils.replace(val, "\"", "\\\"");
        sqlCodeView.getEngine().executeScript(String.format("editor.replaceSelection(\"%s\");", val));
    }

    public static void undo(WebView sqlCodeView) {
        sqlCodeView.getEngine().executeScript("editor.undo();");
    }

    public static void redo(WebView sqlCodeView) {
        sqlCodeView.getEngine().executeScript("editor.redo();");
    }

    public static int getLineCount(WebView sqlCodeView) {
        return (int) sqlCodeView.getEngine().executeScript("editor.lineCount();");
    }

    public static String getLine(WebView sqlCodeView, int line) {
        return (String) sqlCodeView.getEngine().executeScript(String.format("editor.getLine(%d);", line));
    }

    public static void setCursor(WebView sqlCodeView, int line, int ch) {
        sqlCodeView.getEngine().executeScript(String.format("editor.setCursor(%d, %d);", line, ch));
    }

    public static void select(WebView sqlCodeView, int fromLine, int fromCh, int toLine, int toCh) {
        sqlCodeView.getEngine().executeScript(String.format("editor.setSelection({line:%d, ch:%d}, {line:%d, ch:%d});", fromLine, fromCh, toLine, toCh));
    }

    public static void selectLine(WebView sqlCodeView, int line) {
        int lineCount = getLineCount(sqlCodeView);
        if (line >= lineCount) {
            return;
        }
        String lineInfo = getLine(sqlCodeView, line);
        select(sqlCodeView, line, 0, line, lineInfo.length());
    }

    /**
     * SQL编辑器末尾添加SQL语句
     * @param sqlCodeView
     * @param sql
     */
    public static void appendNewLine(WebView sqlCodeView, String sql) {
        if (StringUtils.isBlank(sql)) {
            return;
        }
        int lineCount = getLineCount(sqlCodeView);
        String currCode = getLine(sqlCodeView, lineCount - 1);
        if (1 == lineCount && StringUtils.isBlank(currCode)) {
            insertSqlCode(sqlCodeView, sql);
        } else {
            // 光标定位到最后一行的最后一个字符后
            setCursor(sqlCodeView, lineCount - 1, currCode.length());
            insertSqlCode(sqlCodeView, "\n" + sql);
            lineCount += 1;
        }
    }
}
