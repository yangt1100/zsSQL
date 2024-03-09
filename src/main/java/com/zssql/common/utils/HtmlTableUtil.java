package com.zssql.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class HtmlTableUtil {

    public static String htmlTable(List<String> headers, List<List<String>> values, String title, boolean widthFill) {
        return htmlTable(null, true, headers, values, title, widthFill);
    }

    public static String htmlTable(String tips, boolean showHeader, List<String> headers, List<List<String>> values, String title, boolean widthFill) {
        StringBuilder html = new StringBuilder();
        html.append("<html>");
        html.append("<head>");
        html.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        html.append("<title>").append(CommonUtil.getValue(title, "HTML VIEW")).append("</title>");
        html.append("<style type=\"text/css\"> <!--\n" +
                ".normal {  font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 12px; font-weight: normal; color: #000000}\n" +
                ".medium {  font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 15px; font-weight: bold; color: #000000; text-decoration: none}\n" +
                "table.gridtable { \n" +
                "\tfont-family: verdana,arial,sans-serif; \n" +
                "\tfont-size:14px; \n" +
                "\tcolor:#000000; \n" +
                "\tborder-width: 1px; \n" +
                "\tborder-color: #666666; \n" +
                "\tborder-collapse: collapse; \n" +
                "} \n" +
                "table.gridtable th { \n" +
                "\tborder-width: 1px; \n" +
                "\tpadding: 8px; \n" +
                "\tborder-style: solid; \n" +
                "\tborder-color: #666666; \n" +
                "\tbackground-color: #dedede; \n" +
                "} \n" +
                "table.gridtable td { \n" +
                "\tborder-width: 1px; \n" +
                "\tpadding: 8px; \n" +
                "\tborder-style: solid; \n" +
                "\tborder-color: #666666; \n" +
                "\tbackground-color: #ffffff; \n" +
                "} \n" +
                "--></style>");
        html.append("</head>");
        html.append("<body>");
        if (StringUtils.isNotBlank(tips)) {
            html.append("<h2>").append(tips).append("</h2>");
        }
        if (widthFill) {
            html.append("<table border=1 class=\"gridtable\" width=\"95%\">");
        } else {
            html.append("<table border=1 class=\"gridtable\">");
        }

        if (showHeader) {
            // 表头
            for (String header : headers) {
                html.append("<th bgcolor=silver class='medium'>");
                html.append(header);
                html.append("</th>");
            }
        }

        // 行数据拼接
        for (List<String> value : values) {
            html.append("<tr>");
            for (String v : value) {
                html.append("<td class='normal' valign='top'>").append(v).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }
}
