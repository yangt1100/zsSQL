package com.zssql.domain.dto.ddl;

import com.zssql.domain.vo.DatasouceTableVO;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class IndexRow implements Serializable {
    SpreadsheetCell indexNameCell, indexColCell, indexTypeCell;
    DatasouceTableVO.TableIndexVO originIndex;
    // 关联的列(可能包含已删除的列，通过列的deleted标识判断)
    List<ColumnRow> relateColRows = new ArrayList<>();
    boolean deleted = false;

    public boolean isAllColBlank() {
        return StringUtils.isBlank(indexNameCell.getText()) && StringUtils.isBlank(indexColCell.getText()) && StringUtils.isBlank(indexTypeCell.getText());
    }
}
