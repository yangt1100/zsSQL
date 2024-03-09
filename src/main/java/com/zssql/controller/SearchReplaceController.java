package com.zssql.controller;

import cn.hutool.core.lang.Pair;
import com.zssql.HelloController;
import com.zssql.common.utils.CommonUtil;
import com.zssql.common.utils.MessageAlert;
import com.zssql.common.utils.MouseClickUtils;
import com.zssql.common.utils.SqlEditorUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SearchReplaceController {
    public Button findBtn;
    public Button replaceBtn;
    public Button replaceAllBtn;
    public HBox replaceContextHBox;
    public TextField replaceContextEdit;
    @FXML
    private TextField findContextEdit;
    @FXML
    private CheckBox matchWholeCheckBox;
    @FXML
    private CheckBox caseSensitiveCheckBox;

    private final ToggleGroup searchDirection = new ToggleGroup();
    @FXML
    private RadioButton downSearchRadioBtn;
    @FXML
    private RadioButton upSearchRadioBtn;

    @Setter
    private WebView sqlCodeEditor;
    @Setter
    private HelloController parent;

    private static String findContext = null;
    private static String replaceContext = null;
    private static boolean matchWhole = false;
    private static boolean caseSensitive = false;
    private static boolean upSearch = false;
    private static boolean downSearh = true;

    private Pair<Integer, Integer> currCursor;
    private boolean isFind;

    public void init(boolean isFind) {
        this.isFind = isFind;
        searchDirection.getToggles().addAll(upSearchRadioBtn, downSearchRadioBtn);
        findContextEdit.textProperty().addListener((observable, oldValue, newValue) -> {
            if (null == newValue || newValue.isEmpty()) {
                findBtn.setDisable(true);
                replaceBtn.setDisable(true);
                replaceAllBtn.setDisable(true);
            } else {
                findBtn.setDisable(false);
                replaceBtn.setDisable(false);
                replaceAllBtn.setDisable(false);
            }
        });
        String code = SqlEditorUtils.getSelectSqlCode(sqlCodeEditor);
        if (StringUtils.isNotBlank(code)) {
            findContextEdit.setText(code);
        } else {
            findContextEdit.setText(findContext);
        }
        replaceContextEdit.setText(replaceContext);
        matchWholeCheckBox.setSelected(matchWhole);
        caseSensitiveCheckBox.setSelected(caseSensitive);
        upSearchRadioBtn.setSelected(upSearch);
        downSearchRadioBtn.setSelected(downSearh);
        if (isFind) {
            CommonUtil.hideControls(replaceContextHBox, replaceBtn, replaceAllBtn);
        }
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            close();
        } else if (keyEvent.getCode() == KeyCode.ENTER) {
            findNext();
        }
    }

    public void onFindNext(MouseEvent mouseEvent) {
        findNext();
    }

    public void onFindNextBtnKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            findNext();
        }
    }

    // 查找
    private void findNext() {
        // 暂存当前数据
        findContext = findContextEdit.getText();
        replaceContext = replaceContextEdit.getText();
        matchWhole = matchWholeCheckBox.isSelected();
        caseSensitive = caseSensitiveCheckBox.isSelected();
        upSearch = upSearchRadioBtn.isSelected();
        downSearh = downSearchRadioBtn.isSelected();

        // 获取当前光标位置
        if (upSearch) {
            currCursor = SqlEditorUtils.getCursor(sqlCodeEditor, true);
        } else {
            currCursor = SqlEditorUtils.getCursor(sqlCodeEditor, false);
        }

        // 编辑器当前代码
        String sqlCode = SqlEditorUtils.getAllSqlCode(sqlCodeEditor);
        if (StringUtils.isEmpty(sqlCode) || positionFindContext(sqlCode) < 0) {
            MessageAlert.information(String.format("未找到\"%s\"", findContext));
            return;
        }
        // 按行拆分代码
        List<String> codes = CommonUtil.split(sqlCode, "\n");
        if (CollectionUtils.isEmpty(codes)) {
            MessageAlert.information(String.format("未找到\"%s\"", findContext));
            return;
        }

        // 按行进行查找
        String lineInfo;
        if (upSearch) {
            lineInfo = StringUtils.left(codes.get(currCursor.getKey()), currCursor.getValue());
        } else {
            lineInfo = StringUtils.substring(codes.get(currCursor.getKey()), currCursor.getValue());
        }
        int curLine = currCursor.getKey();
        int count = 0;
        do {
            // 查找
            int pos = positionFindContext(lineInfo);
            if (pos >= 0) {
                if (downSearh && 0 == count) {
                    pos += currCursor.getValue();
                }
                // 当前行查找到内容, 则进行选中
                SqlEditorUtils.select(sqlCodeEditor, curLine, pos, curLine, pos + findContext.length());
                return;
            }
            // 未查找到
            if (upSearch) {
                curLine -= 1;
                if (curLine < 0) {
                    curLine = codes.size() - 1;
                }
            } else {
                curLine += 1;
                if (curLine >= codes.size()) {
                    curLine = 0;
                }
            }
            lineInfo = codes.get(curLine);
            count++;
        } while (count < codes.size() + 1);
        MessageAlert.information(String.format("未找到\"%s\"", findContext));
    }

    /**
     * 定位查找内容
     * @param sqlCode
     * @return
     */
    private int positionFindContext(String sqlCode) {
        if (null == sqlCode) {
            return -1;
        }
        while (true) {
            int position = -1;
            // 根据条件查找位置信息
            if (caseSensitive) {
                if (upSearch) {
                    position = StringUtils.lastIndexOf(sqlCode, findContext);
                } else {
                    position = StringUtils.indexOf(sqlCode, findContext);
                }
            } else {
                if (upSearch) {
                    position = StringUtils.lastIndexOfIgnoreCase(sqlCode, findContext);
                } else {
                    position = StringUtils.indexOfIgnoreCase(sqlCode, findContext);
                }
            }
            if (position < 0) {
                // 未找到
                return -1;
            }
            if (!matchWhole) {
                // 不全字匹配
                return position;
            }
            // 判断当前是否为全字匹配
            int start = position - 1;
            int end = position + findContext.length() + 1;
            if (start < 0) {
                start = 0;
            }
            if (end > sqlCode.length()) {
                end = sqlCode.length();
            }
            String temp = StringUtils.substring(sqlCode, start, end);
            if (StringUtils.equalsIgnoreCase(StringUtils.trim(temp), findContext)) {
                // 为全字匹配
                return position;
            }
            // 不为全字匹配则继续匹配
            if (upSearch) {
                sqlCode = StringUtils.left(sqlCode, position);
            } else {
                sqlCode = StringUtils.substring(sqlCode, position + findContext.length());
            }
        }
    }

    public void onCancel(MouseEvent mouseEvent) {
        if (!MouseClickUtils.isLeftSingleClick(mouseEvent)) {
            return;
        }
        close();
    }

    private void close() {
        parent.findingOrReplacing = false;
        Stage stage = (Stage) findContextEdit.getScene().getWindow();
        stage.close();
    }

    public void onReplace(MouseEvent mouseEvent) {
        String code = SqlEditorUtils.getSelectSqlCode(sqlCodeEditor);
        boolean selectMatch;
        if (caseSensitive) {
            selectMatch = StringUtils.equals(code, findContext);
        } else {
            selectMatch = StringUtils.equalsIgnoreCase(code, findContext);
        }
        if (selectMatch) {
            // 选中的文本与查找内容一直则直接替换
            SqlEditorUtils.replaceSelect(sqlCodeEditor, CommonUtil.getValue(replaceContext, ""));
            return;
        }
        // 查找下一个
        findNext();
    }

    public void onReplaceAll(MouseEvent mouseEvent) {
        // 暂存当前数据
        findContext = findContextEdit.getText();
        replaceContext = replaceContextEdit.getText();
        matchWhole = matchWholeCheckBox.isSelected();
        caseSensitive = caseSensitiveCheckBox.isSelected();
        upSearch = upSearchRadioBtn.isSelected();
        downSearh = downSearchRadioBtn.isSelected();

        String sqlCode = SqlEditorUtils.getAllSqlCode(sqlCodeEditor);
        if (StringUtils.isEmpty(sqlCode)) {
            MessageAlert.information(String.format("替换成功, 本次共替换了 %d 个项目", 0));
            return;
        }

        int count = 0;
        StringBuilder sb = new StringBuilder();
        List<String> codes = CommonUtil.split(sqlCode, "\n");
        // 逐行替换
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            if (StringUtils.isNotEmpty(code)) {
                while (true) {
                    int pos;
                    if (caseSensitive) {
                        pos = StringUtils.indexOf(code, findContext);
                    } else {
                        pos = StringUtils.indexOfIgnoreCase(code, findContext);
                    }
                    if (pos < 0) {
                        break;
                    }
                    // 完成一次替换
                    code = StringUtils.left(code, pos) + CommonUtil.getValue(replaceContext, "") + StringUtils.substring(code, pos + findContext.length());
                    count++;
                }
            }
            sb.append(code);
            if (i < codes.size() - 1) {
                sb.append("\n");
            }
        }
        SqlEditorUtils.setSqlCode(sqlCodeEditor, sb.toString());
        MessageAlert.information(String.format("替换成功, 本次共替换了 %d 个项目", count));
    }
}
