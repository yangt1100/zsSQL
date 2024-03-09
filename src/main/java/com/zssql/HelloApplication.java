package com.zssql;

import com.alibaba.fastjson.JSON;
import com.zssql.common.utils.SQLiteProvider;
import com.zssql.domain.dto.Config;
import com.zssql.domain.entity.sqlite.MainWindowPostion;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.lang3.BooleanUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        SQLiteProvider.init();
        MainWindowPostion postion = SQLiteProvider.getMainWindowPostion();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
        HelloController controller = fxmlLoader.getController();
        stage.setTitle("zsSQL");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(400);
        stage.getIcons().add(new Image("icon.png"));

        Config config = loadConfig();
        if (null == config) {
            config = new Config();
        }
        if (BooleanUtils.isTrue(config.getRestoreWindowPosAfterStart()) && null != postion) {
            // 恢复上一次窗口位置
            if (1 == postion.getFullScreen()) {
                stage.setMaximized(true);
            } else {
                stage.setMaximized(false);
                stage.setX(postion.getX());
                stage.setY(postion.getY());
                stage.setWidth(postion.getWidth());
                stage.setHeight(postion.getHeight());
            }

        }
        stage.show();
        controller.init(config);
    }

    private Config loadConfig() {
        // 读取快照文件
        File file = new File("zssql.cfg");
        if (!file.exists()) {
            return null;
        }

        Config cfg;
        try{
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer =new byte[1024*8];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(int len =0; (len =in.read(buffer)) >0;) {
                baos.write(buffer, 0, len);
            }
            in.close();
            String val = baos.toString(StandardCharsets.UTF_8);
            cfg = JSON.parseObject(val, Config.class);
        } catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
        return cfg;
    }

    public static void main(String[] args) {
        launch();
    }
}