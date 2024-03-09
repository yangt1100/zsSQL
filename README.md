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

![main.png](screenshoot%2Fmain.png)![主界面截图](./img/cacheproxy架构图.jpg)

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
