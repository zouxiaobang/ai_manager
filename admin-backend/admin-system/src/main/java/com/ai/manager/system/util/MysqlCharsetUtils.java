package com.ai.manager.system.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQL 连接字符集：文件 UTF-8，传输 utf8mb4；JDBC 使用 characterEncoding=UTF-8。
 */
public final class MysqlCharsetUtils {

    public static final String JDBC_PARAMS =
            "useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true"
                    + "&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci"
                    + "&serverTimezone=Asia/Shanghai";

    public static final String SESSION_INIT_SQL = "SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci";

    private MysqlCharsetUtils() {}

    public static String buildJdbcUrl(String host, int port, String database) {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?" + JDBC_PARAMS;
    }

    public static void ensureUtf8mb4Session(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(SESSION_INIT_SQL);
        }
    }
}
