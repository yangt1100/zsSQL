# zsSQL (This SQL)

zsSQL(This SQL的简称)是一个基于JavaFx开发的轻量级MySQL可视化客户端, 界面及操作逻辑与市面上流行的MySQL客户端SQLyog基本保持一致

JDK版本: 17+

启动类：com.zssql.Main

JVM参数：
--add-exports=javafx.controls/com.sun.javafx.scene.control.behavior=com.zssql 
--add-exports=javafx.base/com.sun.javafx.event=com.zssql 
--add-opens=javafx.controls/javafx.scene.control.skin=com.zssql 
--add-opens=javafx.graphics/javafx.scene=com.zssql 
--add-exports=javafx.graphics/com.sun.javafx.css=com.zssql 
--add-exports=javafx.graphics/com.sun.javafx.scene=com.zssql 
--add-exports=javafx.graphics/com.sun.javafx.scene.traversal=com.zssql

## 截图快照

主界面

![main.png](screenshoot%2Fmain.png)

SQL脚本着色及格式化

![sql_format.png](screenshoot%2Fsql_format.png)

SQL代码提示

![codetip.png](screenshoot%2Fcodetip.png)

单列模式

![row_model.PNG](screenshoot%2Frow_model.PNG)

表自动生成JavaBean

![javabean.png](screenshoot%2Fjavabean.png)

系统配置

![config.png](screenshoot%2Fconfig.png)

## 第三方依赖
- [controlsfx](https://github.com/controlsfx/controlsfx)  高级控件
- [CodeMirror](https://github.com/codemirror/codemirror5) 代码着色及提示
- [hutool](https://github.com/dromara/hutool)
- [druid](https://github.com/apache/druid)
- [easyexcel](https://github.com/alibaba/easyexcel)
- [fastjson](https://github.com/alibaba/fastjson)
- [sql-formatter](https://github.com/vertical-blank/sql-formatter)
- ......

## 待实现及完善功能
1. 表(Table)修改功能只实现一个雏形，相关DDL脚本生成功能待实现
2. SQL查询结果暂不支持修改，后续计划实现
3. 表数据结果目前只实现行数据编辑功能，更多修改及提交功能待后续实现


