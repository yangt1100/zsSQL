package com.zssql.common.utils;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zssql.common.enums.MySQLTypeEnum;
import com.zssql.common.handler.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.util.Duration;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.controlsfx.control.spreadsheet.SpreadsheetCell;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CommonUtil {
    /**
     * 判断字符串是否为数字
     * @param str
     * @return
     */
    public static boolean isNumeric(final String str){
        try{
            BigDecimal bd = new BigDecimal(str);
        }catch (Exception ex){
            return false;
        }

        return true;
    }

    /**
     * 判断是否有任一字符串为空
     * @param strs
     * @return
     */
    public static boolean isAnyStringBlank(String ...strs){
        if (strs.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }

        return Arrays.stream(strs).anyMatch(str -> StringUtils.isBlank(str));
    }

    /**
     * 判断是否所有字符串都为空
     * @param strs
     * @return
     */
    public static boolean isAllStringBlank(String ...strs){
        if (strs.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }

        return Arrays.stream(strs).allMatch(str -> StringUtils.isBlank(str));
    }

    /**
     * 判断对象是否为null
     * @param obj
     * @return
     */
    public static boolean isObjectNull(Object obj){
        return null == obj;
    }

    /**
     * 判断是否有任一对象为null
     * @param objs
     * @return
     */
    public static boolean isAnyObjectNull(Object ...objs){
        if (objs.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }

        return Arrays.stream(objs).anyMatch(obj -> isObjectNull(obj));
    }

    /**
     * 判断是否所有对象都为null
     * @param objs
     * @return
     */
    public static boolean isAllObjectNull(Object ...objs){
        if (objs.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }
        return Arrays.stream(objs).allMatch(obj -> isObjectNull(obj));
    }

    /**
     * 判断是否有任一对象不为null
     * @param objs
     * @return
     */
    public static boolean isAnyObjectNotNull(Object ...objs){
        if (objs.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }
        return Arrays.stream(objs).anyMatch(obj -> !isObjectNull(obj));
    }

    /**
     * 判断是否所有对象都不为null
     * @param objs
     * @return
     */
    public static boolean isAllObjectNotNull(Object ...objs){
        if (objs.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }
        return Arrays.stream(objs).allMatch(obj -> !isObjectNull(obj));
    }

    /**
     * 判断是否有任一集合为空
     * @param vals
     * @param <T>
     * @return
     */
    public static <T extends Collection> boolean isAnyCollectionEmpty(T ...vals) {
        if (vals.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }
        return Arrays.stream(vals).anyMatch(CollectionUtils::isEmpty);
    }

    /**
     * 判断是否所有集合都为空
     * @param vals
     * @param <T>
     * @return
     */
    public static <T extends Collection> boolean isAllCollectionEmpty(T ...vals) {
        if (vals.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }
        return Arrays.stream(vals).allMatch(CollectionUtils::isEmpty);
    }

    /**
     * 判断是否有任一集合不为空
     * @param vals
     * @param <T>
     * @return
     */
    public static <T extends Collection> boolean isAnyCollectionNotEmpty(T ...vals) {
        if (vals.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }
        return Arrays.stream(vals).anyMatch(CollectionUtils::isNotEmpty);
    }

    /**
     * 判断是否所有集合都不为空
     * @param vals
     * @param <T>
     * @return
     */
    public static <T extends Collection> boolean isAllCollectionNotEmpty(T ...vals) {
        if (vals.length <= 0){
            throw new IllegalArgumentException("参数异常");
        }
        return Arrays.stream(vals).allMatch(CollectionUtils::isNotEmpty);
    }

    /**
     * 判断日期是否有效
     * @param compareDate   比较日期
     * @param validDate     生效日期
     * @param invalidDate   失效日期
     * @return
     */
    public static boolean isDateValid(Date compareDate, Date validDate, Date invalidDate){
        if (null == compareDate){
            return false;
        }

        if (null != validDate && validDate.compareTo(compareDate) >= 0){
            return false;
        }else if (null != invalidDate && invalidDate.compareTo(compareDate) < 0){
            return false;
        }

        return true;
    }

    /**
     * 判断对象是否相等
     * @param o1
     * @param o2
     * @return
     */
    public static <T> boolean isObjectEquals(T o1, T o2){
        if (null == o1 && null == o2){
            return true;
        }else if (null == o1 && null != o2){
            return false;
        }else if (null != o1 && null == o2){
            return false;
        }

        // 判断是否为原始类型
        if (o1.getClass().isPrimitive() && o2.getClass().isPrimitive()){
            return o1 == o2;
        }

        // 判断是否为同一对象
        if (o1 == o2){
            return true;
        }

        // 判断是否实现了Comparable接口
        if (o1.getClass() == o2.getClass() && Comparable.class.isAssignableFrom(o1.getClass())) {
            return ((Comparable)o1).compareTo(o2) == 0;
        }

        // 判断类型是否匹配
        if (!o1.getClass().isAssignableFrom(o2.getClass()) && !o2.getClass().isAssignableFrom(o1.getClass())){
            // 类型不匹配，转换为字符串进行比较
            return o1.toString().equals(o2.toString());
        }

        return Objects.equals(o1, o2);
    }

    /**
     * 字符串是否相等
     * @param str1
     * @param str2
     * @param caseSensitive 是否区分大小写 true-区分 false-不区分
     * @return
     */
    public static boolean isStringEquals(String str1, String str2, boolean caseSensitive) {
        if (null == str1 && null == str2){
            return true;
        }else if (null == str1 && null != str2){
            return false;
        }else if (null != str1 && null == str2){
            return false;
        }

        if (caseSensitive) {
            return str1.equals(str2);
        }else {
            return str1.equalsIgnoreCase(str2);
        }
    }

    /**
     * 判断指定值是否在给定值列表中
     * @param target
     * @param inVals
     * @param <T>
     * @return
     */
    public static <T> boolean isIn(T target, T ...inVals){
        if (inVals.length <= 0){
            return false;
        }

        if (null == target){
            if (Arrays.stream(inVals).anyMatch(Objects::isNull)){
                return true;
            }
            return false;
        }

        return Arrays.stream(inVals).anyMatch(t -> isObjectEquals(target, t));
    }

    /**
     * 获取值，值为空时返回默认值
     * @param val
     * @param defaultVal
     * @return
     */
    public static <T> T getValue(T val, T defaultVal){
        if (null == val){
            return defaultVal;
        }

        if (val instanceof String && StringUtils.isBlank((String)val)){
            return defaultVal;
        }

        if (val instanceof Collection && CollectionUtils.isEmpty((Collection)val)){
            return defaultVal;
        }

        return val;
    }

    /**
     * 获取最大值
     * @param vals
     * @param <T>
     * @return
     */
    public static <T extends Comparable> T getMax(T ...vals) {
        if (vals.length <= 0){
            return null;
        }

        List<T> values = Arrays.stream(vals).filter(val -> !isObjectNull(val)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(values)){
            return null;
        }

        T max = values.get(0);
        for (T value : values) {
            if (value.compareTo(max) > 0){
                max = value;
            }
        }

        return max;
    }

    /**
     * 获取最小值
     * @param vals
     * @param <T>
     * @return
     */
    public static <T extends Comparable> T getMin(T ...vals) {
        if (vals.length <= 0){
            return null;
        }

        List<T> values = Arrays.stream(vals).filter(val -> !isObjectNull(val)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(values)){
            return null;
        }

        T min = values.get(0);
        for (T value : values) {
            if (value.compareTo(min) < 0){
                min = value;
            }
        }

        return min;
    }

    /**
     * 获取指定对象字段值
     * @param obj
     * @param fieldName
     * @return
     */
    public static Object getFieldValue(Object obj, String fieldName){
        if (null == obj || StringUtils.isBlank(fieldName)){
            return null;
        }

        Object val = null;
        if (obj instanceof Map){
            val = ((Map) obj).get(fieldName);
        }else{
            try{
                Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                val = field.get(obj);
            }catch (Exception e){
            }
        }

        return val;
    }

    /**
     * 设置指定对象字段值
     * @param obj
     * @param fieldName
     * @param val
     */
    public static void setFieldValue(Object obj, String fieldName, Object val){
        if (null == obj || StringUtils.isBlank(fieldName)){
            return;
        }

        try{
            if (obj instanceof Map){
                ((Map) obj).put(fieldName, val);
            }else{
                Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, val);
            }
        }catch (Exception e){
        }
    }

    /**
     * 批量设置字段值
     * @param objs
     * @param fieldName
     * @param val
     */
    public static List setFieldValues(List objs, String fieldName, Object val){
        if (CollectionUtils.isEmpty(objs) || StringUtils.isBlank(fieldName)){
            return objs;
        }

        for (Object obj : objs) {
            if (null == obj){
                continue;
            }
            setFieldValue(obj, fieldName, val);
        }

        return objs;
    }

    /**
     * 移除列表中指定字段值等于指定值的元素
     * @param objs
     * @param fieldName
     * @param val
     */
    public static void removeIfMatchField(List objs, String fieldName, Object val){
        if (CollectionUtils.isEmpty(objs) || StringUtils.isBlank(fieldName)){
            return;
        }

        Iterator iter = objs.iterator();
        while (iter.hasNext()){
            Object obj = iter.next();
            if (null != obj && isObjectEquals(getFieldValue(obj, fieldName), val)){
                // 移除符合条件的元素
                iter.remove();
            }
        }
    }

    /**
     * 本地缓存数据获取及处理
     * @param code      缓存key
     * @param cacheMap  缓存map
     * @param cacher    缓存未命中时获取新数据逻辑
     * @param <E>       key类型
     * @param <T>       value类型
     * @return          value值
     */
    public static <E, T> T getLocalCacheVal(E code, Map<E, T> cacheMap, LocalCacher<E, T> cacher){
        if (null == cacheMap){
            throw new IllegalArgumentException("param <cacheMap> is null");
        }

        if (cacheMap.containsKey(code)){
            // 本地缓存命中
            return cacheMap.get(code);
        }

        // 获取新数据
        T val = cacher.getValue(code);
        // 缓存新数据
        cacheMap.put(code, val);

        return val;
    }

    /**
     * 数据分组处理
     * @param code      分组代码
     * @param groupMap  分组Map
     * @param handler   分组数据生成逻辑
     * @param <E>       分组key类型
     * @param <T>       分组value类型
     */
    public static <E, T> void groupBy(E code, Map<E, List<T>> groupMap, GroupHandler<T> handler){
        if (null == groupMap){
            throw new IllegalArgumentException("param <groupMap> is null");
        }

        // 获取分组数据值
        T val = handler.getValue();
        if (null != val){
            // 判断分组key是否存在,不存在则新建
            if (!groupMap.containsKey(code)){
                groupMap.put(code, new ArrayList<>());
            }
            // 添加值到分组列表
            groupMap.get(code).add(val);
        }
    }

    /**
     * 数据转换
     * @param sources   源数据列表
     * @param handler   转换处理器
     * @param <E>       源数据类型
     * @param <T>       目标数据类型
     * @return          目标数据列表
     */
    public static <E, T> List<T> datasTransfer(List<E> sources, TransferHandler<E, T> handler){
        if (CollectionUtils.isEmpty(sources)){
            return Collections.emptyList();
        }

        List<T> targets = new ArrayList<>();
        for (E source : sources) {
            if (null != source){
                T val = handler.transfer(source);
                if (null != val){
                    targets.add(val);
                }
            }
        }

        return targets;
    }

    /**
     * 数据转换
     * @param sources  源数据列表
     * @param handler  转换处理器
     * @param <E>      源数据类型
     * @param <T>      目标数据类型
     * @return         目标数据列表
     */
    public static <E, T> List<T> dataFlatTransfer(List<E> sources, FlatTransferHandler<E, T> handler) {
        if (CollectionUtils.isEmpty(sources)){
            return Collections.emptyList();
        }

        List<T> targets = new ArrayList<>();
        for (E source : sources) {
            if (null != source){
                List<T> temps = new ArrayList<>();
                handler.transfer(source, record -> {
                    if (null != record) {
                        temps.add(record);
                    }
                });
                if (CollectionUtils.isNotEmpty(temps)){
                    targets.addAll(temps);
                }
            }
        }

        return targets;
    }

    /**
     * 列表遍历处理
     * @param datas         待处理列表
     * @param isParallel    是否并行处理
     * @param handler       遍历处理器
     * @param <T>           元素类型
     */
    public static <T> void forEach(Collection<T> datas, boolean isParallel, ForeachHandler<T> handler){
        if (CollectionUtils.isEmpty(datas)){
            return;
        }

        // 待处理数据个数为1时即使传入并行也会按照串行方式处理
        if (isParallel && datas.size() > 1){
            // 并行计算
            datas.parallelStream().forEach(data -> {
                if (null != data){
                    handler.handler(data);
                }
            });
        }else{
            // 串行计算
            datas.stream().forEach(data -> {
                if (null != data){
                    handler.handler(data);
                }
            });
        }
    }

    /**
     * 字符串拆分
     * @param source
     * @param pattern
     * @return
     */
    public static List<String> split(String source, String pattern){
        if (StringUtils.isEmpty(source)){
            return Collections.emptyList();
        }

        if (StringUtils.isEmpty(pattern)){
            return new ArrayList<>(Arrays.asList(source));
        }

        return new ArrayList<>(Arrays.asList(source.split(pattern)));
    }

    /**
     * 判断值是否在指定区间(左开右闭)
     * @param target
     * @param val1
     * @param val2
     * @param <T>
     * @return
     */
    public static <T extends Comparable> boolean isBetween(T target, T val1, T val2){
        if (null == target || null == val1 || null == val2){
            throw new IllegalArgumentException("has null param");
        }

        T min, max;
        if (val1.compareTo(val2) > 0){
            min = val2;
            max = val1;
        }else{
            min = val1;
            max = val2;
        }

        return target.compareTo(min) > 0 && target.compareTo(max) <= 0;
    }

    /**
     * BigDecimal值累加
     * @param objs
     * @param handler
     * @param <T>
     * @return
     */
    public static <T> BigDecimal bigDecimalSum(Collection<T> objs, BigDeciamSumHandler<T> handler) {
        if (CollectionUtils.isEmpty(objs)){
            return BigDecimal.ZERO;
        }

        BigDecimal value = BigDecimal.ZERO;
        for (T obj : objs) {
            if (null != obj){
                BigDecimal val = handler.value(obj);
                if (null != val){
                    value = value.add(val);
                }
            }
        }

        return value;
    }

    /**
     * BigDecimal值累加
     * @param objs
     * @param handler
     * @param <T>
     * @return
     */
    public static <T> BigDecimal bigDecimalFlagSum(Collection<T> objs, BigDeciamFlatSumHandler<T> handler) {
        if (CollectionUtils.isEmpty(objs)){
            return BigDecimal.ZERO;
        }

        BigDecimal[] value = {BigDecimal.ZERO};
        for (T obj : objs) {
            if (null != obj){
                handler.value(obj, (Collector<BigDecimal>) val -> {
                    if (null != val){
                        value[0] = value[0].add(val);
                    }
                });
            }
        }

        return value[0];
    }

    /**
     * json转换为map
     * @param jsonStr
     * @return
     */
    public static Map<String, Object> json2Map(String jsonStr) {
        Map<String, Object> paramsMap = JSON.parseObject(jsonStr, HashMap.class);
        if (MapUtils.isEmpty(paramsMap)){
            throw new IllegalArgumentException("请求参数格式错误");
        }

        return convert2Map(paramsMap);
    }

    /**
     * map转换
     * @param paramsMap
     * @return
     */
    private static Map<String, Object> convert2Map(Map<String, Object> paramsMap){
        if (MapUtils.isEmpty(paramsMap)){
            return paramsMap;
        }

        for (Map.Entry<String, Object> entry : paramsMap.entrySet()){
            if (entry.getValue() instanceof JSONObject) {
                Map<String, Object> tempMap = JSON.parseObject(JSON.toJSONString(entry.getValue()), HashMap.class);
                tempMap = convert2Map(tempMap);
                paramsMap.put(entry.getKey(), tempMap);
            }else if (entry.getValue() instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) entry.getValue();
                if (!(jsonArray.get(0) instanceof JSONObject)){
                    continue;
                }
                List list = JSON.parseArray(JSON.toJSONString(entry.getValue()), HashMap.class);
                list.forEach(obj -> convert2Map((Map<String, Object>) obj));
                paramsMap.put(entry.getKey(), list);
            }
        }

        return paramsMap;
    }

    /**
     * 字符串拼接
     * @param str1
     * @param str2
     * @param seprator
     * @return
     */
    public static String join(String str1, String str2, String seprator) {
        if (StringUtils.isBlank(str1)) {
            return str2;
        }
        if (StringUtils.isBlank(str2)) {
            return str1;
        }
        return str1 + seprator + str2;
    }

    public static String join(Collection<String> strs, String seprator) {
        if (CollectionUtils.isEmpty(strs)) {
            return null;
        }
        StringBuilder val = new StringBuilder();
        for (String str : strs) {
            if (1 == strs.size()) {
                return str;
            }
            val.append(str).append(seprator);
        }
        return val.substring(0, val.length() - seprator.length());
    }

    public static <T> T[] toArray(Collection<T> values, Class<T> claz) {
        if (CollectionUtils.isEmpty(values)) {
            return (T[])Array.newInstance(claz, 0);
        }

        T[] vs = (T[])Array.newInstance(claz, values.size());
        return values.toArray(vs);
    }

    /**
     * 控制控件是否显示
     * @param node 控件
     * @param isShow true-显示 false-隐藏
     */
    private static void showControl(Node node, boolean isShow) {
        node.setVisible(isShow);
        node.setManaged(isShow);
    }

    public static void showControls(Node... nodes) {
        for (Node node : nodes) {
            showControl(node);
        }
    }

    public static void showControl(Node node) {
        showControl(node, true);
    }

    public static void hideControls(Node... nodes) {
        for (Node node : nodes) {
            hideControl(node);
        }
    }

    public static void enableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(false);
        }
    }

    public static void disableControls(Node... nodes) {
        for (Node node : nodes) {
            node.setDisable(true);
        }
    }

    public static void hideControl(Node node) {
        showControl(node, false);
    }

    public static boolean equalsIgnoreCaseIn(String src, String... vals) {
        for (String val : vals) {
            if (StringUtils.equalsIgnoreCase(src, val)) {
                return true;
            }
        }
        return false;
    }

    public static Tooltip toolTip(String text, double size) {
        return toolTip(text, size, new Duration(100));
    }

    public static Tooltip toolTip(String text, double size, Duration duration) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setFont(Font.font(size));
        tooltip.setOpacity(0.75);
        if (null != duration) {
            tooltip.setShowDelay(duration);
        }
        return tooltip;
    }

    /**
     * sql语句是否有限制行数
     * @param sql
     * @return
     */
    public static boolean sqlHasLimit(String sql) {
        String s = StringUtils.lowerCase(sql);
        int idx = StringUtils.lastIndexOf(s, "limit");
        return idx >= 0;
    }


    /**
     * 为SQL增加limit限制，默认为1000
     * @param sql
     * @return
     */
    public static String sqlLimit(String sql) {
        sql = StringUtils.trim(sql);
        if (!StringUtils.startsWithIgnoreCase(sql, "select")) {
            // 非select语句不用添加limit
            return sql;
        }
        return sqlHasLimit(sql)? sql : sql + " limit 0, 1000";
    }

    public static void buttonBind(Button btn, NodeHandler handler) {
        if (null == btn || null == handler) {
            return;
        }

        // 绑定按钮单击事件
        btn.setOnMouseClicked(event -> {
            if (!MouseClickUtils.isLeftSingleClick(event)) {
                return;
            }
            handler.handler();
        });

        // 绑定按钮按键事件
        btn.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handler.handler();
            }
        });
    }

    public static void numricTextField(int min, int max, int defaultVal, TextField... textFields) {
        for (TextField textField : textFields) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (null != newValue && StringUtils.isNotEmpty(newValue)) {
                    if (!StringUtils.isNumeric(newValue)) {
                        StringBuilder val = new StringBuilder();
                        for (int i = 0; i < newValue.length(); i++) {
                            char c = newValue.charAt(i);
                            if (c >= '0' && c <= '9') {
                                val.append(c);
                            }
                        }
                        textField.setText(val.toString());
                    } else {
                        Integer val = Integer.valueOf(newValue);
                        if (val < min) {
                            val = Math.max(min, 0);
                        }
                        if (val > max) {
                            val = Math.max(max, 0);
                        }
                        textField.setText(String.valueOf(Math.max(val, 0)));
                    }
                } else {
                    textField.setText(String.valueOf(Math.max(defaultVal, 0)));
                }
            });
        }
    }

    public static void integerTextField(boolean isUnsigned, TextField... textFields) {
        for (TextField textField : textFields) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (StringUtils.isNotEmpty(newValue)) {
                    try {
                        long value = Long.parseLong(newValue);
                        if (value >= 0) {
                            return;
                        }
                        if (!isUnsigned) {
                            return;
                        }
                        value = Math.abs(value);
                        textField.setText(String.valueOf(value));
                    } catch (Exception e) {
                        StringBuilder val = new StringBuilder();
                        for (int i = 0; i < newValue.length(); i++) {
                            char c = newValue.charAt(i);
                            if ((c >= '0' && c <= '9') || (!isUnsigned && c == '-')) {
                                val.append(c);
                            }
                        }
                        if (isUnsigned) {
                            textField.setText(val.toString());
                            return;
                        }
                        String v = val.toString();
                        if (StringUtils.isBlank(v)) {
                            textField.setText(null);
                        } else if (1 == StringUtils.length(v)) {
                            textField.setText(v);
                        } else {
                            if (StringUtils.substring(v, 1).contains("-")) {
                                textField.setText(StringUtils.left(v, 1) + StringUtils.replace(StringUtils.substring(v, 1), "-", ""));
                            } else {
                                textField.setText(val.toString());
                            }
                        }
                    }
                } else {
                    textField.setText(null);
                }
            });
        }
    }

    public static void decimalTextField(boolean isUnsigned, TextField... textFields) {
        for (TextField textField : textFields) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (StringUtils.isNotEmpty(newValue)) {
                    try {
                        double value = Double.parseDouble(newValue);
                        if (value >= 0) {
                            return;
                        }
                        if (!isUnsigned) {
                            return;
                        }
                        value = Math.abs(value);
                        textField.setText(String.valueOf(value));
                    } catch (Exception e) {
                        StringBuilder val = new StringBuilder();
                        for (int i = 0; i < newValue.length(); i++) {
                            char c = newValue.charAt(i);
                            if ((c >= '0' && c <= '9') || (!isUnsigned && c == '-') || c == '.') {
                                val.append(c);
                            }
                        }
                        String v = val.toString();
                        if (StringUtils.isBlank(v)) {
                            textField.setText(null);
                        } else if (1 == StringUtils.length(v)) {
                            textField.setText(v);
                        } else {
                            if (StringUtils.substring(v, 1).contains("-")) {
                                textField.setText(StringUtils.left(v, 1) + StringUtils.replace(StringUtils.substring(v, 1), "-", ""));
                            } else {
                                textField.setText(val.toString());
                            }
                        }
                    }
                } else {
                    textField.setText(null);
                }
            });
        }
    }

    public static void textFieldSubmit(TextField textField, NodeHandler handler) {
        if (null == textField || null == handler) {
            return;
        }
        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handler.handler();
            }
        });
    }

    public static void propertyTableColumn(boolean isSortable, boolean isEditable, TableColumnBase<?, ?>... columns) {
        for (TableColumnBase<?, ?> column : columns) {
            column.setSortable(isSortable);
            column.setEditable(isEditable);
        }
    }

    public static void sizeTableColumn(TableColumnBase<?, ?> column, Double minWidth, Double maxWidth, Double prefWidth) {
        sizeTableColumn(column, minWidth, maxWidth, prefWidth, null);
    }

    public static void sizeTableColumn(TableColumnBase<?, ?> column, Double minWidth, Double maxWidth, Double prefWidth, Boolean isResizable) {
        if (null != minWidth) {
            column.setMinWidth(minWidth);
        }
        if (null != maxWidth) {
            column.setMaxWidth(maxWidth);
        }
        if (null != prefWidth) {
            column.setPrefWidth(prefWidth);
        }
        if (null != isResizable) {
            column.setResizable(isResizable);
        }
    }

    public static void bindTableColumnField(List<TableColumn<?, ?>> columns, List<String> fields) {
        if (columns.size() != fields.size()) {
            throw new IllegalArgumentException("Illegal argument, size not equal.");
        }
        for (int i = 0; i < columns.size(); i++) {
            bindTableColumnField(columns.get(i), fields.get(i));
        }
    }
    public static void bindTableColumnField(TableColumn<?, ?> column, String field) {
        column.setCellValueFactory(new PropertyValueFactory<>(field));
    }

    public static void setCellEditable(boolean editable, SpreadsheetCell... cells) {
        for (SpreadsheetCell cell : cells) {
            cell.setEditable(editable);
        }
    }

    public static void setCellEditable(boolean editable, List<SpreadsheetCell> cells) {
        for (SpreadsheetCell cell : cells) {
            cell.setEditable(editable);
        }
    }

    public static String toString(Object val) {
        return toString(val, null);
    }

    public static String toString(Object val, MySQLTypeEnum sqlType) {
        if (null == val) {
            return null;
        }
        if (val instanceof Timestamp) {
            return DateFormatUtils.format(new Date(((Timestamp)val).getTime()), "yyyy-MM-dd HH:mm:ss");
        } else if (val instanceof Time) {
            return DateFormatUtils.format((Date)val, "HH:mm:ss");
        }else if (val instanceof Date) {
            if (MySQLTypeEnum.DATE == sqlType) {
                return DateFormatUtils.format((Date)val, "yyyy-MM-dd");
            }
            return DateFormatUtils.format((Date)val, "yyyy-MM-dd HH:mm:ss");
        } else if (val instanceof LocalDate) {
            return LocalDateTimeUtil.format((LocalDate)val, "yyyy-MM-dd");
        } else if (val instanceof LocalDateTime) {
            return LocalDateTimeUtil.format((LocalDateTime)val, "yyyy-MM-dd HH:mm:ss");
        }
        return val.toString();
    }

    public static String toCellString(Object val) {
        return toCellString(val, null);
    }

    public static String toCellString(Object val, MySQLTypeEnum sqlType) {
        String valShow = toString(val, sqlType);
        if (null != valShow) {
            valShow = StringUtils.replace(valShow, "\n", " ");
            valShow = StringUtils.replace(valShow, "\r", " ");
        } else {
            valShow = "(NULL)";
        }
        return valShow;
    }

    public static List<Object> newNullList(int size) {
        if (size <= 0) {
            return new ArrayList<>();
        }
        List<Object> vals = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            vals.add(null);
        }
        return vals;
    }
}
