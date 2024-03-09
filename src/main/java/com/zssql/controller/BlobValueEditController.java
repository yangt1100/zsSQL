package com.zssql.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.google.googlejavaformat.java.Formatter;
import com.zssql.HelloApplication;
import com.zssql.common.utils.CommonUtil;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.lang3.StringUtils;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

public class BlobValueEditController {
    public TextArea blobValueEdit;
    public HBox topHBox;
    public WebView codeView;

    public Button formatBtn;
    public Button saveBtn;
    public Button cancelBtn;

    private enum ShowType {RAW, JSON, XML, SQL, JAVA}

    private ShowType currShowType = ShowType.RAW;

    public void init(Object val) {
        saveBtn.setGraphic(new ImageView(new Image("ok.png")));
        cancelBtn.setGraphic(new ImageView(new Image("cancel.png")));
        CommonUtil.hideControl(codeView);
        blobValueEdit.setEditable(true);
        if (null != val) {
            blobValueEdit.setText(val.toString());
            setCode(val.toString(), null);
        } else {
            setCode("", null);
        }

        ToggleGroup showTypeGrp = new ToggleGroup();
        ToggleButton rawBtn = new ToggleButton("RAW");
        rawBtn.setGraphic(new ImageView(new Image("raw.png")));
        ToggleButton jsonBtn = new ToggleButton("JSON");
        jsonBtn.setGraphic(new ImageView(new Image("json.png")));
        ToggleButton xmlBtn = new ToggleButton("XML");
        xmlBtn.setGraphic(new ImageView(new Image("xml.png")));
        ToggleButton sqlBtn = new ToggleButton("SQL");
        sqlBtn.setGraphic(new ImageView(new Image("sql-f.png")));
        ToggleButton javaBtn = new ToggleButton("JAVA");
        javaBtn.setGraphic(new ImageView(new Image("java.png")));
        showTypeGrp.getToggles().addAll(rawBtn, jsonBtn, xmlBtn, sqlBtn, javaBtn);
        rawBtn.setSelected(true);

        showTypeGrp.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (null == newValue) {
                if (null != oldValue) {
                    oldValue.setSelected(true);
                    //if ()
                } else {
                    rawBtn.setSelected(true);
                    showBlobValue(val, ShowType.RAW);
                    return;
                }
            }
            if (newValue == rawBtn) {
                showBlobValue(val, ShowType.RAW);
            } else if (newValue == jsonBtn) {
                showBlobValue(val, ShowType.JSON);
            } else if (newValue == xmlBtn) {
                showBlobValue(val, ShowType.XML);
            } else if (newValue == sqlBtn) {
                showBlobValue(val, ShowType.SQL);
            } else if (newValue == javaBtn) {
                showBlobValue(val, ShowType.JAVA);
            }
        });

        formatBtn = new Button(null);
        formatBtn.setGraphic(new ImageView(new Image("format.png")));
        formatBtn.setTooltip(CommonUtil.toolTip("格式化", 11, Duration.ONE));
        formatBtn.setVisible(false);

        CommonUtil.buttonBind(formatBtn, this::format);

        Separator sep = new Separator(Orientation.HORIZONTAL);
        sep.setVisible(false);
        HBox.setHgrow(sep, Priority.ALWAYS);
        topHBox.getChildren().addAll(rawBtn, jsonBtn, xmlBtn, sqlBtn, javaBtn, new Label("    更多格式敬请期待..."), sep, formatBtn);

        showBlobValue(val, ShowType.RAW);
        blobValueEdit.requestFocus();
    }

    /**
     * 格式化数据
     */
    private void format() {
        if (null == currShowType || ShowType.RAW == currShowType) {
            return;
        }
        String code = getCode();
        if (StringUtils.isBlank(code)) {
            return;
        }
        switch (currShowType) {
            case JSON:
                try {
                    setCode(JSON.toJSONString(JSON.parse(code), SerializerFeature.PrettyFormat), currShowType);
                } catch (Exception e) {
                }
                break;
            case XML:
                try {
                    Source xmlInput = new StreamSource(new StringReader(code));
                    StringWriter stringWriter = new StringWriter();
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
                    transformer.setOutputProperty("{https://xml.apache.org/xslt}indent-amount", "2");
                    transformer.transform(xmlInput, new StreamResult(stringWriter));

                    setCode(stringWriter.toString().trim(), currShowType);
                } catch (Exception e) {
                }
                break;
            case SQL:
                try {
                    setCode(SqlFormatter.format(code), currShowType);
                } catch (Exception e) {
                }
                break;
            case JAVA:
                try {
                    setCode(new Formatter().formatSource(code), currShowType);
                } catch (Exception e) {
                }
                break;
        }
    }

    private void showBlobValue(Object val, ShowType type) {
        currShowType = type;
        if (ShowType.RAW == type) {
            CommonUtil.showControl(blobValueEdit);
            CommonUtil.hideControls(codeView, formatBtn);
            blobValueEdit.requestFocus();
            return;
        }

        CommonUtil.showControls(codeView, formatBtn);
        CommonUtil.hideControl(blobValueEdit);
        codeView.requestFocus();

        String code = getCode();
        if (StringUtils.isBlank(code)) {
            return;
        }

        setCode(code, type);
    }

    private String applyEditingTemplate(String code, ShowType showType) {
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
        showType = CommonUtil.getValue(showType, ShowType.RAW);
        String mimeType = "application/json";
        switch (showType) {
            case JSON:
                mimeType = "application/json";
                break;
            case XML:
                mimeType = "text/html";
                break;
            case SQL:
                mimeType = "text/x-mysql";
                break;
            case JAVA:
                mimeType = "text/x-java";
                break;
        }
        htmlTemplate = htmlTemplate.replace("${languageType}", mimeType);
        htmlTemplate = htmlTemplate.replace("${code}", code);
        htmlTemplate = htmlTemplate.replace("${readonlyflg}", "false");
        return htmlTemplate;
    }

    public void setCode(String code, ShowType showType) {
        codeView.getEngine().loadContent(applyEditingTemplate(code, showType));
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
        Stage stage = (Stage) blobValueEdit.getScene().getWindow();
        stage.close();
    }
}
