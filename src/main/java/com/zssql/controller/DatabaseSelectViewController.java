package com.zssql.controller;

import cn.hutool.core.bean.BeanUtil;
import com.zssql.HelloApplication;
import com.zssql.HelloController;
import com.zssql.common.enums.DsDriverClassEnum;
import com.zssql.common.utils.*;
import com.zssql.domain.dto.ConnectInfoDto;
import com.zssql.domain.dto.sqlite.DatabaseInfoDto;
import com.zssql.domain.entity.sqlite.DatabaseInfo;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DatabaseSelectViewController implements Initializable {
    // 直连数据库
    @FXML
    public TableView<DatabaseInfoDto> databaseView;
    @FXML
    public TableColumn<DatabaseInfoDto, String> databaseTypeColumn;
    @FXML
    public TableColumn<DatabaseInfoDto, String> databaseConnectNameColumn;
    @FXML
    public MenuItem addDatabaseMenu;
    @FXML
    public MenuItem modifyDatabaseMenu;
    @FXML
    public MenuItem deleteDatabaseMenu;

    @FXML
    public Button cancelBtn;
    public MenuItem refreshDatabaseMenu;

    @Setter
    private HelloController parent;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshDatabaseMenu.setGraphic(new ImageView(new Image("refresh.png")));
        addDatabaseMenu.setGraphic(new ImageView(new Image("add.png")));
        modifyDatabaseMenu.setGraphic(new ImageView(new Image("rename.png")));
        deleteDatabaseMenu.setGraphic(new ImageView(new Image("delete.png")));

        // 绑定直连数据库类型
        databaseConnectNameColumn.setCellValueFactory(new PropertyValueFactory<>("databaseName"));
        databaseTypeColumn.setCellValueFactory(new PropertyValueFactory<>("databaseTypeDesc"));

        databaseView.getSelectionModel().selectedItemProperty().addListener((observableValue, oldVal, newVal) -> {
            if (null == newVal) {
                modifyDatabaseMenu.setDisable(true);
                deleteDatabaseMenu.setDisable(true);
            } else {
                modifyDatabaseMenu.setDisable(false);
                deleteDatabaseMenu.setDisable(false);
            }
        });

        // 加载直连数据库配置
        reloadDatabases(null);
    }

    /**
     * 加载直连数据库配置
     * @param selectDatabaseName 加载后选中的连接名
     */
    public void reloadDatabases(String selectDatabaseName) {
        if (StringUtils.isBlank(selectDatabaseName)) {
            DatabaseInfoDto selectedItem = databaseView.getSelectionModel().getSelectedItem();
            if (null != selectedItem) {
                selectDatabaseName = selectedItem.getDatabaseName();
            }
        }
        databaseView.getItems().clear();
        List<DatabaseInfo> dbs = SQLiteProvider.getDatabaseInofs();
        if (CollectionUtils.isEmpty(dbs)) {
            return;
        }

        List<DatabaseInfoDto> dtos = CommonUtil.datasTransfer(dbs, db -> {
            DatabaseInfoDto dto = new DatabaseInfoDto();
            BeanUtil.copyProperties(db, dto);
            dto.setDatabaseTypeDesc(DsDriverClassEnum.getDbName(dto.getDatabaseType()));
            return dto;
        });
        databaseView.getItems().addAll(dtos);

        // 选中指定的数据库
        if (StringUtils.isNotBlank(selectDatabaseName)) {
            int idx = -1;
            for (int i = 0; i < dtos.size(); i++) {
                DatabaseInfoDto dto = dtos.get(i);
                if (StringUtils.equals(selectDatabaseName, dto.getDatabaseName())) {
                    idx = i;
                    break;
                }
            }
            databaseView.getSelectionModel().select(idx);
        }
    }

    public void onDatabaseViewMouseClicked(MouseEvent mouseEvent) {
        if (!MouseClickUtils.isLeftDoubleClick(mouseEvent)) {
            return;
        }
        if (null == databaseView.getSelectionModel().getSelectedItem()) {
            return;
        }

        showDatabaseEditView(false);
    }

    public void onDatabaseViewKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.INSERT|| (keyEvent.getCode() == KeyCode.I && keyEvent.isControlDown())) {
            // Insert按键弹出新增页面
            showDatabaseEditView(true);
        } else if (keyEvent.getCode() == KeyCode.ENTER) {
            // Enter键弹出修改页面
            showDatabaseEditView(false);
        } else if (keyEvent.getCode() == KeyCode.F5) {
            // 刷新
            reloadDatabases(null);
        } else if (keyEvent.getCode() == KeyCode.DELETE) {
            // 启用/禁用
            deleteDatabase();
        }
    }

    public void onRefreshDatabase(ActionEvent actionEvent) {
        DatabaseInfoDto selectedItem = databaseView.getSelectionModel().getSelectedItem();
        reloadDatabases(null == selectedItem? null : selectedItem.getDatabaseName());
    }

    public void onAddDatabase(ActionEvent actionEvent) {
        showDatabaseEditView(true);
    }

    public void onModifyDatabase(ActionEvent actionEvent) {
        if (null == databaseView.getSelectionModel().getSelectedItem()) {
            return;
        }

        showDatabaseEditView(false);
    }

    public void onDeleteDatabase(ActionEvent actionEvent) {
        if (null == databaseView.getSelectionModel().getSelectedItem()) {
            return;
        }
        deleteDatabase();
    }

    /**
     * 直连数据库信息编辑
     * @param isInsert
     */
    private void showDatabaseEditView(boolean isInsert) {
        DatabaseInfoDto selectedItem = databaseView.getSelectionModel().getSelectedItem();
        int itemIdx = databaseView.getSelectionModel().getSelectedIndex();
        if (!isInsert && null == selectedItem) {
            MessageAlert.warning("请选择需要修改的连接信息");
            return;
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("data-base-edit.fxml"));
            Parent root = fxmlLoader.load();
            DataBaseEditController controller = fxmlLoader.getController();
            controller.setParent(this);
            controller.setItemIdx(isInsert? -1 : itemIdx);
            controller.setDatabaseInfoDto(isInsert? null : selectedItem);
            controller.init();
            Stage stage = new Stage();
            stage.setTitle(isInsert? "新建连接" : "修改连接");
            Scene scene = new Scene(root, 345, 300);
            stage.setScene(scene);
            Stage parent = (Stage) cancelBtn.getScene().getWindow();
            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.getIcons().add(new Image("connect.png"));
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error("系统异常");
        }
    }

    /**
     * 删除直连数据库
     */
    private void deleteDatabase() {
        DatabaseInfoDto selectedItem = databaseView.getSelectionModel().getSelectedItem();
        if (null == selectedItem) {
            return;
        }
        if (!MessageAlert.confirm(String.format("确定要删除数据库[%s]?", selectedItem.getDatabaseName()))) {
            return;
        }

        try {
            SQLiteProvider.deleteDatabase(selectedItem);
        }catch (Exception e) {
            MessageAlert.error(e.getMessage());
        }
        reloadDatabases(null);
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            close();
        }
    }

    @FXML
    void onCancel(MouseEvent event) {
        if (!MouseClickUtils.isLeftSingleClick(event)) {
            return;
        }
        close();
    }

    private void close() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    void onConnect(MouseEvent event) {
        if (!MouseClickUtils.isLeftSingleClick(event)) {
            return;
        }
        ConnectInfoDto dto = new ConnectInfoDto();
        if (null == databaseView.getSelectionModel().getSelectedItem()) {
            MessageAlert.information("请选择需要连接的数据库");
            return;
        }
        dto.setDbInfoDto(databaseView.getSelectionModel().getSelectedItem());

        for (Tab tab : parent.mainTabPanel.getTabs()) {
            if (StringUtils.equals(tab.getText(), dto.getConnectName())) {
                if (!MessageAlert.confirm("当前选中的数据库已连接,是否继续连接?")) {
                    return;
                }
                break;
            }
        }
        try {
            dto.setTables(DataSourceProvider.listTables(dto));
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error(String.format("数据库连接异常: %s", e.getMessage()));
            return;
        }

        parent.newConnect(dto);
        close();
    }
}
