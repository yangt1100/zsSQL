package com.zssql.common.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import cn.hutool.db.*;

import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.sql.SqlExecutor;
import com.alibaba.fastjson.JSON;
import com.zssql.common.exception.ErrorEnum;
import com.zssql.domain.dto.sqlite.DatabaseInfoDto;
import com.zssql.domain.entity.sqlite.DatabaseInfo;
import com.zssql.domain.entity.sqlite.MainWindowPostion;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

public class SQLiteProvider {
    private static volatile boolean loaded = false;

    public static void init() {
        Connection connection = getConnection();
        createTables(connection);
    }

    private static synchronized Connection getConnection() {
        try {
            if (!loaded) {
                // 加载SQLite驱动程序
                Class.forName("org.sqlite.JDBC");
                loaded = true;
            }

            // 创建数据库连接
            return DriverManager.getConnection("jdbc:sqlite:zssql.db");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void createTables(Connection connection) {
        if (null == connection) {
            return;
        }

        Statement statement = null;
        try {
            statement = connection.createStatement();

            // 创建数据库信息表
            statement.executeUpdate(DatabaseInfo.createSQL());
            // 创建主窗体位置信息表
            statement.executeUpdate(MainWindowPostion.createSQL());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DbUtil.close(statement);
        }
    }

    /**
     * 执行查询
     * @param sql
     * @param params
     * @return
     */
    private static List<Entity> execQuery(String sql, Object... params) {
        Connection conn = getConnection();
        if (null == conn) {
            throw new  IllegalStateException("invalid sqlite connection");
        }

        try {
            // 执行查询
            return SqlExecutor.query(conn, sql, new EntityListHandler(), params);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
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
    public static  <T> List<T> execQuery(Class<T> claz, String sql, Object... params) {
        List<Entity> entities = execQuery(sql, params);
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

    public static List<DatabaseInfo> getDatabaseInofs() {
        return execQuery(DatabaseInfo.class, "select * from db_info order by id");
    }

    /**
     * 删除数据库信息
     * @param databaseInfoDto
     */
    public static int deleteDatabase(DatabaseInfoDto databaseInfoDto) {
        if (null == databaseInfoDto) {
            return 0;
        }
        Connection conn = getConnection();
        if (null == conn) {
            throw new  IllegalStateException("invalid sqlite connection");
        }

        try {
            return SqlExecutor.execute(conn, "delete from db_info where id = ?", databaseInfoDto.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "删除数据库信息失败");
        } finally {
            DbUtil.close(conn);
        }
    }

    public static void insertDatabaseInfo(DatabaseInfoDto dto) {
        ExceptionUtil.ifNullThrow(dto, "参数为空");
        Connection conn = getConnection();
        if (null == conn) {
            throw new  IllegalStateException("invalid sqlite connection");
        }
        try {
            SqlExecutor.execute(conn, "insert into db_info(databaseName, databaseType, databaseHost, databaseUser, " +
                            "databasePassword, databasePort, databaseScheme, createTime) " +
                            "values(?,?,?,?,?,?,?,?)",
                    dto.getDatabaseName(), dto.getDatabaseType(), dto.getDatabaseHost(), dto.getDatabaseUser(),
                    dto.getDatabasePassword(), dto.getDatabasePort(), dto.getDatabaseScheme(), DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "新增数据库信息失败");
        } finally {
            DbUtil.close(conn);
        }
    }

    public static void updateDatabaseInfo(DatabaseInfoDto dto) {
        ExceptionUtil.ifNullThrow(dto, "参数为空");
        Connection conn = getConnection();
        if (null == conn) {
            throw new  IllegalStateException("invalid sqlite connection");
        }
        try {
            SqlExecutor.execute(conn, "update db_info set databaseName=?, databaseType=?, databaseHost=?, databaseUser=?, " +
                    "databasePassword=?, databasePort=?, databaseScheme=? where id = ?",
                    dto.getDatabaseName(), dto.getDatabaseType(), dto.getDatabaseHost(), dto.getDatabaseUser(),
                    dto.getDatabasePassword(), dto.getDatabasePort(), dto.getDatabaseScheme(), dto.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "更新数据库信息失败");
        } finally {
            DbUtil.close(conn);
        }
    }

    public static MainWindowPostion getMainWindowPostion() {
        List<MainWindowPostion> postions = execQuery(MainWindowPostion.class, "select * from window_pos order by id");
        if (CollectionUtils.isEmpty(postions)) {
            return null;
        }
        return postions.get(0);
    }

    public static void insertMainWindowPostion(MainWindowPostion dto) {
        ExceptionUtil.ifNullThrow(dto, "参数为空");
        Connection conn = getConnection();
        if (null == conn) {
            throw new  IllegalStateException("invalid sqlite connection");
        }
        try {
            SqlExecutor.execute(conn, "insert into window_pos(id, x, y, width, height, fullScreen) " +
                            "values(?,?,?,?,?,?)",
                    1, dto.getX(), dto.getY(), dto.getWidth(), dto.getHeight(), dto.getFullScreen());
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "新增窗体位置信息失败");
        } finally {
            DbUtil.close(conn);
        }
    }

    public static void updateMainWindowPostion(MainWindowPostion dto) {
        ExceptionUtil.ifNullThrow(dto, "参数为空");
        Connection conn = getConnection();
        if (null == conn) {
            throw new  IllegalStateException("invalid sqlite connection");
        }
        try {
            SqlExecutor.execute(conn, "update window_pos set x=?, y=?, width=?, height=?, fullScreen=? where id = ?",
                    dto.getX(), dto.getY(), dto.getWidth(), dto.getHeight(), dto.getFullScreen(), dto.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw ExceptionUtil.exp(ErrorEnum.ERROR_WITH_REASON, "更新窗体位置信息失败");
        } finally {
            DbUtil.close(conn);
        }
    }
}
