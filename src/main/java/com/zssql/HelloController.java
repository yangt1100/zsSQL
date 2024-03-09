package com.zssql;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.CharsetDetector;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Pair;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.zssql.common.cutom.DraggingTabPaneSupport;
import com.zssql.common.enums.MySQLTypeEnum;
import com.zssql.common.enums.ResultViewType;
import com.zssql.common.enums.SQLType;
import com.zssql.common.utils.*;
import com.zssql.controller.*;
import com.zssql.domain.dto.*;
import com.zssql.domain.dto.snapshot.ConnectSnapshotDto;
import com.zssql.domain.dto.sqlite.DatabaseInfoDto;
import com.zssql.domain.entity.sqlite.DatabaseInfo;
import com.zssql.domain.entity.sqlite.MainWindowPostion;
import com.zssql.domain.vo.DatasouceTableVO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.controlsfx.control.spreadsheet.*;
import org.controlsfx.control.textfield.TextFields;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

public class HelloController implements Initializable {
    @FXML
    public TabPane mainTabPanel;
    @FXML
    public Label tipLabel;
    @FXML
    public Label timeLabel;
    @FXML
    public Label recordLabel;
    @FXML
    public AnchorPane mainPanel;

    public MenuItem exitMenu;
    public MenuItem newConnectPageMenu;
    public MenuItem newSqlPageMenu;
    public MenuItem openSQLMenu;
    public MenuItem saveSQLMenu;
    public MenuItem saveSQLAsMenu;
    public MenuItem formatSQLMenu;
    public MenuItem executeSqlMenu;
    public MenuItem executeAllSqlMenu;
    public MenuItem configMenu;
    public MenuItem abountMenu;
    public MenuItem undoMenu;
    public MenuItem redoMenu;
    public MenuItem cutMenu;
    public MenuItem copyMenu;
    public MenuItem pasteMenu;
    public MenuItem findMenu;
    public MenuItem replaceMenu;
    public ToolBar toolbar;
    public MenuItem formatAllSQLMenu;
    public MenuItem sqlUpperMenu;
    public MenuItem sqlLowerMenu;
    public MenuItem selectAllMenu;
    public VBox restoryHBox;
    public Label restoreTipLabel;
    public ProgressBar restoreProgressBar;

    private Tab newConnnectTab = null;
    // 工具栏按钮
    private Button newConnBtn;
    private Button newSqlBtn;
    private Button openSqlBtn;
    private Button findBtn;
    private Button replaceBtn;
    private Button saveSqlBtn;
    private Button executeBtn;
    private Button executeAllBtn;
    private Button formatBtn;

    private String exportPath = null;

    // 数据库连接Tab与连接实体映射关系
    private final Map<Tab, ConnectControlDto> connectConrolMap = new ConcurrentHashMap<>();
    // SQL编辑器ID与数据库连接页签映射关系, 便于快速定位
    private final Map<String, Tab> sqlCodeId2ConnTabMap = new ConcurrentHashMap<>();
    // SQL编辑器变更事件监听器
    private final SqlCodeChangeListener sqlCodeChangeListener = new SqlCodeChangeListener(connectConrolMap, sqlCodeId2ConnTabMap);

    private volatile boolean savingSnapshot = false;
    private volatile boolean initialized = false;
    private static final long SCHEDULE_PERIOD = 120 * 1000L;             // 定时调度间隔，120秒
    private volatile ScheduledFuture<?> scheduleFuture;                 // 定时调度future，此处仅用于控制只创建一个调度任务
    // 快照存储处理定时调度ExecutorService
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("task-save-snapshot", true));

    // 系统配置
    private Config config = new Config();

    // 是否正展示查找或替换窗口
    public boolean findingOrReplacing = false;

    private DraggingTabPaneSupport connectTabPaneSuppoert = new DraggingTabPaneSupport();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connectTabPaneSuppoert.addSupport(mainTabPanel);
        // 菜单图标设置
        exitMenu.setGraphic(new ImageView(new Image("exit.png")));
        newConnectPageMenu.setGraphic(new ImageView(new Image("connect.png")));
        newSqlPageMenu.setGraphic(new ImageView(new Image("sql.png")));
        openSQLMenu.setGraphic(new ImageView(new Image("open.png")));
        saveSQLMenu.setGraphic(new ImageView(new Image("save.png")));
        saveSQLAsMenu.setGraphic(new ImageView(new Image("saveas.png")));

        undoMenu.setGraphic(new ImageView(new Image("undo.png")));
        redoMenu.setGraphic(new ImageView(new Image("redo.png")));
        cutMenu.setGraphic(new ImageView(new Image("cut.png")));
        copyMenu.setGraphic(new ImageView(new Image("copy.png")));
        pasteMenu.setGraphic(new ImageView(new Image("paste.png")));
        formatSQLMenu.setGraphic(new ImageView(new Image("format.png")));
        findMenu.setGraphic(new ImageView(new Image("find.png")));
        replaceMenu.setGraphic(new ImageView(new Image("replace.png")));

        executeSqlMenu.setGraphic(new ImageView(new Image("execute.png")));
        executeAllSqlMenu.setGraphic(new ImageView(new Image("execute_all.png")));
        configMenu.setGraphic(new ImageView(new Image("config.png")));

        abountMenu.setGraphic(new ImageView(new Image("about.png")));

        // 初始化工具栏
        initToolBar();

        mainTabPanel.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);

        // 新增Tab按钮
        newConnnectTab = new Tab(null);
        newConnnectTab.setGraphic(new ImageView(new Image("add_tab.png")));
        newConnnectTab.setClosable(false);
        newConnnectTab.setTooltip(CommonUtil.toolTip("新建数据库连接", 12));
        mainTabPanel.getTabs().add(newConnnectTab);
        mainTabPanel.getSelectionModel().selectedItemProperty().addListener((observableValue, oldTab, newTab) -> {
            if (newConnnectTab == newTab) {
                mainTabPanel.getSelectionModel().select(oldTab);
                newDatabaseConnect();
            }
        });

        timeLabel.setText(null);
        recordLabel.setText(null);
        tipLabel.setText("请通过Ctrl+N快捷键新建数据库连接");

        mainTabPanel.getSelectionModel().selectedItemProperty().addListener((observableValue, oldTab, newTab) -> {
            if (null != newTab) {
                Stage stage = (Stage) mainPanel.getScene().getWindow();
                stage.setTitle("zsSQL - " + ((Label)newTab.getGraphic()).getText());
            }
            updateStatus();
        });

        mainTabPanel.requestFocus();
    }

    /**
     * 初始化工具栏
     */
    private void initToolBar() {
        // 新建连接
        // 工具栏按钮
        newConnBtn = new Button(null);
        newConnBtn.setGraphic(new ImageView(new Image("connect.png")));
        newConnBtn.setTooltip(CommonUtil.toolTip("新建数据库连接", 12, new Duration(100)));
        toolbar.getItems().add(newConnBtn);
        CommonUtil.buttonBind(newConnBtn, this::newDatabaseConnect);

        // 新建查询编辑器
        newSqlBtn = new Button(null);
        newSqlBtn.setGraphic(new ImageView(new Image("sql.png")));
        newSqlBtn.setTooltip(CommonUtil.toolTip("新建查询编辑器", 12, new Duration(100)));
        toolbar.getItems().add(newSqlBtn);
        CommonUtil.buttonBind(newSqlBtn, () -> onNewSqlPage(null));

        Separator sep1 = new Separator();
        sep1.setOrientation(Orientation.VERTICAL);
        toolbar.getItems().add(sep1);

        // 打开SQL文件
        openSqlBtn = new Button(null);
        openSqlBtn.setGraphic(new ImageView(new Image("open.png")));
        openSqlBtn.setTooltip(CommonUtil.toolTip("打开SQL文件", 12, new Duration(100)));
        toolbar.getItems().add(openSqlBtn);
        CommonUtil.buttonBind(openSqlBtn, () -> onOpenSQL(null));

        // 保存SQL文件
        saveSqlBtn = new Button(null);
        saveSqlBtn.setGraphic(new ImageView(new Image("save.png")));
        saveSqlBtn.setTooltip(CommonUtil.toolTip("保存SQL文件", 12, new Duration(100)));
        toolbar.getItems().add(saveSqlBtn);
        CommonUtil.buttonBind(saveSqlBtn, () -> onSaveSQL(null));

        Separator sep2 = new Separator();
        sep2.setOrientation(Orientation.VERTICAL);
        toolbar.getItems().add(sep2);

        // 查找
        findBtn = new Button(null);
        findBtn.setGraphic(new ImageView(new Image("find.png")));
        findBtn.setTooltip(CommonUtil.toolTip("查找", 12, new Duration(100)));
        toolbar.getItems().add(findBtn);
        CommonUtil.buttonBind(findBtn, () -> onFindCode(null));

        // 替换
        replaceBtn = new Button(null);
        replaceBtn.setGraphic(new ImageView(new Image("replace.png")));
        replaceBtn.setTooltip(CommonUtil.toolTip("替换", 12, new Duration(100)));
        toolbar.getItems().add(replaceBtn);
        CommonUtil.buttonBind(replaceBtn, () -> onReplaceCode(null));

        Separator sep3 = new Separator();
        sep3.setOrientation(Orientation.VERTICAL);
        toolbar.getItems().add(sep3);

        // 执行SQL
        executeBtn = new Button(null);
        executeBtn.setGraphic(new ImageView(new Image("execute.png")));
        executeBtn.setTooltip(CommonUtil.toolTip("执行选中的SQL", 12, new Duration(100)));
        toolbar.getItems().add(executeBtn);
        CommonUtil.buttonBind(executeBtn, () -> executeSQL(false));

        // 执行所有SQL
        executeAllBtn = new Button(null);
        executeAllBtn.setGraphic(new ImageView(new Image("execute_all.png")));
        executeAllBtn.setTooltip(CommonUtil.toolTip("执行所有SQL", 12, new Duration(100)));
        toolbar.getItems().add(executeAllBtn);
        CommonUtil.buttonBind(executeAllBtn, () -> executeSQL(true));

        Separator sep4 = new Separator();
        sep4.setOrientation(Orientation.VERTICAL);
        toolbar.getItems().add(sep4);

        // 格式化SQL
        formatBtn = new Button(null);
        formatBtn.setGraphic(new ImageView(new Image("format.png")));
        formatBtn.setTooltip(CommonUtil.toolTip("格式化选中的SQL脚本", 12, new Duration(100)));
        toolbar.getItems().add(formatBtn);
        CommonUtil.buttonBind(formatBtn, () -> formatSql(false));

        // 配置
        Button configBtn = new Button(null);
        configBtn.setGraphic(new ImageView(new Image("config.png")));
        configBtn.setTooltip(CommonUtil.toolTip("选项设置", 12, new Duration(100)));
        toolbar.getItems().add(configBtn);
        CommonUtil.buttonBind(configBtn, () -> onConfig(null));
    }

    public void init(Config cfg) {
        config = cfg;

        initialized = false;
        CommonUtil.showControl(mainTabPanel);
        CommonUtil.hideControl(restoryHBox);

        Stage mainStage = (Stage)mainTabPanel.getScene().getWindow();
        // 捕获窗口关闭事件
        mainStage.setOnCloseRequest(windowEvent -> {
            // 保存窗口位置信息
            saveWindowPostion();
            // 保存连接快照信息
            saveConnSnapshot();
            // 退出程序
            System.exit(0);
        });
        // 刷新菜单及工具栏状态
        refreshMenuAndToolBarStatus();

        boolean restore = false;
        if (BooleanUtils.isTrue(config.getRestoreConnAfterStart())) {
            // 恢复上一次连接快照
            restore = restoreConnSnapshot();
        }
        // 如果已经创建了数据库连接则不再自动弹出新建连接页面
        if (!restore && MapUtils.isEmpty(connectConrolMap)) {
            newDatabaseConnect();
        }

        if (!restore) {
            initialized = true;
            refreshMenuAndToolBarStatus();
        }

        // 快照存储调度任务创建
        if (scheduleFuture == null) {
            synchronized (this) {
                if (scheduleFuture == null) {
                    // 创建定时任务
                    scheduleFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
                        Platform.runLater(this::saveConnSnapshot);
                    }, SCHEDULE_PERIOD, SCHEDULE_PERIOD, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * 存储配置
     * @param config
     */
    public void saveConfig(Config config) {
        try{
            this.config = config;
            // 快照信息写入文件
            File file = new File("zssql.cfg");
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] b = JSON.toJSONString(config, SerializerFeature.PrettyFormat).getBytes();
            out.write(b,0,b.length);
            out.flush();
            out.close();
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * 恢复上一次连接快照
     */
    private boolean restoreConnSnapshot() {
        // 读取快照文件
        File file = new File("zssql.ss");
        if (!file.exists()) {
            return false;
        }

        List<ConnectSnapshotDto> snapshots;
        try{
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer =new byte[1024*8];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(int len =0; (len =in.read(buffer)) >0;) {
                baos.write(buffer, 0, len);
            }
            in.close();
            String val = baos.toString(StandardCharsets.UTF_8);
            snapshots = JSON.parseArray(val, ConnectSnapshotDto.class);
        } catch (Exception ex){
            ex.printStackTrace();
            return false;
        }

        if (CollectionUtils.isEmpty(snapshots)) {
            return false;
        }

        CommonUtil.hideControl(mainTabPanel);
        CommonUtil.showControl(restoryHBox);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    updateProgress(10, 100);
                    // 查询数据库信息
                    List<DatabaseInfo> dbs = SQLiteProvider.getDatabaseInofs();
                    // 恢复连接
                    int[] idx = {0};
                    for (ConnectSnapshotDto snapshot : snapshots) {
                        try {
                            idx[0]++;
                            Platform.runLater(() -> {
                                restoreTipLabel.setText(String.format("正在恢复数据库连接 %s...... [%d/%d]", snapshot.getDatabaseName(), idx[0], snapshots.size()));
                            });
                            updateProgress(10 + (long)((idx[0] - 1) * 100.0 / snapshots.size()), 100);
                            ConnectInfoDto dto = new ConnectInfoDto();
                            for (DatabaseInfo db : dbs) {
                                if (StringUtils.equals(db.getDatabaseName(), snapshot.getDatabaseName())) {
                                    DatabaseInfoDto dbInfo = new DatabaseInfoDto();
                                    BeanUtil.copyProperties(db, dbInfo);
                                    dto.setDbInfoDto(dbInfo);
                                    dto.setTables(DataSourceProvider.listTables(dto));
                                    break;
                                }
                            }
                            updateProgress(10 + (long)((idx[0]) * 50.0 / snapshots.size()), 100);
                            if (null == dto.getDbInfoDto()) {
                                throw new IllegalStateException("invalid direct database: " + snapshot.getDatabaseName());
                            }
                            Platform.runLater(() -> {
                                // 创建数据库连接
                                Tab connTab = newConnect(dto);
                                if (CollectionUtils.isEmpty(snapshot.getSqlTabs())) {
                                    return;
                                }
                                // 创建SQL页签
                                for (ConnectSnapshotDto.SqlSnapshotDto sqlSnapshotDto : snapshot.getSqlTabs()) {
                                    Tab sqlTab = addSqlTab(connTab, sqlSnapshotDto.getSql());
                                    if (StringUtils.isNotBlank(sqlSnapshotDto.getSqlFilePath())) {
                                        connectConrolMap.get(connTab).getSqlControlMap().get(sqlTab).setSqlFilePath(sqlSnapshotDto.getSqlFilePath());
                                        connectConrolMap.get(connTab).getSqlControlMap().get(sqlTab).setModified(sqlSnapshotDto.isModified());
                                        sqlTab.setTooltip(CommonUtil.toolTip(sqlSnapshotDto.getSqlFilePath(), 12));
                                        ((Label) sqlTab.getGraphic()).setText(FileNameUtil.getName(sqlSnapshotDto.getSqlFilePath()) + (sqlSnapshotDto.isModified() ? " *" : ""));
                                    } else if (StringUtils.isNotBlank(sqlSnapshotDto.getSqlTabName())) {
                                        ((Label) sqlTab.getGraphic()).setText(sqlSnapshotDto.getSqlTabName());
                                    }
                                }
                                // 移除第一个自动创建的空Tab
                                Tab firstSqlTab = connectConrolMap.get(connTab).getSqlTabPane().getTabs().get(0);
                                Map<String, Tab> sqlCodeIdMap = connectConrolMap.get(connTab).getSqlCodeIdMap();
                                for (Map.Entry<String, Tab> entry : sqlCodeIdMap.entrySet()) {
                                    if (firstSqlTab == entry.getValue()) {
                                        sqlCodeId2ConnTabMap.remove(entry.getKey());
                                    }
                                }
                                connectConrolMap.get(connTab).closeSqlTab(firstSqlTab);
                                connectConrolMap.get(connTab).getSqlTabPane().getTabs().remove(firstSqlTab);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    updateProgress(100, 100);
                    Thread.sleep(100);
                } catch (Exception e) {
                    MessageAlert.error(e.getMessage());
                } finally {
                    Platform.runLater(() -> {
                        CommonUtil.showControl(mainTabPanel);
                        CommonUtil.hideControl(restoryHBox);
                        initialized = true;
                        refreshMenuAndToolBarStatus();
                    });
                }

                return null;
            }
        };
        restoreProgressBar.progressProperty().bind(task.progressProperty());
        TaskUtils.submit(task);

        return true;
    }

    /**
     * 存储连接快照
     */
    private void saveConnSnapshot() {
        if (CollectionUtils.isEmpty(mainTabPanel.getTabs())) {
            return;
        }
        if (savingSnapshot) {
            return;
        }
        try{
            // 组装连接快照信息
            savingSnapshot = true;
            List<ConnectSnapshotDto> snapshots = new ArrayList<>();
            for (Tab connTab : mainTabPanel.getTabs()) {
                ConnectControlDto connectControlDto = connectConrolMap.get(connTab);
                if (null == connectControlDto || connTab == newConnnectTab) {
                    continue;
                }
                ConnectSnapshotDto snapshot = new ConnectSnapshotDto();
                snapshots.add(snapshot);
                snapshot.setDatabaseName(connectControlDto.getConnectInfo().getDbInfoDto().getDatabaseName());
                if (CollectionUtils.isEmpty(connectControlDto.getSqlTabPane().getTabs())) {
                    continue;
                }
                for (Tab sqlTab : connectControlDto.getSqlTabPane().getTabs()) {
                    SqlControlDto sqlControlDto = connectControlDto.getSqlControlMap().get(sqlTab);
                    if (null == sqlControlDto) {
                        continue;
                    }
                    String sqlTabName = null;
                    if (StringUtils.isNotBlank(sqlTab.getText())) {
                        sqlTabName = sqlTab.getText();
                    } else {
                        sqlTabName = ((Label)sqlTab.getGraphic()).getText();
                    }

                    String sql = SqlEditorUtils.getAllSqlCode(sqlControlDto.getSqlCodeView());
                    snapshot.addSql(sqlTabName, StringUtils.trim(sql), sqlControlDto.getSqlFilePath(), sqlControlDto.isModified());
                }
            }

            if (CollectionUtils.isEmpty(snapshots)) {
                return;
            }

            // 快照信息写入文件
            File file = new File("zssql.ss");
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] b = JSON.toJSONString(snapshots, SerializerFeature.PrettyFormat).getBytes();
            out.write(b,0,b.length);
            out.flush();
            out.close();
        } catch(Exception ex){
            ex.printStackTrace();
        } finally {
            savingSnapshot = false;
        }
    }

    /**
     * 保存窗口位置
     */
    private void saveWindowPostion() {
        Stage mainStage = (Stage)mainTabPanel.getScene().getWindow();
        boolean isInsert = false;
        MainWindowPostion postion = SQLiteProvider.getMainWindowPostion();
        if (null == postion) {
            isInsert = true;
            postion = new MainWindowPostion();
            postion.setId(1);
        }

        postion.setX(mainStage.getX());
        postion.setY(mainStage.getY());
        postion.setWidth(mainStage.getWidth());
        postion.setHeight(mainStage.getHeight());
        postion.setFullScreen(mainStage.isMaximized()? 1:0);
        if (isInsert) {
            SQLiteProvider.insertMainWindowPostion(postion);
        } else {
            SQLiteProvider.updateMainWindowPostion(postion);
        }
    }

    /**
     * 新建数据库连接
     * @param actionEvent
     */
    public void onNewDatabaseConnect(ActionEvent actionEvent) {
        newDatabaseConnect();
    }

    private void newDatabaseConnect() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("database-select-view.fxml"));
            Parent root = fxmlLoader.load();
            DatabaseSelectViewController controller = fxmlLoader.getController();
            controller.setParent(this);
            Stage stage = new Stage();
            stage.setTitle("连接到数据库");
            Scene scene = new Scene(root, 500, 400);
            stage.setScene(scene);
            Stage parent = (Stage) mainTabPanel.getScene().getWindow();
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
     * 新建数据库连接
     * @param dto
     */
    public Tab newConnect(ConnectInfoDto dto) {
        if (null == dto) {
            return null;
        }

        Label connTabLabel = new Label(dto.getConnectName());
        Tab tab = new Tab();
        connTabLabel.setGraphic(new ImageView(new Image("local_db.png")));
        tab.setGraphic(connTabLabel);
        // 连接关闭请求
        tab.setOnCloseRequest(event -> {
            if (!BooleanUtils.isTrue(config.getTipSaveAfterCloseTab())) {
                return;
            }
            ConnectControlDto connectControlDto = connectConrolMap.get(tab);
            if (CollectionUtils.isEmpty(connectControlDto.getSqlTabPane().getTabs())) {
                return;
            }
            Boolean allSave = null;
            for (Tab sqlTab : connectControlDto.getSqlTabPane().getTabs()) {
                if (null != allSave) {
                    // 选中了保存所有或所有不保存
                    if (!allSave) {
                        // 都不保存
                        return;
                    }
                }
                if (!connectControlDto.getSqlControlMap().containsKey(sqlTab)) {
                    continue;
                }
                SqlControlDto sqlControlDto = connectControlDto.getSqlControlMap().get(sqlTab);
                if (StringUtils.isNotBlank(sqlControlDto.getSqlFilePath()) && !sqlControlDto.isModified()) {
                    // SQL页签已关联到文件且未变更, 无需保存
                    continue;
                }
                String sqlCode = SqlEditorUtils.getAllSqlCode(sqlControlDto.getSqlCodeView());
                if (StringUtils.isEmpty(sqlCode) && StringUtils.isBlank(sqlControlDto.getSqlFilePath())) {
                    // SQL页签未关联文件且SQL编辑器空，无需保存
                    continue;
                }
                // 需要提示保存，则需要先选中当前SQL页签
                connectControlDto.getSqlTabPane().getSelectionModel().select(sqlTab);
                ButtonType bt = BooleanUtils.isTrue(allSave)? ButtonType.YES :  MessageAlert.confirmConnSqlSave("该选项卡的内容已被更改", "是否保存这些改动");
                if (bt.equals(ButtonType.CANCEL)) {
                    // 点击取消，则退出关闭
                    event.consume();
                    return;
                } else if (bt.equals(ButtonType.YES)) {
                    saveSqlToFile(tab, sqlTab, false);
                } else if (bt.equals(MessageAlert.allSave)) {
                    saveSqlToFile(tab, sqlTab, false);
                    allSave = true;
                } else if (bt.equals(MessageAlert.allNotSave)) {
                    allSave = false;
                }
            }
        });
        tab.setOnClosed(event -> {
            // tab 被关闭
            closeConnectTab(tab);
            refreshMenuAndToolBarStatus();
        });

        // 创建连接页面
        ConnectControlDto controlDto = new ConnectControlDto();
        controlDto.setConnectInfo(dto);
        connectConrolMap.put(tab, controlDto);
        createConnectPage(tab, dto);
        mainTabPanel.getTabs().add(mainTabPanel.getTabs().size() - 1, tab);
        mainTabPanel.getSelectionModel().select(tab);

        tipLabel.setText("就绪");
        refreshMenuAndToolBarStatus();

        return tab;
    }

    /**
     * 关闭指定数据库连接
     * @param tab 数据库连接页
     */
    private void closeConnectTab(Tab tab) {
        if (null == tab || newConnnectTab == tab) {
            return;
        }
        if (!connectConrolMap.containsKey(tab)) {
            return;
        }

        // 移除当前数据库连接下所有SQL编辑器ID与SQL页签对应关系
        Set<String> sqlIds = connectConrolMap.get(tab).getSqlCodeIdMap().keySet();
        if (CollectionUtils.isNotEmpty(sqlIds)) {
            sqlIds.forEach(sqlCodeId2ConnTabMap::remove);
        }

        // 关闭连接并移除连接映射
        connectConrolMap.get(tab).close();
        connectConrolMap.remove(tab);
    }

    /**
     * 创建连接页面
     * @param connectTab 所属tab页
     * @param dto 连接信息
     */
    private void createConnectPage(Tab connectTab, ConnectInfoDto dto) {
        AnchorPane pane = new AnchorPane();
        connectTab.setContent(pane);
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(Orientation.HORIZONTAL);
        mainSplitPane.setDividerPositions(0.2, 0.8);
        AnchorPane.setLeftAnchor(mainSplitPane, 0.0);
        AnchorPane.setRightAnchor(mainSplitPane, 0.0);
        AnchorPane.setTopAnchor(mainSplitPane, 0.0);
        AnchorPane.setBottomAnchor(mainSplitPane, 0.0);
        pane.getChildren().add(mainSplitPane);

        // 左侧窗口
        AnchorPane leftPane = new AnchorPane();
        VBox leftVBox = new VBox();
        leftVBox.setPadding(new Insets(3));
        leftVBox.setSpacing(2);
        Label tableLabel = new Label("筛选表格");
        TextField filterTableEdit = TextFields.createClearableTextField();
        TextFields.bindAutoCompletion(filterTableEdit, dto.getTables().stream().map(DatasouceTableVO::getTableName).toList());
        filterTableEdit.setPromptText("表过滤器");
        VBox.setMargin(tableLabel, new Insets(5, 3, 3, 3));
        VBox.setMargin(filterTableEdit, new Insets(3));
        ListView<DatasouceTableVO> tableInfoView = new ListView<>();
        ListView<DatasouceTableVO> filterTableView = new ListView<>();
        VBox.setMargin(tableInfoView, new Insets(3));
        VBox.setMargin(filterTableView, new Insets(3));
        leftVBox.getChildren().addAll(tableLabel, filterTableEdit, tableInfoView, filterTableView);
        VBox.setVgrow(tableInfoView, Priority.ALWAYS);
        VBox.setVgrow(filterTableView, Priority.ALWAYS);
        leftPane.getChildren().add(leftVBox);
        AnchorPane.setLeftAnchor(leftVBox, 0.0);
        AnchorPane.setRightAnchor(leftVBox, 0.0);
        AnchorPane.setTopAnchor(leftVBox, 0.0);
        AnchorPane.setBottomAnchor(leftVBox, 0.0);
        mainSplitPane.getItems().add(leftPane);
        SplitPane.setResizableWithParent(leftPane, false);
        // 添加表清单
        tableInfoView.getItems().clear();
        tableInfoView.getItems().addAll(dto.getTables());
        connectConrolMap.get(connectTab).setFilterTableEdit(filterTableEdit);
        connectConrolMap.get(connectTab).setTableInfoView(tableInfoView);
        connectConrolMap.get(connectTab).setFilterTableView(filterTableView);

        // 表清单列表控件处理
        handlerTableListView(connectTab, dto, filterTableEdit, tableInfoView, filterTableView);

        // 右侧窗口(Tab窗口)
        AnchorPane rightPane = new AnchorPane();
        VBox rightVBox = new VBox();
        rightVBox.setPadding(new Insets(3));
        rightVBox.setSpacing(5);
        TabPane sqlTabPane = new TabPane();
        DraggingTabPaneSupport sqlTabPaneSupport = new DraggingTabPaneSupport();
        sqlTabPaneSupport.addSupport(sqlTabPane);
        sqlTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.SELECTED_TAB);
        rightVBox.getChildren().add(sqlTabPane);
        rightPane.getChildren().add(rightVBox);
        VBox.setVgrow(sqlTabPane, Priority.ALWAYS);
        AnchorPane.setLeftAnchor(rightVBox, 0.0);
        AnchorPane.setRightAnchor(rightVBox, 0.0);
        AnchorPane.setTopAnchor(rightVBox, 0.0);
        AnchorPane.setBottomAnchor(rightVBox, 0.0);
        mainSplitPane.getItems().add(rightPane);
        connectConrolMap.get(connectTab).setSqlTabPane(sqlTabPane);

        // 新建SQL查询编辑器Tab按钮
        Tab newSqlTab = new Tab(null);
        newSqlTab.setGraphic(new ImageView(new Image("add_tab.png")));
        newSqlTab.setClosable(false);
        newSqlTab.setTooltip(CommonUtil.toolTip("新查询编辑器", 12));
        sqlTabPane.getTabs().add(newSqlTab);
        sqlTabPane.getSelectionModel().selectedItemProperty().addListener((observableValue, oldTab, newTab) -> {
            if (newTab == newSqlTab) {
                addSqlTab(connectTab, "");
            }
            updateStatus();
            updateDetailView();
            refreshMenuAndToolBarStatus();
        });

        // 新建SQL查询器页签
        addSqlTab(connectTab, "");
    }

    private void handlerTableListView(Tab connectTab, ConnectInfoDto dto, TextField filterTableEdit, ListView<DatasouceTableVO> tableInfoView, ListView<DatasouceTableVO> filterTableView) {
        filterTableEdit.setText(null);
        CommonUtil.hideControl(filterTableView);

        // 表清单双击处理，需要在当前选中的SQL查询编辑器中添加表名
        List.of(tableInfoView, filterTableView).forEach(view -> {
            view.setOnMouseClicked(mouseEvent -> {
                if (!MouseClickUtils.isLeftDoubleClick(mouseEvent)) {
                    return;
                }
                DatasouceTableVO selectedItem = view.getSelectionModel().getSelectedItem();
                if (null == selectedItem) {
                    return;
                }
                String tableName = selectedItem.getTableName();
                // 获取选中的查询编辑器页签
                Tab sqlTab = connectConrolMap.get(connectTab).getSqlTabPane().getSelectionModel().getSelectedItem();
                if (null == sqlTab) {
                    return;
                }
                if (!connectConrolMap.get(connectTab).getSqlControlMap().containsKey(sqlTab)) {
                    return;
                }
                // 获取对应的代码编辑器
                WebView sqlCodeView = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).getSqlCodeView();
                if (null == sqlCodeView) {
                    return;
                }
                if (StringUtils.isNotEmpty(SqlEditorUtils.getSelectSqlCode(sqlCodeView))) {
                    SqlEditorUtils.replaceSelect(sqlCodeView, tableName);
                } else {
                    SqlEditorUtils.insertSqlCode(sqlCodeView, tableName);
                }

                sqlCodeView.requestFocus();
            });
        });

        // 上下文菜单
        ContextMenu menu = new ContextMenu();
        MenuItem refreshMenu = new MenuItem("刷新");
        refreshMenu.setGraphic(new ImageView(new Image("refresh.png")));
        MenuItem createMenu = new MenuItem("创建表");
        createMenu.setGraphic(new ImageView(new Image("ceate_table.png")));
        MenuItem modifyMenu = new MenuItem("修改表");
        modifyMenu.setGraphic(new ImageView(new Image("modify_table.png")));
        modifyMenu.setDisable(true);
        MenuItem deleteMenu = new MenuItem("删除表");
        deleteMenu.setGraphic(new ImageView(new Image("delete_table.png")));
        deleteMenu.setDisable(true);
        MenuItem selectSqlMenu = new MenuItem("查询表");
        selectSqlMenu.setGraphic(new ImageView(new Image("query_table.png")));
        selectSqlMenu.setDisable(true);
        MenuItem tableDataMenu = new MenuItem("表数据");
        tableDataMenu.setGraphic(new ImageView(new Image("table_view.png")));
        tableDataMenu.setDisable(true);
        MenuItem genJavaObjMenu = new MenuItem("生成Java对象");
        genJavaObjMenu.setGraphic(new ImageView(new Image("java.png")));
        genJavaObjMenu.setDisable(true);
        menu.getItems().addAll(refreshMenu, new SeparatorMenuItem(), createMenu,
                modifyMenu, deleteMenu, new SeparatorMenuItem(), selectSqlMenu, new SeparatorMenuItem(), tableDataMenu,
                new SeparatorMenuItem(), genJavaObjMenu);
        tableInfoView.setContextMenu(menu);
        filterTableView.setContextMenu(menu);

        tableInfoView.getSelectionModel().selectedItemProperty().addListener((observableValue, oldVal, newVal) -> {
            if (StringUtils.isNotEmpty(filterTableEdit.getText())) {
                return;
            }
            if (null != newVal) {
                modifyMenu.setDisable(false);
                deleteMenu.setDisable(false);
                selectSqlMenu.setDisable(false);
                genJavaObjMenu.setDisable(false);
                tableDataMenu.setDisable(false);
            } else {
                modifyMenu.setDisable(true);
                deleteMenu.setDisable(true);
                selectSqlMenu.setDisable(true);
                genJavaObjMenu.setDisable(true);
                tableDataMenu.setDisable(true);
            }
            updateDetailView();
        });
        filterTableView.getSelectionModel().selectedItemProperty().addListener((observableValue, oldVal, newVal) -> {
            if (StringUtils.isEmpty(filterTableEdit.getText())) {
                return;
            }
            if (null != newVal) {
                modifyMenu.setDisable(false);
                deleteMenu.setDisable(false);
                selectSqlMenu.setDisable(false);
                genJavaObjMenu.setDisable(false);
                tableDataMenu.setDisable(false);
            } else {
                modifyMenu.setDisable(true);
                deleteMenu.setDisable(true);
                selectSqlMenu.setDisable(true);
                genJavaObjMenu.setDisable(true);
                tableDataMenu.setDisable(true);
            }
            updateDetailView();
        });

        // 筛选编辑框文本发生变化
        filterTableEdit.textProperty().addListener((observable, oldValue, newValue) -> {
            if (StringUtils.isEmpty(newValue)) {
                CommonUtil.showControl(tableInfoView);
                CommonUtil.hideControl(filterTableView);
                if (null == tableInfoView.getSelectionModel().getSelectedItem()) {
                    modifyMenu.setDisable(false);
                    deleteMenu.setDisable(false);
                    selectSqlMenu.setDisable(false);
                    genJavaObjMenu.setDisable(false);
                    tableDataMenu.setDisable(false);
                } else {
                    modifyMenu.setDisable(true);
                    deleteMenu.setDisable(true);
                    selectSqlMenu.setDisable(true);
                    genJavaObjMenu.setDisable(true);
                    tableDataMenu.setDisable(true);
                }
            } else {
                // 过滤出符合条件的表
                List<DatasouceTableVO> filterTables = dto.getTables().stream().filter(t -> StringUtils.containsIgnoreCase(t.getTableName(), newValue)).toList();
                filterTableView.getItems().clear();
                filterTableView.getItems().addAll(filterTables);
                // 过滤数据
                CommonUtil.showControl(filterTableView);
                CommonUtil.hideControl(tableInfoView);
                if (null == filterTableView.getSelectionModel().getSelectedItem()) {
                    modifyMenu.setDisable(false);
                    deleteMenu.setDisable(false);
                    selectSqlMenu.setDisable(false);
                    genJavaObjMenu.setDisable(false);
                    tableDataMenu.setDisable(false);
                } else {
                    modifyMenu.setDisable(true);
                    deleteMenu.setDisable(true);
                    selectSqlMenu.setDisable(true);
                    genJavaObjMenu.setDisable(true);
                    tableDataMenu.setDisable(true);
                }
            }
            updateDetailView();
        });

        // 刷新数据库表结构
        refreshMenu.setOnAction(actionEvent -> {
            ConnectInfoDto connectInfo = connectConrolMap.get(connectTab).getConnectInfo();
            List<DatasouceTableVO> tables = DataSourceProvider.listTables(connectInfo);
            if (CollectionUtils.isEmpty(tables)) {
                return;
            }
            connectInfo.getTables().clear();
            connectInfo.getTables().addAll(tables);
            connectConrolMap.get(connectTab).getConnectInfo().setTables(tables);
            connectConrolMap.get(connectTab).getTableInfoView().getItems().clear();
            connectConrolMap.get(connectTab).getTableInfoView().getItems().addAll(tables);
            if (StringUtils.isNotEmpty(connectConrolMap.get(connectTab).getFilterTableEdit().getText())) {
                List<DatasouceTableVO> filterTables = tables.stream().filter(t -> StringUtils.containsIgnoreCase(t.getTableName(), connectConrolMap.get(connectTab).getFilterTableEdit().getText())).toList();
                connectConrolMap.get(connectTab).getFilterTableView().getItems().clear();
                connectConrolMap.get(connectTab).getFilterTableView().getItems().addAll(filterTables);
            }
            updateDetailView();
        });

        // 创建表
        createMenu.setOnAction(actionEvent -> {
            addTableDDLTab(connectTab, null);
        });

        // 修改表
        modifyMenu.setOnAction(actionEvent -> {
            DatasouceTableVO selectedItem = StringUtils.isEmpty(filterTableEdit.getText())? tableInfoView.getSelectionModel().getSelectedItem() : filterTableView.getSelectionModel().getSelectedItem();
            if (null == selectedItem) {
                return;
            }
            addTableDDLTab(connectTab, selectedItem);
        });

        // 删除表
        deleteMenu.setOnAction(actionEvent -> {
            DatasouceTableVO selectedItem = StringUtils.isEmpty(filterTableEdit.getText())? tableInfoView.getSelectionModel().getSelectedItem() : filterTableView.getSelectionModel().getSelectedItem();
            if (null == selectedItem) {
                return;
            }
            TextInputDialog td = new TextInputDialog("");
            td.setTitle("删除表");
            td.setContentText("需删除的表名:");
            td.setHeaderText(String.format("是否确定要删除表[%s]？\n删除后表及表数据都将被彻底删除且不可恢复\n如确认需要删除请在下方手动输入需要删除的表名并确认", selectedItem.getTableName()));
            Optional<String> str = td.showAndWait();
            if (str.isEmpty()) {
                return;
            }
            if (!StringUtils.equalsIgnoreCase(str.get(), selectedItem.getTableName())) {
                MessageAlert.information("输入的表名与选择需要删除的表名不一致");
                return;
            }

            try {
                // 执行删除表脚本
                DataSourceProvider.executeSqls(dto, Collections.singletonList(String.format("drop table %s", selectedItem.getTableName())));
            } catch (Exception e) {
                MessageAlert.error(String.format("删除失败:%s", e.getMessage()));
                return;
            }
            // 删除成功触发一次刷新
            refreshMenu.fire();
            MessageAlert.information("删除成功");
        });

        // 查询表
        selectSqlMenu.setOnAction(actionEvent -> {
            DatasouceTableVO selectedItem = StringUtils.isEmpty(filterTableEdit.getText())? tableInfoView.getSelectionModel().getSelectedItem() : filterTableView.getSelectionModel().getSelectedItem();
            if (null == selectedItem) {
                return;
            }
            String tableName = selectedItem.getTableName();
            // 获取选中的查询编辑器页签
            Tab sqlTab = connectConrolMap.get(connectTab).getSqlTabPane().getSelectionModel().getSelectedItem();
            if (null == sqlTab) {
                return;
            }
            String sql = "select * from " + tableName + ";";
            if (!connectConrolMap.get(connectTab).getSqlControlMap().containsKey(sqlTab)) {
                MessageAlert.information("请先切换到任一查询页面");
                return;
            }
            // 获取对应的代码编辑器
            WebView sqlCodeView = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).getSqlCodeView();
            if (null == sqlCodeView) {
                return;
            }

            // SQL编辑器添加查询SQL新行
            SqlEditorUtils.appendNewLine(sqlCodeView, sql);
            // 选中最后一行
            int lineCount = SqlEditorUtils.getLineCount(sqlCodeView);
            SqlEditorUtils.selectLine(sqlCodeView, lineCount - 1);
            // 执行选中的SQL查询
            executeSQL(false);
        });
        // 表数据
        tableDataMenu.setOnAction(event -> {
            DatasouceTableVO selectedItem = StringUtils.isEmpty(filterTableEdit.getText())? tableInfoView.getSelectionModel().getSelectedItem() : filterTableView.getSelectionModel().getSelectedItem();
            if (null == selectedItem) {
                return;
            }
            String tableName = selectedItem.getTableName();
            addTableDataTab(connectTab, tableName);
        });
        // 生成Java代码
        genJavaObjMenu.setOnAction(event -> {
            DatasouceTableVO selectedItem = StringUtils.isEmpty(filterTableEdit.getText())? tableInfoView.getSelectionModel().getSelectedItem() : filterTableView.getSelectionModel().getSelectedItem();
            if (null == selectedItem) {
                return;
            }
            showGenJavaView(selectedItem);
        });
        tableInfoView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F5) {
                refreshMenu.fire();
            }
        });
        filterTableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.F5) {
                refreshMenu.fire();
            }
        });
    }

    /**
     * 添加SQL页
     */
    private Tab addSqlTab(Tab connectTab, String code) {
        ConnectControlDto controlDto = connectConrolMap.get(connectTab);
        Label sqlTabLabel = new Label("查询  ");
        Tab sqlTab = new Tab(null);
        sqlTabLabel.setGraphic(new ImageView(new Image("sql.png")));
        sqlTab.setGraphic(sqlTabLabel);
        AnchorPane sqlMainPane = new AnchorPane();
        SplitPane sqlSplitPane = new SplitPane();
        sqlSplitPane.setOrientation(Orientation.VERTICAL);
        sqlSplitPane.setDividerPositions(0.4, 0.6);
        sqlMainPane.getChildren().add(sqlSplitPane);
        sqlTab.setContent(sqlMainPane);
        AnchorPane.setLeftAnchor(sqlSplitPane, 0.0);
        AnchorPane.setRightAnchor(sqlSplitPane, 0.0);
        AnchorPane.setTopAnchor(sqlSplitPane, 0.0);
        AnchorPane.setBottomAnchor(sqlSplitPane, 0.0);
        controlDto.getSqlTabPane().getTabs().add(controlDto.getSqlTabPane().getTabs().size() - 1, sqlTab);
        controlDto.getSqlTabPane().getSelectionModel().select(sqlTab);
        connectConrolMap.get(connectTab).getSqlControlMap().put(sqlTab, new SqlControlDto());
        sqlTab.setOnCloseRequest(event -> {
            if (!BooleanUtils.isTrue(config.getTipSaveAfterCloseTab())) {
                return;
            }
            // 请求关闭SQL页签
            SqlControlDto sqlControlDto = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab);
            if (null == sqlControlDto) {
                return;
            }
            if (StringUtils.isNotBlank(sqlControlDto.getSqlFilePath()) && !sqlControlDto.isModified()) {
                // 已关联SQL文件但未发生变更
                return;
            }
            if (StringUtils.isBlank(sqlControlDto.getSqlFilePath())
                    && StringUtils.isBlank(SqlEditorUtils.getAllSqlCode(sqlControlDto.getSqlCodeView()))) {
                // 未关联文件且SQL编辑器文本内容为空
                return;
            }
            ButtonType bt = MessageAlert.confirmSave("该选项卡的内容已被更改", "是否保存这些改动");
            if (bt.equals(ButtonType.YES)) {
                // 先保存SQL
                saveSqlToFile(connectTab, sqlTab, false);
            } else if (bt.equals(ButtonType.CANCEL)) {
                // 点击取消不关闭页签
                event.consume();
            }
        });
        sqlTab.setOnClosed(event -> {
            // 关闭SQL标签页
            Map<String, Tab> sqlCodeIdMap = connectConrolMap.get(connectTab).getSqlCodeIdMap();
            for (Map.Entry<String, Tab> entry : sqlCodeIdMap.entrySet()) {
                if (sqlTab == entry.getValue()) {
                    sqlCodeId2ConnTabMap.remove(entry.getKey());
                }
            }
            connectConrolMap.get(connectTab).closeSqlTab(sqlTab);
        });

        // SQL Tab menu
        ContextMenu sqlTabMenu = new ContextMenu();
        MenuItem closeMenu = new MenuItem("关闭");
        closeMenu.setGraphic(new ImageView(new Image("close.png")));
        MenuItem renameMenu = new MenuItem("重命名");
        renameMenu.setGraphic(new ImageView(new Image("rename.png")));
        sqlTabMenu.getItems().addAll(closeMenu, renameMenu);
        sqlTab.setContextMenu(sqlTabMenu);

        closeMenu.setOnAction(actionEvent -> {
            if (!BooleanUtils.isTrue(config.getTipSaveAfterCloseTab())) {
                connectConrolMap.get(connectTab).closeSqlTab(sqlTab);
                controlDto.getSqlTabPane().getTabs().remove(sqlTab);
                return;
            }
            // 关闭SQL标签页
            SqlControlDto sqlControlDto = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab);
            if (null == sqlControlDto) {
                connectConrolMap.get(connectTab).closeSqlTab(sqlTab);
                controlDto.getSqlTabPane().getTabs().remove(sqlTab);
                return;
            }
            if (StringUtils.isNotBlank(sqlControlDto.getSqlFilePath()) && !sqlControlDto.isModified()) {
                // 已关联SQL文件但未发生变更
                connectConrolMap.get(connectTab).closeSqlTab(sqlTab);
                controlDto.getSqlTabPane().getTabs().remove(sqlTab);
                return;
            }
            if (StringUtils.isBlank(sqlControlDto.getSqlFilePath())
                    && StringUtils.isBlank(SqlEditorUtils.getAllSqlCode(sqlControlDto.getSqlCodeView()))) {
                // 未关联文件且SQL编辑器文本内容为空
                connectConrolMap.get(connectTab).closeSqlTab(sqlTab);
                controlDto.getSqlTabPane().getTabs().remove(sqlTab);
                return;
            }
            ButtonType bt = MessageAlert.confirmSave("该选项卡的内容已被更改", "是否保存这些改动");
            if (bt.equals(ButtonType.YES)) {
                // 先保存SQL
                saveSqlToFile(connectTab, sqlTab, false);
                connectConrolMap.get(connectTab).closeSqlTab(sqlTab);
                controlDto.getSqlTabPane().getTabs().remove(sqlTab);
            } else if (bt.equals(ButtonType.NO)) {
                connectConrolMap.get(connectTab).closeSqlTab(sqlTab);
                controlDto.getSqlTabPane().getTabs().remove(sqlTab);
            }
        });
        renameMenu.setOnAction(actionEvent -> {
            if (StringUtils.isNotBlank(connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).getSqlFilePath())) {
                MessageAlert.information("当前页签已关联SQL文件,不支持重命名");
                return;
            }
            TextInputDialog td = new TextInputDialog(sqlTabLabel.getText());
            td.setTitle("重命名标签");
            td.setContentText("名称");
            td.setHeaderText("请输入新的标签名称");
            Optional<String> str = td.showAndWait();
            if (str.isPresent()) {
                String name = str.get();
                if (StringUtils.isNotBlank(name)) {
                    sqlTabLabel.setText(name);
                }
            }
        });

        // SQL窗口(右上角)
        AnchorPane sqlPane = new AnchorPane();
        VBox sqlVBox = new VBox();
        sqlVBox.setPadding(new Insets(3));
        sqlVBox.setSpacing(2);
        Label sqlLabel = new Label("快捷键:  [F9]->执行选中的SQL   [Ctrl+F9]->执行所有SQL   [F12]->格式化SQL   [Ctrl+Enter]->代码提示");
        VBox.setMargin(sqlLabel, new Insets(1, 3, 1, 3));
        WebView codeView = new WebView();
        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).setSqlCodeView(codeView);
        // 组装SQL编辑器
        assembleSqlCodeEditor(connectTab, sqlTab, codeView, controlDto.getTableInfoView(), code, false);
        VBox.setMargin(codeView, new Insets(1, 3, 3, 3));
        sqlVBox.getChildren().addAll(sqlLabel, codeView);
        VBox.setVgrow(codeView, Priority.ALWAYS);
        sqlPane.getChildren().add(sqlVBox);
        AnchorPane.setLeftAnchor(sqlVBox, 0.0);
        AnchorPane.setRightAnchor(sqlVBox, 0.0);
        AnchorPane.setTopAnchor(sqlVBox, 0.0);
        AnchorPane.setBottomAnchor(sqlVBox, 0.0);
        SplitPane.setResizableWithParent(sqlPane, false);
        sqlSplitPane.getItems().add(sqlPane);

        // 结果窗口(右下角)
        AnchorPane resultPane = new AnchorPane();
        VBox resultVBox = new VBox();
        resultVBox.setPadding(new Insets(3));
        TabPane resultTabPane = new TabPane();
        resultTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        AnchorPane.setLeftAnchor(resultVBox, 0.0);
        AnchorPane.setRightAnchor(resultVBox, 0.0);
        AnchorPane.setTopAnchor(resultVBox, 0.0);
        AnchorPane.setBottomAnchor(resultVBox, 0.0);
        resultVBox.getChildren().addAll(resultTabPane);
        VBox.setVgrow(resultTabPane, Priority.ALWAYS);
        resultPane.getChildren().add(resultVBox);
        sqlSplitPane.getItems().add(resultPane);
        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).setResultTabPane(resultTabPane);
        resultTabPane.getSelectionModel().selectedItemProperty().addListener((observableValue, tab, t1) -> updateStatus());
        // 创建表明细页签
        createTableDetailTabPage(resultTabPane, connectTab, sqlTab);
        // 更新菜单及工具栏按钮状态
        refreshMenuAndToolBarStatus();
        return sqlTab;
    }

    private void createTableDetailTabPage(TabPane resultTabPane, Tab connectTab, Tab sqlTab) {
        // 详情页面
        Tab logTab = new Tab("详情");
        logTab.setGraphic(new ImageView(new Image("about.png")));
        resultTabPane.getTabs().add(logTab);
        AnchorPane logPane = new AnchorPane();
        VBox logVBox = new VBox();
        logVBox.setPadding(new Insets(3));
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logVBox.getChildren().add(logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        logPane.getChildren().add(logVBox);
        AnchorPane.setLeftAnchor(logVBox, 0.0);
        AnchorPane.setRightAnchor(logVBox, 0.0);
        AnchorPane.setTopAnchor(logVBox, 0.0);
        AnchorPane.setBottomAnchor(logVBox, 0.0);
        logTab.setContent(logPane);
        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).setLogTab(logTab);
        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).setLogArea(logArea);

        // 明细页面
        Tab detailTab = new Tab("信息");
        detailTab.setGraphic(new ImageView(new Image("detail.png")));
        resultTabPane.getTabs().add(detailTab);
        AnchorPane detailPane = new AnchorPane();
        VBox detailVBox = new VBox();
        detailVBox.setPadding(new Insets(3));
        WebView detailView = new WebView();
        detailVBox.getChildren().add(detailView);
        VBox.setVgrow(detailView, Priority.ALWAYS);
        detailPane.getChildren().add(detailVBox);
        AnchorPane.setLeftAnchor(detailVBox, 0.0);
        AnchorPane.setRightAnchor(detailVBox, 0.0);
        AnchorPane.setTopAnchor(detailVBox, 0.0);
        AnchorPane.setBottomAnchor(detailVBox, 0.0);
        detailTab.setContent(detailPane);
        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).setDetailTab(detailTab);
        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).setDetailView(detailView);
        // 更新明细信息
        showDetailView(connectTab, sqlTab, detailView,
                StringUtils.isEmpty(connectConrolMap.get(connectTab).getFilterTableEdit().getText())? connectConrolMap.get(connectTab).getTableInfoView() : connectConrolMap.get(connectTab).getFilterTableView());
    }

    /**
     * 添加表数据页签
     * @param connectTab
     * @param tableName
     */
    private void addTableDataTab(Tab connectTab, String tableName) {
        // 表数据页签
        Label dataTabLabel = new Label("表数据: " + tableName);
        Tab dataTab = new Tab(null);
        dataTabLabel.setGraphic(new ImageView(new Image("table_view.png")));
        dataTab.setGraphic(dataTabLabel);
        dataTab.setClosable(true);
        ConnectControlDto controlDto = connectConrolMap.get(connectTab);
        controlDto.getSqlTabPane().getTabs().add(controlDto.getSqlTabPane().getTabs().size() - 1, dataTab);
        controlDto.getSqlTabPane().getSelectionModel().select(dataTab);
        AnchorPane dataPane = new AnchorPane();
        VBox dataVBox = new VBox();
        dataVBox.setPadding(new Insets(3));
        // 结果工具栏
        HBox toolHBox = new HBox();
        toolHBox.setPadding(new Insets(1, 5, 1, 3));
        toolHBox.setSpacing(3);
        toolHBox.setAlignment(Pos.CENTER_LEFT);
        // 表数据结果视图
        SpreadsheetView dataView = new SpreadsheetView();
        dataView.setEditable(true);
        dataView.setShowColHeaderMenu(false);
        dataView.setShowRowHeaderMenu(false);
        dataView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        dataView.setLabelEditFlg(true);
        // 数据库信息
        HBox tableInfoHBox = new HBox();
        tableInfoHBox.setAlignment(Pos.CENTER_LEFT);
        tableInfoHBox.setPadding(new Insets(3));
        tableInfoHBox.setSpacing(5);
        TextField filterField = new TextField();
        filterField.setEditable(false);
        filterField.setPromptText("请通过工具栏筛选器编辑筛选条件");
        tableInfoHBox.getChildren().addAll(new Label("  筛选条件:"), filterField);
        HBox.setHgrow(filterField, Priority.ALWAYS);
        VBox.setVgrow(dataView, Priority.ALWAYS);
        dataVBox.getChildren().addAll(toolHBox, dataView, tableInfoHBox);
        dataPane.getChildren().add(dataVBox);
        AnchorPane.setLeftAnchor(dataVBox, 0.0);
        AnchorPane.setRightAnchor(dataVBox, 0.0);
        AnchorPane.setTopAnchor(dataVBox, 0.0);
        AnchorPane.setBottomAnchor(dataVBox, 0.0);
        dataTab.setContent(dataPane);
        handlerDataView(connectTab, dataTab, dataView, toolHBox, filterField, tableName);
    }

    /**
     * 表数据页签控件初始化及事件处理
     * @param connectTab
     * @param dataView
     * @param toolHBox
     */
    private void handlerDataView(Tab connectTab, Tab dataTab, SpreadsheetView dataView,
                                 HBox toolHBox, TextField filterField, String tableName) {
        DataResultControlDto controlDto = new DataResultControlDto();
        controlDto.setTableName(tableName);
        DatasouceTableVO datasouceTableVO = connectConrolMap.get(connectTab).getTableInfoView().getItems().stream()
                .filter(v -> CommonUtil.isObjectEquals(v.getTableName(), tableName)).findAny().orElse(null);
        controlDto.setTableVO(datasouceTableVO);
        controlDto.setDataTab(dataTab);
        controlDto.setDataView(dataView);
        controlDto.setFilterField(filterField);

        Button exportSelectBtn = new Button(null);
        exportSelectBtn.setGraphic(new ImageView(new Image("export_select.png")));
        exportSelectBtn.setTooltip(CommonUtil.toolTip("导出选中行", 12, Duration.ONE));
        controlDto.setExportSelectBtn(exportSelectBtn);
        Button exportAllBtn = new Button(null);
        exportAllBtn.setGraphic(new ImageView(new Image("export_all.png")));
        exportAllBtn.setTooltip(CommonUtil.toolTip("导出所有数据", 12, Duration.ONE));
        controlDto.setExportAllBtn(exportAllBtn);

        Button addRowBtn = new Button(null);
        addRowBtn.setGraphic(new ImageView(new Image("add_row.png")));
        addRowBtn.setTooltip(CommonUtil.toolTip("添加新行", 12, Duration.ONE));
        controlDto.setAddRowBtn(addRowBtn);
        Button copyRowBtn = new Button(null);
        copyRowBtn.setGraphic(new ImageView(new Image("copy_row.png")));
        copyRowBtn.setTooltip(CommonUtil.toolTip("为当前行创建副本", 12, Duration.ONE));
        controlDto.setCopyRowBtn(copyRowBtn);
        Button saveRowBtn = new Button(null);
        saveRowBtn.setGraphic(new ImageView(new Image("save.png")));
        saveRowBtn.setTooltip(CommonUtil.toolTip("保存变更", 12, Duration.ONE));
        controlDto.setSaveRowBtn(saveRowBtn);
        Button deleteRowBtn = new Button(null);
        deleteRowBtn.setGraphic(new ImageView(new Image("delete.png")));
        deleteRowBtn.setTooltip(CommonUtil.toolTip("删除选中行", 12, Duration.ONE));
        controlDto.setDeleteRowBtn(deleteRowBtn);
        Button cancelRowBtn = new Button(null);
        cancelRowBtn.setGraphic(new ImageView(new Image("cancel_edit.png")));
        cancelRowBtn.setTooltip(CommonUtil.toolTip("取消变更", 12, Duration.ONE));
        controlDto.setCancelRowBtn(cancelRowBtn);

        Separator hseparator = new Separator();
        hseparator.setOrientation(Orientation.HORIZONTAL);
        HBox.setHgrow(hseparator, Priority.ALWAYS);
        hseparator.setVisible(false);
        Button filterBtn = new Button(null);
        filterBtn.setGraphic(new ImageView(new Image("filter.png")));
        filterBtn.setTooltip(CommonUtil.toolTip("筛选器", 12, Duration.ONE));
        controlDto.setFilterBtn(filterBtn);
        Button refreshBtn = new Button(null);
        refreshBtn.setGraphic(new ImageView(new Image("refresh.png")));
        refreshBtn.setTooltip(CommonUtil.toolTip("刷新", 12, Duration.ONE));
        controlDto.setRefreshBtn(refreshBtn);
        Button leftBtn = new Button("◀");
        leftBtn.setDisable(true);
        controlDto.setLeftBtn(leftBtn);
        TextField firstLineEdit = new TextField();
        firstLineEdit.setMaxWidth(80);
        firstLineEdit.setText("0");
        controlDto.setFirstLineEdit(firstLineEdit);
        CommonUtil.numricTextField(0, 999999999, 0, firstLineEdit);
        Button rightBtn = new Button("▶");
        controlDto.setRightBtn(rightBtn);
        TextField rowCntEdit = new TextField();
        rowCntEdit.setMaxWidth(80);
        rowCntEdit.setText("1000");
        CommonUtil.numricTextField(1, 999999999, 1000, rowCntEdit);
        controlDto.setRowCntEdit(rowCntEdit);
        toolHBox.getChildren().addAll(exportSelectBtn, exportAllBtn, new Separator(Orientation.VERTICAL),
                addRowBtn, copyRowBtn, saveRowBtn, deleteRowBtn, cancelRowBtn, new Separator(Orientation.VERTICAL),
                hseparator,new Separator(Orientation.VERTICAL), filterBtn, refreshBtn, new Separator(Orientation.VERTICAL),
                new Label("第一行: "), leftBtn, firstLineEdit, rightBtn,
                new Separator(Orientation.VERTICAL), new Label("行数: "), rowCntEdit);

        refreshDataView(connectConrolMap.get(connectTab).getConnectInfo(), controlDto, tableName);
    }


    private void refreshDataView(ConnectInfoDto connectInfo, DataResultControlDto dataResultContorl, String tableName) {
        if (null == dataResultContorl || StringUtils.isBlank(tableName)) {
            return;
        }
        // 拼接SQL
        StringBuilder sql = new StringBuilder("select * from ");
        sql.append(tableName);
        if (StringUtils.isNotBlank(dataResultContorl.getFilterField().getText())) {
            // 过滤条件
            sql.append(" where ").append(dataResultContorl.getFilterField().getText());
        }
        // 分页限制
        sql.append(" limit ").append(dataResultContorl.getFirstLineEdit().getText()).append(", ").append(dataResultContorl.getRowCntEdit().getText());
        // 执行SQL
        List<Result<SqlValueDto>> vals = DataSourceProvider.querySQLVals(connectInfo, Collections.singletonList(sql.toString()));
        if (CollectionUtils.isEmpty(vals) || !vals.get(0).isSuccess() || null == vals.get(0).getData()) {
            return;
        }

        dataResultContorl.setOriginData(vals.get(0).getData());
        dataResultContorl.showData();
    }

    /**
     * 显示表DDL修改页面
     * @param connectTab 所属数据库连接页签
     * @param tableVO 修改的表，为空时代表新建表
     */
    private void addTableDDLTab(Tab connectTab, DatasouceTableVO tableVO) {
        // DDL页签
        Label ddlTabLabel = new Label(null == tableVO? "新建表" : tableVO.getTableName());
        Tab ddlTab = new Tab(null);
        ddlTabLabel.setGraphic(new ImageView(new Image(null == tableVO? "ceate_table.png" : "modify_table.png")));
        ddlTab.setGraphic(ddlTabLabel);
        ddlTab.setClosable(true);
        ConnectControlDto controlDto = connectConrolMap.get(connectTab);
        controlDto.getSqlTabPane().getTabs().add(controlDto.getSqlTabPane().getTabs().size() - 1, ddlTab);
        controlDto.getSqlTabPane().getSelectionModel().select(ddlTab);
        AnchorPane ddlPane = new AnchorPane();
        VBox ddlVBox = new VBox();
        ddlVBox.setPadding(new Insets(12, 3, 3, 3));
        ddlVBox.setSpacing(5);
        // 首行：表名+表注释
        HBox firstHBox = new HBox();
        firstHBox.setAlignment(Pos.CENTER_LEFT);
        firstHBox.setSpacing(5);
        firstHBox.setPadding(new Insets(3, 5, 3, 5));
        TextField tableNameEdit = new TextField();
        tableNameEdit.setPrefWidth(160);
        tableNameEdit.setText(null == tableVO? null : tableVO.getTableName());
        TextField tableCommentEdit = new TextField();
        tableCommentEdit.setPrefWidth(200);
        tableCommentEdit.setText(null == tableVO? null : tableVO.getComment());
        Separator sep1 = new Separator(Orientation.HORIZONTAL);
        sep1.setVisible(false);
        sep1.setPrefWidth(15);
        firstHBox.getChildren().addAll(new Label("表名: "), tableNameEdit, sep1, new Label("表注释: "), tableCommentEdit);
        // 第二行: 引擎+字符集
        HBox secondHBox = new HBox();
        secondHBox.setAlignment(Pos.CENTER_LEFT);
        secondHBox.setSpacing(5);
        secondHBox.setPadding(new Insets(3, 5, 3, 5));
        TextField tableEngineEdit = new TextField();
        tableEngineEdit.setPrefWidth(160);
        tableEngineEdit.setText("InnoDB");
        tableEngineEdit.setEditable(false);
        TextField tableCharsetEdit = new TextField();
        tableCharsetEdit.setPrefWidth(200);
        tableCharsetEdit.setEditable(false);
        tableCharsetEdit.setText("uft8mb4");
        Separator sep2 = new Separator(Orientation.HORIZONTAL);
        sep2.setVisible(false);
        sep2.setPrefWidth(15);
        secondHBox.getChildren().addAll(new Label("引擎: "), tableEngineEdit, sep2, new Label("字符集: "), tableCharsetEdit);
        // 表结构页签组件
        TabPane tableTabPane = new TabPane();
        VBox.setVgrow(tableTabPane, Priority.ALWAYS);
        tableTabPane.setPadding(new Insets(3, 5, 3, 5));
        TableDDLDto ddlDto = new TableDDLDto(tableVO);
        ddlDto.setDdlTab(ddlTab);
        ddlDto.setTableNameEdit(tableNameEdit);
        ddlDto.setTableCommentEdit(tableCommentEdit);
        ddlDto.setTableEngineEdit(tableEngineEdit);
        ddlDto.setTableCharsetEdit(tableCharsetEdit);
        ddlDto.setTableTabPane(tableTabPane);
        addTableDDLTabCtls(tableTabPane, tableVO, ddlDto);

        // 处理按钮HBox
        HBox opeHBox = new HBox();
        opeHBox.setAlignment(Pos.CENTER_RIGHT);
        opeHBox.setPadding(new Insets(3, 15, 3, 3));
        opeHBox.setSpacing(20);
        Button saveBtn = new Button("保存");
        saveBtn.setGraphic(new ImageView(new Image("ok.png")));
        Button cancelBtn = new Button("还原");
        cancelBtn.setGraphic(new ImageView(new Image("cancel.png")));
        opeHBox.getChildren().addAll(saveBtn, cancelBtn);

        ddlVBox.getChildren().addAll(firstHBox, secondHBox, tableTabPane, opeHBox);
        ddlPane.getChildren().add(ddlVBox);
        AnchorPane.setLeftAnchor(ddlVBox, 0.0);
        AnchorPane.setRightAnchor(ddlVBox, 0.0);
        AnchorPane.setTopAnchor(ddlVBox, 0.0);
        AnchorPane.setBottomAnchor(ddlVBox, 0.0);
        ddlTab.setContent(ddlPane);

        // 初始化视图
        ddlDto.setSaveBtn(saveBtn);
        ddlDto.setCancelBtn(cancelBtn);
        ddlDto.initalTableDDLView();
    }

    private void addTableDDLTabCtls(TabPane tableTabPane, DatasouceTableVO tableVO, TableDDLDto ddlDto) {
        // 列页签
        Tab columnTab = new Tab("列 ");
        columnTab.setClosable(false);
        columnTab.setGraphic(new ImageView(new Image("show_detail.png")));
        AnchorPane columnPane = new AnchorPane();
        VBox columnVBox = new VBox();
        columnVBox.setPadding(new Insets(3));
        columnVBox.setSpacing(5);
        // 工具栏
        HBox columnToolHBox = new HBox();
        columnToolHBox.setAlignment(Pos.CENTER_LEFT);
        columnToolHBox.setSpacing(5);
        columnToolHBox.setPadding(new Insets(1));
        Button addColumnBtn = new Button(null);
        addColumnBtn.setGraphic(new ImageView(new Image("add.png")));
        addColumnBtn.setTooltip(CommonUtil.toolTip("插入新列", 11, Duration.ONE));
        Button removeColumnBtn = new Button(null);
        removeColumnBtn.setGraphic(new ImageView(new Image("remove.png")));
        removeColumnBtn.setTooltip(CommonUtil.toolTip("删除选中列", 11, Duration.ONE));
        Button moveUpColumnBtn = new Button(null);
        moveUpColumnBtn.setGraphic(new ImageView(new Image("up.png")));
        moveUpColumnBtn.setTooltip(CommonUtil.toolTip("上移", 11, Duration.ONE));
        Button moveDownColumnBtn = new Button(null);
        moveDownColumnBtn.setGraphic(new ImageView(new Image("down.png")));
        moveDownColumnBtn.setTooltip(CommonUtil.toolTip("下移", 11, Duration.ONE));
        columnToolHBox.getChildren().addAll(addColumnBtn, removeColumnBtn, new Separator(Orientation.VERTICAL), moveUpColumnBtn, moveDownColumnBtn);
        // 字段列表
        SpreadsheetView columnView = new SpreadsheetView();
        columnView.setShowColHeaderMenu(false);
        columnView.setShowRowHeaderMenu(false);
        columnView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        columnView.setEditable(true);
        ddlDto.setAddColumnBtn(addColumnBtn);
        ddlDto.setRemoveColumnBtn(removeColumnBtn);
        ddlDto.setMoveUpColumnBtn(moveUpColumnBtn);
        ddlDto.setMoveDownColumnBtn(moveDownColumnBtn);
        ddlDto.setColumnView(columnView);

        VBox.setVgrow(columnView, Priority.ALWAYS);
        columnVBox.getChildren().addAll(columnToolHBox, columnView);
        columnPane.getChildren().add(columnVBox);
        AnchorPane.setLeftAnchor(columnVBox, 0.0);
        AnchorPane.setRightAnchor(columnVBox, 0.0);
        AnchorPane.setTopAnchor(columnVBox, 0.0);
        AnchorPane.setBottomAnchor(columnVBox, 0.0);
        columnTab.setContent(columnPane);

        Tab indexTab = new Tab("索引 ");
        indexTab.setClosable(false);
        indexTab.setGraphic(new ImageView(new Image("index.png")));
        AnchorPane indexPane = new AnchorPane();
        VBox indexVBox = new VBox();
        indexVBox.setPadding(new Insets(3));
        indexVBox.setSpacing(5);
        // 工具栏
        HBox indexToolHBox = new HBox();
        indexToolHBox.setAlignment(Pos.CENTER_LEFT);
        indexToolHBox.setSpacing(5);
        indexToolHBox.setPadding(new Insets(1));
        Button addIndexBtn = new Button(null);
        addIndexBtn.setGraphic(new ImageView(new Image("add.png")));
        addIndexBtn.setTooltip(CommonUtil.toolTip("插入新索引", 11, Duration.ONE));
        Button removeIndexBtn = new Button(null);
        removeIndexBtn.setGraphic(new ImageView(new Image("remove.png")));
        removeIndexBtn.setTooltip(CommonUtil.toolTip("删除选中索引", 11, Duration.ONE));
        indexToolHBox.getChildren().addAll(addIndexBtn, removeIndexBtn);
        // 索引列表
        SpreadsheetView indexView = new SpreadsheetView();
        indexView.setShowColHeaderMenu(false);
        indexView.setShowRowHeaderMenu(false);
        indexView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        indexView.setEditable(true);
        VBox.setVgrow(indexView, Priority.ALWAYS);
        indexVBox.getChildren().addAll(indexToolHBox, indexView);
        indexPane.getChildren().add(indexVBox);
        AnchorPane.setLeftAnchor(indexVBox, 0.0);
        AnchorPane.setRightAnchor(indexVBox, 0.0);
        AnchorPane.setTopAnchor(indexVBox, 0.0);
        AnchorPane.setBottomAnchor(indexVBox, 0.0);
        indexTab.setContent(indexPane);
        ddlDto.setAddIndexBtn(addIndexBtn);
        ddlDto.setRemoveIndexBtn(removeIndexBtn);
        ddlDto.setIndexView(indexView);

        Tab reviewTab = new Tab("SQL预览");
        reviewTab.setClosable(false);
        reviewTab.setGraphic(new ImageView(new Image("review.png")));
        tableTabPane.getTabs().addAll(columnTab, indexTab, reviewTab);
        AnchorPane reviewPanel = new AnchorPane();
        VBox reviewVBox = new VBox();
        reviewVBox.setPadding(new Insets(3));
        reviewVBox.setSpacing(5);
        WebView codeView = new WebView();
        ddlDto.setCodeView(codeView);
        assembleSqlCodeEditor(null, null, codeView, null, TableDDLDto.DEFAULT_CODE_TEXT, true);
        reviewVBox.getChildren().add(codeView);
        VBox.setVgrow(codeView, Priority.ALWAYS);
        reviewPanel.getChildren().add(reviewVBox);
        AnchorPane.setLeftAnchor(reviewVBox, 0.0);
        AnchorPane.setRightAnchor(reviewVBox, 0.0);
        AnchorPane.setTopAnchor(reviewVBox, 0.0);
        AnchorPane.setBottomAnchor(reviewVBox, 0.0);
        reviewTab.setContent(reviewPanel);

        ddlDto.setColumnTab(columnTab);
        ddlDto.setIndexTab(indexTab);
        ddlDto.setReviewTab(reviewTab);
    }

    private void assembleSqlCodeEditor(Tab connTab, Tab sqlTab, WebView codeView, ListView<DatasouceTableVO> tableInfoView, String sqlCode, boolean isReadOnly) {
        String htmlTemplate = null;
        try {
            InputStream in = HelloApplication.class.getResource("AutoComplateCodeEditor.html").openStream();
            byte[] buffer =new byte[1024*8];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(int len =0; (len =in.read(buffer)) >0;) {
                baos.write(buffer, 0, len);
            }
            htmlTemplate = baos.toString(StandardCharsets.UTF_8);
            baos.flush();
            baos.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StringUtils.isBlank(htmlTemplate)) {
            htmlTemplate = "<html><body><h2>加载异常</h2><body></html>";
        }

        StringBuilder tableNames = new StringBuilder("[");
        StringBuilder tableFiedls = new StringBuilder("[");
        if (null != tableInfoView) {
            for (int i = 0; i < tableInfoView.getItems().size(); i++) {
                DatasouceTableVO tb = tableInfoView.getItems().get(i);
                tableNames.append("\"").append(tb.getTableName()).append("\"");
                tableFiedls.append("[");
                for (int j = 0; j < tb.getColumns().size(); j++) {
                    String field = tb.getColumns().get(j);
                    tableFiedls.append("\"").append(field).append("\"");
                    if (j < tb.getColumns().size() - 1) {
                        tableFiedls.append(",");
                    }
                }
                tableFiedls.append("]");

                if (i < tableInfoView.getItems().size() - 1) {
                    tableNames.append(",");
                    tableFiedls.append(",");
                }
            }
        }
        tableNames.append("]");
        tableFiedls.append("]");

        String uid = "";
        if (!isReadOnly) {
            // SQL编辑器ID生成
            uid = UUID.randomUUID().toString().replace("-", "");
            // 维护SQL编辑器ID与SQL页签关系
            connectConrolMap.get(connTab).getSqlCodeIdMap().put(uid, sqlTab);
            sqlCodeId2ConnTabMap.put(uid, connTab);
        }

        String mimeType = "text/x-mysql";
        htmlTemplate = htmlTemplate.replace("${tableNames}", tableNames.toString());
        htmlTemplate = htmlTemplate.replace("${tableFields}", tableFiedls.toString());
        htmlTemplate = htmlTemplate.replace("${languageType}", mimeType);
        htmlTemplate = htmlTemplate.replace("${code}", CommonUtil.getValue(sqlCode, ""));
        htmlTemplate = htmlTemplate.replace("${readonlyflg}", isReadOnly? "true" : "false");
        htmlTemplate = htmlTemplate.replace("${uid}", uid);
        if (!isReadOnly) {
            codeView.getEngine().getLoadWorker().stateProperty().addListener(((observableValue, oldVal, newVal) -> {
                if (newVal == Worker.State.SUCCEEDED) {
                    // 增加监听代码变更事件
                    JSObject window = (JSObject)codeView.getEngine().executeScript("window");
                    window.setMember("codeChanged", sqlCodeChangeListener);
                }
            }));
        }

        codeView.getEngine().loadContent(htmlTemplate);
    }

    /**
     * 新建SQL查询编辑器
     * @param actionEvent
     */
    public void onNewSqlPage(ActionEvent actionEvent) {
        if (mainTabPanel.getTabs().size() <= 1) {
            MessageAlert.information("当前未打开数据库连接");
            return;
        }
        // 先查找当前选中的数据库连接
        Tab connectTab = mainTabPanel.getSelectionModel().getSelectedItem();
        if (null == connectTab || connectTab == newConnnectTab) {
            return;
        }
        addSqlTab(connectTab, "");
    }

    /**
     * 退出
     * @param actionEvent
     */
    public void onClose(ActionEvent actionEvent) {
        saveWindowPostion();
        Stage stage = (Stage) mainTabPanel.getScene().getWindow();
        stage.close();
    }

    /**
     * 执行SQL
     * @param actionEvent
     */
    public void onExecuteSQL(ActionEvent actionEvent) {
        executeSQL(false);
    }

    /**
     * 执行所有SQL
     * @param actionEvent
     */
    public void onExecuteAllSQL(ActionEvent actionEvent) {
        executeSQL(true);
    }

    /**
     * 执行SQL
     * @param isAll 是否执行所有
     */
    private void executeSQL(boolean isAll) {
        // 先查找当前选中的数据库连接
        Tab connectTab = mainTabPanel.getSelectionModel().getSelectedItem();
        if (null == connectTab || connectTab == newConnnectTab) {
            return;
        }

        // 查找选中的SQL页签
        Tab sqlTab = connectConrolMap.get(connectTab).getSqlTabPane().getSelectionModel().getSelectedItem();
        if (null == sqlTab) {
            return;
        }

        if (!connectConrolMap.get(connectTab).getSqlControlMap().containsKey(sqlTab)) {
            return;
        }

        // 获取SQL控件
        SqlControlDto sqlControl = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab);
        if (null == sqlControl || null == sqlControl.getSqlCodeView()) {
            return;
        }

        String text;
        if (isAll) {
            // 执行所有
            text = SqlEditorUtils.getAllSqlCode(sqlControl.getSqlCodeView());
        } else {
            // 执行选中
            text = SqlEditorUtils.getSelectSqlCode(sqlControl.getSqlCodeView());
            if (StringUtils.isBlank(text)
                    && BooleanUtils.isTrue(config.getAutoExecuteCursorSql())) {
                // 获取光标所在SQL
                text = getCursorSql(sqlControl.getSqlCodeView());
            }
        }
        // 去除SQL中的注释
        text = sqlRemoveComment(text);
        if (StringUtils.isBlank(text)) {
            if (isAll) {
                MessageAlert.information("请输入SQL");
                return;
            } else {
                if (BooleanUtils.isTrue(config.getAutoExecuteCursorSql())) {
                    MessageAlert.information("请移动光标到需要执行的SQL或选中需要执行的SQL");
                    return;
                } else {
                    MessageAlert.information("请选中需要执行的SQL");
                    return;
                }
            }
        }

        List<String> sqls = CommonUtil.split(text, ";");
        sqls.removeIf(sql -> {
            if (StringUtils.isNotBlank(sql)) {
                sql = sql.trim();
            }
            return StringUtils.isBlank(sql);
        });
        if (CollectionUtils.isEmpty(sqls)) {
            if (isAll) {
                MessageAlert.information("请输入SQL");
                return;
            } else {
                if (BooleanUtils.isTrue(config.getAutoExecuteCursorSql())) {
                    MessageAlert.information("请移动光标到需要执行的SQL或选中需要执行的SQL");
                    return;
                } else {
                    MessageAlert.information("请选中需要执行的SQL");
                    return;
                }
            }
        }

        for (int i = 0; i < sqls.size(); i++) {
            String sql = sqls.get(i).trim();
            sql = StringUtils.replace(sql, "\n", " ");
            // 去除连续空格
            sql = sql.replaceAll("\\s+", " ");
            sqls.set(i, sql);
        }

        Label sqlTabGraphic = (Label) sqlTab.getGraphic();
        sqlTabGraphic.setGraphic(new ImageView(new Image("progress.gif")));
        sqlControl.disable();
        updateStatus();

        // 多线程执行SQL
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    long start = System.currentTimeMillis();
                    List<Result<SqlValueDto>> vals = DataSourceProvider.executeSqls(connectConrolMap.get(connectTab).getConnectInfo(),
                            CommonUtil.datasTransfer(sqls, CommonUtil::sqlLimit));
                    long timeMill = System.currentTimeMillis() - start;
                    vals = assembleColumnComment(connectConrolMap.get(connectTab).getTableInfoView(), vals);
                    List<Result<SqlValueDto>> finalVals = vals;
                    Platform.runLater(() -> {
                        // 移除上一次结果标签页(注意此时不要移除明细页签)
                        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).removeAllResultTab();
                        // 创建结果页面
                        for (int i = 0; i < finalVals.size(); i++) {
                            createResultTab(connectTab, sqlTab, connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).getResultTabPane(),
                                    sqls.get(i), finalVals.get(i), i + 1, timeMill);
                        }
                        // 选中第一个结果页签
                        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).getResultTabPane().getSelectionModel().select(0);
                        if (BooleanUtils.isTrue(config.getFocusSqlEditorAfterExecute())) {
                            sqlControl.getSqlCodeView().requestFocus();
                        }
                        // 详情信息设置
                        sqlControl.getLogArea().setText(null);
                        StringBuilder log = new StringBuilder();
                        log.append(String.format(" %d 个SQL已执行, 其中 %d 个执行成功,  %d 个执行失败", finalVals.size(),
                                finalVals.stream().filter(Result::isSuccess).toList().size(),
                                finalVals.stream().filter(val -> !val.isSuccess()).toList().size()));
                        log.append("\n\n");
                        for (int i = 0; i < finalVals.size(); i++) {
                            String sql = sqls.get(i);
                            sql = CommonUtil.sqlLimit(sql);
                            log.append(String.format("SQL %d : %s", i+1, sql.length() > 150? StringUtils.left(sql, 150) + "..." : sql)).append("\n\n");
                            Result<SqlValueDto> result = finalVals.get(i);
                            if (result.isSuccess()) {
                                switch (result.getData().getType()) {
                                    case EXECUTE:
                                        log.append(String.format("共 %d 行受到影响\n", CommonUtil.getValue(result.getData().getEffectRow(), 0)));
                                        break;
                                    case QUERY:
                                        log.append(String.format("返回了 %d 行\n", CommonUtil.getValue(result.getData().getVals().size(), 0)));
                                        break;
                                }
                                log.append("\n");
                                // 耗时信息
                                if (null != result.getData().getExecuteMill()) {
                                    log.append(String.format("执行耗时   : %d ms\n", result.getData().getExecuteMill()));
                                }
                            } else {
                                log.append(String.format("错误信息: %s", CommonUtil.getValue(result.getMessage(), result.getInfo()))).append("\n");
                            }
                            log.append("--------------------------------------------------\n\n");
                        }

                        log.append(String.format("总计耗时   : %d ms\n\n", timeMill));
                        sqlControl.getLogArea().setText(log.toString());
                    });
                } catch (Exception e) {
                    MessageAlert.error(e.getMessage());
                } finally {
                    Platform.runLater(() -> {
                        sqlTabGraphic.setGraphic(new ImageView(new Image("sql.png")));
                        sqlControl.enable();
                        updateStatus();
                        if (BooleanUtils.isTrue(config.getFocusSqlEditorAfterExecute())) {
                            sqlControl.getSqlCodeView().requestFocus();
                        }
                    });
                }

                return null;
            }
        };
        TaskUtils.submit(task);
    }

    /**
     * 根据返回结果组合查询列注释
     * @param tableInfoView
     * @param vals
     * @return
     */
    private List<Result<SqlValueDto>> assembleColumnComment(ListView<DatasouceTableVO> tableInfoView, List<Result<SqlValueDto>> vals) {
        if (CollectionUtils.isEmpty(tableInfoView.getItems())) {
            return vals;
        }
        if (CollectionUtils.isEmpty(vals)) {
            return vals;
        }
        for (Result<SqlValueDto> val : vals) {
            if (null == val || !val.isSuccess() || null == val.getData() || val.getData().getType() != SQLType.QUERY) {
                continue;
            }
            SqlValueDto dto = val.getData();
            if (CollectionUtils.isEmpty(dto.getColumnBelongTables())) {
                break;
            }
            List<String> comments = new ArrayList<>();
            Map<String, DatasouceTableVO> tableVOMap = new HashMap<>();
            for (int i = 0; i < dto.getColumnLabels().size(); i++) {
                // 列名
                String columnLabel = dto.getColumnLabels().get(i);
                // 表名
                String tableName = dto.getColumnBelongTables().size() > i? dto.getColumnBelongTables().get(i) : null;
                if (StringUtils.isBlank(tableName)) {
                    comments.add("");
                } else {
                    String comment = null;
                    DatasouceTableVO tableVO = CommonUtil.getLocalCacheVal(tableName, tableVOMap,
                            tn -> tableInfoView.getItems().stream().filter(item -> StringUtils.equalsIgnoreCase(item.getTableName(), tableName)).findAny().orElse(null));
                    if (null != tableVO) {
                        if (CollectionUtils.isNotEmpty(tableVO.getColumns()) && CollectionUtils.isNotEmpty(tableVO.getComments())) {
                            for (int j = 0; j < tableVO.getColumns().size(); j++) {
                                if (StringUtils.equalsIgnoreCase(tableVO.getColumns().get(j), columnLabel)) {
                                    comment = tableVO.getComments().size() > j? tableVO.getComments().get(j) : null;
                                    break;
                                }
                            }
                        }
                    }
                    comments.add(CommonUtil.getValue(comment, ""));
                }
            }
            dto.setColumnComments(comments);
        }
        return vals;
    }

    /**
     * 获取光标所在位置SQL语句
     * @param sqlCodeView
     * @return
     */
    private String getCursorSql(WebView sqlCodeView) {
        // 获取当前光标所在位置(行及列)
        Pair<Integer, Integer> cursor = SqlEditorUtils.getCursor(sqlCodeView, false);
        int line = cursor.getKey();
        int pos = cursor.getValue();
        // 获取SQL编辑器所有脚本
        String sql = SqlEditorUtils.getAllSqlCode(sqlCodeView);
        if (StringUtils.isBlank(sql)) {
            return null;
        }
        // 去除注释
        sql = sqlRemoveComment(sql);
        // SQL按行拆分
        List<String> sqls = CommonUtil.split(sql, "\n");
        String prev = "";
        String suff = "";
        for (int i=line; i>=0; i--) {
            // 逐行往前搜索
            String ss;
            if (i == line) {
                ss = StringUtils.left(sqls.get(i), pos);
            } else {
                ss = sqls.get(i) + "\n";
            }
            if (!StringUtils.contains(ss, ";")) {
                prev = ss + prev;
                continue;
            } else {
                int p = StringUtils.lastIndexOf(ss, ";");
                prev = StringUtils.substring(ss, p+1) + prev;
                break;
            }
        }
        for (int i=line; i<sqls.size(); i++) {
            // 向后逐行搜索
            String ss;
            if (i == line) {
                ss = StringUtils.substring(sqls.get(i), pos) + "\n";
            } else {
                ss = sqls.get(i) + "\n";
            }
            if (!StringUtils.contains(ss, ";")) {
                suff = suff + ss;
                continue;
            } else {
                int p = StringUtils.indexOf(ss, ";");
                suff = suff + StringUtils.left(ss, p);
                break;
            }
        }
        return prev + suff;
    }

    /**
     * 去除SQL中的注释
     * @param sql
     * @return
     */
    private String sqlRemoveComment(String sql) {
        if (StringUtils.isBlank(sql)) {
            return sql;
        }
        // 匹配及删除多行注释
        while (true) {
            // 查找多行注释起始位置
            int start = StringUtils.indexOf(sql, "/*");
            if (start < 0) {
                break;
            }
            String temp = StringUtils.substring(sql, start);
            // 查找多行注释结束位置
            int end = StringUtils.indexOf(temp, "*/");
            if (end < 0) {
                break;
            }

            String comment = StringUtils.left(temp, end + 2);
            StringBuilder cc = new StringBuilder();
            for (int i = 0; i < comment.length(); i++) {
                if (comment.charAt(i) == '\n') {
                    cc.append("\n");
                } else {
                    cc.append(" ");
                }
            }

            // 多行注释替换
            sql = StringUtils.left(sql, start) + cc + StringUtils.substring(temp, end + 2);
        }

        StringBuilder sb = new StringBuilder();
        // 去除单行注释
        for (String line : CommonUtil.split(sql, "\n")) {
            if (StringUtils.isBlank(line) || StringUtils.indexOfAny(line, "#", "-- ") < 0) {
                // 空行或不包含注释
                sb.append(line).append("\n");
                continue;
            }

            int pos = StringUtils.indexOfAny(line, "#", "-- ");
            String comment = StringUtils.substring(line, pos);
            StringBuilder cc = new StringBuilder();
            for (int i = 0; i < comment.length(); i++) {
                if (comment.charAt(i) == '\n') {
                    cc.append("\n");
                } else {
                    cc.append(" ");
                }
            }
            // 剔除注释
            sb.append(StringUtils.left(line, pos)).append(cc).append("\n");
        }

        return sb.toString();
    }

    /**
     * 创建SQL结果页面
     * @param resultTabPane
     * @param sql
     * @param result
     * @param index
     */
    private void createResultTab(Tab connectTab, Tab sqlTab, TabPane resultTabPane, String sql, Result<SqlValueDto> result, int index, long timeMill) {
        Tab tab = new Tab("结果 " + index);
        ExecLogDto logDto = new ExecLogDto("执行成功", timeMill, null);
        if (result.isSuccess() && null != result.getData()) {
            SqlValueDto dto = result.getData();
            if (null == dto.getType() || SQLType.QUERY == dto.getType()) {
                // 查询类型
                tab.setGraphic(new ImageView(new Image("result.png")));
                // 执行成功
                AnchorPane pane = new AnchorPane();
                VBox vBox = new VBox();
                vBox.setPadding(new Insets(3));
                vBox.setSpacing(1);
                // 结果工具栏
                HBox toolHBox = new HBox();
                toolHBox.setPadding(new Insets(1, 5, 1, 3));
                toolHBox.setSpacing(3);
                toolHBox.setAlignment(Pos.CENTER_LEFT);
                // 构建结果视图实体
                ResultViewCtlDto rstViewDto = new ResultViewCtlDto();
                rstViewDto.setConnectTab(connectTab);
                rstViewDto.setSqlTab(sqlTab);
                rstViewDto.setResultTab(tab);
                rstViewDto.setSql(sql);
                // 结果列表
                SpreadsheetView resultView = new SpreadsheetView();
                resultView.setShowColHeaderMenu(false);
                resultView.setShowRowHeaderMenu(false);
                resultView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                resultView.setEditable(false);
                resultView.setPlaceholder(new Label(""));
                rstViewDto.setTableView(resultView);
                // HTML结果视图
                WebView resultHtmlView = new WebView();
                rstViewDto.setHtmlView(resultHtmlView);
                // 行视图
                VBox rowVBox = new VBox();
                SpreadsheetView resultRowView = new SpreadsheetView();
                resultRowView.setShowColHeaderMenu(false);
                resultRowView.setShowRowHeaderMenu(false);
                resultRowView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                resultRowView.setEditable(false);
                resultRowView.setPlaceholder(new Label(""));
                rstViewDto.setRowView(resultRowView);
                HBox rowHBox = new HBox();
                rowHBox.setPadding(new Insets(1, 5, 1, 3));
                rowHBox.setSpacing(3);
                rowHBox.setAlignment(Pos.CENTER_LEFT);
                Button startBtn = new Button(null);
                startBtn.setGraphic(new ImageView(new Image("start.png")));
                startBtn.setTooltip(CommonUtil.toolTip("第一行", 10, Duration.ONE));
                Button preBtn = new Button(null);
                preBtn.setGraphic(new ImageView(new Image("left.png")));
                preBtn.setTooltip(CommonUtil.toolTip("上一行", 10, Duration.ONE));
                Button nextBtn = new Button(null);
                nextBtn.setGraphic(new ImageView(new Image("right.png")));
                nextBtn.setTooltip(CommonUtil.toolTip("下一行", 10, Duration.ONE));
                Button endBtn = new Button(null);
                endBtn.setGraphic(new ImageView(new Image("end.png")));
                endBtn.setTooltip(CommonUtil.toolTip("最后行", 10, Duration.ONE));
                TextField lineEdit = new TextField();
                lineEdit.setMaxWidth(60);
                rowHBox.getChildren().addAll(startBtn, preBtn, new Separator(Orientation.VERTICAL), nextBtn, endBtn,
                        new Separator(Orientation.HORIZONTAL), new Label("当前第:"), lineEdit, new Label("行"));
                rowVBox.getChildren().addAll(resultRowView, rowHBox);
                VBox.setVgrow(resultRowView, Priority.ALWAYS);
                RowResultControlDto rowControlDto = new RowResultControlDto(rowVBox, resultRowView, startBtn, preBtn, nextBtn, endBtn, lineEdit);
                rstViewDto.setRowControlDto(rowControlDto);
                // SQL文本框
                HBox hBox = new HBox();
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.setPadding(new Insets(3));
                hBox.setSpacing(5);
                Label sqlLable = new Label("SQL:");
                TextField sqlArea = new TextField(CommonUtil.sqlLimit(sql));
                sqlArea.setEditable(false);
                rstViewDto.setSqlArea(sqlArea);
                hBox.getChildren().addAll(sqlLable, sqlArea);
                HBox.setHgrow(sqlArea, Priority.ALWAYS);
                vBox.getChildren().addAll(toolHBox, resultView, resultHtmlView, rowVBox, hBox);
                VBox.setVgrow(resultView, Priority.ALWAYS);
                VBox.setVgrow(resultHtmlView, Priority.ALWAYS);
                VBox.setVgrow(rowVBox, Priority.ALWAYS);
                pane.getChildren().add(vBox);
                AnchorPane.setLeftAnchor(vBox, 0.0);
                AnchorPane.setRightAnchor(vBox, 0.0);
                AnchorPane.setTopAnchor(vBox, 0.0);
                AnchorPane.setBottomAnchor(vBox, 0.0);
                tab.setContent(pane);
                showResultView(rstViewDto, dto);
                logDto.setRecordCnt(dto.getVals().size());

                // 处理结果视图事件
                handlerResultView(rstViewDto, toolHBox, dto);
            } else {
                // 执行类型
                tab.setGraphic(new ImageView(new Image("result_execute.png")));
                AnchorPane pane = new AnchorPane();
                VBox vBox = new VBox();
                vBox.setPadding(new Insets(3));
                WebView resultView = new WebView();
                HBox hBox = new HBox();
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.setPadding(new Insets(3));
                hBox.setSpacing(5);
                Label sqlLable = new Label("SQL:");
                TextField sqlArea = new TextField(CommonUtil.sqlLimit(sql));
                sqlArea.setEditable(false);
                hBox.getChildren().addAll(sqlLable, sqlArea);
                HBox.setHgrow(sqlArea, Priority.ALWAYS);
                vBox.getChildren().addAll(resultView, hBox);
                VBox.setVgrow(resultView, Priority.ALWAYS);
                pane.getChildren().add(vBox);
                AnchorPane.setLeftAnchor(vBox, 0.0);
                AnchorPane.setRightAnchor(vBox, 0.0);
                AnchorPane.setTopAnchor(vBox, 0.0);
                AnchorPane.setBottomAnchor(vBox, 0.0);
                tab.setContent(pane);
                List<List<String>> sqlRsts = new ArrayList<>();
                sqlRsts.add(Arrays.asList("SQL:", sql));
                sqlRsts.add(Arrays.asList("effect rows:", null == dto.getEffectRow()? "0" : String.valueOf(dto.getEffectRow())));
                sqlRsts.add(Arrays.asList("execute times(ms):", null == dto.getExecuteMill()? "0" : String.valueOf(dto.getExecuteMill())));
                String view = HtmlTableUtil.htmlTable("SQL执行成功", false, Collections.emptyList(), sqlRsts, "脚本执行结果", false);

                resultView.getEngine().loadContent(view);
            }
        } else {
            tab.setGraphic(new ImageView(new Image("result_err.png")));
            // 执行失败
            AnchorPane pane = new AnchorPane();
            VBox vBox = new VBox();
            vBox.setPadding(new Insets(3));
            WebView resultView = new WebView();
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_LEFT);
            hBox.setPadding(new Insets(3));
            hBox.setSpacing(5);
            Label sqlLable = new Label("SQL:");
            TextField sqlArea = new TextField(CommonUtil.sqlLimit(sql));
            sqlArea.setEditable(false);
            hBox.getChildren().addAll(sqlLable, sqlArea);
            HBox.setHgrow(sqlArea, Priority.ALWAYS);
            vBox.getChildren().addAll(resultView, hBox);
            VBox.setVgrow(resultView, Priority.ALWAYS);
            pane.getChildren().add(vBox);
            AnchorPane.setLeftAnchor(vBox, 0.0);
            AnchorPane.setRightAnchor(vBox, 0.0);
            AnchorPane.setTopAnchor(vBox, 0.0);
            AnchorPane.setBottomAnchor(vBox, 0.0);
            tab.setContent(pane);

            List<List<String>> sqlRsts = new ArrayList<>();
            sqlRsts.add(Arrays.asList("SQL:", sql));
            sqlRsts.add(Arrays.asList("Error Message:", "<p style=\"color: red;\">" + result.getMessage() + "</p>"));
            String view = HtmlTableUtil.htmlTable("<p style=\"color: red;\">SQL执行失败</p>", false, Collections.emptyList(), sqlRsts, "脚本执行结果", false);

            resultView.getEngine().loadContent(view);
        }
        resultTabPane.getTabs().add(resultTabPane.getTabs().size() - 2, tab);
        connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab).getExecLogMap().put(tab, logDto);
    }

    private void handlerResultView(ResultViewCtlDto rstViewDto, HBox toolHBox, SqlValueDto dto) {
        Button exportSelectBtn = new Button(null);
        exportSelectBtn.setGraphic(new ImageView(new Image("export_select.png")));
        exportSelectBtn.setTooltip(CommonUtil.toolTip("导出选中行", 12, Duration.ONE));
        rstViewDto.setExportSelectBtn(exportSelectBtn);
        Button exportAllBtn = new Button(null);
        exportAllBtn.setGraphic(new ImageView(new Image("export_all.png")));
        exportAllBtn.setTooltip(CommonUtil.toolTip("导出所有数据", 12, Duration.ONE));
        rstViewDto.setExportAllBtn(exportAllBtn);
        ToggleGroup displayGrp = new ToggleGroup();
        ToggleButton tableViewBtn = new ToggleButton(null);
        tableViewBtn.setGraphic(new ImageView(new Image("table_view.png")));
        tableViewBtn.setTooltip(CommonUtil.toolTip("网格视图", 12, Duration.ONE));
        ToggleButton htmlViewBtn = new ToggleButton(null);
        htmlViewBtn.setGraphic(new ImageView(new Image("html_view.png")));
        htmlViewBtn.setTooltip(CommonUtil.toolTip("HTML视图", 12, Duration.ONE));
        ToggleButton formViewBtn = new ToggleButton(null);
        formViewBtn.setGraphic(new ImageView(new Image("form_view.png")));
        formViewBtn.setTooltip(CommonUtil.toolTip("行视图", 12, Duration.ONE));
        displayGrp.getToggles().addAll(tableViewBtn, htmlViewBtn, formViewBtn);
        rstViewDto.setTableViewBtn(tableViewBtn);
        rstViewDto.setHtmlViewBtn(htmlViewBtn);
        rstViewDto.setRowViewBtn(formViewBtn);
        displayGrp.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (null == newValue) {
                tableViewBtn.setSelected(true);
                return;
            }
            ResultViewType typ = ResultViewType.TABLE;
            exportSelectBtn.setDisable(true);
            if (newValue.equals(tableViewBtn)) {
                exportSelectBtn.setDisable(false);
            } else if (htmlViewBtn.equals(newValue)) {
                typ = ResultViewType.HTML;
            } else if (formViewBtn.equals(newValue)) {

                if (CollectionUtils.isNotEmpty(rstViewDto.getTableView().getSelectionModel().getSelectedCells())) {
                    rstViewDto.getRowControlDto().setSelectIdx(rstViewDto.getTableView().getSelectionModel().getSelectedCells().get(0).getRow());
                }
                typ = ResultViewType.ROW;
            }
            connectConrolMap.get(rstViewDto.getConnectTab()).getSqlControlMap().get(rstViewDto.getSqlTab()).getResultViewTypeMap().put(rstViewDto.getResultTab(), typ);
            showResultView(rstViewDto, dto);
        });
        tableViewBtn.setSelected(true);

        Separator hseparator = new Separator();
        hseparator.setOrientation(Orientation.HORIZONTAL);
        HBox.setHgrow(hseparator, Priority.ALWAYS);
        hseparator.setVisible(false);
        Button refreshBtn = new Button(null);
        refreshBtn.setGraphic(new ImageView(new Image("refresh.png")));
        refreshBtn.setTooltip(CommonUtil.toolTip("刷新", 12, Duration.ONE));
        Button leftBtn = new Button("◀");
        leftBtn.setDisable(true);
        rstViewDto.setLeftBtn(leftBtn);
        TextField firstLineEdit = new TextField();
        firstLineEdit.setMaxWidth(80);
        firstLineEdit.setText("0");
        CommonUtil.numricTextField(0, 999999999, 0, firstLineEdit);
        rstViewDto.setFirstLineEdit(firstLineEdit);
        Button rightBtn = new Button("▶");
        rstViewDto.setRightBtn(rightBtn);
        TextField rowCntEdit = new TextField();
        rowCntEdit.setMaxWidth(80);
        rowCntEdit.setText("1000");
        rstViewDto.setRowCntEdit(rowCntEdit);
        CommonUtil.numricTextField(1, 999999999, 1000, rowCntEdit);
        toolHBox.getChildren().addAll(exportSelectBtn, exportAllBtn, new Separator(Orientation.VERTICAL), tableViewBtn, formViewBtn, htmlViewBtn,
                hseparator, refreshBtn, new Separator(Orientation.VERTICAL),
                new Label("第一行: "), leftBtn, firstLineEdit, rightBtn, new Separator(Orientation.VERTICAL), new Label("行数: "), rowCntEdit);
        if (CommonUtil.sqlHasLimit(rstViewDto.getSql()) || dto.getVals().size() < 1000) {
            // SQL本身带了limit限制或SQL本身不带limit限制但数据量小于1000条，则禁用翻页控件
            CommonUtil.disableControls(leftBtn, firstLineEdit, rightBtn, rowCntEdit);
        }
        // 工具栏按钮事件绑定
        CommonUtil.buttonBind(exportSelectBtn, () -> {
            ObservableList<TablePosition> selectedCells = rstViewDto.getTableView().getSelectionModel().getSelectedCells();
            List<Integer> rows = new ArrayList<>();
            for (TablePosition cell : selectedCells) {
                int row = cell.getRow();
                if (!rows.contains(row)) {
                    rows.add(row);
                }
            }
            exportResults(dto, false, rows, rstViewDto.getTableView());
        });
        CommonUtil.buttonBind(exportAllBtn, () -> exportResults(dto, true, null, rstViewDto.getTableView()));
        CommonUtil.buttonBind(refreshBtn, () -> {
            Integer firstLine = Integer.valueOf(firstLineEdit.getText());
            if (firstLine > 0) {
                leftBtn.setDisable(false);
            }
            refreshResult(rstViewDto, dto);
        });
        CommonUtil.textFieldSubmit(firstLineEdit, () -> {
            Integer firstLine = Integer.valueOf(firstLineEdit.getText());
            if (firstLine > 0) {
                leftBtn.setDisable(false);
            }
            refreshResult(rstViewDto, dto);
        });
        CommonUtil.textFieldSubmit(rowCntEdit, () -> refreshResult(rstViewDto, dto));
        CommonUtil.buttonBind(leftBtn, () -> {
            Integer firstLine = Integer.valueOf(firstLineEdit.getText());
            if (firstLine <= 0) {
                leftBtn.setDisable(true);
                return;
            }
            Integer rowCnt = Integer.valueOf(rowCntEdit.getText());
            firstLine = firstLine - rowCnt;
            if (firstLine <= 0) {
                leftBtn.setDisable(true);
                firstLine = 0;
            }
            firstLineEdit.setText(String.valueOf(firstLine));
            refreshResult(rstViewDto, dto);
        });
        CommonUtil.buttonBind(rightBtn, () -> {
            Integer firstLine = Integer.valueOf(firstLineEdit.getText());
            if (firstLine <= 0) {
                firstLine = 0;
            }
            Integer rowCnt = Integer.valueOf(rowCntEdit.getText());
            firstLine = firstLine + rowCnt;
            if (firstLine > 0) {
                leftBtn.setDisable(false);
            }
            firstLineEdit.setText(String.valueOf(firstLine));
            refreshResult(rstViewDto, dto);
        });
    }

    private void refreshResult(ResultViewCtlDto rstViewDto, SqlValueDto dto) {
        String sql = rstViewDto.getSql();
        boolean hasLimit = CommonUtil.sqlHasLimit(sql);
        if (!hasLimit && StringUtils.startsWithIgnoreCase(sql, "select")) {
            // 拼接limit限制语句
            sql = sql + " limit " + rstViewDto.getFirstLineEdit().getText() + ", " + rstViewDto.getRowCntEdit().getText();
            // 更新SQL语句
            rstViewDto.getSqlArea().setText(sql);
        }

        ConnectInfoDto connectInfo = connectConrolMap.get(rstViewDto.getConnectTab()).getConnectInfo();
        SqlControlDto sqlControl = connectConrolMap.get(rstViewDto.getConnectTab()).getSqlControlMap().get(rstViewDto.getSqlTab());

        // 重置选择项
        rstViewDto.getTableView().getSelectionModel().clearSelection();
        rstViewDto.getRowControlDto().setSelectIdx(0);

        Label sqlTabGraphic = (Label) rstViewDto.getSqlTab().getGraphic();
        sqlTabGraphic.setGraphic(new ImageView(new Image("progress.gif")));
        sqlControl.disable();
        // 多线程执行SQL
        String finalSql = sql;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<Result<SqlValueDto>> vals = DataSourceProvider.querySQLVals(connectInfo, Collections.singletonList(finalSql));
                    if (CollectionUtils.isEmpty(vals)) {
                        return null;
                    }
                    // 组合列注释
                    vals = assembleColumnComment(connectConrolMap.get(rstViewDto.getConnectTab()).getTableInfoView(), vals);

                    List<Result<SqlValueDto>> finalVals = vals;
                    Platform.runLater(() -> {
                        if (!finalVals.get(0).isSuccess()) {
                            MessageAlert.error(finalVals.get(0).getMessage());
                        } else {
                            BeanUtil.copyProperties(finalVals.get(0).getData(), dto);
                            showResultView(rstViewDto, dto);
                            Tab resultTab = sqlControl.getResultTabPane().getSelectionModel().getSelectedItem();
                            int rowCntLimit = Integer.parseInt(rstViewDto.getRowCntEdit().getText());
                            if (dto.getVals().size() < rowCntLimit) {
                                rstViewDto.getRightBtn().setDisable(true);
                            } else if (!hasLimit) {
                                rstViewDto.getRightBtn().setDisable(false);
                            }
                            ExecLogDto execLogDto = sqlControl.getExecLogMap().get(resultTab);
                            if (null != execLogDto) {
                                execLogDto.setRecordCnt(dto.getVals().size());
                            }
                        }
                    });
                } catch (Exception e) {
                    MessageAlert.error(e.getMessage());
                } finally {
                    Platform.runLater(() -> {
                        sqlTabGraphic.setGraphic(new ImageView(new Image("sql.png")));
                        sqlControl.enable();
                        rstViewDto.getTableView().requestFocus();
                        updateStatus();
                    });
                }

                return null;
            }
        };
        TaskUtils.submit(task);
    }

    private void exportResults(SqlValueDto dto, boolean exportAll, List<Integer> idxs, Node parentNode) {
        if (null == dto) {
            return;
        }
        if (!exportAll && CollectionUtils.isEmpty(idxs)) {
            MessageAlert.information("请选中需要导出的数据行");
            return;
        }
        if (CollectionUtils.isEmpty(dto.getVals())) {
            MessageAlert.information("导出数据为空");
            return;
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("export-select-view.fxml"));
            Parent root = fxmlLoader.load();
            ExportSelectViewController controller = fxmlLoader.getController();
            controller.init(dto.getColumnLabels());
            controller.setExportHandler((type, fields) -> {
                if (null == type || CollectionUtils.isEmpty(fields)) {
                    return;
                }
                switch (type) {
                    case EXCEL -> exportExcelResult(dto, fields, exportAll, idxs, parentNode);
                    case CSV -> exportCSVResult(dto, fields, exportAll, idxs, parentNode);
                    case HTML -> exportHTMLResult(dto, fields, exportAll, idxs, parentNode);
                    case SQL -> exportSQLResult(dto, fields, exportAll, idxs, parentNode);
                }
            });
            Stage stage = new Stage();
            stage.setTitle("导出");
            Scene scene = new Scene(root, 357, 405);
            stage.setScene(scene);
            Stage parent = (Stage) parentNode.getScene().getWindow();
            if (exportAll) {
                stage.getIcons().add(new Image("export_all.png"));
            } else {
                stage.getIcons().add(new Image("export_select.png"));
            }
            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error("系统异常");
        }
    }

    private void exportExcelResult(SqlValueDto dto, Set<String> fields, boolean exportAll, List<Integer> idxs, Node parentNode) {
        FileChooser fc = new FileChooser();
        if (StringUtils.isBlank(exportPath)) {
            fc.setInitialDirectory(new File("."));
        } else {
            fc.setInitialDirectory(new File(exportPath));
        }
        fc.setTitle("导出");
        fc.setInitialFileName("data-" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel File", "*.xlsx"));

        File file = fc.showSaveDialog(parentNode.getScene().getWindow());
        if (null == file) {
            return;
        }
        exportPath = file.getParentFile().getAbsolutePath();

        // 表头
        List<List<String>> headers = new ArrayList<>();
        for (String columnLabel : dto.getColumnLabels()) {
            if (!fields.contains(columnLabel)) {
                continue;
            }
            headers.add(List.of(columnLabel));
        }
        ErrorDto errorDto = new ErrorDto();
        final int[] idx = {0};
        boolean isSuccess = ExportUtil.exportExcel(file, "data", headers, null, errorDto, () -> {
            if (idx[0] == 0) {
                idx[0]++;
                List<List<Object>> datas = new ArrayList<>();
                for (int i = 0; i < dto.getVals().size(); i++) {
                    if (!exportAll && !idxs.contains(i)) {
                        continue;
                    }
                    List<Object> dds = new ArrayList<>();
                    for (int j = 0; j < dto.getVals().get(i).size(); j++) {
                        String columnLabel = dto.getColumnLabels().get(j);
                        if (!fields.contains(columnLabel)) {
                            continue;
                        }
                        Object val = dto.getVals().get(i).get(j);
                        if ((val instanceof Timestamp)) {
                            val = new Date(((Timestamp)val).getTime());
                        }
                        dds.add(val);
                    }
                    datas.add(dds);
                }
                return datas;
            } else {
                return Collections.emptyList();
            }
        });
        if (!isSuccess) {
            MessageAlert.error(String.format("导出失败: %s", errorDto.getErrorMessage()));
            return;
        }

        MessageAlert.information("导出成功");
    }

    private void exportCSVResult(SqlValueDto dto, Set<String> fields, boolean exportAll, List<Integer> idxs, Node parentNode) {
        FileChooser fc = new FileChooser();
        if (StringUtils.isBlank(exportPath)) {
            fc.setInitialDirectory(new File("."));
        } else {
            fc.setInitialDirectory(new File(exportPath));
        }
        fc.setTitle("导出");
        fc.setInitialFileName("data-" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File", "*.csv"));

        File file = fc.showSaveDialog(parentNode.getScene().getWindow());
        if (null == file) {
            return;
        }
        exportPath = file.getParentFile().getAbsolutePath();

        // 组装数据
        StringBuilder val = new StringBuilder();
        // 标题
        List<String> headers = new ArrayList<>();
        for (String columnLabel : dto.getColumnLabels()) {
            if (!fields.contains(columnLabel)) {
                continue;
            }
            headers.add(columnLabel);
        }
        val.append(CommonUtil.join(headers, ",")).append("\n");
        for (int i = 0; i < dto.getVals().size(); i++) {
            if (!exportAll && !idxs.contains(i)) {
                continue;
            }
            List<String> strs = new ArrayList<>();
            for (int j = 0; j < dto.getVals().get(i).size(); j++) {
                String columnLabel = dto.getColumnLabels().get(j);
                if (!fields.contains(columnLabel)) {
                    continue;
                }
                Object obj = dto.getVals().get(i).get(j);
                if (null == obj) {
                    strs.add("");
                } else {
                    strs.add(CommonUtil.toString(obj));
                }
            }
            val.append(CommonUtil.join(strs, ","));
            if (i < dto.getVals().size() - 1) {
                val.append("\n");
            }
        }

        // 数据写入
        try{
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] b = val.toString().getBytes(StandardCharsets.UTF_8);
            out.write(b,0,b.length);
            out.flush();
            out.close();
        }catch(IOException ex){
            MessageAlert.error("导出失败: 写入数据发生异常");
            return;
        }

        MessageAlert.information("导出成功");
    }

    private void exportHTMLResult(SqlValueDto dto, Set<String> fields, boolean exportAll, List<Integer> idxs, Node parentNode) {
        FileChooser fc = new FileChooser();
        if (StringUtils.isBlank(exportPath)) {
            fc.setInitialDirectory(new File("."));
        } else {
            fc.setInitialDirectory(new File(exportPath));
        }
        fc.setTitle("导出");
        fc.setInitialFileName("data-" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + ".html");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML File", "*.html"));

        File file = fc.showSaveDialog(parentNode.getScene().getWindow());
        if (null == file) {
            return;
        }
        exportPath = file.getParentFile().getAbsolutePath();

        // 数据写入
        try{
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] b = assembleHtmlResult(dto, false, fields, exportAll, idxs).getBytes(StandardCharsets.UTF_8);
            out.write(b,0,b.length);
            out.flush();
            out.close();
        }catch(IOException ex){
            MessageAlert.error("导出失败: 写入数据发生异常");
            return;
        }

        MessageAlert.information("导出成功");
    }

    private void exportSQLResult(SqlValueDto dto, Set<String> fields, boolean exportAll, List<Integer> idxs, Node parentNode) {
        FileChooser fc = new FileChooser();
        if (StringUtils.isBlank(exportPath)) {
            fc.setInitialDirectory(new File("."));
        } else {
            fc.setInitialDirectory(new File(exportPath));
        }
        fc.setTitle("导出");
        fc.setInitialFileName("data-" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + ".sql");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL File", "*.sql"));

        File file = fc.showSaveDialog(parentNode.getScene().getWindow());
        if (null == file) {
            return;
        }
        exportPath = file.getParentFile().getAbsolutePath();

        // 标题
        Set<String> tables = new HashSet<>();
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < dto.getColumnLabels().size(); i++) {
            String columnLabel = dto.getColumnLabels().get(i);
            if (!fields.contains(columnLabel)) {
                continue;
            }
            headers.add(columnLabel);
            if (CollectionUtils.isNotEmpty(dto.getColumnBelongTables())) {
                tables.add(null == dto.getColumnBelongTables().get(i)? "" : dto.getColumnBelongTables().get(i));
            }
        }
        if (CollectionUtils.isEmpty(headers)) {
            MessageAlert.information("没有可供导出的列");
            return;
        }
        String tableName = "t";
        if (CollectionUtils.isNotEmpty(tables) && 1 == tables.size()) {
            // 只有一个表
            tableName = new ArrayList<>(tables).get(0);
        }
        List<String> sqls = new ArrayList<>();
        for (int i = 0; i < dto.getVals().size(); i++) {
            if (!exportAll && !idxs.contains(i)) {
                continue;
            }
            StringBuilder sql = new StringBuilder();
            sql.append("insert into `").append(tableName).append("` (");
            // 拼接字段
            for (int h = 0; h < headers.size(); h++) {
                if (0 != h) {
                    sql.append(", ");
                }
                sql.append("`").append(headers.get(h)).append("`");
            }
            sql.append(") ");
            sql.append("values(");

            // 拼接值
            StringBuilder colVal = new StringBuilder();
            for (int j = 0; j < dto.getVals().get(i).size(); j++) {
                String columnLabel = dto.getColumnLabels().get(j);
                if (!fields.contains(columnLabel)) {
                    continue;
                }
                if (StringUtils.isNotBlank(colVal)) {
                    colVal.append(", ");
                }
                Object obj = dto.getVals().get(i).get(j);
                if (null == obj) {
                    colVal.append("null");
                } else {
                    colVal.append("\"").append(CommonUtil.toString(obj)).append("\"");
                }
            }
            sql.append(colVal).append(");");
            sqls.add(sql.toString());
        }

        if (CollectionUtils.isEmpty(sqls)) {
            MessageAlert.information("没有可供导出的数据");
            return;
        }

        // 数据写入
        try{
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] b = CommonUtil.join(sqls, "\n").getBytes(StandardCharsets.UTF_8);
            out.write(b,0,b.length);
            out.flush();
            out.close();
        }catch(IOException ex){
            MessageAlert.error("导出失败: 写入数据发生异常");
            return;
        }

        MessageAlert.information("导出成功");
    }

    // 导出行视图结果
    private void exportRowView(TableView<RowValueDto> resultRowView) {
        if (CollectionUtils.isEmpty(resultRowView.getItems())) {
            MessageAlert.information("导出数据为空");
            return;
        }
        FileChooser fc = new FileChooser();
        if (StringUtils.isBlank(exportPath)) {
            fc.setInitialDirectory(new File("."));
        } else {
            fc.setInitialDirectory(new File(exportPath));
        }
        fc.setTitle("导出");
        fc.setInitialFileName("row-data-" + DateFormatUtils.format(new Date(), "yyyyMMddHHmmss") + ".xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(".xlsx", "*.xlsx"));

        File file = fc.showSaveDialog(resultRowView.getScene().getWindow());
        if (null == file) {
            return;
        }
        exportPath = file.getParentFile().getAbsolutePath();

        // 表头
        List<List<String>> headers = new ArrayList<>();
        for (TableColumn column : resultRowView.getColumns()) {
            headers.add(List.of(column.getText()));
        }
        ErrorDto errorDto = new ErrorDto();
        final int[] idx = {0};
        boolean isSuccess = ExportUtil.exportExcel(file, "row-data", headers, null, errorDto, () -> {
            if (idx[0] == 0) {
                idx[0]++;
                List<List<Object>> datas = new ArrayList<>();
                for (RowValueDto item : resultRowView.getItems()) {
                    List<Object> data = new ArrayList<>();
                    data.add(item.getIdx());
                    data.add(item.getColumn());
                    data.add(item.getValue());
                    data.add(item.getComment());
                    datas.add(data);
                }
                return datas;
            } else {
                return Collections.emptyList();
            }
        });
        if (!isSuccess) {
            MessageAlert.error(String.format("导出失败: %s", errorDto.getErrorMessage()));
            return;
        }

        MessageAlert.information("导出成功");
    }

    private void showCompareResult(SqlValueDto valueDto, List<Integer> selectIdxs, Node parentNode) {
        if (CollectionUtils.isEmpty(selectIdxs)) {
            return;
        }
        try {
            showItemResult(valueDto, selectIdxs, true, parentNode);
        } catch (Exception e) {
            MessageAlert.error("结果比对页面显示异常");
        }
    }

    private void showSingleResult(SqlValueDto valueDto, int selectIdx, Node parentNode) {
        if (null == valueDto || selectIdx < 0) {
            return;
        }
        try {
            showItemResult(valueDto, Collections.singletonList(selectIdx), false, parentNode);
        } catch (Exception e) {
            MessageAlert.error("结果查看页面显示异常");
        }
    }

    private void showItemResult(SqlValueDto valueDto, List<Integer> selectIdxs, boolean isCompare, Node parentNode) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("data-query-result.fxml"));
            Parent root = fxmlLoader.load();
            DataQueryResultController controller = fxmlLoader.getController();
            controller.init(valueDto, selectIdxs, isCompare);
            Stage stage = new Stage();
            stage.setTitle(isCompare? "结果比对" : "结果查看");
            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);
            Stage parent = (Stage) parentNode.getScene().getWindow();
            if (isCompare) {
                stage.getIcons().add(new Image("compare.png"));
            } else {
                stage.getIcons().add(new Image("show_detail.png"));
            }
            stage.initOwner(parent);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error("系统异常");
        }
    }

    private void showResultView(ResultViewCtlDto rstViewDto, SqlValueDto dto) {
        // 获取结果视图类型
        ResultViewType resultViewType = connectConrolMap.get(rstViewDto.getConnectTab()).getSqlControlMap()
                .get(rstViewDto.getSqlTab()).getResultViewTypeMap().get(rstViewDto.getResultTab());
        if (null == resultViewType) {
            resultViewType = ResultViewType.TABLE;
        }
        // 根据不同的结果视图类型显示和隐藏不同的控件
        switch (resultViewType) {
            case TABLE -> {
                CommonUtil.showControl(rstViewDto.getTableView());
                CommonUtil.hideControls(rstViewDto.getHtmlView(), rstViewDto.getRowControlDto().getRowVBox());
                // 显示内容
                showResultTableView(rstViewDto.getTableView(), dto);
                // 关联表格视图菜单
                attachTableRstViewMenu(rstViewDto, dto);
            }
            case HTML -> {
                CommonUtil.showControl(rstViewDto.getHtmlView());
                CommonUtil.hideControls(rstViewDto.getTableView(), rstViewDto.getRowControlDto().getRowVBox());
                // 显示内容
                showResultHTMLView(rstViewDto.getHtmlView(), dto);
            }
            case ROW -> {
                CommonUtil.showControl(rstViewDto.getRowControlDto().getRowVBox());
                CommonUtil.hideControls(rstViewDto.getTableView(), rstViewDto.getHtmlView());
                // 显示内容
                showResultRowView(rstViewDto.getRowControlDto(), rstViewDto.getTableView(), dto);
            }
        }


    }

    private void attachTableRstViewMenu(ResultViewCtlDto rstViewDto, SqlValueDto dto) {
        ContextMenu menu = new ContextMenu();
        MenuItem refreshMenu = new MenuItem("刷新");
        refreshMenu.setGraphic(new ImageView(new Image("refresh.png")));
        MenuItem copyMenu = new MenuItem("复制选中行");
        copyMenu.setGraphic(new ImageView(new Image("copy.png")));
        MenuItem exportMenu = new MenuItem("导出所有行");
        exportMenu.setGraphic(new ImageView(new Image("export_all.png")));
        MenuItem exportSelect = new MenuItem("导出选中行");
        exportSelect.setGraphic(new ImageView(new Image("export_select.png")));
        MenuItem showSelectRowMenu = new MenuItem("显示行视图");
        showSelectRowMenu.setGraphic(new ImageView(new Image("form_view.png")));
        menu.getItems().addAll(refreshMenu,
                new SeparatorMenuItem(), copyMenu,
                new SeparatorMenuItem(), exportMenu,
                new SeparatorMenuItem(), exportSelect,
                new SeparatorMenuItem(), showSelectRowMenu);
        rstViewDto.getTableView().setContextMenu(menu);

        // 刷新
        refreshMenu.setOnAction(actionEvent -> {
            Integer firstLine = Integer.valueOf(rstViewDto.getFirstLineEdit().getText());
            if (firstLine > 0) {
                rstViewDto.getLeftBtn().setDisable(false);
            }
            refreshResult(rstViewDto, dto);
        });
        // 复制
        copyMenu.setOnAction(actionEvent -> {
            ObservableList<TablePosition> selectedCells = rstViewDto.getTableView().getSelectionModel().getSelectedCells();
            if (CollectionUtils.isEmpty(selectedCells)) {
                MessageAlert.information("请选择需要复制的行");
                return;
            }
            List<Integer> idxs = new ArrayList<>();
            for (TablePosition selectedCell : selectedCells) {
                if (!idxs.contains(selectedCell.getRow())) {
                    idxs.add(selectedCell.getRow());
                }
            }
            StringBuilder value = new StringBuilder();
            value.append(CommonUtil.join(dto.getColumnLabels(), ",")).append("\n");
            for (Integer idx : idxs) {
                List<Object> objects = dto.getVals().get(idx);
                StringBuilder val = new StringBuilder();
                for (int n = 0; n < objects.size(); n++) {
                    if (0 == n) {
                        val.append(CommonUtil.toString(objects.get(n)));
                    } else {
                        val.append(",").append(CommonUtil.toString(objects.get(n)));
                    }
                }
                value.append(val).append("\n");
            }
            Map<DataFormat, Object> content = new HashMap<>();
            content.put(DataFormat.PLAIN_TEXT, value.toString());
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(content);
        });
        // 导出
        exportMenu.setOnAction(actionEvent -> {
            exportResults(dto, true, null, rstViewDto.getTableView());
        });
        // 导出选中行
        exportSelect.setOnAction(actionEvent -> {
            ObservableList<TablePosition> selectedCells = rstViewDto.getTableView().getSelectionModel().getSelectedCells();
            List<Integer> rows = new ArrayList<>();
            for (TablePosition cell : selectedCells) {
                int row = cell.getRow();
                if (!rows.contains(row)) {
                    rows.add(row);
                }
            }
            exportResults(dto, false, rows, rstViewDto.getTableView());
        });
        showSelectRowMenu.setOnAction(event -> {
            ObservableList<TablePosition> selectedCells = rstViewDto.getTableView().getSelectionModel().getSelectedCells();
            if (CollectionUtils.isEmpty(selectedCells)) {
                rstViewDto.getRowControlDto().setSelectIdx(0);
            } else {
                rstViewDto.getRowControlDto().setSelectIdx(selectedCells.get(0).getRow());
            }

            rstViewDto.getRowViewBtn().setSelected(true);
        });
        rstViewDto.getTableView().setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.C && keyEvent.isControlDown()) {
                // 复制单元格内容
                List<TablePosition> selectedCells = rstViewDto.getTableView().getSelectionModel().getSelectedCells();
                if (CollectionUtils.isEmpty(selectedCells)) {
                    return;
                }
                List<Pair<Integer, Integer>> selectIdxs = new ArrayList<>();
                for (TablePosition selectedCell : selectedCells) {
                    if (selectedCell.getRow() < 0 || selectedCell.getColumn() < 0) {
                        continue;
                    }
                    selectIdxs.add(new Pair<>(selectedCell.getRow(), selectedCell.getColumn()));
                }
                if (CollectionUtils.isEmpty(selectIdxs)) {
                    return;
                }

                StringBuilder values = new StringBuilder();
                int idx = 0;
                for (Pair<Integer, Integer> selectIdx : selectIdxs) {
                    int row = selectIdx.getKey();
                    int column = selectIdx.getValue();
                    Object val = dto.getVals().get(row).get(column);
                    if (null != val) {
                        if (0 != idx) {
                            values.append(",");
                        }
                        values.append(CommonUtil.toString(val));
                        idx++;
                    }
                }

                Clipboard clipboard = Clipboard.getSystemClipboard();
                Map<DataFormat, Object> val = new HashMap<>();
                val.put(DataFormat.PLAIN_TEXT, values.toString());
                clipboard.setContent(val);
            }
        });

        rstViewDto.getTableView().getSelectionModel().getModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // 结果列表选择行改变时, 需要与行视图进行关联变动
            if (null == newValue || CollectionUtils.isEmpty(newValue)) {
                rstViewDto.getRowControlDto().setSelectIdx(0);
            } else {
                rstViewDto.getRowControlDto().setSelectIdx(newValue.get(0).getRow());
            }
        });
    }

    /**
     * 显示网格视图结果
     * @param resultView
     * @param dto
     */
    private void showResultTableView(SpreadsheetView resultView, SqlValueDto dto) {
        GridBase resultGrid = new GridBase(1, dto.getColumnLabels().size());
        ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();
        Map<Integer, Integer> maxCharMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(dto.getVals())) {
            for (int row = 0; row < dto.getVals().size(); row++) {
                List<Object> vals = dto.getVals().get(row);
                ObservableList<SpreadsheetCell> cells = FXCollections.observableArrayList();
                for (int i = 0; i < vals.size(); i++) {
                    Object val = vals.get(i);
                    MySQLTypeEnum colType = MySQLTypeEnum.getByTypeName(dto.getSqlTypeNames().get(i));
                    String valShow = CommonUtil.toCellString(val,colType);
                    SpreadsheetCell cell;
                    if (null != colType && colType.isBlob()) {
                        // 大字段
                        cell = SpreadsheetCellType.STRING.createCell(row, i, 1, 1, null);
                        HBox cellHBox = new HBox();
                        cellHBox.setPadding(new Insets(0.5));
                        cellHBox.setAlignment(Pos.CENTER_LEFT);
                        Label valLabel = new Label(valShow);
                        if (null == val) {
                            valLabel.setDisable(true);
                        }
                        valLabel.setAlignment(Pos.CENTER_LEFT);
                        Separator sep = new Separator(Orientation.HORIZONTAL);
                        sep.setVisible(false);
                        Button valBtn = new Button("...");
                        HBox.setHgrow(sep, Priority.ALWAYS);
                        cellHBox.getChildren().addAll(valLabel, sep, valBtn);
                        cellHBox.setFillHeight(false);
                        cell.setGraphic(cellHBox);
                        CommonUtil.buttonBind(valBtn, () -> {
                            try {
                                FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("blob-value-view.fxml"));
                                Parent root = fxmlLoader.load();
                                BlobValueViewController controller = fxmlLoader.getController();
                                controller.init(val);
                                Stage stage = new Stage();
                                stage.setTitle("Blob字段值");
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
                        // 非大字段
                        if (null == val) {
                            cell = SpreadsheetCellType.STRING.createCell(row, i, 1, 1, null);
                            Label nullLabel = new Label(valShow);
                            nullLabel.setDisable(true);
                            nullLabel.setAlignment(Pos.CENTER_LEFT);
                            cell.setGraphic(nullLabel);
                        } else {
                            cell = SpreadsheetCellType.STRING.createCell(row, i, 1, 1, valShow);
                        }
                    }
                    cells.add(cell);
                    if (!maxCharMap.containsKey(i)) {
                        maxCharMap.put(i, CommonUtil.getValue(CommonUtil.toString(val), "(NULL)").getBytes().length);
                    } else {
                        maxCharMap.put(i, CommonUtil.getMax(CommonUtil.getValue(CommonUtil.toString(val, colType), "(NULL)").getBytes().length, maxCharMap.get(i)));
                    }
                }
                rows.add(cells);
            }
        } else {
            // 结果集为空
            ObservableList<SpreadsheetCell> cells = FXCollections.observableArrayList();
            for (int i = 0; i < dto.getColumnLabels().size(); i++) {
                Object val = null;
                String valShow = CommonUtil.toCellString(val);
                SpreadsheetCell cell = SpreadsheetCellType.STRING.createCell(0, i, 1, 1, null);
                Label nullLabel = new Label(valShow);
                nullLabel.setDisable(true);
                nullLabel.setAlignment(Pos.CENTER_LEFT);
                cell.setGraphic(nullLabel);
                cells.add(cell);
                if (!maxCharMap.containsKey(i)) {
                    maxCharMap.put(i, CommonUtil.getValue(CommonUtil.toString(val), "(NULL)").getBytes().length);
                } else {
                    maxCharMap.put(i, CommonUtil.getMax(CommonUtil.getValue(CommonUtil.toString(val), "(NULL)").getBytes().length, maxCharMap.get(i)));
                }
            }
            rows.add(cells);
        }
        resultGrid.setRows(rows);
        resultView.setGrid(resultGrid);
        resultView.setContextMenu(null);
        int idx = 0;
        for (SpreadsheetColumn column : resultView.getColumns()) {
            maxCharMap.put(idx, CommonUtil.getMax(dto.getColumnLabels().get(idx).getBytes().length, maxCharMap.get(idx)));
            column.setText(dto.getColumnLabels().get(idx));
            column.setMinWidth(50);
            int factor = 9;
            MySQLTypeEnum colType = MySQLTypeEnum.getByTypeName(dto.getSqlTypeNames().get(idx));
            if (null != colType && colType.isBlob()) {
                factor = 11;
            }
            column.setPrefWidth(CommonUtil.getMin(400, CommonUtil.getValue(maxCharMap.get(idx), 60) * factor));
            idx++;
        }
        if (CollectionUtils.isEmpty(dto.getVals())) {
            // 结果集为空则删除占位的行
            resultGrid.getRows().clear();
        }
    }

    // 展示HTML
    private void showResultHTMLView(WebView resultHtmlView, SqlValueDto dto) {
        resultHtmlView.setContextMenuEnabled(false);
        resultHtmlView.getEngine().loadContent(assembleHtmlResult(dto, true, null, true, null));
    }

    private String assembleHtmlResult(SqlValueDto dto, boolean allField, Set<String> fields, boolean exportAll, List<Integer> idxs) {
        List<String> headers = new ArrayList<>();
        List<List<String>> values = new ArrayList<>();
        // 表头
        headers.add("#");
        for (String columnLabel : dto.getColumnLabels()) {
            if (!allField && !fields.contains(columnLabel)) {
                continue;
            }
            headers.add(columnLabel);
        }
        // 行数据拼接
        int idx = 1;
        for (int i = 0; i < dto.getVals().size(); i++) {
            if (!exportAll && !idxs.contains(i)) {
                continue;
            }
            List<String> value = new ArrayList<>();
            List<Object> rowVals = dto.getVals().get(i);
            value.add(String.valueOf(idx++));
            for (int j = 0; j < rowVals.size(); j++) {
                if (!allField) {
                    String columnLabel = dto.getColumnLabels().get(j);
                    if (!fields.contains(columnLabel)) {
                        continue;
                    }
                }
                Object val = rowVals.get(j);
                value.add(null == val? "" : CommonUtil.toString(val));
            }
            values.add(value);
        }
        return HtmlTableUtil.htmlTable(headers, values, "查询结果", false);
    }

    private void showResultRowView(RowResultControlDto rowControlDto, SpreadsheetView resultView, SqlValueDto dto) {
        if (rowControlDto.getSelectIdx() < 0) {
            rowControlDto.setSelectIdx(0);
        }
        rowControlDto.getLineEdit().setText(String.valueOf(rowControlDto.getSelectIdx() + 1));
        CommonUtil.enableControls(rowControlDto.getStartBtn(), rowControlDto.getPreBtn(), rowControlDto.getNextBtn(), rowControlDto.getEndBtn());
        if (CollectionUtils.isEmpty(dto.getVals())) {
            // 没有记录
            CommonUtil.disableControls(rowControlDto.getStartBtn(), rowControlDto.getPreBtn(), rowControlDto.getNextBtn(), rowControlDto.getEndBtn());
            rowControlDto.getLineEdit().setText("0");
            rowControlDto.setSelectIdx(0);
        } else if (rowControlDto.getSelectIdx() >= dto.getVals().size()) {
            // 显示行大于总行数则禁用下一行及最后行按钮
            CommonUtil.disableControls(rowControlDto.getNextBtn(), rowControlDto.getEndBtn());
            rowControlDto.setSelectIdx(dto.getVals().size() - 1);
            rowControlDto.getLineEdit().setText(String.valueOf(rowControlDto.getSelectIdx() + 1));
        } else if (1 == dto.getVals().size()) {
            CommonUtil.disableControls(rowControlDto.getStartBtn(), rowControlDto.getPreBtn(), rowControlDto.getNextBtn(), rowControlDto.getEndBtn());
        } else if (0 == rowControlDto.getSelectIdx()) {
            CommonUtil.disableControls(rowControlDto.getStartBtn(), rowControlDto.getPreBtn());
        } else if (dto.getVals().size() - 1 == rowControlDto.getSelectIdx()) {
            CommonUtil.disableControls(rowControlDto.getNextBtn(), rowControlDto.getEndBtn());
        }

        // 展示数据
        List<String> headers = Arrays.asList("column", "value", "comment", "from table");
        GridBase rowGrid = new GridBase(1, headers.size());
        ObservableList<ObservableList<SpreadsheetCell>> rows = FXCollections.observableArrayList();
        Map<Integer, Integer> maxCharMap = new HashMap<>();
        for (int i = 0; i <headers.size(); i++) {
            maxCharMap.put(i, 0);
        }
        List<Object> vals = CollectionUtils.isNotEmpty(dto.getVals())? dto.getVals().get(rowControlDto.getSelectIdx()) : CommonUtil.newNullList(dto.getColumnLabels().size());
        for (int row = 0; row < dto.getColumnLabels().size(); row++) {
            ObservableList<SpreadsheetCell> cells = FXCollections.observableArrayList();
            int colIdx = 0;
            // 列名
            maxCharMap.put(colIdx, CommonUtil.getMax(CommonUtil.getValue(dto.getColumnLabels().get(row), "(NULL)").getBytes().length, maxCharMap.get(colIdx)));
            cells.add(SpreadsheetCellType.STRING.createCell(row, colIdx++, 1, 1, dto.getColumnLabels().get(row)));
            // 值
            Object val = vals.get(row);
            String valShow = CommonUtil.toCellString(val);
            maxCharMap.put(colIdx, CommonUtil.getMax(valShow.getBytes().length, maxCharMap.get(colIdx)));
            SpreadsheetCell cell;
            MySQLTypeEnum colType = MySQLTypeEnum.getByTypeName(dto.getSqlTypeNames().get(row));
            if (null != colType && colType.isBlob()) {
                // 大字段
                cell = SpreadsheetCellType.STRING.createCell(row, colIdx++, 1, 1, null);
                HBox cellHBox = new HBox();
                cellHBox.setPadding(new Insets(0.5));
                cellHBox.setAlignment(Pos.CENTER_LEFT);
                Label valLabel = new Label(valShow);
                if (null == val) {
                    valLabel.setDisable(true);
                }
                valLabel.setAlignment(Pos.CENTER_LEFT);
                Separator sep = new Separator(Orientation.HORIZONTAL);
                sep.setVisible(false);
                Button valBtn = new Button("...");
                HBox.setHgrow(sep, Priority.ALWAYS);
                cellHBox.getChildren().addAll(valLabel, sep, valBtn);
                cellHBox.setFillHeight(false);
                cell.setGraphic(cellHBox);
                CommonUtil.buttonBind(valBtn, () -> {
                    try {
                        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("blob-value-view.fxml"));
                        Parent root = fxmlLoader.load();
                        BlobValueViewController controller = fxmlLoader.getController();
                        controller.init(val);
                        Stage stage = new Stage();
                        stage.setTitle("Blob字段值");
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
                // 非大字段
                if (null == val) {
                    cell = SpreadsheetCellType.STRING.createCell(row, colIdx++, 1, 1, null);
                    Label nullLabel = new Label(valShow);
                    nullLabel.setDisable(true);
                    nullLabel.setAlignment(Pos.CENTER_LEFT);
                    cell.setGraphic(nullLabel);
                } else {
                    cell = SpreadsheetCellType.STRING.createCell(row, colIdx++, 1, 1, valShow);
                }
            }
            cells.add(cell);
            // 注释
            maxCharMap.put(colIdx, CommonUtil.getMax(CommonUtil.getValue(CollectionUtils.isEmpty(dto.getColumnComments())? null : dto.getColumnComments().get(row),
                    "(NULL)").getBytes().length, maxCharMap.get(colIdx)));
            cells.add(SpreadsheetCellType.STRING.createCell(row, colIdx++, 1, 1, CollectionUtils.isEmpty(dto.getColumnComments())? null : dto.getColumnComments().get(row)));
            // 所属表
            maxCharMap.put(colIdx, CommonUtil.getMax(CommonUtil.getValue(CollectionUtils.isEmpty(dto.getColumnBelongTables())? null : dto.getColumnBelongTables().get(row),
                    "(NULL)").getBytes().length, maxCharMap.get(colIdx)));
            cells.add(SpreadsheetCellType.STRING.createCell(row, colIdx++, 1, 1, CollectionUtils.isEmpty(dto.getColumnBelongTables())? null : dto.getColumnBelongTables().get(row)));

            rows.add(cells);
        }
        rowGrid.setRows(rows);
        rowControlDto.getResultRowView().setGrid(rowGrid);
        int idx = 0;
        for (SpreadsheetColumn column : rowControlDto.getResultRowView().getColumns()) {
            maxCharMap.put(idx, CommonUtil.getMax(headers.get(idx).getBytes().length, maxCharMap.get(idx)));
            column.setText(headers.get(idx));
            column.setMinWidth(50);
            int factor = 9;
            if (1 == idx) {
                factor = 11;
            }
            column.setPrefWidth(CommonUtil.getMin(400, (CommonUtil.getValue(maxCharMap.get(idx), 60) + 1) * factor));
            idx++;
        }
        if (CollectionUtils.isEmpty(dto.getVals())) {
            // 结果集为空时清空占位行
            rowGrid.getRows().clear();
        } else {
            resultView.getSelectionModel().selectCells(new javafx.util.Pair<>(rowControlDto.getSelectIdx(), 0));
        }

        // 按钮事件注册
        CommonUtil.buttonBind(rowControlDto.getStartBtn(), () -> {
            rowControlDto.setSelectIdx(0);
            showResultRowView(rowControlDto, resultView, dto);
        });
        CommonUtil.buttonBind(rowControlDto.getPreBtn(), () -> {
            rowControlDto.setSelectIdx(rowControlDto.getSelectIdx() - 1);
            showResultRowView(rowControlDto, resultView, dto);
        });
        CommonUtil.buttonBind(rowControlDto.getNextBtn(), () -> {
            rowControlDto.setSelectIdx(rowControlDto.getSelectIdx() + 1);
            showResultRowView(rowControlDto, resultView, dto);
        });
        CommonUtil.buttonBind(rowControlDto.getEndBtn(), () -> {
            rowControlDto.setSelectIdx(dto.getVals().size() - 1);
            showResultRowView(rowControlDto, resultView, dto);
        });
        CommonUtil.numricTextField(0, dto.getVals().size(), 1, rowControlDto.getLineEdit());
        CommonUtil.textFieldSubmit(rowControlDto.getLineEdit(), () -> {
            rowControlDto.setSelectIdx(Integer.parseInt(rowControlDto.getLineEdit().getText()) - 1);
            showResultRowView(rowControlDto, resultView, dto);
        });
        rowControlDto.getResultRowView().setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.C && keyEvent.isControlDown()) {
                // 复制单元格内容
                List<TablePosition> selectedCells = rowControlDto.getResultRowView().getSelectionModel().getSelectedCells();
                if (CollectionUtils.isEmpty(selectedCells)) {
                    return;
                }
                List<Pair<Integer, Integer>> selectIdxs = new ArrayList<>();
                for (TablePosition selectedCell : selectedCells) {
                    if (selectedCell.getRow() < 0 || selectedCell.getColumn() < 0) {
                        continue;
                    }
                    selectIdxs.add(new Pair<>(selectedCell.getRow(), selectedCell.getColumn()));
                }
                if (CollectionUtils.isEmpty(selectIdxs)) {
                    return;
                }

                StringBuilder values = new StringBuilder();
                int index = 0;
                for (Pair<Integer, Integer> selectIdx : selectIdxs) {
                    int row = selectIdx.getKey();
                    int column = selectIdx.getValue();

                    String value;
                    if (0 == column) {
                        // 复制列
                        value = dto.getColumnLabels().get(row);
                    } else if (1 == column) {
                        // 复制列值
                        value = CommonUtil.toString(dto.getVals().get(Integer.valueOf(rowControlDto.getLineEdit().getText()) - 1).get(row));
                    } else if (2 == column){
                        // 复制注释
                        value = CollectionUtils.isEmpty(dto.getColumnComments())? null : dto.getColumnComments().get(row);
                    } else {
                        // 所属表
                        value = CollectionUtils.isEmpty(dto.getColumnBelongTables())? null : dto.getColumnBelongTables().get(row);
                    }
                    if (null != value) {
                        if (0 != index) {
                            values.append(",");
                        }
                        values.append(value);
                        index++;
                    }
                }

                Clipboard clipboard = Clipboard.getSystemClipboard();
                Map<DataFormat, Object> val = new HashMap<>();
                val.put(DataFormat.PLAIN_TEXT, values.toString());
                clipboard.setContent(val);
            }
        });
    }

    private void updateStatus() {
        // 先查找当前选中的数据库连接
        Tab connectTab = mainTabPanel.getSelectionModel().getSelectedItem();
        if (null == connectTab || connectTab == newConnnectTab) {
            initStatus();
            return;
        }

        // 查找选中的SQL页签
        Tab sqlTab = connectConrolMap.get(connectTab).getSqlTabPane().getSelectionModel().getSelectedItem();
        if (null == sqlTab) {
            initStatus();
            return;
        }

        if (!connectConrolMap.get(connectTab).getSqlControlMap().containsKey(sqlTab)) {
            return;
        }

        // 获取SQL控件
        SqlControlDto sqlControl = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab);
        if (null == sqlControl || null == sqlControl.getSqlCodeView()) {
            initStatus();
            return;
        }

        if (sqlControl.getSqlCodeView().isDisable()) {
            processStatus();
            return;
        }

        // 获取当前选中结果页签
        Tab resultTab = sqlControl.getResultTabPane().getSelectionModel().getSelectedItem();
        if (null == resultTab || !sqlControl.getExecLogMap().containsKey(resultTab)) {
            initStatus();
            return;
        }
        // 当前结果页签执行记录
        ExecLogDto execLogDto = sqlControl.getExecLogMap().get(resultTab);
        if (null == execLogDto) {
            initStatus();
            return;
        }
        tipLabel.setText(execLogDto.getTip());
        timeLabel.setText(execLogDto.timeTip());
        recordLabel.setText(execLogDto.recordCntTip());
    }

    /**
     * 更新表明细信息展示
     */
    private void updateDetailView() {
        // 先查找当前选中的数据库连接
        Tab connectTab = mainTabPanel.getSelectionModel().getSelectedItem();
        if (null == connectTab || connectTab == newConnnectTab) {
            initStatus();
            return;
        }

        // 获取表清单列表
        ListView<DatasouceTableVO> tableInfoView = StringUtils.isEmpty(connectConrolMap.get(connectTab).getFilterTableEdit().getText())? connectConrolMap.get(connectTab).getTableInfoView() : connectConrolMap.get(connectTab).getFilterTableView();
        if (null == tableInfoView) {
            return;
        }

        // 查找选中的SQL页签
        Tab sqlTab = connectConrolMap.get(connectTab).getSqlTabPane().getSelectionModel().getSelectedItem();
        if (null == sqlTab) {
            initStatus();
            return;
        }

        // 获取SQL控件
        SqlControlDto sqlControl = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab);
        if (null == sqlControl || null == sqlControl.getDetailView()) {
            initStatus();
            return;
        }

        // 展示表明细信息
        showDetailView(connectTab, sqlTab, sqlControl.getDetailView(), tableInfoView);
    }

    /**
     * 展示表明细信息
     * @param detailView
     * @param tableInfoView
     */
    private void showDetailView(Tab connectTab, Tab sqlTab, WebView detailView, ListView<DatasouceTableVO> tableInfoView) {
        DatasouceTableVO selectedTable = tableInfoView.getSelectionModel().getSelectedItem();
        if (null == selectedTable) {
            // 未选中表, 展示表清单信息
            List<List<String>> values = new ArrayList<>();

            // 数据拼接
            int idx = 1;
            for (DatasouceTableVO item : tableInfoView.getItems()) {
                List<String> value = new ArrayList<>();
                value.add(String.valueOf(idx++));
                value.add(item.getTableName());
                value.add(null == item.getComment()? "" : item.getComment());
                values.add(value);
            }
            detailView.getEngine().loadContent(HtmlTableUtil.htmlTable(Arrays.asList("#", "表", "表名"), values, "表信息", true));
        } else {
            // 选中了表,则展示建表语句
            try {
                String createSql = null;
                String tableName = selectedTable.getTableName();
                String sql = "SHOW CREATE TABLE " + tableName;
                List<Result<SqlValueDto>> vals = DataSourceProvider.querySQLVals(connectConrolMap.get(connectTab).getConnectInfo(), Collections.singletonList(sql));
                if (CollectionUtils.isEmpty(vals) || !vals.get(0).isSuccess() || null == vals.get(0).getData()) {
                    throw new IllegalStateException("query create table failed");
                }
                SqlValueDto val = vals.get(0).getData();
                for (int i = 0; i < val.getColumnLabels().size(); i++) {
                    if (StringUtils.equalsIgnoreCase(val.getColumnLabels().get(i), "Create Table")) {
                        createSql = String.valueOf(val.getVals().get(0).get(i));
                        break;
                    }
                }
                createSql = "-- DDL建表语句\n" + createSql;
                assembleSqlCodeEditor(connectTab, sqlTab, detailView, null, createSql, true);
            } catch (Exception e) {
                e.printStackTrace();
                detailView.getEngine().loadContent("<html><body><h2>处理</h2><body></html>");
            }
        }
    }

    private void initStatus() {
        tipLabel.setText("就绪");
        timeLabel.setText(null);
        recordLabel.setText(null);
    }

    private void processStatus() {
        tipLabel.setText("执行查询...");
        timeLabel.setText(null);
        recordLabel.setText(null);
    }

    public void onAbout(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("about.fxml"));
            Parent root = fxmlLoader.load();
            Stage stage = new Stage();
            stage.setTitle("关于zsSQL");
            Scene scene = new Scene(root, 506, 154);
            stage.setScene(scene);
            Stage parent = (Stage) mainTabPanel.getScene().getWindow();
            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.getIcons().add(new Image("icon.png"));
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新菜单及工具栏状态
     */
    public void refreshMenuAndToolBarStatus() {
        if (CollectionUtils.isEmpty(mainTabPanel.getTabs()) || mainTabPanel.getTabs().size() <= 1 || !initialized) {
            if (!initialized) {
                newConnectPageMenu.setDisable(true);
                newConnBtn.setDisable(true);
            } else {
                newConnectPageMenu.setDisable(false);
                newConnBtn.setDisable(false);
            }
            // 未连接任何数据库
            newSqlPageMenu.setDisable(true);
            openSQLMenu.setDisable(true);
            saveSQLMenu.setDisable(true);
            saveSQLAsMenu.setDisable(true);
            undoMenu.setDisable(true);
            redoMenu.setDisable(true);
            cutMenu.setDisable(true);
            copyMenu.setDisable(true);
            pasteMenu.setDisable(true);
            selectAllMenu.setDisable(true);
            findMenu.setDisable(true);
            replaceMenu.setDisable(true);
            formatSQLMenu.setDisable(true);
            formatAllSQLMenu.setDisable(true);
            sqlUpperMenu.setDisable(true);
            sqlLowerMenu.setDisable(true);
            executeSqlMenu.setDisable(true);
            executeAllSqlMenu.setDisable(true);

            CommonUtil.disableControls(newSqlBtn, openSqlBtn, saveSqlBtn, findBtn, replaceBtn, executeBtn, executeAllBtn, formatBtn);
        } else {
            // 获取当前选中的SQL页签
            SqlControlDto activeSqlControl = getActiveSqlControl();
            newConnectPageMenu.setDisable(false);
            newConnBtn.setDisable(false);
            newSqlPageMenu.setDisable(false);
            openSQLMenu.setDisable(false);
            saveSQLMenu.setDisable(null == activeSqlControl);
            saveSQLAsMenu.setDisable(null == activeSqlControl);
            undoMenu.setDisable(null == activeSqlControl);
            redoMenu.setDisable(null == activeSqlControl);
            cutMenu.setDisable(null == activeSqlControl);
            copyMenu.setDisable(null == activeSqlControl);
            pasteMenu.setDisable(null == activeSqlControl);
            selectAllMenu.setDisable(null == activeSqlControl);
            findMenu.setDisable(null == activeSqlControl);
            replaceMenu.setDisable(null == activeSqlControl);
            formatSQLMenu.setDisable(null == activeSqlControl);
            formatAllSQLMenu.setDisable(null == activeSqlControl);
            sqlUpperMenu.setDisable(null == activeSqlControl);
            sqlLowerMenu.setDisable(null == activeSqlControl);
            executeSqlMenu.setDisable(null == activeSqlControl);
            executeAllSqlMenu.setDisable(null == activeSqlControl);
            CommonUtil.enableControls(newSqlBtn, openSqlBtn);
            if (null != activeSqlControl) {
                CommonUtil.enableControls(saveSqlBtn, findBtn, replaceBtn, executeBtn, executeAllBtn, formatBtn);
            } else {
                CommonUtil.disableControls(saveSqlBtn, findBtn, replaceBtn, executeBtn, executeAllBtn, formatBtn);
            }
        }
    }

    private SqlControlDto getActiveSqlControl() {
        // 先查找当前选中的数据库连接
        Tab connectTab = mainTabPanel.getSelectionModel().getSelectedItem();
        if (null == connectTab || connectTab == newConnnectTab) {
            return null;
        }

        // 查找选中的SQL页签
        Tab sqlTab = connectConrolMap.get(connectTab).getSqlTabPane().getSelectionModel().getSelectedItem();
        if (null == sqlTab) {
            return null;
        }

        if (!connectConrolMap.get(connectTab).getSqlControlMap().containsKey(sqlTab)) {
            return null;
        }

        // 获取SQL控件
        return connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab);
    }

    private WebView getActiveSqlEditor() {
        // 获取SQL控件
        SqlControlDto sqlControl = getActiveSqlControl();
        if (null == sqlControl || null == sqlControl.getSqlCodeView()) {
            return null;
        }
        return sqlControl.getSqlCodeView();
    }

    public void onUndo(ActionEvent actionEvent) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }

        SqlEditorUtils.undo(sqlEditor);
    }

    public void onRedo(ActionEvent actionEvent) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        SqlEditorUtils.redo(sqlEditor);
    }

    public void onCut(ActionEvent actionEvent) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        // 获取选中的文本
        String code = SqlEditorUtils.getSelectSqlCode(sqlEditor);
        if (StringUtils.isEmpty(code)) {
            return;
        }
        Map<DataFormat, Object> content = new HashMap<>();
        content.put(DataFormat.PLAIN_TEXT, code);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        clipboard.setContent(content);
        // 移除选中的文本
        SqlEditorUtils.replaceSelect(sqlEditor, "");
    }

    public void onCopy(ActionEvent actionEvent) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        // 获取选中的文本
        String code = SqlEditorUtils.getSelectSqlCode(sqlEditor);
        if (StringUtils.isEmpty(code)) {
            return;
        }
        Map<DataFormat, Object> content = new HashMap<>();
        content.put(DataFormat.PLAIN_TEXT, code);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        clipboard.setContent(content);
    }

    public void onPaste(ActionEvent actionEvent) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        String code = SqlEditorUtils.getSelectSqlCode(sqlEditor);
        if (StringUtils.isNotEmpty(code)) {
            SqlEditorUtils.replaceSelect(sqlEditor, "");
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        String val = clipboard.getString();
        if (StringUtils.isNotEmpty(val)) {
            SqlEditorUtils.insertSqlCode(sqlEditor, val);
        }
    }

    public void onSelectAll(ActionEvent actionEvent) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        // 获取最后一行信息
        int lineCount = SqlEditorUtils.getLineCount(sqlEditor);
        String lastLineInfo = SqlEditorUtils.getLine(sqlEditor, lineCount - 1);
        SqlEditorUtils.select(sqlEditor, 0, 0, lineCount-1, lastLineInfo.length());
    }

    /**
     * 保存SQL到文件
     * @param connectTab
     * @param sqlTab
     */
    private void saveSqlToFile(Tab connectTab, Tab sqlTab, boolean saveOther) {
        SqlControlDto sqlControlDto = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab);
        if (null == sqlControlDto) {
            return;
        }

        File file;
        if (StringUtils.isBlank(sqlControlDto.getSqlFilePath()) || saveOther) {
            FileChooser fc = new FileChooser();
            if (StringUtils.isBlank(exportPath)) {
                fc.setInitialDirectory(new File("."));
            } else {
                fc.setInitialDirectory(new File(exportPath));
            }
            fc.setTitle("另存为");
            fc.setInitialFileName(null);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));

            file = fc.showSaveDialog(mainTabPanel.getScene().getWindow());
            if (null != file) {
                exportPath = file.getParentFile().getAbsolutePath();
                // 设置文件路径与SQL页签关系
                sqlControlDto.setSqlFilePath(file.getAbsolutePath());
            }
        } else {
            file = new File(sqlControlDto.getSqlFilePath());
        }
        if (null == file) {
            return;
        }

        // 数据写入
        BufferedOutputStream out = null;
        try{
            out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] b = SqlEditorUtils.getAllSqlCode(sqlControlDto.getSqlCodeView()).getBytes(StandardCharsets.UTF_8);
            out.write(b,0,b.length);
            out.flush();
        }catch(IOException ex){
            ex.printStackTrace();
            MessageAlert.error("保存SQL系统异常:" + ex.getMessage());
            return;
        } finally {
            IoUtil.close(out);
        }

        // 修改状态置为否
        sqlControlDto.setModified(false);
        sqlTab.setTooltip(CommonUtil.toolTip(sqlControlDto.getSqlFilePath(), 12));
        ((Label)sqlTab.getGraphic()).setText(FileNameUtil.getName(sqlControlDto.getSqlFilePath()));
    }

    /**
     * 保存SQL
     * @param actionEvent
     */
    public void onSaveSQL(ActionEvent actionEvent) {
        // 先查找当前选中的数据库连接
        Tab connectTab = mainTabPanel.getSelectionModel().getSelectedItem();
        if (null == connectTab || connectTab == newConnnectTab) {
            return;
        }

        // 查找选中的SQL页签
        Tab sqlTab = connectConrolMap.get(connectTab).getSqlTabPane().getSelectionModel().getSelectedItem();
        if (null == sqlTab) {
            return;
        }

        saveSqlToFile(connectTab, sqlTab, false);
    }

    public void onSaveSQLAs(ActionEvent actionEvent) {
        // 先查找当前选中的数据库连接
        Tab connectTab = mainTabPanel.getSelectionModel().getSelectedItem();
        if (null == connectTab || connectTab == newConnnectTab) {
            return;
        }

        // 查找选中的SQL页签
        Tab sqlTab = connectConrolMap.get(connectTab).getSqlTabPane().getSelectionModel().getSelectedItem();
        if (null == sqlTab) {
            return;
        }

        saveSqlToFile(connectTab, sqlTab, true);
    }

    public void onOpenSQL(ActionEvent actionEvent) {
        // 先查找当前选中的数据库连接
        Tab connectTab = mainTabPanel.getSelectionModel().getSelectedItem();
        if (null == connectTab || connectTab == newConnnectTab) {
            return;
        }

        FileChooser fc = new FileChooser();
        if (StringUtils.isBlank(exportPath)) {
            fc.setInitialDirectory(new File("."));
        } else {
            fc.setInitialDirectory(new File(exportPath));
        }
        fc.setTitle("打开SQL文件");
        fc.setInitialFileName(null);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));

        File file = fc.showOpenDialog(mainTabPanel.getScene().getWindow());
        if (null == file) {
            return;
        }

        String val = null;
        try{
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer =new byte[1024*8];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(int len =0; (len =in.read(buffer)) >0;) {
                baos.write(buffer, 0, len);
            }
            // 识别文件编码
            Charset charset = CharsetDetector.detect(file);
            val = baos.toString(CommonUtil.getValue(charset, StandardCharsets.UTF_8));
        }catch(IOException ex){
            ex.printStackTrace();
            MessageAlert.error("打开SQL文件异常:" + ex.getMessage());
            return;
        }

        // 新增一个Sql页签
        Tab sqlTab = addSqlTab(connectTab, val);
        SqlControlDto sqlControlDto = connectConrolMap.get(connectTab).getSqlControlMap().get(sqlTab);
        exportPath = file.getParentFile().getAbsolutePath();
        // 设置文件路径与SQL页签关系
        sqlControlDto.setSqlFilePath(file.getAbsolutePath());
        sqlControlDto.setModified(false);
        sqlTab.setTooltip(CommonUtil.toolTip(sqlControlDto.getSqlFilePath(), 12));
        ((Label)sqlTab.getGraphic()).setText(FileNameUtil.getName(sqlControlDto.getSqlFilePath()));
    }

    /**
     * 代码格式化
     * @param actionEvent
     */
    public void onFormat(ActionEvent actionEvent) {
        formatSql(false);
    }

    public void onFormatAll(ActionEvent actionEvent) {
        formatSql(true);
    }

    private void formatSql(boolean isAll) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        String sql;
        if (isAll) {
            sql = SqlEditorUtils.getAllSqlCode(sqlEditor);
        } else {
            sql = SqlEditorUtils.getSelectSqlCode(sqlEditor);
        }
        if (StringUtils.isBlank(sql)) {
            return;
        }

        sql = SqlFormatter.format(sql);
        if (StringUtils.isBlank(sql)) {
            return;
        }

        // 格式化框架存在BUG，会把!=分开，此处再进行合并
        sql = StringUtils.replace(sql, "! =", "!=");

        if (isAll) {
            SqlEditorUtils.setSqlCode(sqlEditor, sql);
        } else {
            SqlEditorUtils.replaceSelect(sqlEditor, sql);
        }
    }

    /**
     * 配置页面
     * @param actionEvent
     */
    public void onConfig(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("config.fxml"));
            Parent root = fxmlLoader.load();
            ConfigController controller = fxmlLoader.getController();
            controller.setParent(this);
            controller.init(config);
            Stage stage = new Stage();
            stage.setTitle("选项");
            Scene scene = new Scene(root, 276, 254);
            stage.setScene(scene);
            Stage parent = (Stage) mainTabPanel.getScene().getWindow();
            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.getIcons().add(new Image("config.png"));
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error("系统异常");
        }
    }

    public void onSelectCodeUpper(ActionEvent actionEvent) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        String code = SqlEditorUtils.getSelectSqlCode(sqlEditor);
        if (StringUtils.isBlank(code)) {
            return;
        }
        code = StringUtils.upperCase(code);
        SqlEditorUtils.replaceSelect(sqlEditor, code);
    }

    public void onSelectCodeLower(ActionEvent actionEvent) {
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        String code = SqlEditorUtils.getSelectSqlCode(sqlEditor);
        if (StringUtils.isBlank(code)) {
            return;
        }
        code = StringUtils.lowerCase(code);
        SqlEditorUtils.replaceSelect(sqlEditor, code);
    }

    public void onFindCode(ActionEvent actionEvent) {
        if (findingOrReplacing) {
            // 不允许同时展示多个查找或替换窗口
            return;
        }
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        sqlEditor.requestFocus();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("search-or-replace.fxml"));
            Parent root = fxmlLoader.load();
            SearchReplaceController controller = fxmlLoader.getController();
            controller.setSqlCodeEditor(sqlEditor);
            controller.setParent(this);
            controller.init(true);
            Stage stage = new Stage();
            stage.setTitle("查找");
            Scene scene = new Scene(root, 478, 170);
            stage.setScene(scene);
            Stage parent = (Stage) mainTabPanel.getScene().getWindow();
            stage.initOwner(parent);
            stage.getIcons().add(new Image("find.png"));
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.setOnCloseRequest(event -> {
                findingOrReplacing = false;
            });
            stage.show();
            findingOrReplacing = true;
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error("系统异常");
        }
    }

    public void onReplaceCode(ActionEvent actionEvent) {
        if (findingOrReplacing) {
            // 不允许同时展示多个查找或替换窗口
            return;
        }
        WebView sqlEditor = getActiveSqlEditor();
        if (null == sqlEditor) {
            return;
        }
        sqlEditor.requestFocus();
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("search-or-replace.fxml"));
            Parent root = fxmlLoader.load();
            SearchReplaceController controller = fxmlLoader.getController();
            controller.setSqlCodeEditor(sqlEditor);
            controller.setParent(this);
            controller.init(false);
            Stage stage = new Stage();
            stage.setTitle("替换");
            Scene scene = new Scene(root, 478, 190);
            stage.setScene(scene);
            Stage parent = (Stage) mainTabPanel.getScene().getWindow();
            stage.initOwner(parent);
            stage.getIcons().add(new Image("replace.png"));
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.setOnCloseRequest(event -> {
                findingOrReplacing = false;
            });
            stage.show();
            findingOrReplacing = true;
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error("系统异常");
        }
    }

    private void showGenJavaView(DatasouceTableVO selectedItem) {
        if (CollectionUtils.isEmpty(selectedItem.getTypes())) {
            MessageAlert.warning("当前表暂不支持生成Java对象");
            return;
        }
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("gen-java-view.fxml"));
            Parent root = fxmlLoader.load();
            GenJavaViewController controller = fxmlLoader.getController();
            controller.init(selectedItem);
            Stage stage = new Stage();
            stage.setTitle("生成Java对象");
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            Stage parent = (Stage) mainTabPanel.getScene().getWindow();
            stage.initOwner(parent);
            stage.getIcons().add(new Image("java.png"));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            MessageAlert.error("系统异常");
        }
    }

    /**
     * SQL编辑器代码变更事件监听器
     */
    public static class SqlCodeChangeListener {
        private final Map<Tab, ConnectControlDto> connectConrolMap;
        private final Map<String, Tab> sqlCodeId2ConnTabMap;

        public SqlCodeChangeListener(Map<Tab, ConnectControlDto> connectConrolMap, Map<String, Tab> sqlCodeId2ConnTabMap) {
            this.connectConrolMap = connectConrolMap;
            this.sqlCodeId2ConnTabMap = sqlCodeId2ConnTabMap;
        }

        /**
         * SQL代码发生变更
         * @param uid 代码编辑器ID
         */
        public void changed(String uid) {
            if (StringUtils.isBlank(uid)) {
                return;
            }
            if (!sqlCodeId2ConnTabMap.containsKey(uid)) {
                return;
            }
            if (MapUtils.isEmpty(connectConrolMap)) {
                return;
            }
            ConnectControlDto connectControlDto = connectConrolMap.get(sqlCodeId2ConnTabMap.get(uid));
            if (null == connectControlDto) {
                return;
            }
            Tab sqlTab = connectControlDto.getSqlCodeIdMap().get(uid);
            if (null == sqlTab) {
                return;
            }
            SqlControlDto sqlControlDto = connectControlDto.getSqlControlMap().get(sqlTab);
            if (null == sqlControlDto) {
                return;
            }
            if (StringUtils.isBlank(sqlControlDto.getSqlFilePath())) {
                // SQL编辑器没有关联到文件路径,则不进行处理
                return;
            }
            // 如果已经关联到SQL文件，则标记为已修改
            sqlControlDto.setModified(true);
            if (!StringUtils.endsWith(((Label)sqlTab.getGraphic()).getText(), "*")) {
                ((Label)sqlTab.getGraphic()).setText(((Label)sqlTab.getGraphic()).getText() + " *");
            }
        }
    }
}