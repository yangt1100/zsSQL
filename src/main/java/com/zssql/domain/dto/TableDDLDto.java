package com.zssql.domain.dto;

import com.zssql.HelloApplication;
import com.zssql.common.enums.DDLColHeaderEnum;
import com.zssql.common.enums.DDLIndexHeaderEnum;
import com.zssql.common.enums.MySQLTypeEnum;
import com.zssql.common.enums.SQLCreateLengthType;
import com.zssql.common.utils.CommonUtil;
import com.zssql.common.utils.MessageAlert;
import com.zssql.common.utils.MouseClickUtils;
import com.zssql.common.utils.SqlEditorUtils;
import com.zssql.controller.IndexSelectViewController;
import com.zssql.domain.dto.ddl.ColumnRow;
import com.zssql.domain.dto.ddl.IndexRow;
import com.zssql.domain.vo.DatasouceTableVO;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.spreadsheet.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * DDL页面处理实体
 */
@Data
public class TableDDLDto implements Serializable {
    // 页签
    private Tab ddlTab;
    // 原始表数据, 创建表时为null
    private final DatasouceTableVO originTableVO;
    // 表信息
    private TextField tableNameEdit;
    private TextField tableCommentEdit;
    private TextField tableEngineEdit;
    private TextField tableCharsetEdit;
    private Button saveBtn;
    private Button cancelBtn;
    private TabPane tableTabPane;
    private Tab columnTab, indexTab, reviewTab;
    // 列视图
    private Button addColumnBtn;
    private Button removeColumnBtn;
    private Button moveUpColumnBtn;
    private Button moveDownColumnBtn;
    private SpreadsheetView columnView;
    private GridBase columnGrid;
    private List<ColumnRow> columnRows;
    // 索引视图
    private Button addIndexBtn, removeIndexBtn;
    private SpreadsheetView indexView;
    private GridBase indexGrid;
    private List<IndexRow> indexRows;
    // SQL预览视图
    private WebView codeView;
    // 已删除的列
    private List<ColumnRow> deletedColRows;
    private List<IndexRow> deletedIndexRows;

    private final BooleanProperty modifiedFlg = new SimpleBooleanProperty(this, "modified", false);

    private static final int ROW_SPAN = 1;
    private static final int COL_SPAN = 1;

    public static final String DEFAULT_CODE_TEXT = "/* 没有需要提交执行的SQL */";
    public static final String PRIMARY_INDEX_NAME = "PRIMARY";

    public TableDDLDto(DatasouceTableVO tableVO) {
        this.originTableVO = tableVO;

        columnRows = new ArrayList<>();
        deletedColRows = new ArrayList<>();

        indexRows = new ArrayList<>();
        deletedIndexRows = new ArrayList<>();
    }

    private void setModified(boolean value) { modifiedFlg.set(value); }
    private boolean isModified() { return modifiedFlg.get(); }

    private List<TableColumnDto> getOriginColumns() {
        return null == originTableVO? Collections.emptyList() : originTableVO.getColumnInfos();
    }

    private List<DatasouceTableVO.TableIndexVO> getOriginIndexs() {
        return null == originTableVO? Collections.emptyList() : originTableVO.allIndexs();
    }

    public void initalTableDDLView() {
        // 修改状态标识值变化事件处理
        modifiedFlg.addListener((observable, oldValue, newValue) -> {
            Label tabLabel = (Label)ddlTab.getGraphic();
            if (BooleanUtils.isTrue(newValue)) {
                // 发生变更
                if (!StringUtils.endsWithIgnoreCase(tabLabel.getText(), "*")) {
                    tabLabel.setText(tabLabel.getText() + " *");
                }
            } else {
                // 未发生变更
                if (StringUtils.endsWithIgnoreCase(tabLabel.getText(), "*")) {
                    tabLabel.setText(StringUtils.left(tabLabel.getText(), tabLabel.getText().length() - 2));
                }
            }
        });
        // DDL页签关闭事件处理
        ddlTab.setOnCloseRequest(event -> {
            if (!isModified()) {
                return;
            }
            ButtonType bt = MessageAlert.confirmSave("该选项卡的内容已被更改", "是否保存这些改动");
            if (bt.equals(ButtonType.YES)) {
                // 保存变更
                if (!effectDDL()) {
                    // 保存失败则不关闭页签
                    event.consume();
                }
            } else if (bt.equals(ButtonType.CANCEL)) {
                // 点击取消不关闭页签
                event.consume();
            }
        });

        // SQL预览页签被选中时需要生成DDL语句
        tableTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (reviewTab == newValue) {
                buildDDLSQL();
            }
        });

        // 列视图初始化
        columnGrid = new GridBase(1, DDLColHeaderEnum.headerNames().size());
        ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();
        if (CollectionUtils.isEmpty(getOriginColumns())) {
            // 新建表
            for (int i=0; i<6; i++) {
                rows.add(addColumnRow(i, new TableColumnDto()));
            }
        } else {
            for (int i = 0; i < getOriginColumns().size(); i++) {
                rows.add(addColumnRow(i, getOriginColumns().get(i)));
            }
            // 额外新增一个空行
            rows.add(addColumnRow(getOriginColumns().size(), new TableColumnDto()));
        }

        columnGrid.setRows(rows);
        columnView.setGrid(columnGrid);

        int idx = 0;
        for (SpreadsheetColumn column : columnView.getColumns()) {
            column.setText(DDLColHeaderEnum.headerNames().get(idx++));
        }
        columnView.setContextMenu(null);
        // 更新所有行状态
        updateColumnRowStatus();

        // 工具栏按钮状态变更
        CommonUtil.disableControls(removeColumnBtn, moveUpColumnBtn, moveDownColumnBtn);
        columnView.getSelectionModel().getModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (null == newValue) {
                CommonUtil.disableControls(removeColumnBtn, moveUpColumnBtn, moveDownColumnBtn);
                return;
            }
            updateColumnViewToolStatus();
        });

        CommonUtil.buttonBind(addColumnBtn, () -> {
            int insertIdx = columnView.getItems().size();
            List<TablePosition> selectedCells = columnView.getSelectionModel().getSelectedCells();
            if (CollectionUtils.isNotEmpty(selectedCells)) {
                insertIdx = selectedCells.get(0).getRow();
            }
            insertColumnRow(insertIdx, null);
        });
        CommonUtil.buttonBind(removeColumnBtn, () -> {
            List<TablePosition> selectedCells = columnView.getSelectionModel().getSelectedCells();
            if (CollectionUtils.isEmpty(selectedCells)) {
                return;
            }
            if (!MessageAlert.confirm("确定删除选中列?")) {
                return;
            }
            removeColumnRow(selectedCells.get(0).getRow());
        });
        CommonUtil.buttonBind(moveUpColumnBtn, () -> {
            List<TablePosition> selectedCells = columnView.getSelectionModel().getSelectedCells();
            if (CollectionUtils.isEmpty(selectedCells)) {
                return;
            }
            moveUpColumnRow(selectedCells.get(0).getRow(), selectedCells.get(0).getColumn());
        });
        CommonUtil.buttonBind(moveDownColumnBtn, () -> {
            List<TablePosition> selectedCells = columnView.getSelectionModel().getSelectedCells();
            if (CollectionUtils.isEmpty(selectedCells)) {
                return;
            }
            moveDownColumnRow(selectedCells.get(0).getRow(), selectedCells.get(0).getColumn());
        });
        CommonUtil.buttonBind(cancelBtn, this::cancelModify);

        for (TextField textField : Arrays.asList(tableNameEdit, tableCommentEdit, tableEngineEdit, tableCharsetEdit)) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                setModified(true);
                if (reviewTab == tableTabPane.getSelectionModel().getSelectedItem()) {
                    // 当前选中的为SQL预览页签，则需要构建DDL语句
                    buildDDLSQL();
                }
            });
        }

        // 索引视图初始化
        indexGrid = new GridBase(1, DDLIndexHeaderEnum.headerNames().size());
        ObservableList<ObservableList<SpreadsheetCell>> idxRows = FXCollections.observableArrayList();
        if (CollectionUtils.isNotEmpty(getOriginIndexs())) {
            for (int i = 0; i < getOriginIndexs().size(); i++) {
                idxRows.add(addIndexRow(i, getOriginIndexs().get(i)));
            }
        }
        // 额外新增一个空行
        idxRows.add(addIndexRow(getOriginIndexs().size(), new DatasouceTableVO.TableIndexVO()));

        indexGrid.setRows(idxRows);
        indexView.setGrid(indexGrid);

        idx = 0;
        for (SpreadsheetColumn column : indexView.getColumns()) {
            if (DDLIndexHeaderEnum.IDX_NAME.getColIdx() == idx) {
                column.setPrefWidth(190);
                column.setMinWidth(120);
            } else if (DDLIndexHeaderEnum.IDX_COL.getColIdx() == idx) {
                column.setPrefWidth(230);
                column.setMinWidth(160);
            }
            column.setText(DDLIndexHeaderEnum.headerNames().get(idx++));
        }
        indexView.setContextMenu(null);
        // 更新所有行状态
        updateIndexRowStatus();
        // 工具栏按钮状态变更
        CommonUtil.disableControls(removeIndexBtn);
        indexView.getSelectionModel().getModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (null == newValue) {
                CommonUtil.disableControls(removeIndexBtn);
                return;
            }
            updateIndexViewToolStatus();
        });
        // 工具栏按钮事件绑定
        CommonUtil.buttonBind(addIndexBtn, () -> {
            insertIndexRow( indexRows.size(), null);
        });
        CommonUtil.buttonBind(removeIndexBtn, () -> {
            List<TablePosition> selectedCells = indexView.getSelectionModel().getSelectedCells();
            if (CollectionUtils.isEmpty(selectedCells)) {
                return;
            }
            if (!MessageAlert.confirm("确定删除选中索引?")) {
                return;
            }
            removeIndexRow(selectedCells.get(0).getRow());
        });

        // 操作按钮状态绑定
        saveBtn.disableProperty().bind(modifiedFlg.not());
        cancelBtn.disableProperty().bind(modifiedFlg.not());
    }

    /**
     * 更新列视图工具栏状态
     */
    private void updateColumnViewToolStatus() {
        List<TablePosition> selectedCells = columnView.getSelectionModel().getSelectedCells();
        if (CollectionUtils.isEmpty(selectedCells)) {
            CommonUtil.disableControls(removeColumnBtn, moveUpColumnBtn, moveDownColumnBtn);
        } else {
            CommonUtil.enableControls(removeColumnBtn, moveUpColumnBtn, moveDownColumnBtn);
            int selectRow = selectedCells.get(0).getRow();
            if (0 == selectRow) {
                CommonUtil.disableControls(moveUpColumnBtn);
            }
            if (columnView.getItems().size() - 1 == selectRow) {
                CommonUtil.disableControls(moveDownColumnBtn);
            }
        }
    }

    private void updateIndexViewToolStatus() {
        List<TablePosition> selectedCells = indexView.getSelectionModel().getSelectedCells();
        if (CollectionUtils.isEmpty(selectedCells)) {
            CommonUtil.disableControls(removeIndexBtn);
        } else {
            CommonUtil.enableControls(removeIndexBtn);
        }
    }

    private ObservableList<SpreadsheetCell> addColumnRow(int row, TableColumnDto dto) {
        ColumnRow columnRow = new ColumnRow();
        columnRow.setOriginColumn(dto);
        final ObservableList<SpreadsheetCell> cells = FXCollections.observableArrayList();

        // 列名
        SpreadsheetCell colNameCell = SpreadsheetCellType.STRING
                .createCell(row, DDLColHeaderEnum.COL_NAME.getColIdx(), ROW_SPAN, COL_SPAN, dto.getName());
        cells.add(colNameCell);
        columnRow.setColNameCell(colNameCell);
        // 数据类型
        SpreadsheetCell colTypeCell = SpreadsheetCellType.FilterList(MySQLTypeEnum.typeNames())
                .createCell(row, DDLColHeaderEnum.COL_TYPE.getColIdx(), ROW_SPAN, COL_SPAN, dto.getType());
        cells.add(colTypeCell);
        columnRow.setColTypeCell(colTypeCell);
        // 长度
        SpreadsheetCell colLenCell = SpreadsheetCellType
                .STRING.createCell(row, DDLColHeaderEnum.COL_LENGTH.getColIdx(), ROW_SPAN, COL_SPAN, dto.getLength());
        cells.add(colLenCell);
        columnRow.setColLenCell(colLenCell);
        // 默认值
        SpreadsheetCell colDefCell = SpreadsheetCellType.STRING.createCell(row, DDLColHeaderEnum
                .COL_DEFVAL.getColIdx(), ROW_SPAN, COL_SPAN, dto.getDefaultVal());
        cells.add(colDefCell);
        columnRow.setColDefCell(colDefCell);
        // 主键
        SpreadsheetCell isPrimaryCell = SpreadsheetCellType.STRING
                .createCell(row, DDLColHeaderEnum.COL_PRIMARY.getColIdx(), ROW_SPAN, COL_SPAN, null);
        isPrimaryCell.setEditable(false);
        HBox primaryHBox = new HBox();
        primaryHBox.setAlignment(Pos.CENTER);
        CheckBox isPrimaryCheckBox = new CheckBox();
        isPrimaryCheckBox.setText(null);
        isPrimaryCheckBox.setSelected(BooleanUtils.isTrue(dto.isPrimary()));
        primaryHBox.getChildren().add(isPrimaryCheckBox);
        isPrimaryCell.setGraphic(primaryHBox);
        primaryHBox.setOnMouseClicked(event -> {
            if (MouseClickUtils.isLeftSingleClick(event) && !isPrimaryCheckBox.isDisable()) {
                isPrimaryCheckBox.setSelected(!isPrimaryCheckBox.isSelected());
            }
        });
        cells.add(isPrimaryCell);
        columnRow.setIsPrimaryCheckBox(isPrimaryCheckBox);
        // 非空
        SpreadsheetCell isNotNullCell = SpreadsheetCellType.STRING
                .createCell(row, DDLColHeaderEnum.COL_NOTNULL.getColIdx(), ROW_SPAN, COL_SPAN, null);
        isNotNullCell.setEditable(false);
        HBox notNullHBox = new HBox();
        notNullHBox.setAlignment(Pos.CENTER);
        CheckBox isNotNullCheckBox = new CheckBox();
        isNotNullCheckBox.setText(null);
        isNotNullCheckBox.setSelected(BooleanUtils.isTrue(dto.isNotnull()));
        notNullHBox.getChildren().add(isNotNullCheckBox);
        isNotNullCell.setGraphic(notNullHBox);
        notNullHBox.setOnMouseClicked(event -> {
            if (MouseClickUtils.isLeftSingleClick(event) && !isNotNullCheckBox.isDisable()) {
                isNotNullCheckBox.setSelected(!isNotNullCheckBox.isSelected());
            }
        });
        cells.add(isNotNullCell);
        columnRow.setIsNotNullCheckBox(isNotNullCheckBox);
        // Unsigned
        SpreadsheetCell isUnsignedCell = SpreadsheetCellType.STRING
                .createCell(row, DDLColHeaderEnum.COL_UNSIGNED.getColIdx(), ROW_SPAN, COL_SPAN, null);
        isUnsignedCell.setEditable(false);
        HBox unsignedHBox = new HBox();
        unsignedHBox.setAlignment(Pos.CENTER);
        CheckBox isUnsignedCheckBox = new CheckBox();
        isUnsignedCheckBox.setText(null);
        isUnsignedCheckBox.setSelected(BooleanUtils.isTrue(dto.isUnsigned()));
        unsignedHBox.getChildren().add(isUnsignedCheckBox);
        isUnsignedCell.setGraphic(unsignedHBox);
        unsignedHBox.setOnMouseClicked(event -> {
            if (MouseClickUtils.isLeftSingleClick(event) && !isUnsignedCheckBox.isDisable()) {
                isUnsignedCheckBox.setSelected(!isUnsignedCheckBox.isSelected());
            }
        });
        cells.add(isUnsignedCell);
        columnRow.setIsUnsignedCheckBox(isUnsignedCheckBox);
        // 自增
        SpreadsheetCell isAutoIncrCell = SpreadsheetCellType.STRING
                .createCell(row, DDLColHeaderEnum.COL_AUTOINCR.getColIdx(), ROW_SPAN, COL_SPAN, null);
        isAutoIncrCell.setEditable(false);
        HBox autoIncrHBox = new HBox();
        autoIncrHBox.setAlignment(Pos.CENTER);
        CheckBox isAutoIncrCheckBox = new CheckBox();
        isAutoIncrCheckBox.setText(null);
        isAutoIncrCheckBox.setSelected(BooleanUtils.isTrue(dto.isAutoIncrement()));
        autoIncrHBox.getChildren().add(isAutoIncrCheckBox);
        isAutoIncrCell.setGraphic(autoIncrHBox);
        autoIncrHBox.setOnMouseClicked(event -> {
            if (MouseClickUtils.isLeftSingleClick(event) && !isAutoIncrCheckBox.isDisable()) {
                isAutoIncrCheckBox.setSelected(!isAutoIncrCheckBox.isSelected());
            }
        });
        cells.add(isAutoIncrCell);
        columnRow.setIsAutoIncrCheckBox(isAutoIncrCheckBox);
        // 更新
        SpreadsheetCell isAutoUpdateCell = SpreadsheetCellType.STRING
                .createCell(row, DDLColHeaderEnum.COL_AUTOUPDATE.getColIdx(), ROW_SPAN, COL_SPAN, null);
        isAutoUpdateCell.setEditable(false);
        HBox autoUpdateHBox = new HBox();
        autoUpdateHBox.setAlignment(Pos.CENTER);
        CheckBox isAutoUpdateCheckBox = new CheckBox();
        isAutoUpdateCheckBox.setText(null);
        isAutoUpdateCheckBox.setSelected(BooleanUtils.isTrue(dto.isAutoUpdate()));
        autoUpdateHBox.getChildren().add(isAutoUpdateCheckBox);
        isAutoUpdateCell.setGraphic(autoUpdateHBox);
        autoUpdateHBox.setOnMouseClicked(event -> {
            if (MouseClickUtils.isLeftSingleClick(event) && !isAutoUpdateCheckBox.isDisable()) {
                isAutoUpdateCheckBox.setSelected(!isAutoUpdateCheckBox.isSelected());
            }
        });
        cells.add(isAutoUpdateCell);
        columnRow.setIsAutoUpdateCheckBox(isAutoUpdateCheckBox);
        // 注释
        SpreadsheetCell commentCell = SpreadsheetCellType.STRING
                .createCell(row, DDLColHeaderEnum.COL_COMMENT.getColIdx(), ROW_SPAN, COL_SPAN, dto.getComment());
        cells.add(commentCell);
        columnRow.setCommentCell(commentCell);

        // 添加到记录中
        columnRows.add(row, columnRow);
        return cells;
    }

    private ObservableList<SpreadsheetCell> addIndexRow(int row, DatasouceTableVO.TableIndexVO indexVO) {
        IndexRow indexRow = new IndexRow();
        indexRow.setOriginIndex(indexVO);
        final ObservableList<SpreadsheetCell> cells = FXCollections.observableArrayList();

        // 索引名
        SpreadsheetCell indexNameCell = SpreadsheetCellType.STRING.createCell(row, DDLIndexHeaderEnum.IDX_NAME.getColIdx(), ROW_SPAN, COL_SPAN, indexVO.getIndexName());
        cells.add(indexNameCell);
        indexRow.setIndexNameCell(indexNameCell);

        // 设置索引与列关联
        List<ColumnRow> relateColRows = new ArrayList<>();
        for (String column : indexVO.getColumns()) {
            for (ColumnRow columnRow : columnRows) {
                if (StringUtils.equalsIgnoreCase(columnRow.getColNameCell().getText(), column)) {
                    relateColRows.add(columnRow);
                    break;
                }
            }
        }
        indexRow.setRelateColRows(relateColRows);

        // 数据类型
        String indexCol = CommonUtil.join(relateColRows.stream().map(r -> r.getColNameCell().getText()).collect(Collectors.toList()), ",");
        SpreadsheetCell indexColCell = SpreadsheetCellType.STRING_HANDLER(indexRow, irow -> {
            try {
                // 当前所有有效列
                List<ColumnRow> columnDtos = getValidNameTypeCols();
                // 索引已配置的列(需要剔除已删除的列)
                List<ColumnRow> indexDtos = irow.getRelateColRows().stream().filter(r -> !r.isDeleted()).collect(Collectors.toList());
                // 移除已选择的列
                columnDtos.removeAll(indexDtos);

                FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("index-select-view.fxml"));
                Parent root = fxmlLoader.load();
                IndexSelectViewController controller = fxmlLoader.getController();
                controller.setSourceCols(columnDtos);
                controller.setTargetCols(indexDtos);
                controller.init();
                controller.setIndexHandler(idxCols -> {
                    int idx = IntStream.range(0, indexRows.size()).filter(pos -> irow == indexRows.get(pos)).findFirst().orElse(-1);
                    if (idx >= 0) {
                        irow.setRelateColRows(idxCols);
                        refreshIndexColText(idx, irow);
                    }
                });
                Stage stage = new Stage();
                stage.setTitle("索引列");
                Scene scene = new Scene(root, 482, 278);
                stage.setScene(scene);
                Stage parent = (Stage) indexView.getScene().getWindow();
                stage.getIcons().add(new Image("show_detail.png"));
                stage.initOwner(parent);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.setMaximized(false);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                MessageAlert.error("系统异常");
            }
        }).createCell(row, DDLIndexHeaderEnum.IDX_COL.getColIdx(), ROW_SPAN, COL_SPAN, indexCol);
        cells.add(indexColCell);
        indexRow.setIndexColCell(indexColCell);
        
        // 索引类型
        String indexType = "";
        if (StringUtils.equalsIgnoreCase(indexVO.getIndexName(), PRIMARY_INDEX_NAME)) {
            indexType = PRIMARY_INDEX_NAME;
        } else if (indexVO.isUnique()) {
            indexType = "UNIQUE";
        }
        SpreadsheetCell indexTypeCell = SpreadsheetCellType.LIST(Arrays.asList(PRIMARY_INDEX_NAME, "UNIQUE", ""))
                .createCell(row, DDLIndexHeaderEnum.IDX_TYPE.getColIdx(), ROW_SPAN, COL_SPAN, indexType);
        cells.add(indexTypeCell);
        indexRow.setIndexTypeCell(indexTypeCell);

        // 添加到记录中
        indexRows.add(row, indexRow);
        return cells;
    }

    /**
     * 刷新所有索引列文本
     */
    private void refreshIndexColText() {
        for (int row = 0; row < indexRows.size(); row++) {
            refreshIndexColText(row, indexRows.get(row));
        }
    }

    /**
     * 刷新索引列文本
     * @param row
     */
    private void refreshIndexColText(int row, IndexRow indexRow) {
        String colText = CommonUtil.join(indexRow.getRelateColRows().stream().filter(r -> !r.isDeleted())
                .map(r -> r.getColNameCell().getText()).collect(Collectors.toList()), ",");
        indexGrid.setCellValue(row, DDLIndexHeaderEnum.IDX_COL.getColIdx(), colText);
    }

    /**
     * 获取当前可用的列(列名或列类型不为空)
     * @return
     */
    private List<ColumnRow> getValidNameTypeCols() {
        if (CollectionUtils.isEmpty(columnRows)) {
            return Collections.emptyList();
        }
        List<ColumnRow> cols = new ArrayList<>();
        for (ColumnRow columnRow : columnRows) {
            if (columnRow.isDeleted() || CommonUtil.isAllStringBlank(columnRow.getColNameCell().getText(), columnRow.getColTypeCell().getText())) {
                continue;
            }
            cols.add(columnRow);
        }
        return cols;
    }

    /**
     * 更新所有行状态
     */
    private void updateColumnRowStatus() {
        if (CollectionUtils.isEmpty(columnRows)) {
            return;
        }
        for (ColumnRow columnRow : columnRows) {
            updateColumnRowStatus(columnRow);
        }
    }

    /**
     * 更新指定行状态
     * @param columnRow
     */
    private void updateColumnRowStatus(ColumnRow columnRow) {
        if (null == columnRow) {
            return;
        }
        updateTableColumnRowStatus(columnRow);
        // 单元格值变化事件处理(列名及主键字段需要单独处理，因为涉及到与索引联动, 自增列也需要单独控制只能有一个字段选择自增)
        columnRow.allCells().stream()
                .filter(c -> !Arrays.asList(columnRow.getColNameCell()).contains(c)).toList()
                .forEach(cell -> {
            cell.itemProperty().addListener((observable, oldValue, newValue) -> {
                if (!CommonUtil.isObjectEquals(oldValue, newValue)) {
                    // 发生变更
                    setModified(true);
                }
                updateTableColumnRowStatus(columnRow);
                if (CollectionUtils.isEmpty(columnRows) || StringUtils.isNotBlank(columnRows.get(columnRows.size() - 1).getColNameCell().getText())) {
                    insertColumnRow(CollectionUtils.isEmpty(columnRows)? 0 : columnRows.size(), null);
                }
            });
        });
        // 通用选择框字段发生变更(非空,unsigned,自动更新)
        Arrays.asList(columnRow.getIsNotNullCheckBox(), columnRow.getIsUnsignedCheckBox(), columnRow.getIsAutoUpdateCheckBox()).forEach(checkBox -> {
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (!CommonUtil.isObjectEquals(oldValue, newValue)) {
                    // 发生变更
                    setModified(true);
                }
                updateTableColumnRowStatus(columnRow);
                if (CollectionUtils.isEmpty(columnRows) || StringUtils.isNotBlank(columnRows.get(columnRows.size() - 1).getColNameCell().getText())) {
                    insertColumnRow(CollectionUtils.isEmpty(columnRows)? 0 : columnRows.size(), null);
                }
            });
        });
        // 列名发生变更
        columnRow.getColNameCell().itemProperty().addListener((observable, oldValue, newValue) -> {
            if (!CommonUtil.isObjectEquals(oldValue, newValue)) {
                // 发生变更
                setModified(true);
            }
            // 找到对应的行
            int row = IntStream.range(0, columnRows.size()).filter(pos -> columnRow == columnRows.get(pos)).findFirst().orElse(-1);
            // 判断是否存在重复的列名
            if (null != newValue) {
                for (int pos = 0; pos < columnRows.size(); pos++) {
                    if (pos == row) {
                        continue;
                    }
                    if (StringUtils.isNotBlank(newValue.toString()) && StringUtils.equalsIgnoreCase(columnRows.get(pos).getColNameCell().getText(), newValue.toString())) {
                        // 存在重复列名
                        columnGrid.setCellValue(row, DDLColHeaderEnum.COL_NAME.getColIdx(), oldValue);
                        MessageAlert.warning("列名已存在");
                        return;
                    }
                }
            }
            if (null == newValue || StringUtils.isBlank(newValue.toString())) {
                // 文件名被清空，如果数据类型也未选择，则需要清空所有列数据
                if (StringUtils.isBlank(columnRow.getColumnType())) {
                    if (StringUtils.isNotBlank(columnRow.getColLenCell().getText())) {
                        columnRow.getColLenCell().setEditable(true);
                        columnGrid.setCellValue(row, DDLColHeaderEnum.COL_LENGTH.getColIdx(), null);
                    }
                    if (StringUtils.isNotBlank(columnRow.getColDefCell().getText())) {
                        columnRow.getColDefCell().setEditable(true);
                        columnGrid.setCellValue(row, DDLColHeaderEnum.COL_DEFVAL.getColIdx(), null);
                    }
                    columnRow.setPrimary(false);
                    columnRow.setNotNull(false);
                    columnRow.setUnsigned(false);
                    columnRow.setAutoIncr(false);
                    columnRow.setAutoUpdate(false);
                    if (StringUtils.isNotBlank(columnRow.getCommentCell().getText())) {
                        columnRow.getCommentCell().setEditable(true);
                        columnGrid.setCellValue(row, DDLColHeaderEnum.COL_COMMENT.getColIdx(), null);
                    }
                }
            }
            // 更新列状态
            updateTableColumnRowStatus(columnRow);
            // 刷新索引列展示文本
            refreshIndexColText();
            if (CollectionUtils.isEmpty(columnRows) || StringUtils.isNotBlank(columnRows.get(columnRows.size() - 1).getColNameCell().getText())) {
                insertColumnRow(CollectionUtils.isEmpty(columnRows)? 0 : columnRows.size(), null);
            }
        });
        // 主键字段发生变更
        columnRow.getIsPrimaryCheckBox().selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!CommonUtil.isObjectEquals(oldValue, newValue)) {
                // 发生变更
                setModified(true);
            }
            // 更新列字段行状态
            updateTableColumnRowStatus(columnRow);
            // 重载主键索引
            reloadPrmaryIndex();
            if (CollectionUtils.isEmpty(columnRows) || StringUtils.isNotBlank(columnRows.get(columnRows.size() - 1).getColNameCell().getText())) {
                insertColumnRow(CollectionUtils.isEmpty(columnRows)? 0 : columnRows.size(), null);
            }
        });
        // 自增字段发生变化
        columnRow.getIsAutoIncrCheckBox().selectedProperty().addListener((observable, oldValue, newValue) -> {
            // 找到对应的行
            int row = IntStream.range(0, columnRows.size()).filter(pos -> columnRow == columnRows.get(pos)).findFirst().orElse(-1);
            if (BooleanUtils.isTrue(newValue)) {
                // 选择自增，需要判断是否有其他自增字段， 因为一个表只能有一个自增字段
                for (int pos = 0; pos < columnRows.size(); pos++) {
                    if (pos == row) {
                        continue;
                    }
                    if (columnRows.get(pos).isAutoIncr()) {
                        // 存在其他自增字段
                        columnRow.setAutoIncr(false);
                        MessageAlert.warning("不能设置多个自增字段");
                        return;
                    }
                }
            }
            if (!CommonUtil.isObjectEquals(oldValue, newValue)) {
                // 发生变更
                setModified(true);
            }
            updateTableColumnRowStatus(columnRow);
            if (CollectionUtils.isEmpty(columnRows) || StringUtils.isNotBlank(columnRows.get(columnRows.size() - 1).getColNameCell().getText())) {
                insertColumnRow(CollectionUtils.isEmpty(columnRows)? 0 : columnRows.size(), null);
            }
        });
    }

    /**
     * 重载主键索引
     */
    private void reloadPrmaryIndex() {
        // 查找所有主键字段
        List<ColumnRow> primaryColRows = columnRows.stream().filter(ColumnRow::isPrimary).toList();

        // 查找主键索引
        int primaryRow = -1;
        for (int row = 0; row < indexRows.size(); row++) {
            if (StringUtils.equalsIgnoreCase(indexRows.get(row).getIndexNameCell().getText(), PRIMARY_INDEX_NAME)) {
                primaryRow = row;
                break;
            }
        }
        if (CollectionUtils.isEmpty(primaryColRows) && primaryRow < 0) {
            // 没有主键索引且没有主键字段, 不做处理
            return;
        } else if (CollectionUtils.isEmpty(primaryColRows) && primaryRow >= 0){
            // 没有主键字段，有主键索引，删除主键索引
            removeIndexRow(primaryRow);
        } else if (CollectionUtils.isNotEmpty(primaryColRows) && primaryRow < 0) {
            // 有主键字段, 没有主键索引，则需要添加主键索引
            DatasouceTableVO.TableIndexVO indexVO = new DatasouceTableVO.TableIndexVO();
            indexVO.setIndexName(PRIMARY_INDEX_NAME);
            for (int i = 0; i < primaryColRows.size(); i++) {
                indexVO.put((short) i, primaryColRows.get(i).getColNameCell().getText());
            }
            insertIndexRow(0, indexVO);
        } else {
            // 主键字段和主键索引都存在, 则需要更新主键索引
            IndexRow primaryIndexRow = indexRows.get(primaryRow);
            primaryIndexRow.getRelateColRows().clear();
            primaryIndexRow.getRelateColRows().addAll(primaryColRows);
            // 刷新索引列展示
            refreshIndexColText(primaryRow, primaryIndexRow);
        }
    }

    private void updateTableColumnRowStatus(ColumnRow columnRow) {
        List<SpreadsheetCell> cells = new ArrayList<>(columnRow.allCells());

        int row = IntStream.range(0, columnRows.size()).filter(pos -> columnRow == columnRows.get(pos)).findFirst().orElse(-1);
        if (row <= -1) {
            return;
        }

        // 先将所有单元格设置为默认的可编辑
        CommonUtil.setCellEditable(true, columnRow.allCells());
        CommonUtil.enableControls(columnRow.getIsPrimaryCheckBox(), columnRow.getIsNotNullCheckBox(), columnRow.getIsUnsignedCheckBox(), columnRow.getIsAutoIncrCheckBox(), columnRow.getIsAutoUpdateCheckBox());
        cells.remove(columnRow.getColNameCell());
        if (StringUtils.isBlank(columnRow.getColNameCell().getText())) {
            // 列名为空时当前行其他单元格都不可编辑
            CommonUtil.setCellEditable(false, cells);
            CommonUtil.disableControls(columnRow.getIsPrimaryCheckBox(), columnRow.getIsNotNullCheckBox(), columnRow.getIsUnsignedCheckBox(), columnRow.getIsAutoIncrCheckBox(), columnRow.getIsAutoUpdateCheckBox());
        } else {
            Object colType = columnRow.getColTypeCell().getItem();
            cells.remove(columnRow.getColTypeCell());
            if (null == colType || null == MySQLTypeEnum.getByTypeName(colType.toString())) {
                // 未选中数据类型
                CommonUtil.setCellEditable(false, cells);
                CommonUtil.disableControls(columnRow.getIsPrimaryCheckBox(), columnRow.getIsNotNullCheckBox(), columnRow.getIsUnsignedCheckBox(), columnRow.getIsAutoIncrCheckBox(), columnRow.getIsAutoUpdateCheckBox());
            } else {
                MySQLTypeEnum mysqlType = MySQLTypeEnum.getByTypeName(colType.toString());
                // 判断字段创建长度类型
                if (Objects.requireNonNull(mysqlType.getLengthType()) == SQLCreateLengthType.NONE) {
                    // 长度不可用时需要自动清空
                    CommonUtil.setCellEditable(true, columnRow.getColLenCell());
                    columnGrid.setCellValue(row, DDLColHeaderEnum.COL_LENGTH.getColIdx(), null);
                    CommonUtil.setCellEditable(false, columnRow.getColLenCell());
                } else {
                    CommonUtil.setCellEditable(true, columnRow.getColLenCell());
                }
                // 固定可编辑的单元格
                CommonUtil.setCellEditable(true, columnRow.getColDefCell(), columnRow.getCommentCell());
                CommonUtil.enableControls(columnRow.getIsPrimaryCheckBox(), columnRow.getIsNotNullCheckBox());
                // 是否可设置Unsigned
                if (!mysqlType.isCanUnsigned()) {
                    columnRow.setUnsigned(false);
                    CommonUtil.disableControls(columnRow.getIsUnsignedCheckBox());
                } else {
                    CommonUtil.enableControls(columnRow.getIsUnsignedCheckBox());
                }
                // 是否自增
                if (!mysqlType.isDecimal() || StringUtils.isNotBlank(columnRow.getColDefCell().getText())) {
                    // 非数字类型或者默认值存在值时禁用自增
                    columnRow.setAutoIncr(false);
                    CommonUtil.disableControls(columnRow.getIsAutoIncrCheckBox());
                } else {
                    CommonUtil.enableControls(columnRow.getIsAutoIncrCheckBox());
                }
                // 是否自动更新
                if (!Arrays.asList(MySQLTypeEnum.TIMESTAMP, MySQLTypeEnum.DATETIME).contains(mysqlType)) {
                    columnRow.setAutoUpdate(false);
                    CommonUtil.disableControls(columnRow.getIsAutoUpdateCheckBox());
                } else {
                    CommonUtil.enableControls(columnRow.getIsAutoUpdateCheckBox());
                }

                if (columnRow.isPrimary()) {
                    // 字段为主键时，非空标识必须为是
                    columnRow.setNotNull(true);
                    CommonUtil.disableControls(columnRow.getIsNotNullCheckBox());
                } else {
                    CommonUtil.enableControls(columnRow.getIsNotNullCheckBox());
                }
            }
        }
    }

    // 更新索引行状态
    private void updateIndexRowStatus() {
        if (CollectionUtils.isEmpty(indexRows)) {
            return;
        }
        for (IndexRow indexRow : indexRows) {
            updateIndexRowStatus(indexRow);
        }
    }

    private void updateIndexRowStatus(IndexRow indexRow) {
        if (null == indexRow) {
            return;
        }
        updateTableIndexRowStatus(indexRow);

        // 单元格值变化事件处理
        // 索引名称单元格发生变化
        indexRow.getIndexNameCell().itemProperty().addListener((observable, oldValue, newValue) -> {
            // 找到对应的行
            int row = IntStream.range(0, indexRows.size()).filter(pos -> indexRow == indexRows.get(pos)).findFirst().orElse(-1);
            if (null != newValue && StringUtils.equalsIgnoreCase(newValue.toString(), PRIMARY_INDEX_NAME)) {
                if (!StringUtils.equalsIgnoreCase(indexRow.getIndexTypeCell().getText(), PRIMARY_INDEX_NAME)) {
                    indexGrid.setCellValue(row, DDLIndexHeaderEnum.IDX_NAME.getColIdx(), oldValue);
                    MessageAlert.warning("索引名不能手动修改为PRIMARY, 如需设置主键请通过索引类型选择");
                    return;
                }
            }
            // 判断是否存在重复的索引名
            if (null != newValue) {
                for (int pos = 0; pos < indexRows.size(); pos++) {
                    if (pos == row) {
                        continue;
                    }
                    if (StringUtils.equalsIgnoreCase(indexRows.get(pos).getIndexNameCell().getText(), newValue.toString())) {
                        // 存在重复索引名
                        indexGrid.setCellValue(row, DDLIndexHeaderEnum.IDX_NAME.getColIdx(), oldValue);
                        MessageAlert.warning("索引名已存在");
                        return;
                    }
                }
            }
            if (!CommonUtil.isObjectEquals(oldValue, newValue)) {
                // 发生变更
                setModified(true);
            }
            updateTableIndexRowStatus(indexRow);
            if (CollectionUtils.isEmpty(indexRows) || !indexRows.get(indexRows.size() - 1).isAllColBlank()) {
                insertIndexRow(CollectionUtils.isEmpty(indexRows)? 0 : indexRows.size(), null);
            }
        });
        // 索引列发生变化
        indexRow.getIndexColCell().itemProperty().addListener((observable, oldValue, newValue) -> {
            if (!CommonUtil.isObjectEquals(oldValue, newValue)) {
                // 发生变更
                setModified(true);
            }
            if (StringUtils.equalsIgnoreCase(indexRow.getIndexTypeCell().getText(), PRIMARY_INDEX_NAME)) {
                // 主键列发生了变化，需要更新列视图中所有主键勾选状态
                Set<String> primaryCols = new HashSet<>(CommonUtil.split(null == newValue? "" : newValue.toString(), ","));
                updateTableColumnRowPrimaryStatus(primaryCols);
            }
            updateTableIndexRowStatus(indexRow);
            if (CollectionUtils.isEmpty(indexRows) || !indexRows.get(indexRows.size() - 1).isAllColBlank()) {
                insertIndexRow(CollectionUtils.isEmpty(indexRows)? 0 : indexRows.size(), null);
            }
        });
        // 索引类型发生变化
        indexRow.getIndexTypeCell().itemProperty().addListener((observable, oldValue, newValue) -> {
            // 找到对应的行
            int row = IntStream.range(0, indexRows.size()).filter(pos -> indexRow == indexRows.get(pos)).findFirst().orElse(-1);
            if (null != newValue && StringUtils.equalsIgnoreCase(newValue.toString(), PRIMARY_INDEX_NAME)) {
                // 选择了主键
                for (int pos = 0; pos < indexRows.size(); pos++) {
                    if (pos == row) {
                        continue;
                    }
                    if (StringUtils.equalsIgnoreCase(indexRows.get(pos).getIndexTypeCell().getText(), PRIMARY_INDEX_NAME)) {
                        // 存在多个主键
                        indexGrid.setCellValue(row, DDLIndexHeaderEnum.IDX_TYPE.getColIdx(), oldValue);
                        MessageAlert.warning("不能定义多个主键");
                        return;
                    }
                }
                // 主键检查通过，需要修改索引名
                indexGrid.setCellValue(row, DDLIndexHeaderEnum.IDX_NAME.getColIdx(), PRIMARY_INDEX_NAME);
                // 获取主键字段并更新列视图主键选项
                List<String> primaryCols = CommonUtil.split(indexRow.getIndexColCell().getText(), ",");
                updateTableColumnRowPrimaryStatus(new HashSet<>(primaryCols));
            } else if (null != oldValue && StringUtils.equalsIgnoreCase(oldValue.toString(), PRIMARY_INDEX_NAME)){
                // 索引类型从主键修改为其他，清空索引名称
                indexRow.getIndexNameCell().setEditable(true);
                indexGrid.setCellValue(row, DDLIndexHeaderEnum.IDX_NAME.getColIdx(), null);
                // 清空主键选项
                updateTableColumnRowPrimaryStatus(null);
            }

            if (!CommonUtil.isObjectEquals(oldValue, newValue)) {
                // 发生变更
                setModified(true);
            }
            updateTableIndexRowStatus(indexRow);
            if (CollectionUtils.isEmpty(indexRows) || !indexRows.get(indexRows.size() - 1).isAllColBlank()) {
                insertIndexRow(CollectionUtils.isEmpty(indexRows)? 0 : indexRows.size(), null);
            }
        });
    }

    /**
     * 更新列视图主键勾选状态
     * @param primaryCols 主键字段
     */
    private void updateTableColumnRowPrimaryStatus(Set<String> primaryCols) {
        if (null == primaryCols) {
            primaryCols = new HashSet<>();
        }
        Set<String> tt = new HashSet<>();
        for (String primaryCol : primaryCols) {
            tt.add(StringUtils.upperCase(primaryCol));
        }
        primaryCols = tt;
        for (int row = 0; row < columnRows.size(); row++) {
            ColumnRow columnRow = columnRows.get(row);
            String colName = columnRow.getColNameCell().getText();
            if (StringUtils.isBlank(colName)) {
                continue;
            }
            if (primaryCols.contains(StringUtils.upperCase(colName))) {
                // 选中主键及非空选项
                columnRow.setPrimary(true);
                columnRow.setNotNull(true);
            } else {
                // 清空主键选项
                columnRow.setPrimary(false);
            }
        }
    }

    private void updateTableIndexRowStatus(IndexRow indexRow) {
        CommonUtil.setCellEditable(true, indexRow.getIndexNameCell(), indexRow.getIndexColCell(), indexRow.getIndexTypeCell());
        String indexName = indexRow.getIndexNameCell().getText();
        if (StringUtils.equalsIgnoreCase(indexName, PRIMARY_INDEX_NAME)) {
            indexRow.getIndexNameCell().setEditable(false);
        }
    }

    /**
     * 取消修改
     */
    private void cancelModify() {
        // 表信息重置
        if (null == originTableVO) {
            tableNameEdit.setText(null);
            tableCommentEdit.setText(null);
        } else {
            tableNameEdit.setText(originTableVO.getTableName());
            tableCommentEdit.setText(originTableVO.getComment());
        }
        SqlEditorUtils.setSqlCode(codeView, DEFAULT_CODE_TEXT);
        // 修改标识重置
        setModified(false);
        // 列视图重置
        columnRows.clear();
        deletedColRows.clear();
        columnGrid.getRows().clear();
        // 索引视图重置
        indexRows.clear();
        deletedIndexRows.clear();
        indexGrid.getRows().clear();
        initalTableDDLView();
    }

    /**
     * 新增列信息
     * @param row
     * @param tableColumnDto
     */
    public void insertColumnRow(int row, TableColumnDto tableColumnDto) {
        if (row <= 0) {
            row = 0;
        } else if (columnRows.size() <= row) {
            row = columnRows.size();
        }
        boolean nullRow = false;
        if (null == tableColumnDto) {
            nullRow = true;
            tableColumnDto = new TableColumnDto();
        }

        // 添加行数据
        ObservableList<SpreadsheetCell> rowCells = addColumnRow(row, tableColumnDto);
        // 列表添加行
        columnGrid.getRows().add(row, rowCells);
        if (!nullRow) {
            setModified(true);
        }
        // 添加位置之后的后单元格更新行号
        for (int i = row + 1; i < columnRows.size(); i++) {
            ColumnRow columnRow = columnRows.get(i);
            int finalI = i;
            columnRow.allCells().forEach(cell -> cell.setRow(finalI));
        }

        // 更新所有单元格状态
        updateColumnRowStatus();
        // 更新工具栏状态
        updateColumnViewToolStatus();
    }

    /**
     * 删除指定列
     * @param row
     */
    public void removeColumnRow(int row) {
        if (row < 0 || row >= columnRows.size()) {
            return;
        }

        ColumnRow currColRow = columnRows.get(row);
        // 判断字段是否关联到索引
        Set<IndexRow> relateIndexRows = new HashSet<>();
        for (IndexRow indexRow : indexRows) {
            if (indexRow.getRelateColRows().contains(currColRow)) {
                relateIndexRows.add(indexRow);
            }
        }
        if (CollectionUtils.isNotEmpty(relateIndexRows)) {
            // 字段关联到索引, 则需要提示
            if (!MessageAlert.confirm(String.format("列[%s]被一个或多个索引引用, 删除该列将会同时修改关联的索引\n\n确定需要删除?", currColRow.getColNameCell().getText()))) {
                return;
            }

            // 确定删除列，则需要先修改索引信息
            for (IndexRow indexRow : relateIndexRows) {
                if (1 == indexRow.getRelateColRows().stream().filter(r -> !r.isDeleted()).toList().size()) {
                    // 关联的有效列只有1个时说明该索引需要删除
                    int idxRow = IntStream.range(0, indexRows.size()).filter(pos -> indexRow == indexRows.get(pos)).findFirst().orElse(-1);
                    removeIndexRow(idxRow);
                }
            }
        }

        // 备份删除的列
        currColRow.setDeleted(true);
        deletedColRows.add(currColRow);
        // 删除列
        columnRows.remove(row);
        columnGrid.getRows().remove(row);
        setModified(true);
        // 所有列所属单元格更新行号
        for (int i = 0; i < columnRows.size(); i++) {
            ColumnRow columnRow = columnRows.get(i);
            int finalI = i;
            columnRow.allCells().forEach(cell -> cell.setRow(finalI));
        }

        // 更新所有单元格状态
        updateColumnRowStatus();
        // 刷新索引文本
        refreshIndexColText();

        // 如果列已经全部删除或最后一行不为空行则新增一行
        if (CollectionUtils.isEmpty(columnRows) || StringUtils.isNotBlank(columnRows.get(columnRows.size() - 1).getColNameCell().getText())) {
            insertColumnRow(CollectionUtils.isEmpty(columnRows)? 0 : columnRows.size(), null);
        }
        // 更新工具栏状态
        updateColumnViewToolStatus();
    }

    /**
     * 列向上移动
     * @param row
     */
    private void moveUpColumnRow(int row, int column) {
        if (row <= 0 || row >= columnRows.size()) {
            return;
        }

        // 数据交换
        ColumnRow currRow = columnRows.get(row);
        columnRows.set(row, columnRows.get(row - 1));
        columnRows.set(row - 1, currRow);
        // 列交换
        ObservableList<SpreadsheetCell> currCells = columnGrid.getRows().get(row);
        columnGrid.getRows().set(row, columnGrid.getRows().get(row - 1));
        columnGrid.getRows().set(row - 1, currCells);

        if (!StringUtils.isAnyBlank(columnRows.get(row).getColNameCell().getText(), columnRows.get(row - 1).getColNameCell().getText())) {
            setModified(true);
        }

        // 所有列所属单元格更新行号
        for (int i = 0; i < columnRows.size(); i++) {
            ColumnRow columnRow = columnRows.get(i);
            int finalI = i;
            columnRow.allCells().forEach(cell -> cell.setRow(finalI));
        }

        // 更新所有单元格状态
        updateColumnRowStatus();

        // 如果列已经全部删除或最后一行不为空行则新增一行
        if (CollectionUtils.isEmpty(columnRows) || StringUtils.isNotBlank(columnRows.get(columnRows.size() - 1).getColNameCell().getText())) {
            insertColumnRow(CollectionUtils.isEmpty(columnRows)? 0 : columnRows.size(), null);
        }

        // 选中上一行
        columnView.getSelectionModel().selectCells(Arrays.asList(new Pair<>(row - 1, column)));

        // 更新工具栏状态
        updateColumnViewToolStatus();
    }

    /**
     * 列向下移动
     * @param row
     */
    private void moveDownColumnRow(int row, int column) {
        if (row < 0 || row > columnRows.size() - 1) {
            return;
        }

        // 数据交换
        ColumnRow currRow = columnRows.get(row);
        columnRows.set(row, columnRows.get(row + 1));
        columnRows.set(row + 1, currRow);
        // 列交换
        ObservableList<SpreadsheetCell> currCells = columnGrid.getRows().get(row);
        columnGrid.getRows().set(row, columnGrid.getRows().get(row + 1));
        columnGrid.getRows().set(row + 1, currCells);

        if (!StringUtils.isAnyBlank(columnRows.get(row).getColNameCell().getText(), columnRows.get(row + 1).getColNameCell().getText())) {
            setModified(true);
        }

        // 所有列所属单元格更新行号
        for (int i = 0; i < columnRows.size(); i++) {
            ColumnRow columnRow = columnRows.get(i);
            int finalI = i;
            columnRow.allCells().forEach(cell -> cell.setRow(finalI));
        }

        // 更新所有单元格状态
        updateColumnRowStatus();

        // 如果列已经全部删除或最后一行不为空行则新增一行
        if (CollectionUtils.isEmpty(columnRows) || StringUtils.isNotBlank(columnRows.get(columnRows.size() - 1).getColNameCell().getText())) {
            insertColumnRow(CollectionUtils.isEmpty(columnRows)? 0 : columnRows.size(), null);
        }

        // 选中下一行
        columnView.getSelectionModel().selectCells(Arrays.asList(new Pair<>(row + 1, column)));

        // 更新工具栏状态
        updateColumnViewToolStatus();
    }

    /**
     * 新增索引行
     * @param indexVO
     */
    private void insertIndexRow(int row, DatasouceTableVO.TableIndexVO indexVO) {
        if (row <= 0) {
            row = 0;
        } else if (columnRows.size() <= row) {
            row = columnRows.size();
        }
        boolean nullRow = false;
        if (null == indexVO) {
            nullRow = true;
            indexVO = new DatasouceTableVO.TableIndexVO();
        }
        // 添加行数据
        ObservableList<SpreadsheetCell> rowCells = addIndexRow(row, indexVO);
        // 列表添加行
        indexGrid.getRows().add(row, rowCells);
        if (!nullRow) {
            setModified(true);
        }
        // 添加位置之后的后单元格更新行号
        for (int i = row + 1; i < indexRows.size(); i++) {
            IndexRow indexRow = indexRows.get(i);
            indexRow.getIndexNameCell().setRow(i);
            indexRow.getIndexColCell().setRow(i);
            indexRow.getIndexTypeCell().setRow(i);
        }

        // 更新所有单元格状态
        updateIndexRowStatus();
        // 更新工具栏状态
        updateIndexViewToolStatus();
    }

    /**
     * 删除索引行
     * @param row
     */
    private void removeIndexRow(int row) {
        if (row < 0 || row >= indexRows.size()) {
            return;
        }

        // 备份删除的列
        indexRows.get(row).setDeleted(true);
        deletedIndexRows.add(indexRows.get(row));
        // 删除列
        indexRows.remove(row);
        indexGrid.getRows().remove(row);
        setModified(true);
        // 所有列所属单元格更新行号
        for (int i = 0; i < indexRows.size(); i++) {
            IndexRow indexRow = indexRows.get(i);
            indexRow.getIndexNameCell().setRow(i);
            indexRow.getIndexColCell().setRow(i);
            indexRow.getIndexTypeCell().setRow(i);
        }

        // 更新所有单元格状态
        updateIndexRowStatus();
        // 更新工具栏状态
        updateIndexViewToolStatus();
        // 更新索引列文本展示
        refreshIndexColText(row, indexRows.get(row));

        // 如果列已经全部删除或最后一行不为空行则新增一行
        if (CollectionUtils.isEmpty(indexRows) || !indexRows.get(indexRows.size() - 1).isAllColBlank()) {
            insertIndexRow(CollectionUtils.isEmpty(indexRows)? 0 : indexRows.size(), null);
        }
    }

    // 提交DDL变更
    private boolean effectDDL() {
        if (!checkDDLInfo()) {
            return false;
        }
        // 生成DDL语句
        List<String> sqls = buildDDLSQL();
        // TODO：执行SQL
        return true;
    }

    /**
     * 检查DDL页面配置值是否合理
     * @return
     */
    private boolean checkDDLInfo() {
        // TODO
        return true;
    }

    /**
     * 构建DDL语句
     * @return
     */
    private List<String> buildDDLSQL() {
        if (!isModified()) {
            SqlEditorUtils.setSqlCode(codeView, DEFAULT_CODE_TEXT);
            return Collections.emptyList();
        }
        if (null == originTableVO) {
            // 创建表
            return new ArrayList<>(Arrays.asList(buildCreateTableSQL()));
        } else {
            // 修改表
            return buildModifyTableSQL();
        }
    }

    /**
     * 构建创建表SQL
     * @return
     */
    private String buildCreateTableSQL() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE `").append(StringUtils.isBlank(tableNameEdit.getText())? "" : tableNameEdit.getText().trim()).append("` (\n");
        // 字段
        for (ColumnRow columnRow : columnRows) {
            if (CommonUtil.isAllStringBlank(columnRow.getColumnName(), columnRow.getColumnType())) {
                // 列名和数据类型为空则认为无效字段
                continue;
            }
            sql.append("  ");

            // 根据列视图行获取转换后的列信息
            TableColumnDto columnDto = columnRow.to();
            // 字段名
            sql.append("`").append(columnDto.getName()).append("`");
            // 字段类型
            if (StringUtils.isNotBlank(columnDto.getType())) {
                sql.append(" ").append(columnDto.getType());
            }
            // 长度
            if (StringUtils.isNotBlank(columnDto.getLength())) {
                sql.append("(").append(columnDto.getLength()).append(")");
            }
            // Unsigned
            if (columnDto.isUnsigned()) {
                sql.append(" UNSIGNED");
            }
            // 非空
            if (columnDto.isNotnull()) {
                sql.append(" NOT NULL");
            }

            // 自增
            if (columnDto.isAutoIncrement()) {
                sql.append(" AUTO_INCREMENT");
            }

            // 默认值
            if (StringUtils.isNotBlank(columnDto.getDefaultVal())) {
                sql.append(" DEFAULT '").append(columnDto.getDefaultVal()).append("'");
            }

            // 更新
            if (columnDto.isAutoUpdate()) {
                sql.append(" ON UPDATE CURRENT_TIMESTAMP");
            }

            // 注释
            if (StringUtils.isNotBlank(columnDto.getComment())) {
                sql.append(" COMMENT '").append(columnDto.getComment()).append("'");
            }

            sql.append(",\n");
        }
        // 索引
        for (IndexRow indexRow : indexRows) {
            if (CommonUtil.isAllStringBlank(indexRow.getIndexNameCell().getText(), indexRow.getIndexColCell().getText())) {
                // 索引名和索引列为空则认为无效字段
                continue;
            }
            // 列组合
            List<String> cols = indexRow.getRelateColRows().stream().filter(r -> !r.isDeleted()).map(r -> r.getColNameCell().getText()).toList();
            String idxCol = "";
            for (int i = 0; i < cols.size(); i++) {
                idxCol = idxCol + "`" + cols.get(i) + "`";
                if (i < cols.size() - 1) {
                    idxCol += ",";
                }
            }

            sql.append("  ");

            if (StringUtils.equalsIgnoreCase(indexRow.getIndexNameCell().getText(), PRIMARY_INDEX_NAME)) {
                // 主键
                sql.append("PRIMARY KEY (").append(idxCol).append(")");
            } else if (StringUtils.equalsIgnoreCase(indexRow.getIndexTypeCell().getText(), "UNIQUE")) {
                // 唯一索引
                sql.append("UNIQUE INDEX `").append(StringUtils.isBlank(indexRow.getIndexNameCell().getText())? "" : indexRow.getIndexNameCell().getText()).append("` (").append(idxCol).append(")");
            } else {
                // 普通索引
                sql.append("KEY `").append(StringUtils.isBlank(indexRow.getIndexNameCell().getText())? "" : indexRow.getIndexNameCell().getText()).append("` (").append(idxCol).append(")");
            }
            sql.append(",\n");
        }
        if (StringUtils.endsWithIgnoreCase(sql.toString(), ",\n")) {
            // 去除末尾符号
            String tempSql = StringUtils.trim(sql.toString());
            tempSql = StringUtils.left(tempSql, tempSql.length() - 1) + "\n";
            sql = new StringBuilder(tempSql);
        }
        sql.append(")");
        if (StringUtils.isNotBlank(tableEngineEdit.getText())) {
            sql.append(" ENGINE=").append(tableEngineEdit.getText());
        }
        if (StringUtils.isNotBlank(tableCharsetEdit.getText())) {
            sql.append(" DEFAULT CHARSET=").append(tableCharsetEdit.getText());
        }
        if (StringUtils.isNotBlank(tableCommentEdit.getText())) {
            sql.append(" COMMENT='").append(tableCommentEdit.getText()).append("'");
        }
        sql.append(";");
        SqlEditorUtils.setSqlCode(codeView, sql.toString());
        return sql.toString();
    }

    /**
     * 构建修改表SQL
     * @return
     */
    private List<String> buildModifyTableSQL() {
        if (null == originTableVO) {
            return Collections.emptyList();
        }
        List<String> sqls = new ArrayList<>();
        String tableName = originTableVO.getTableName();
        if (!StringUtils.equalsIgnoreCase(originTableVO.getComment(), tableCommentEdit.getText())) {
            // 表注释发生变更
            sqls.add("ALTER TABLE `" + tableName + "` COMMENT='" + (StringUtils.isBlank(tableCommentEdit.getText())? "" : tableCommentEdit.getText().trim()) + "';");
        }

        // 删除字段及索引

        // 新增字段
       /* ColumnRow preColRow = null;
        List<String> addColSqls = new ArrayList<>();
        for (ColumnRow currRow : columnRows) {
            if (currRow.isDeleted()) {
                continue;
            }
            if ((null == currRow.getOriginColumn() || CommonUtil.isAllStringBlank(currRow.getOriginColumn().getName(), currRow.getOriginColumn().getType()))
                    && !CommonUtil.isAllStringBlank(currRow.getColumnName(), currRow.getColumnType())) {
                // 新增字段处理
                // 根据列视图行获取转换后的列信息
                TableColumnDto columnDto = currRow.to();
                StringBuilder sql = new StringBuilder("  ADD COLUMN ");
                // 字段名
                sql.append("`").append(columnDto.getName()).append("`");
                // 字段类型
                if (StringUtils.isNotBlank(columnDto.getType())) {
                    sql.append(" ").append(columnDto.getType());
                }
                // 长度
                if (StringUtils.isNotBlank(columnDto.getLength())) {
                    sql.append("(").append(columnDto.getLength()).append(")");
                }
                // Unsigned
                if (columnDto.isUnsigned()) {
                    sql.append(" UNSIGNED");
                }
                // 非空
                if (columnDto.isNotnull()) {
                    sql.append(" NOT NULL");
                }

                // 自增
                if (columnDto.isAutoIncrement()) {
                    sql.append(" AUTO_INCREMENT");
                }

                // 默认值
                if (StringUtils.isNotBlank(columnDto.getDefaultVal())) {
                    sql.append(" DEFAULT '").append(columnDto.getDefaultVal()).append("'");
                }

                // 更新
                if (columnDto.isAutoUpdate()) {
                    sql.append(" ON UPDATE CURRENT_TIMESTAMP");
                }

                // 注释
                if (StringUtils.isNotBlank(columnDto.getComment())) {
                    sql.append(" COMMENT '").append(columnDto.getComment()).append("'");
                }
                // 添加位置
                if (null == preColRow) {
                    sql.append(" FIRST");
                } else {
                    sql.append(" AFTER ").append(preColRow.getColumnName());
                }
                addColSqls.add(sql.toString());
            }

            preColRow = currRow;
        }
        if (CollectionUtils.isNotEmpty(addColSqls)) {
            // 存在新增字段
            String head = "ALTER TABLE `" + tableName + "`";
            if (1 == addColSqls.size()) {
                sqls.add(head + " " + addColSqls.get(0).trim());
            } else {
                addColSqls.add(0, head);
                sqls.add(CommonUtil.join(addColSqls, ",\n"));
            }
        }
*/

        if (!StringUtils.equalsIgnoreCase(originTableVO.getTableName(), (StringUtils.isBlank(tableNameEdit.getText())? "" : tableNameEdit.getText().trim()))) {
            // 表名发生变更, 放在最后
            sqls.add("RENAME TABLE `" + originTableVO.getTableName() + "` TO `" + (StringUtils.isBlank(tableNameEdit.getText())? "" : tableNameEdit.getText().trim()) + "`;");
        }

        if (CollectionUtils.isEmpty(sqls)) {
            SqlEditorUtils.setSqlCode(codeView, DEFAULT_CODE_TEXT);
        } else {
            SqlEditorUtils.setSqlCode(codeView, CommonUtil.join(sqls, "\n\n"));
        }

        return sqls;
    }
}
