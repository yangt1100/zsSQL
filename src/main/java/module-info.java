module com.zssql {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;
    requires commons.collections;
    requires commons.lang3;
    requires fastjson;
    requires java.sql;
    requires cn.hutool;
    requires sqlite.jdbc;
    requires druid;
    requires swagger.annotations;
    requires java.naming;
    requires java.management;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires javafx.web;
    requires poi;
    requires poi.ooxml;
    requires easyexcel.core;
    requires jdk.jsobject;
    requires sql.formatter;
    requires mysql.connector.java;
    requires javafx.media;
    requires java.net.http;
    requires java.desktop;
    requires com.google.googlejavaformat;

    opens com.zssql to javafx.fxml;
    opens com.zssql.controller to javafx.fxml;
    exports com.zssql;
    exports com.zssql.common.enums;
    exports com.zssql.controller;
    exports com.zssql.domain.entity.sqlite;
    exports com.zssql.domain.dto.sqlite;
    exports com.zssql.domain.dto;
    exports com.zssql.common.utils;
    exports com.zssql.domain.vo;
    exports com.zssql.domain.dto.snapshot;
    exports com.zssql.domain.dto.ddl;
    exports com.zssql.common.cutom;
}