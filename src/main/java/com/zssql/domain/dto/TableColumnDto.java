package com.zssql.domain.dto;

import com.zssql.common.enums.MySQLTypeEnum;
import com.zssql.common.enums.SQLCreateLengthType;
import com.zssql.common.utils.CommonUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class TableColumnDto implements Serializable {
    private String name;
    private String type;
    private Integer size;
    private Integer digits;
    private String defaultVal;
    private boolean primary;
    private boolean notnull;
    private boolean unsigned;
    private boolean autoIncrement;
    private boolean autoUpdate;
    private String comment;
    // enum或set类型字段候选值列表
    private List<String> vals = new ArrayList<>();

    public String getLength() {
        if (null == size) {
            return null;
        }
        String length = String.valueOf(size);
        SQLCreateLengthType lengthType = MySQLTypeEnum.getLengthTypeByTypeName(type);
        switch (lengthType) {
            case NONE:
                return null;
            case ONLY:
                return length;
            case BOTH:
            case EITHER:
                if (null != digits && digits > 0) {
                    length = length + "," + digits;
                }
                return length;
            default:
                return null;
        }
    }

    public void setLength(String length) {
        if (StringUtils.isBlank(length)) {
            size = null;
            digits = null;
            return;
        }

        List<String> strs = CommonUtil.split(length, ",");
        if (1 == strs.size()) {
            size = Integer.valueOf(strs.get(0));
            digits = null;
        } else  {
            if (StringUtils.isBlank(strs.get(0))) {
                size = 0;
            } else {
                size = Integer.valueOf(strs.get(0));
            }
            if (StringUtils.isBlank(strs.get(1))) {
                digits = null;
            } else {
                digits = Integer.valueOf(strs.get(1));
                if (0 <= digits) {
                    digits = null;
                }
            }
        }

        SQLCreateLengthType lengthType = MySQLTypeEnum.getLengthTypeByTypeName(type);
        switch (lengthType) {
            case NONE:
                size = null;
                digits = null;
                break;
            case ONLY:
                digits = null;
                break;
        }
    }
}
