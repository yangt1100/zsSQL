package com.zssql.controller;

import com.mysql.cj.MysqlType;
import com.zssql.HelloApplication;
import com.zssql.domain.vo.DatasouceTableVO;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class GenJavaViewController {
    @FXML
    private CheckBox camelCaseCheckBox;
    @FXML
    private WebView codeView;
    @FXML
    private CheckBox commentCheckBox;
    @FXML
    private CheckBox lombokCheckBox;

    @Setter
    private DatasouceTableVO tableVO;

    public void init(DatasouceTableVO tableVO) {
        this.tableVO = tableVO;
        camelCaseCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            refreshJavaCode();
        });
        lombokCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            refreshJavaCode();
        });
        commentCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            refreshJavaCode();
        });

        refreshJavaCode();
    }

    /**
     * 生成Java代码
     */
    private void refreshJavaCode() {
        boolean camelCaseFlg = camelCaseCheckBox.isSelected();
        boolean lombokFlg = lombokCheckBox.isSelected();
        boolean commentFlg = commentCheckBox.isSelected();

        StringBuilder header = new StringBuilder();
        StringBuilder field = new StringBuilder();
        StringBuilder gsFnc = new StringBuilder();

        header.append("package com.zssql.entity;\n\n");
        Set<String> classNames = new HashSet<>();
        if (lombokFlg) {
            // lombok注解
            field.append("@Data\n");
            classNames.add("lombok.Data");
        }
        // 类型
        field.append(String.format("public class %s implements Serializable {\n", genClassName()));
        // 序列化字段
        field.append("    private static final long serialVersionUID = 1L;\n\n");
        // 字段循环处理
        classNames.add("java.io.Serializable");
        for (int i = 0; i < tableVO.getColumns().size(); i++) {
            String column = tableVO.getColumns().get(i);
            if (camelCaseFlg) {
                // 驼峰命名
                column = key2Camel(column);
            }
            String comment = CollectionUtils.isEmpty(tableVO.getComments())? null : tableVO.getComments().get(i);
            String type = tableVO.getTypes().get(i);
            MysqlType mysqlType = MysqlType.getByName(type);
            String className = "java.lang.String";
            if (null != mysqlType) {
                className = mysqlType.getClassName();
            }
            if (StringUtils.equalsIgnoreCase(type, "DATETIME")) {
                className = "java.time.LocalDateTime";
            } else if (StringUtils.equalsIgnoreCase(type, "DATE")) {
                className = "java.time.LocalDate";
            } else if (StringUtils.equalsIgnoreCase(type, "TIME")) {
                className = "java.time.LocalTime";
            }
            if (commentFlg && StringUtils.isNotBlank(comment)) {
                // 注释
                field.append("    // ").append(comment).append("\n");
            }
            String simpleClassName = simpleClassName(className);
            String packageName = classPackage(className);
            // 字段
            field.append("    private ").append(simpleClassName).append(" ").append(column).append(";\n");
            // Get Set方法
            if (!lombokFlg) {
                if (StringUtils.isBlank(gsFnc)) {
                    gsFnc.append("\n");
                }
                // GET方法
                gsFnc.append("    public ").append(simpleClassName).append(" ").append(nameOfGet(column, simpleClassName)).append("() {\n");
                gsFnc.append("        return ").append(column).append(";\n");
                gsFnc.append("    }\n\n");
                // SET方法
                gsFnc.append("    public void ").append(nameOfSet(column, simpleClassName)).append("(").append(simpleClassName).append(" ").append(column).append(") {\n");
                gsFnc.append("        this.").append(column).append(" = ").append(column).append(";\n");
                gsFnc.append("    }\n\n");
            }

            // 包引入
            if (!StringUtils.equals(packageName, "java.lang")) {
                classNames.add(className);
            }
        }

        for (String className : classNames.stream().sorted().toList()) {
            header.append("import ").append(className).append(";\n");
        }

        String javaCode = header.toString().trim() + "\n\n" + field.toString().trim() + "\n" + gsFnc + "}";

        setCode(javaCode);
    }

    private String nameOfGet(String column, String simpleClassName) {
        if (StringUtils.startsWithIgnoreCase(column, "is") && StringUtils.equalsIgnoreCase(simpleClassName, "Boolean")) {
            return column;
        }

        return "get" + StringUtils.left(StringUtils.upperCase(column), 1) + StringUtils.substring(column, 1);
    }

    private String nameOfSet(String column, String simpleClassName) {
        return "set" + StringUtils.left(StringUtils.upperCase(column), 1) + StringUtils.substring(column, 1);
    }

    private String classPackage(String className) {
        if (StringUtils.isBlank(className)) {
            return null;
        }
        int pos = StringUtils.lastIndexOf(className, ".");
        if (pos < 0) {
            return className;
        }
        return StringUtils.left(className, pos);
    }

    private String simpleClassName(String className) {
        if (StringUtils.isBlank(className)) {
            return null;
        }
        int pos = StringUtils.lastIndexOf(className, ".");
        if (pos < 0) {
            return className;
        }
        return StringUtils.substring(className, pos + 1);
    }

    private String genClassName() {
        String className = key2Camel(tableVO.getTableName());
        return StringUtils.upperCase(StringUtils.left(className, 1)) + StringUtils.substring(className, 1);
    }

    private static String key2Camel(String key) {
        if (StringUtils.isBlank(key)) {
            return key;
        }

        StringBuilder ckey = new StringBuilder();
        boolean upper = false;
        for (int i=0; i<key.length(); i++) {
            char c = key.charAt(i);
            if ('_' == c) {
                upper = true;
            } else {
                if (upper) {
                    ckey.append(StringUtils.upperCase(String.valueOf(c)));
                } else {
                    ckey.append(c);
                }
                upper = false;
            }
        }
        return ckey.toString();
    }

    private String applyEditingTemplate(String code) {
        String htmlTemplate = null;
        try {
            InputStream in = HelloApplication.class.getResource("CodeEditor.html").openStream();
            byte[] buffer = new byte[1024*8];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for(int len =0; (len =in.read(buffer)) >0;) {
                baos.write(buffer, 0, len);
            }
            htmlTemplate = baos.toString("utf-8");
            baos.flush();
            baos.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StringUtils.isBlank(htmlTemplate)) {
            return "<html><body><h2>加载异常</h2><body></html>";
        }
        String mimeType = "text/x-java";
        htmlTemplate = htmlTemplate.replace("${languageType}", mimeType);
        htmlTemplate = htmlTemplate.replace("${code}", code);
        htmlTemplate = htmlTemplate.replace("${readonlyflg}", "true");
        return htmlTemplate;
    }

    public void setCode(String code) {
        codeView.getEngine().loadContent(applyEditingTemplate(code));
    }

    public String getCode() {
        return (String) codeView.getEngine().executeScript("editor.getValue();");
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ESCAPE) {
            close();
        }
    }

    private void close() {
        Stage stage = (Stage) codeView.getScene().getWindow();
        stage.close();
    }
}
