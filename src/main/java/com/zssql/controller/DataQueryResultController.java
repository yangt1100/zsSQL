package com.zssql.controller;

import com.zssql.HelloApplication;
import com.zssql.common.utils.CommonUtil;
import com.zssql.common.utils.HtmlTableUtil;
import com.zssql.common.utils.MessageAlert;
import com.zssql.common.utils.MouseClickUtils;
import com.zssql.domain.dto.SqlValueDto;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;

import java.net.URL;
import java.util.*;

public class DataQueryResultController implements Initializable {
    public WebView resultView;
    public ComboBox<ShowType> showTypeComboBox;
    public TextField fieldEdit;
    public Button fieldBtn;

    private SqlValueDto valueDto;
    private List<Integer> selectIdxs;
    private boolean isCompare;

    @Getter
    public enum ShowType {
        ALL("全部字段"),
        APOINT("指定字段"),
        DIFFER("差异字段"),
        ;

        private String name;

        ShowType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static List<ShowType> single() {
            return Arrays.asList(ALL, APOINT);
        }

        public static List<ShowType> compare() {
            return Arrays.asList(ALL, DIFFER, APOINT);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        CommonUtil.hideControls(fieldEdit, fieldBtn);
    }

    public void init(SqlValueDto valueDto, List<Integer> selectIdxs, boolean isCompare) {
        if (CollectionUtils.isEmpty(selectIdxs)) {
            close();
            return;
        }

        this.valueDto = valueDto;
        this.selectIdxs = selectIdxs;
        this.isCompare = isCompare;

        showTypeComboBox.getItems().clear();
        if (isCompare) {
            showTypeComboBox.getItems().addAll(ShowType.compare());
        } else {
            showTypeComboBox.getItems().addAll(ShowType.single());
        }

        showTypeComboBox.getSelectionModel().selectedItemProperty().addListener((observableValue, oldShowType, newShowType) -> {
            switch (newShowType) {
                case ALL, DIFFER:
                    CommonUtil.hideControls(fieldEdit, fieldBtn);
                    break;
                case APOINT:
                    CommonUtil.showControls(fieldEdit, fieldBtn);
                    break;
            }
            refreshResult();
        });

        showTypeComboBox.getSelectionModel().select(ShowType.ALL);
    }

    public List<String> getShowFields() {
        switch (showTypeComboBox.getSelectionModel().getSelectedItem()) {
            case ALL:
                return valueDto.getColumnLabels();
            case APOINT:
                return CommonUtil.split(fieldEdit.getText(), ",");
        }
        return valueDto.getColumnLabels();
    }

    public void onFieldBtnMouseClicked(MouseEvent mouseEvent) {
        if (!MouseClickUtils.isLeftSingleClick(mouseEvent)) {
            return;
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("data-field-select.fxml"));
            Parent root = fxmlLoader.load();
            DataFieldSelectController controller = fxmlLoader.getController();
            controller.setAllFields(valueDto.getColumnLabels());
            controller.setFieldEdit(fieldEdit);
            controller.setParent(this);
            controller.init();
            Stage stage = new Stage();
            stage.setTitle("字段选择");
            Scene scene = new Scene(root, 240, 320);
            stage.setScene(scene);
            Stage parent = (Stage) resultView.getScene().getWindow();
            stage.getIcons().clear();
            stage.initOwner(parent);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            MessageAlert.error("系统异常");
        }
    }

    public void refreshResult() {
        if (!isCompare) {
            // 结果查看
            String html = assembleResultHtml(selectIdxs.get(0));
            resultView.getEngine().loadContent(html);
        } else {
            // 结果比对
            String html = assembleCompareHtml();
            resultView.getEngine().loadContent(html);
        }
    }

    private String assembleResultHtml(int showIdx) {
        List<List<String>> values = new ArrayList<>();
        int idx = 1;
        List<String> showFields = getShowFields();
        List<Object> vals = valueDto.getVals().get(showIdx);
        for (int i = 0; i < valueDto.getColumnLabels().size(); i++) {
            String columnLable = valueDto.getColumnLabels().get(i);
            Object val = vals.size() > i? vals.get(i) : "";
            String comment = CollectionUtils.isNotEmpty(valueDto.getColumnComments()) && valueDto.getColumnComments().size() > i? valueDto.getColumnComments().get(i) : "";

            if (!showFields.contains(columnLable)) {
                continue;
            }
            values.add(Arrays.asList(String.valueOf(idx++), columnLable, (null == val? "" : val.toString()), comment));
        }
        return HtmlTableUtil.htmlTable(Arrays.asList("序号", "字段", "值", "注释"), values, "查看记录", true);
    }

    private String assembleCompareHtml() {
        List<String> headers = new ArrayList<>();
        List<List<String>> values = new ArrayList<>();

        // 记录数量
        int count = selectIdxs.size();
        // 表头
        headers.add("序号");
        headers.add("字段");
        for (int i = 0; i < count; i++) {
            headers.add("记录" + (i+1));
        }
        headers.add("注释");

        List<String> showFields = getShowFields();
        // 数据拼接
        int idx = 1;
        for (int col = 0; col < valueDto.getColumnLabels().size(); col++) {
            String columnLabel = valueDto.getColumnLabels().get(col);
            if (!showFields.contains(columnLabel)) {
                continue;
            }
            // 当前字段值
            List<Object> objects = new ArrayList<>();
            for (Integer selectIdx : selectIdxs) {
                List<Object> val = valueDto.getVals().get(selectIdx);
                if (val.size() > col) {
                    objects.add(val.get(col));
                } else {
                    objects.add(null);
                }
            }
            boolean isAllEquals = isAllEquals(objects);

            if (showTypeComboBox.getSelectionModel().getSelectedItem() == ShowType.DIFFER && isAllEquals) {
                continue;
            }

            List<String> value = new ArrayList<>();
            value.add(String.valueOf(idx++));
            value.add(columnLabel);
            for (int i = 0; i < count; i++) {
                if (i >= objects.size()) {
                    break;
                }
                if (!isAllEquals) {
                    value.add("<p style=\"color: red;\">" + (null == objects.get(i)? "" : objects.get(i)) + "</p>");
                } else {
                    value.add(null == objects.get(i)? "" : objects.get(i).toString());
                }
            }
            // 注释
            value.add(CollectionUtils.isNotEmpty(valueDto.getColumnComments()) && valueDto.getColumnComments().size() > col? valueDto.getColumnComments().get(col) : "");
            values.add(value);
        }

        return HtmlTableUtil.htmlTable(headers, values, "结果比对", true);
    }

    private boolean isAllEquals(List<Object> objects) {
        if (CollectionUtils.isEmpty(objects) || 1 == objects.size()) {
            return true;
        }

        for (int i = 1; i < objects.size(); i++) {
            if (!CommonUtil.isObjectEquals(objects.get(0), objects.get(i))) {
                return false;
            }
        }

        return true;
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            // ESC键关闭窗口
            close();
        }
    }

    private void close() {
        Stage stage = (Stage) resultView.getScene().getWindow();
        stage.close();
    }
}
