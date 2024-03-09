package com.zssql.domain.dto;

import com.zssql.HelloApplication;
import com.zssql.common.enums.MySQLTypeEnum;
import com.zssql.common.enums.ValEditType;
import com.zssql.common.utils.CommonUtil;
import com.zssql.common.utils.MessageAlert;
import com.zssql.controller.BlobValueEditController;
import com.zssql.controller.BlobValueViewController;
import com.zssql.domain.vo.DatasouceTableVO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.controlsfx.control.spreadsheet.*;

import java.io.Serializable;
import java.util.*;

@Data
public class DataResultControlDto implements Serializable {
    // 表数据Tab页签
    private Tab dataTab;
    // 对应表名称
    private String tableName;
    // 表结构
    private DatasouceTableVO tableVO;
    // 表数据网格
    private SpreadsheetView dataView;
    // 表数据Grid
    private GridBase dataGrid;
    // 原始数据
    private SqlValueDto originData;
    // 行数据
    List<DataRowInfo> dataRows;
    // 删除的行数据
    List<DataRowInfo> deleteDataRows;
    // 筛选器只读编辑框
    private TextField filterField;
    // 工具栏按钮
    private Button exportSelectBtn;
    private Button exportAllBtn;
    private Button addRowBtn;
    private Button copyRowBtn;
    private Button saveRowBtn;
    private Button deleteRowBtn;
    private Button cancelRowBtn;
    private Button filterBtn;
    private Button refreshBtn;
    private Button leftBtn;
    private TextField firstLineEdit;
    private Button rightBtn;
    private TextField rowCntEdit;

    // 修改状态
    BooleanProperty isModified = new SimpleBooleanProperty(false);

    private void setModify(boolean modify) {
        isModified.set(modify);
    }

    private boolean getModify() {
        return isModified.get();
    }

    public void showData() {
        if (null == originData) {
            return;
        }

        // 绑定控件事件
        bindControlEvent();

        dataRows = new ArrayList<>();
        deleteDataRows = new ArrayList<>();
        if (null != dataGrid) {
            dataGrid.getRows().clear();
        }
        dataGrid = new GridBase(1, originData.getColumnLabels().size());
        ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();
        Map<Integer, Integer> maxCharMap = new HashMap<>();
        for (int row = 0; row < originData.getVals().size(); row++) {
            rows.add(addRowData(row, originData.getVals().get(row), maxCharMap));
        }
        dataGrid.setRows(rows);
        dataView.setGrid(dataGrid);
        dataView.setContextMenu(null);
        int idx = 0;
        for (SpreadsheetColumn column : dataView.getColumns()) {
            maxCharMap.put(idx, CommonUtil.getMax(originData.getColumnLabels().get(idx).getBytes().length, maxCharMap.get(idx)));
            column.setText(originData.getColumnLabels().get(idx));
            column.setMinWidth(50);
            int factor = 9;
            MySQLTypeEnum colType = MySQLTypeEnum.getByTypeName(originData.getSqlTypeNames().get(idx));
            if (null != colType && colType.isBlob()) {
                factor = 11;
            }
            column.setPrefWidth(CommonUtil.getMin(400, CommonUtil.getValue(maxCharMap.get(idx), 60) * factor));
            idx++;
        }
    }

    private void bindControlEvent() {
        isModified.addListener(((observable, oldValue, newValue) -> {
            Label dataLabel = (Label) dataTab.getGraphic();
            String tabText = dataLabel.getText();
            if (BooleanUtils.isTrue(newValue)) {
                if (!StringUtils.endsWithIgnoreCase(tabText, "*")) {
                    dataLabel.setText(tabText + " *");
                }
            } else {
                if (StringUtils.endsWithIgnoreCase(tabText, "*")) {
                    dataLabel.setText(StringUtils.left(tabText, tabText.length() - 2));
                }
            }
        }));

        dataTab.setOnCloseRequest(event -> {
            if (!getModify()) {
                return;
            }
            ButtonType bt = MessageAlert.confirmSave("表数据已发生变更", "是否保存这些改动");
            if (bt.equals(ButtonType.YES)) {
                // TODO: 提交变更
            } else if (bt.equals(ButtonType.CANCEL)) {
                // 取消，不关闭
                event.consume();
            }
        });

        // 工具栏按钮状态绑定
        saveRowBtn.disableProperty().bind(isModified.not());
        cancelRowBtn.disableProperty().bind(isModified.not());
        CommonUtil.disableControls(copyRowBtn, deleteRowBtn);
        dataView.getSelectionModel().getModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (null == newValue) {
                CommonUtil.disableControls(copyRowBtn, deleteRowBtn);
            } else {
                CommonUtil.enableControls(copyRowBtn, deleteRowBtn);
            }
        }));

        CommonUtil.buttonBind(cancelRowBtn, () -> {
            setModify(false);
            showData();
        });
    }

    private ObservableList<SpreadsheetCell> addRowData(int row, List<Object> vals, Map<Integer, Integer> maxCharMap) {
        ObservableList<SpreadsheetCell> cells = FXCollections.observableArrayList();
        DataRowInfo dataRow = new DataRowInfo();
        dataRow.setCells(cells);
        dataRow.setValLabels(new ArrayList<>());
        dataRow.setOriginVals(new ArrayList<>(vals));
        dataRow.setVals(new ArrayList<>(vals));
        dataRows.add(row, dataRow);
        for (int i = 0; i < vals.size(); i++) {
            Object val = vals.get(i);
            // 字段名
            String columnName = originData.getColumnLabels().get(i);
            TableColumnDto columnDto = tableVO.getColumnInfos().stream().filter(ci -> CommonUtil.isObjectEquals(ci.getName(), columnName)).findAny().orElse(null);
            MySQLTypeEnum colType = MySQLTypeEnum.getByTypeName(null == columnDto? originData.getSqlTypeNames().get(i) : columnDto.getType());
            ValEditType editType = CommonUtil.getValue(MySQLTypeEnum.getValEditType(null == columnDto?
                    originData.getSqlTypeNames().get(i) : columnDto.getType()), ValEditType.STRING);
            String valShow = CommonUtil.toCellString(val, colType);
            SpreadsheetCell cell;
            if (ValEditType.BLOB == editType) {
                // 大字段
                cell = SpreadsheetCellType.STRING.createCell(row, i, 1, 1, null);
                cell.setEditable(false);
                HBox cellHBox = new HBox();
                cellHBox.setPadding(new Insets(0.5));
                cellHBox.setAlignment(Pos.CENTER_LEFT);
                Label valLabel = new Label(valShow);
                if (null == val) {
                    valLabel.setDisable(true);
                }
                valLabel.setAlignment(Pos.CENTER_LEFT);
                dataRow.getValLabels().add(valLabel);
                Separator sep = new Separator(Orientation.HORIZONTAL);
                sep.setVisible(false);
                Button valBtn = new Button("...");
                HBox.setHgrow(sep, Priority.ALWAYS);
                cellHBox.getChildren().addAll(valLabel, sep, valBtn);
                cellHBox.setFillHeight(false);
                cell.setGraphic(cellHBox);
                CommonUtil.buttonBind(valBtn, () -> {
                    try {
                        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("blob-value-edit.fxml"));
                        Parent root = fxmlLoader.load();
                        BlobValueEditController controller = fxmlLoader.getController();
                        controller.init(val);
                        Stage stage = new Stage();
                        stage.setTitle("更新Blob字段值");
                        Scene scene = new Scene(root, 600, 400);
                        stage.setScene(scene);
                        Stage parent = (Stage) valBtn.getScene().getWindow();
                        stage.getIcons().add(new Image("icon.png"));
                        stage.initOwner(parent);
                        stage.initModality(Modality.WINDOW_MODAL);
                        stage.setMaximized(false);
                        stage.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        MessageAlert.error("系统异常");
                    }
                });
            } else {
                HBox valHbox = new HBox();
                valHbox.setPadding(new Insets(0.5, 1, 0.5, 0.5));
                valHbox.setAlignment(Pos.CENTER_LEFT);
                Label valLabel = new Label(valShow);
                valLabel.setAlignment(Pos.CENTER_LEFT);
                valHbox.getChildren().add(valLabel);
                switch (editType) {
                    case INTEGER, U_INTEGER:
                        cell = SpreadsheetCellType.LABEL_LONG(valLabel, ValEditType.U_INTEGER == editType)
                                .createCell(row, i, 1, 1, null);
                        cell.setEditable(true);
                        break;
                    case DECIMAL, U_DECIMAL:
                        cell = SpreadsheetCellType.LABEL_DECIMAL(valLabel, ValEditType.U_DECIMAL == editType)
                                .createCell(row, i, 1, 1, null);
                        cell.setEditable(true);
                        break;
                    case DATE:
                        cell = SpreadsheetCellType.LABEL_DATE(valLabel).createCell(row, i, 1, 1, null);
                        cell.setEditable(true);
                        break;
                    case DATETIME:
                        cell = SpreadsheetCellType.LABEL_DATETIME(valLabel).createCell(row, i, 1, 1, null);
                        cell.setEditable(true);
                        break;
                    case ENUM:
                        cell = SpreadsheetCellType.LABEL_LIST(null == columnDto? Collections.emptyList() : columnDto.getVals(), valLabel)
                                .createCell(row, i, 1, 1, null);
                        cell.setEditable(true);
                        break;
                    case SET:
                        cell = SpreadsheetCellType.LABEL_LIST_CHECK(null == columnDto? Collections.emptyList() : columnDto.getVals(), valLabel)
                                .createCell(row, i, 1, 1, null);
                        cell.setEditable(true);
                        break;
                    case STRING:
                    default:
                        cell = SpreadsheetCellType.LABEL_STRING(valLabel).createCell(row, i, 1, 1, null);
                        cell.setEditable(true);
                        break;
                }
                cell.setGraphic(valHbox);
                if (null == val) {
                    valLabel.setDisable(true);
                }
                dataRow.getValLabels().add(valLabel);
                valLabel.textProperty().addListener((observableValue, oldValue, newValue) -> {
                    DataRowInfo dr = dataRows.get(cell.getRow());
                    if (null == dr) {
                        return;
                    }
                    int colIdx = cell.getColumn();
                    // 更新Label文本
                    if (null == newValue) {
                        valLabel.setText(CommonUtil.toCellString(newValue));
                        valLabel.setDisable(true);
                    } else {
                        valLabel.setDisable(false);
                    }
                    // 更新值
                    dr.getVals().set(colIdx, newValue);
                    // 判断值是否发生变更
                    if (!CommonUtil.isObjectEquals(CommonUtil.toString(dr.getOriginVals().get(colIdx), colType), CommonUtil.toString(newValue, colType))) {
                        // 已发生变更
                        ((Region)valLabel.getParent()).setBackground(new Background(new BackgroundFill(Paint.valueOf("#fce4d6"), CornerRadii.EMPTY, new Insets(1))));
                        dr.getModifyIdxs().add(colIdx);
                    } else {
                        ((Region)valLabel.getParent()).setBackground(new Background(new BackgroundFill(Paint.valueOf("#ffffff"), CornerRadii.EMPTY, new Insets(1))));
                        dr.getModifyIdxs().remove(colIdx);
                    }
                    updateControlStatus();
                });
            }
            cells.add(cell);
            if (null != maxCharMap) {
                if (!maxCharMap.containsKey(i)) {
                    maxCharMap.put(i, CommonUtil.getValue(CommonUtil.toString(val, colType), "(NULL)").getBytes().length);
                } else {
                    maxCharMap.put(i, CommonUtil.getMax(CommonUtil.getValue(CommonUtil.toString(val, colType), "(NULL)").getBytes().length, maxCharMap.get(i)));
                }
            }
        }
        return cells;
    }

    public void updateControlStatus() {
        // 判断表数据是否发生变更
        boolean isModified = false;
        for (DataRowInfo dataRow : dataRows) {
            if (CollectionUtils.isNotEmpty(dataRow.getModifyIdxs())) {
                isModified = true;
                break;
            }
        }
        setModify(isModified);

        CommonUtil.enableControls(exportSelectBtn, exportAllBtn, addRowBtn, copyRowBtn, deleteRowBtn, filterBtn, refreshBtn, leftBtn, firstLineEdit, rightBtn, rowCntEdit);
        int firstLine = Integer.parseInt(firstLineEdit.getText());
        int rowCnt = Integer.parseInt(rowCntEdit.getText());
        if (firstLine <= 0) {
            CommonUtil.disableControls(leftBtn);
        }
        if (dataView.getItems().size() < rowCnt) {
            CommonUtil.disableControls(rightBtn);
        }
    }

    @Data
    protected class DataRowInfo implements Serializable {
        private List<SpreadsheetCell> cells;
        private List<Label> valLabels;
        private List<Object> originVals;
        private List<Object> vals;
        private Set<Integer> modifyIdxs = new HashSet<>();
        private boolean deleted = false;
    }
}
