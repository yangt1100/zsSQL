package com.zssql.domain.dto.ddl;

import com.zssql.domain.dto.TableColumnDto;
import javafx.scene.control.CheckBox;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
public class ColumnRow implements Serializable {
    SpreadsheetCell colNameCell, colTypeCell, colLenCell, colDefCell, commentCell;
    CheckBox isPrimaryCheckBox, isNotNullCheckBox, isUnsignedCheckBox, isAutoIncrCheckBox, isAutoUpdateCheckBox;
    TableColumnDto originColumn;
    boolean deleted = false;

    public List<SpreadsheetCell> allCells() {
        return Arrays.asList(colNameCell, colTypeCell, colLenCell, colDefCell, commentCell);
    }

    public String getColumnName() {
        return colNameCell.getText();
    }

    public String getColumnType() {
        return colTypeCell.getText();
    }

    public boolean isAutoUpdate() {
        return isAutoUpdateCheckBox.isSelected();
    }

    public void setAutoUpdate(boolean autoUpdate) {
        isAutoUpdateCheckBox.setSelected(autoUpdate);
    }

    public boolean isPrimary() {
        return isPrimaryCheckBox.isSelected();
    }

    public void setPrimary(boolean primary) {
        isPrimaryCheckBox.setSelected(primary);
    }

    public boolean isNotNull() {
        return isNotNullCheckBox.isSelected();
    }

    public void setNotNull(boolean notNull) {
        isNotNullCheckBox.setSelected(notNull);
    }

    public boolean isUnsigned() {
        return isUnsignedCheckBox.isSelected();
    }

    public void setUnsigned(boolean unsigned) {
        isUnsignedCheckBox.setSelected(unsigned);
    }

    public boolean isAutoIncr() {
        return isAutoIncrCheckBox.isSelected();
    }

    public void setAutoIncr(boolean autoIncr) {
        isAutoIncrCheckBox.setSelected(autoIncr);
    }

    /**
     * 转换成列信息
     * @return
     */
    public TableColumnDto to() {
        TableColumnDto dto = new TableColumnDto();
        dto.setName(StringUtils.isBlank(getColumnName())? "" : getColumnName().trim());
        dto.setType(StringUtils.isBlank(getColumnType())? "" : getColumnType().trim());
        dto.setLength(colLenCell.getText());
        dto.setDefaultVal(colDefCell.getText());
        dto.setPrimary(isPrimary());
        dto.setNotnull(isNotNull());
        dto.setUnsigned(isUnsigned());
        dto.setAutoIncrement(isAutoIncr());
        dto.setAutoUpdate(isAutoUpdate());
        dto.setComment(StringUtils.isBlank(commentCell.getText())? "" : commentCell.getText());
        return dto;
    }
}
