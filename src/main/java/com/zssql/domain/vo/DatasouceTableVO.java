package com.zssql.domain.vo;

import com.zssql.domain.dto.TableColumnDto;
import com.zssql.domain.dto.TableDDLDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javafx.util.Pair;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * 数据源表信息
 */
@Data
@ApiModel(value = "数据源表信息")
public class DatasouceTableVO implements Serializable {
    @ApiModelProperty(value = "表名称")
    private String tableName;
    @ApiModelProperty(value = "表注释")
    private String comment;
    @ApiModelProperty(value = "表字段")
    private List<String> columns = new ArrayList<>();
    @ApiModelProperty(value = "字段注释")
    private List<String> comments = new ArrayList<>();
    @ApiModelProperty(value = "字段类型")
    private List<String> types = new ArrayList<>();
    @ApiModelProperty(value = "主键字段")
    private List<String> primaryKeyColums = new ArrayList<>();
    @ApiModelProperty(value = "索引信息")
    private Map<String, TableIndexVO> indexs = new LinkedHashMap<>();
    @ApiModelProperty(value = "字段信息")
    private List<TableColumnDto> columnInfos = new ArrayList<>();

    @Override
    public String toString() {
        return tableName + (StringUtils.isBlank(comment)? "" : " (" + comment + ")");
    }

    public List<TableIndexVO> allIndexs() {
        List<TableIndexVO> indexVOS = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(primaryKeyColums)) {
            TableIndexVO vo = new TableIndexVO();
            vo.setIndexName(TableDDLDto.PRIMARY_INDEX_NAME);
            for (int pos = 0; pos < primaryKeyColums.size(); pos++) {
                vo.put((short) pos, primaryKeyColums.get(pos));
            }
            vo.setUnique(true);
            indexVOS.add(vo);
        }

        for (Map.Entry<String, TableIndexVO> entry : indexs.entrySet()) {
            indexVOS.add(entry.getValue());
        }
        return indexVOS;
    }

    @Data
    public static class TableIndexVO implements Serializable {
        // 索引名称
        private String indexName;
        // 是否唯一索引
        private boolean isUnique;
        // 索引字段
        @Setter(AccessLevel.NONE)
        private List<String> columns = new ArrayList<>();

        // 索引字段(排序前)
        @Getter(AccessLevel.NONE)
        private List<Pair<Short, String>> cols = new ArrayList<>();

        public List<String> getColumns() {
            if (CollectionUtils.isEmpty(cols)) {
                return Collections.emptyList();
            }
            if (cols.size() == columns.size()) {
                return columns;
            }
            columns.clear();

            // 按顺序排列
            Collections.sort(cols, Comparator.comparing(Pair::getKey));
            for (Pair<Short, String> col : cols) {
                columns.add(col.getValue());
            }
            return columns;
        }

        public void put(Short order, String column) {
            cols.add(new Pair<>(order, column));
        }
    }
}
