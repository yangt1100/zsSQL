package com.zssql.domain.dto;

import cn.hutool.db.DbUtil;
import com.zssql.domain.vo.DatasouceTableVO;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ConnectControlDto implements Serializable {
    // 连接信息
    private ConnectInfoDto connectInfo;
    // 过滤表编辑框
    private TextField filterTableEdit;
    // 表清单列表
    private ListView<DatasouceTableVO> tableInfoView;
    // 过滤表清单列表
    private ListView<DatasouceTableVO> filterTableView;
    // SQL查询编辑器Tab容器
    private TabPane sqlTabPane;
    // SQL查询编辑器Tab与控件映射
    private Map<Tab, SqlControlDto> sqlControlMap = new ConcurrentHashMap<>();
    // SQL查询编辑器Tab与SQL编辑器ID映射关系
    private Map<String, Tab> sqlCodeIdMap = new ConcurrentHashMap<>();

    // 连接关闭
    public void close() {
        if (null != connectInfo.getDataSource()) {
            // 关闭数据库连接
            DbUtil.close(connectInfo.getDataSource());
        }
    }

    public void closeSqlTab(Tab sqlTab) {
        if (null == sqlTab) {
            return;
        }
        if (!sqlControlMap.containsKey(sqlTab)) {
            return;
        }
        sqlControlMap.remove(sqlTab);
        // 删除SQL编辑器ID与SQL页签映射关系
        if (sqlCodeIdMap.containsValue(sqlTab)) {
            Set<String> ids = new HashSet<>();
            for (Map.Entry<String, Tab> entry : sqlCodeIdMap.entrySet()) {
                if (sqlTab == entry.getValue()) {
                    ids.add(entry.getKey());
                }
            }
            for (String id : ids) {
                sqlCodeIdMap.remove(id);
            }
        }
    }
}
