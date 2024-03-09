package com.zssql.common.utils;

import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.sql.SqlExecutor;
import com.alibaba.druid.filter.config.ConfigTools;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.fastjson.JSON;
import com.zssql.common.enums.DsDriverClassEnum;
import com.zssql.common.enums.FieldTypeEnum;
import com.zssql.common.enums.SQLType;
import com.zssql.common.exception.ErrorEnum;
import com.zssql.domain.dto.ConnectInfoDto;
import com.zssql.domain.dto.SqlValueDto;
import com.zssql.domain.dto.TableColumnDto;
import com.zssql.domain.dto.sqlite.DatabaseInfoDto;
import com.zssql.domain.vo.DatasouceTableVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * 数据源处理器
 */
public class DataSourceProvider {
    private static Set<String> driverClassNames = Collections.synchronizedSet(new HashSet<>());

    /**
     * 获取数据库连接
     * @param dto
     * @return
     */
    public static Connection getConnection(ConnectInfoDto dto) {
        DataSource dataSource = initDataSource(dto);
        if (null == dataSource) {
            throw ExceptionUtil.exp(String.format("数据库[%s]获取数据库数据源失败:", dto.getDbInfoDto().getDatabaseName()));
        }

        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(String.format("数据库[%s]获取数据库连接失败:", dto.getDbInfoDto().getDatabaseName()));
        }
    }

    /**
     * 初始化数据库连接池
     * @param dto
     * @return
     */
    private static DataSource initDataSource(ConnectInfoDto dto) {
        if (null == dto || null == dto.getDbInfoDto()) {
            return null;
        }
        if (null != dto.getDataSource()) {
            return dto.getDataSource();
        }
        DsDriverClassEnum driverClassEnum = null;
        try {
            driverClassEnum = DsDriverClassEnum.getDriverClassEnum(dto.getDbInfoDto().getDatabaseType());
            if (null == driverClassEnum) {
                throw ExceptionUtil.exp(String.format("数据库[%s]未找到对应的驱动类", dto.getDbInfoDto().getDatabaseName()));
            }
            // 检查驱动类加载
            if (!driverClassNames.contains(driverClassEnum.getDriverClass())) {
                Class.forName(driverClassEnum.getDriverClass());
                driverClassNames.add(driverClassEnum.getDriverClass());
            }

            DruidDataSource dataSource = new DruidDataSource();
            Properties props =new Properties();
            props.setProperty("remarks", "true"); //设置可以获取remarks信息
            props.setProperty("useInformationSchema", "true");
            dataSource.setConnectProperties(props);
            dataSource.setDriverClassName(driverClassEnum.getDriverClass());
            dataSource.setUrl(driverClassEnum.getJDBCUrl(dto.getDbInfoDto()));
            dataSource.setUsername(dto.getDbInfoDto().getDatabaseUser());
            dataSource.setPassword(ConfigTools.decrypt(dto.getDbInfoDto().getDatabasePassword()));
            dataSource.setInitialSize(2);
            dataSource.setMaxActive(5);
            dataSource.setMinIdle(3);
            dataSource.setTestWhileIdle(true);
            dataSource.setMaxWait(20000);
            dataSource.setValidationQuery("select 'X'");

            dto.setDataSource(dataSource);
            return dataSource;
        } catch (ClassNotFoundException e){
            throw ExceptionUtil.exp(String.format("数据库[%s]对应驱动类加载失败", dto.getDbInfoDto().getDatabaseName()));
        } catch (Exception e) {
            throw ExceptionUtil.exp(String.format("数据库[%s]初始化数据源失败", dto.getDbInfoDto().getDatabaseName()));
        }
    }

    /**
     * 执行查询
     * @param conn
     * @param sql
     * @param params
     * @return
     */
    public static List<Entity> execQuery(Connection conn, String sql, Object... params) {
        ExceptionUtil.matchThrow(StringUtils.isBlank(sql), "SQL is null");
        ExceptionUtil.ifNullThrow(conn, "get ds connection failed");

        try {
            // 执行查询
            List<Entity> entities = SqlExecutor.query(conn, sql, new EntityListHandler(), params);
            return entities;
        } catch (SQLException e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "query exception");
        } finally {
            DbUtil.close(conn);
        }
    }

    /**
     * 执行查询
     * @param claz  返回的对象类型
     * @param sql
     * @param params
     * @param <T>
     * @return
     * 备注：字段支持简单驼峰装换，例如数据库字段db_type可以转换为实体的db_type和dbType字段
     */
    public static  <T> List<T> execQuery(Connection conn, Class<T> claz, String sql, Object... params) {
        List<Entity> entities = execQuery(conn, sql, params);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        // Entity对象转换成指定类型对象
        return CommonUtil.datasTransfer(entities, entity -> {
            Map<String, Object> entityMap = JSON.parseObject(JSON.toJSONString(entity), HashMap.class);
            Set<String> keys = new HashSet<>(entityMap.keySet());
            for (String key : keys) {
                // 判断字段名是否包含指定分隔符
                if (!StringUtils.contains(key,"_")) {
                    // 不包含
                    continue;
                }
                // 包含下划线分隔符时增加根据驼峰转换的key
                String ckey = key2Camel(key);
                entityMap.put(ckey, entityMap.get(key));
            }
            return JSON.parseObject(JSON.toJSONString(entityMap), claz);
        });
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

    /**
     * 执行增删改SQL
     * @param conn
     * @param sql
     * @param params
     * @return 影响的行数
     */
    public static int execute(Connection conn, String sql, Object... params) {
        ExceptionUtil.matchThrow(StringUtils.isBlank(sql), "SQL is null");
        ExceptionUtil.ifNullThrow(conn, "get ds connection failed");

        try {
            // 执行SQL
            return SqlExecutor.execute(conn, sql, params);
        } catch (SQLException e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "execute exception");
        } finally {
            DbUtil.close(conn);
        }
    }

    /**
     * 获取SQL值
     * @param conn 数据源
     * @param sql  SQL语句
     * @return 执行SQL结果值
     */
    public static SqlValueDto getSQLVal(Connection conn, String sql) {
        ExceptionUtil.matchThrow(StringUtils.isBlank(sql), "SQL is null");
        ExceptionUtil.ifNullThrow(conn, "get ds connection failed");
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            // 执行SQL
            resultSet =  ps.executeQuery();
            SqlValueDto dto = new SqlValueDto();
            // 解析返回结果类型
            Map<String, FieldTypeEnum> columMap = new HashMap<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int count = metaData.getColumnCount();
            if (count <= 0) {
                return null;
            }
            // 列数量
            dto.setColumnCount(count);
            for (int i = 1; i <= count; i++) {
                String columnName = metaData.getColumnName(i);
                dto.getCloumnNames().add(columnName);
                String columnLabel = metaData.getColumnLabel(i);
                dto.getColumnLabels().add(columnLabel);
                String tableName = metaData.getTableName(i);
                dto.getColumnBelongTables().add(tableName);
                int columnType = metaData.getColumnType(i);
                FieldTypeEnum fieldType = convertFieldType(columnType);
                if (null != fieldType) {
                    columMap.put(columnLabel, fieldType);
                    dto.getColumnTypes().add(fieldType.getType());
                } else {
                    dto.getColumnTypes().add(-1);
                }
                dto.getSqlTypes().add(columnType);
                String columnTypeName = metaData.getColumnTypeName(i);
                dto.getSqlTypeNames().add(columnTypeName);
            }
            if (MapUtils.isEmpty(columMap)) {
                return dto;
            }

            // 解析返回结果
            while (resultSet.next()) {
                List<Object> vals = new ArrayList<>();
                for (String label : dto.getColumnLabels()) {
                    Object o = resultSet.getObject(label);
                    vals.add(o);
                }
                dto.getVals().add(vals);
            }
            return dto;
        } catch (SQLException e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, e.getMessage());
        } finally {
            DbUtil.close(resultSet, ps, conn);
        }
    }

    /**
     * SQL字段类型与指定字段类型转换
     * @param columnType SQL字段类型
     * @return FieldTypeEnum字段类型
     */
    private static FieldTypeEnum convertFieldType(int columnType) {
        switch (columnType) {
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                return FieldTypeEnum.INTEGER;
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return FieldTypeEnum.DECIMAL;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.BLOB:
            case Types.CLOB:
            case Types.NCLOB:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.SQLXML:
                return FieldTypeEnum.STRING;
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return FieldTypeEnum.DATE;
            default:
                return null;
        }
    }


    /**
     * 测试数据源连通性
     * @param databaseInfoDto 数据源
     * @return 连通结果
     */
    public static boolean testConnect(DatabaseInfoDto databaseInfoDto) throws Exception {
        DsDriverClassEnum driverClassEnum = DsDriverClassEnum.getDriverClassEnum(databaseInfoDto.getDatabaseType());
        if (null == driverClassEnum) {
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "数据库驱动不存在");
        }
        // 检查驱动类加载
        if (!driverClassNames.contains(driverClassEnum.getDriverClass())) {
            Class.forName(driverClassEnum.getDriverClass());
            driverClassNames.add(driverClassEnum.getDriverClass());
        }

        DruidDataSource dataSource = null;
        DruidPooledConnection conn = null;
        try {
            dataSource = new DruidDataSource();
            dataSource.setDriverClassName(driverClassEnum.getDriverClass());
            dataSource.setUrl(driverClassEnum.getJDBCUrl(databaseInfoDto));
            dataSource.setUsername(databaseInfoDto.getDatabaseUser());
            dataSource.setPassword(ConfigTools.decrypt(databaseInfoDto.getDatabasePassword()));
            dataSource.setInitialSize(1);
            dataSource.setMaxActive(2);
            dataSource.setMinIdle(1);
            dataSource.setTestWhileIdle(false);
            dataSource.setBreakAfterAcquireFailure(true);
            dataSource.setMaxWait(3000);
            dataSource.setNotFullTimeoutRetryCount(1);

            conn = dataSource.getConnection(1000);
            return null != conn;
        } finally {
            DbUtil.close(conn, dataSource);
        }
    }

    public static List<DatasouceTableVO> listTables(ConnectInfoDto dto) {
        Connection conn = getConnection(dto);
        List<DatasouceTableVO> tables = listTables(conn);
        if (CollectionUtils.isNotEmpty(tables)) {
            for (DatasouceTableVO vo : tables) {
                // 获取字段扩展信息
                List<String> onUpdateColumns = new ArrayList<>();
                Map<String, List<String>> enumValMap = new HashMap<>();
                try {
                    SqlValueDto tableDesc = getSQLVal(getConnection(dto), "DESCRIBE " + vo.getTableName());
                    if (null != tableDesc && CollectionUtils.isNotEmpty(tableDesc.getColumnLabels()) && CollectionUtils.isNotEmpty(tableDesc.getVals())) {
                        int fieldIdx = -1;
                        int extraIdx = -1;
                        int typeIdx = -1;
                        for (int i = 0; i < tableDesc.getColumnLabels().size(); i++) {
                            String column = tableDesc.getColumnLabels().get(i);
                            if (StringUtils.equalsIgnoreCase(column, "Field")) {
                                fieldIdx = i;
                            } else if (StringUtils.equalsIgnoreCase(column, "Extra")) {
                                extraIdx = i;
                            } else if (StringUtils.equalsIgnoreCase(column, "Type")) {
                                typeIdx = i;
                            }
                        }
                        if (fieldIdx >= 0 && extraIdx >= 0) {
                            for (List<Object> val : tableDesc.getVals()) {
                                Object field = val.get(fieldIdx);
                                Object extra = val.get(extraIdx);
                                if (null != extra && null != field) {
                                    String extraStr = extra.toString();
                                    if (StringUtils.containsIgnoreCase(extraStr, "on update")) {
                                        onUpdateColumns.add(StringUtils.upperCase(field.toString()));
                                    }
                                }
                            }
                        }
                        if (fieldIdx >= 0 && typeIdx >= 0) {
                            for (List<Object> val : tableDesc.getVals()) {
                                Object field = val.get(fieldIdx);
                                Object type = val.get(typeIdx);
                                if (null != type && null != field) {
                                    String typStr = type.toString();
                                    if (StringUtils.startsWithIgnoreCase(typStr, "enum")
                                            || StringUtils.startsWithIgnoreCase(typStr, "set")) {
                                        // 解析候选列表值
                                        int pos = StringUtils.indexOf(typStr, "(");
                                        if (pos >= 0) {
                                            typStr = StringUtils.substring(typStr, pos + 1);
                                            pos = StringUtils.lastIndexOf(typStr, ")");
                                            if (pos >= 0) {
                                                typStr = StringUtils.left(typStr, pos);
                                                List<String> evals = CommonUtil.split(typStr, ",");
                                                if (CollectionUtils.isNotEmpty(evals)) {
                                                    for (int i = 0; i < evals.size(); i++) {
                                                        String eval = evals.get(i);
                                                        // 去除前后字符
                                                        eval = StringUtils.substring(eval, 1, eval.length() - 1);
                                                        eval = StringUtils.replace(eval, "''", "'");
                                                        evals.set(i, eval);
                                                    }
                                                    enumValMap.put(field.toString(), evals);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (CollectionUtils.isNotEmpty(onUpdateColumns)) {
                        for (String onUpdateColumn : onUpdateColumns) {
                            for (TableColumnDto columnInfo : vo.getColumnInfos()) {
                                if (StringUtils.equalsIgnoreCase(columnInfo.getName(), onUpdateColumn)) {
                                    columnInfo.setAutoUpdate(true);
                                    break;
                                }
                            }
                        }
                    }
                    if (MapUtils.isNotEmpty(enumValMap)) {
                        for (Map.Entry<String, List<String>> entry : enumValMap.entrySet()) {
                            for (TableColumnDto columnInfo : vo.getColumnInfos()) {
                                if (StringUtils.equalsIgnoreCase(columnInfo.getName(), entry.getKey())) {
                                    columnInfo.setVals(entry.getValue());
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return tables;
    }

    /**
     * 获取指定数据源表清单
     * @param conn 数据源
     * @return 表清单
     */
    private static List<DatasouceTableVO> listTables(Connection conn) {
        if (conn == null) {
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON,  "get ds connection failed");
        }
        ResultSet tableRet = null;
        try {
            List<DatasouceTableVO> vos = new ArrayList<>();
            DatabaseMetaData metaData = conn.getMetaData();
            tableRet = metaData.getTables(conn.getCatalog(), conn.getSchema(), "%", new String[] { "TABLE" });
            while (tableRet.next()) {
                DatasouceTableVO vo = new DatasouceTableVO();
                vo.setTableName(tableRet.getString("TABLE_NAME"));
                vo.setComment(tableRet.getString("REMARKS"));

                // 获取主键
                ResultSet primaryKeys = metaData.getPrimaryKeys(conn.getCatalog(), conn.getSchema(), vo.getTableName());
                ResultSetMetaData pkmd = primaryKeys.getMetaData();
                Set<String> primarys = new HashSet<>();
                while (primaryKeys.next()) {
                    for (int i = 1; i <= pkmd.getColumnCount(); i++) {
                        if ("COLUMN_NAME".equalsIgnoreCase(pkmd.getColumnName(i))) {
                            vo.getPrimaryKeyColums().add(primaryKeys.getString(i));
                            primarys.add(StringUtils.upperCase(primaryKeys.getString(i)));
                        }
                    }
                }
                // 索引信息
                ResultSet indexInfo = metaData.getIndexInfo(conn.getCatalog(), conn.getSchema(), vo.getTableName(), false, false);
                while (indexInfo.next()) {
                    String indexName = indexInfo.getString("INDEX_NAME");
                    if (StringUtils.equalsIgnoreCase(indexName, "PRIMARY")) {
                        // 剔除主键
                        continue;
                    }
                    //如果为真则说明索引值不唯一，为假则说明索引值必须唯一。
                    boolean nonUnique = indexInfo.getBoolean("NON_UNIQUE");
                    short ordinalPosition = indexInfo.getShort("ORDINAL_POSITION");
                    String columnName = indexInfo.getString("COLUMN_NAME");
                    if (!vo.getIndexs().containsKey(indexName)) {
                        DatasouceTableVO.TableIndexVO indexVO = new DatasouceTableVO.TableIndexVO();
                        indexVO.setIndexName(indexName);
                        indexVO.setUnique(!nonUnique);
                        vo.getIndexs().put(indexName, indexVO);
                    }
                    vo.getIndexs().get(indexName).put(ordinalPosition, columnName);
                }

                ResultSet columnRS = null;
                try {
                    columnRS = metaData.getColumns(conn.getCatalog(), conn.getSchema(), vo.getTableName(), "%");
                    List<String> columns = new ArrayList<>();
                    List<String> comments = new ArrayList<>();
                    List<String> typeNames = new ArrayList<>();
                    List<TableColumnDto> columnInfos = new ArrayList<>();
                    while (columnRS.next()) {
                        String columnName = columnRS.getString("COLUMN_NAME");
                        String remark = columnRS.getString("REMARKS");
                        String typeName = columnRS.getString("TYPE_NAME");
                        boolean unsigned = false;
                        if (StringUtils.endsWithIgnoreCase(typeName, "unsigned")) {
                            unsigned = true;
                            typeName = StringUtils.removeEndIgnoreCase(typeName, "unsigned");
                            typeName = StringUtils.trim(typeName);
                        }
                        int datasize = columnRS.getInt("COLUMN_SIZE");
                        int digits = columnRS.getInt("DECIMAL_DIGITS");
                        String defVal = columnRS.getString("COLUMN_DEF");
                        String isAutoincrement = columnRS.getString("IS_AUTOINCREMENT");
                        String isNullable = columnRS.getString("IS_NULLABLE");
                        TableColumnDto info = new TableColumnDto();
                        info.setName(columnName);
                        info.setType(StringUtils.lowerCase(typeName));
                        info.setComment(remark);
                        info.setSize(datasize);
                        info.setDigits(digits);
                        info.setDefaultVal(defVal);
                        info.setPrimary(primarys.contains(StringUtils.upperCase(columnName)));
                        info.setAutoIncrement(StringUtils.equalsIgnoreCase(isAutoincrement, "yes"));
                        info.setNotnull(!StringUtils.equalsIgnoreCase(isNullable, "yes"));
                        info.setUnsigned(unsigned);
                        info.setAutoUpdate(false);
                        columns.add(columnName);
                        comments.add(remark);
                        typeNames.add(typeName);
                        columnInfos.add(info);

                    }
                    vo.setColumns(columns);
                    vo.setComments(comments);
                    vo.setTypes(typeNames);
                    vo.setColumnInfos(columnInfos);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    DbUtil.close(columnRS);
                }
                vos.add(vo);
            }

            return vos;
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "get table info failed");
        } finally {
            DbUtil.close(tableRet, conn);
        }
    }

    public static List<Result<SqlValueDto>> executeSqls(ConnectInfoDto connectInfoDto, List<String> sqls) {
        List<Result<SqlValueDto>> results = new ArrayList<>();
        for (String sql : sqls) {
            Result<SqlValueDto> result = new Result<>();
            try {
                long start = System.currentTimeMillis();
                Connection conn = getConnection(connectInfoDto);
                SqlValueDto dto;
                if (!StringUtils.startsWithIgnoreCase(sql, "select")
                        && !StringUtils.startsWithIgnoreCase(sql, "explain")
                        && !StringUtils.startsWithIgnoreCase(sql, "with")
                        && !StringUtils.startsWithIgnoreCase(sql, "DESCRIBE")
                        && !StringUtils.startsWithIgnoreCase(sql, "DESC")
                        && !StringUtils.startsWithIgnoreCase(sql, "SHOW")) {
                    // 非查询语句
                    dto = executeSql(conn, sql);
                    dto.setType(SQLType.EXECUTE);
                } else {
                    // 查询语句
                    dto = getSQLVal(conn, sql);
                    if (null != dto) {
                        dto.setType(SQLType.QUERY);

                        for (List<Object> val : dto.getVals()) {
                            for (int pos = 0; pos < val.size(); pos++) {
                                if (null == val.get(pos)) {
                                    continue;
                                }
                                if (val.get(pos) instanceof Timestamp) {
                                    val.set(pos, new Date(((Timestamp)val.get(pos)).getTime()));
                                }
                            }
                        }
                    }
                }
                if (null != dto) {
                    dto.setExecuteMill(System.currentTimeMillis() - start);
                }
                results.add(new Result<>(dto));
            } catch (Exception e) {
                e.printStackTrace();
                result.setMessage("sql execute error: " + e.getMessage());
                results.add(result);
            }
        }
        return results;
    }

    private static SqlValueDto executeSql(Connection conn, String sql) {
        ExceptionUtil.matchThrow(StringUtils.isBlank(sql), "SQL is null");
        try {
            // 执行SQL
            int count = SqlExecutor.execute(conn, sql);
            SqlValueDto dto = new SqlValueDto();
            dto.setEffectRow(count);
            return dto;
        } catch (SQLException e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, e.getMessage());
        } finally {
            DbUtil.close(conn);
        }
    }

    /**
     * 查询SQL值
     * @param connectInfoDto
     * @param sqls
     * @return
     */
    public static List<Result<SqlValueDto>> querySQLVals(ConnectInfoDto connectInfoDto, List<String> sqls) {
        List<Result<SqlValueDto>> results = new ArrayList<>();
        for (String reqInfo : sqls) {
            Result<SqlValueDto> result = new Result<>();
            try {
                long start = System.currentTimeMillis();
                Connection conn = getConnection(connectInfoDto);
                SqlValueDto dto = getSQLVal(conn, reqInfo);
                if (null != dto) {
                    dto.setType(SQLType.QUERY);
                    dto.setExecuteMill(System.currentTimeMillis() - start);
                }
                results.add(new Result<>(dto));
            } catch (Exception e) {
                e.printStackTrace();
                result.setMessage("sql execute error: " + e.getMessage());
                results.add(result);
            }
        }
        return results;
    }
}
