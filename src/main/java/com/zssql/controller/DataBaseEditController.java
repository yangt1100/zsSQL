package com.zssql.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.druid.filter.config.ConfigTools;
import com.zssql.common.enums.DsDriverClassEnum;
import com.zssql.common.utils.*;
import com.zssql.domain.dto.ErrorDto;
import com.zssql.domain.dto.sqlite.DatabaseInfoDto;
import com.zssql.domain.entity.sqlite.DatabaseInfo;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class DataBaseEditController {

    @FXML
    private Button cancelBtn;

    @FXML
    private TextField databaseNameEdit;

    @FXML
    private PasswordField databasePasswordEdit;

    @FXML
    private TextField databasePortEdit;

    @FXML
    private TextField databaseSchemeEdit;

    @FXML
    private ComboBox<DsDriverClassEnum> databaseTypeComboBox;

    @FXML
    private TextField databaseUrlEdit;

    @FXML
    private TextField databaseUserEdit;

    @Setter
    private DatabaseSelectViewController parent;
    @Setter
    private int itemIdx;
    @Setter
    private DatabaseInfoDto databaseInfoDto;

    public void init() {
        databaseTypeComboBox.getItems().addAll(DsDriverClassEnum.values());
        if (itemIdx >= 0) {
            // 修改
            databaseNameEdit.setEditable(false);
            databaseNameEdit.setText(databaseInfoDto.getDatabaseName());
            databaseTypeComboBox.getSelectionModel().select(DsDriverClassEnum.getDriverClassEnum(databaseInfoDto.getDatabaseType()));
            databaseUrlEdit.setText(databaseInfoDto.getDatabaseHost());
            databaseUserEdit.setText(databaseInfoDto.getDatabaseUser());
            try {
                databasePasswordEdit.setText(ConfigTools.decrypt(databaseInfoDto.getDatabasePassword()));
            } catch (Exception e) {
                e.printStackTrace();
                databasePasswordEdit.setText(databaseInfoDto.getDatabasePassword());
            }
            databasePortEdit.setText(String.valueOf(databaseInfoDto.getDatabasePort()));
            databaseSchemeEdit.setText(databaseInfoDto.getDatabaseScheme());
        } else {
            // 默认选中MySQL
            databaseTypeComboBox.getSelectionModel().select(DsDriverClassEnum.MYSQL);
        }
    }

    @FXML
    void onDatabaseSave(MouseEvent event) {
        if (!MouseClickUtils.isLeftSingleClick(event)) {
            return;
        }
        ErrorDto errorDto = new ErrorDto();
        DatabaseInfoDto dto = new DatabaseInfoDto();
        if (itemIdx >= 0) {
            BeanUtil.copyProperties(databaseInfoDto, dto);
        }
        if (!assembleDatabaseInfo(dto, errorDto)) {
            MessageAlert.information(errorDto.getErrorMessage());
            return;
        }

        if (itemIdx < 0) {
            // 新增
            List<DatabaseInfo> dis = SQLiteProvider.getDatabaseInofs();
            if (CollectionUtils.isNotEmpty(dis) && dis.stream().anyMatch(d -> CommonUtil.isObjectEquals(d.getDatabaseName(), dto.getDatabaseName()))) {
                MessageAlert.information(String.format("连接名[%s]已存在,请重新输入", dto.getDatabaseName()));
                return;
            }
            try {
                SQLiteProvider.insertDatabaseInfo(dto);
                MessageAlert.information("新增成功");
            } catch (Exception e) {
                e.printStackTrace();
                MessageAlert.error("新增数据库信息失败:" + e.getMessage());
                return;
            }
        } else {
            // 更新
            try {
                SQLiteProvider.updateDatabaseInfo(dto);
                MessageAlert.information("更新成功");
            } catch (Exception e) {
                e.printStackTrace();
                MessageAlert.error("更新数据库信息失败:" + e.getMessage());
                return;
            }
        }
        // 重新刷新数据库信息
        parent.reloadDatabases(dto.getDatabaseName());
        close();
    }

    @FXML
    void onTestConnection(MouseEvent event) {
        if (!MouseClickUtils.isLeftSingleClick(event)) {
            return;
        }

        ErrorDto errorDto = new ErrorDto();
        DatabaseInfoDto dto = new DatabaseInfoDto();
        if (!assembleDatabaseInfo(dto, errorDto)) {
            MessageAlert.information(errorDto.getErrorMessage());
            return;
        }

        try {
            if (DataSourceProvider.testConnect(dto)) {
                MessageAlert.information("测试通过");
            } else {
                MessageAlert.error("测试不通过");
            }
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error(e.getMessage());
        }

    }

    private boolean assembleDatabaseInfo(DatabaseInfoDto dto, ErrorDto errorDto) {
        if (null == dto) {
            ErrorDto.setErrorInfo(errorDto, "参数异常");
            return false;
        }

        if (StringUtils.isBlank(databaseNameEdit.getText())) {
            ErrorDto.setErrorInfo(errorDto, "请输入连接名");
            return false;
        }
        dto.setDatabaseName(databaseNameEdit.getText().trim());
        if (null == databaseTypeComboBox.getSelectionModel().getSelectedItem()) {
            ErrorDto.setErrorInfo(errorDto, "请选择数据库类型");
            return false;
        }
        dto.setDatabaseType(databaseTypeComboBox.getSelectionModel().getSelectedItem().getDsType());

        if (StringUtils.isBlank(databaseUrlEdit.getText())) {
            ErrorDto.setErrorInfo(errorDto, "请输入数据库地址");
            return false;
        }
        dto.setDatabaseHost(databaseUrlEdit.getText().trim());

        if (StringUtils.isBlank(databaseUserEdit.getText())) {
            ErrorDto.setErrorInfo(errorDto, "请输入用户名");
            return false;
        }
        dto.setDatabaseUser(databaseUserEdit.getText().trim());

        if (StringUtils.isBlank(databasePasswordEdit.getText())) {
            ErrorDto.setErrorInfo(errorDto, "请输入密码");
            return false;
        }
        try {
            dto.setDatabasePassword(ConfigTools.encrypt(databasePasswordEdit.getText().trim()));
        } catch (Exception e) {
            e.printStackTrace();
            ErrorDto.setErrorInfo(errorDto, "密码处理失败");
            return false;
        }

        if (StringUtils.isBlank(databasePortEdit.getText())) {
            ErrorDto.setErrorInfo(errorDto, "请输入数据库端口");
            return false;
        }
        if (!StringUtils.isNumeric(databasePortEdit.getText().trim())) {
            ErrorDto.setErrorInfo(errorDto, "数据库端口必须为数字类型");
            return false;
        }
        dto.setDatabasePort(Integer.valueOf(databasePortEdit.getText().trim()));

        if (StringUtils.isBlank(databaseSchemeEdit.getText())) {
            ErrorDto.setErrorInfo(errorDto, "请输入数据库");
            return false;
        }
        dto.setDatabaseScheme(databaseSchemeEdit.getText().trim());

        return true;
    }

    @FXML
    public void onClose(MouseEvent mouseEvent) {
        if (!MouseClickUtils.isLeftSingleClick(mouseEvent)) {
            return;
        }
        close();
    }

    private void close() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            close();
        }
    }
}
