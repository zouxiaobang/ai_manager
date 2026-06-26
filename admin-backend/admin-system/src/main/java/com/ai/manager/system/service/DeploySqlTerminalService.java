package com.ai.manager.system.service;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.domain.vo.DeploySqlBatchItemVO;
import com.ai.manager.system.domain.vo.DeploySqlExecuteResultVO;
import com.ai.manager.system.util.MysqlCharsetUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
public class DeploySqlTerminalService {

    private static final int MAX_ROWS = 1000;
    private static final int MAX_BATCH_STATEMENTS = 200;
    private static final int QUERY_TIMEOUT_SECONDS = 60;

    private final DataSource primaryDataSource;
    private final String node118Host;
    private final int node118Port;
    private final String node118Database;
    private final String node118Username;
    private final String node118Password;

    private volatile HikariDataSource node118DataSource;

    public DeploySqlTerminalService(
            DataSource primaryDataSource,
            @Value("${ai-manager.deploy.sql-terminal.node118.host:192.168.0.118}") String node118Host,
            @Value("${ai-manager.deploy.sql-terminal.node118.port:3306}") int node118Port,
            @Value("${ai-manager.deploy.sql-terminal.node118.database:ai_manager_admin}") String node118Database,
            @Value("${ai-manager.deploy.sql-terminal.node118.username:ai_manager}") String node118Username,
            @Value("${ai-manager.deploy.sql-terminal.node118.password:123456}") String node118Password) {
        this.primaryDataSource = primaryDataSource;
        this.node118Host = node118Host;
        this.node118Port = node118Port;
        this.node118Database = node118Database;
        this.node118Username = node118Username;
        this.node118Password = node118Password;
    }

    public DeploySqlExecuteResultVO execute(String targetRaw, String sqlRaw) {
        String target = normalizeTarget(targetRaw);
        List<String> statements = splitStatements(sqlRaw);
        if (statements.isEmpty()) {
            throw new BusinessException(400, "SQL 不能为空");
        }

        long started = System.currentTimeMillis();
        DeploySqlExecuteResultVO result = new DeploySqlExecuteResultVO();
        result.setTarget(target);
        result.setTargetLabel(targetLabel(target));
        result.setSql(sqlRaw == null ? "" : sqlRaw.trim());
        result.setStatementCount(statements.size());

        try (Connection connection = openConnection(target)) {
            MysqlCharsetUtils.ensureUtf8mb4Session(connection);

            boolean hasQuery = statements.stream().anyMatch(DeploySqlTerminalService::isQueryStatement);
            if (hasQuery && statements.size() > 1) {
                throw new BusinessException(400, "查询语句一次只能执行一条，请勿使用分号分隔多条");
            }
            if (hasQuery) {
                fillQueryResult(connection, statements.get(0), result);
                result.setStatementType("query");
                result.setMessage("查询成功，返回 " + result.getRowCount() + " 行"
                        + (result.getRowCount() >= MAX_ROWS ? "（已达上限 " + MAX_ROWS + "）" : ""));
            } else if (statements.size() == 1) {
                int affected = executeUpdate(connection, statements.get(0));
                result.setStatementType("update");
                result.setAffectedRows(affected);
                result.setMessage("执行成功，影响 " + affected + " 行");
            } else {
                executeBatchDml(connection, statements, result);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("SQL terminal execute failed [{}]: {}", target, ex.getMessage());
            throw new BusinessException(500, "SQL 执行失败: " + ex.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - started);
        return result;
    }

    private void executeBatchDml(Connection connection, List<String> statements, DeploySqlExecuteResultVO result)
            throws SQLException {
        if (statements.size() > MAX_BATCH_STATEMENTS) {
            throw new BusinessException(400, "批量执行最多支持 " + MAX_BATCH_STATEMENTS + " 条语句");
        }
        for (String statement : statements) {
            if (!isBatchDmlStatement(statement)) {
                throw new BusinessException(400, "批量执行仅支持 INSERT / UPDATE / DELETE，且不可与查询混用");
            }
        }

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        List<DeploySqlBatchItemVO> batchItems = new ArrayList<>();
        int totalAffected = 0;
        try {
            for (int i = 0; i < statements.size(); i++) {
                String statement = statements.get(i);
                int affected = executeUpdate(connection, statement);
                totalAffected += affected;

                DeploySqlBatchItemVO item = new DeploySqlBatchItemVO();
                item.setIndex(i + 1);
                item.setSql(statement);
                item.setAffectedRows(affected);
                batchItems.add(item);
            }
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }

        result.setStatementType("batch");
        result.setAffectedRows(totalAffected);
        result.setBatchItems(batchItems);
        result.setMessage("批量执行成功，共 " + statements.size() + " 条语句，影响 " + totalAffected + " 行");
    }

    private void fillQueryResult(Connection connection, String sql, DeploySqlExecuteResultVO result)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.setMaxRows(MAX_ROWS);
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                ResultSetMetaData meta = resultSet.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columns = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }
                result.setColumns(columns);

                List<List<Object>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    List<Object> row = new ArrayList<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(normalizeCell(resultSet.getObject(i)));
                    }
                    rows.add(row);
                }
                result.setRows(rows);
                result.setRowCount(rows.size());
            }
        }
    }

    private int executeUpdate(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            return statement.executeUpdate(sql);
        }
    }

    private Connection openConnection(String target) throws SQLException {
        if ("node118".equals(target)) {
            return node118DataSource().getConnection();
        }
        return primaryDataSource.getConnection();
    }

    private synchronized DataSource node118DataSource() {
        if (node118DataSource == null) {
            HikariConfig config = new HikariConfig();
            String jdbcUrl = MysqlCharsetUtils.buildJdbcUrl(node118Host, node118Port, node118Database)
                    + "&connectTimeout=10000&socketTimeout=60000";
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(node118Username);
            config.setPassword(node118Password);
            config.setConnectionInitSql(MysqlCharsetUtils.SESSION_INIT_SQL);
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(0);
            config.setPoolName("deploy-sql-node118");
            node118DataSource = new HikariDataSource(config);
        }
        return node118DataSource;
    }

    static List<String> splitStatements(String sqlRaw) {
        if (sqlRaw == null || sqlRaw.isBlank()) {
            return List.of();
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        for (int i = 0; i < sqlRaw.length(); i++) {
            char ch = sqlRaw.charAt(i);
            if (ch == '\'') {
                if (inSingleQuote && i + 1 < sqlRaw.length() && sqlRaw.charAt(i + 1) == '\'') {
                    current.append("''");
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                current.append(ch);
                continue;
            }
            if (ch == ';' && !inSingleQuote) {
                String statement = current.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        String last = current.toString().trim();
        if (!last.isEmpty()) {
            statements.add(last);
        }
        return statements;
    }

    private static boolean isQueryStatement(String sql) {
        String upper = sql.stripLeading().toUpperCase(Locale.ROOT);
        return upper.startsWith("SELECT")
                || upper.startsWith("SHOW")
                || upper.startsWith("DESC")
                || upper.startsWith("DESCRIBE")
                || upper.startsWith("EXPLAIN")
                || upper.startsWith("WITH");
    }

    private static boolean isBatchDmlStatement(String sql) {
        String upper = sql.stripLeading().toUpperCase(Locale.ROOT);
        return upper.startsWith("INSERT")
                || upper.startsWith("UPDATE")
                || upper.startsWith("DELETE");
    }

    private static String normalizeTarget(String targetRaw) {
        String target = targetRaw == null ? "" : targetRaw.trim().toLowerCase(Locale.ROOT);
        if (target.isEmpty() || "local".equals(target)) {
            return "local";
        }
        if (Set.of("node118", "118", "data").contains(target)) {
            return "node118";
        }
        throw new BusinessException(400, "不支持的 SQL 连接目标: " + targetRaw);
    }

    private static String targetLabel(String target) {
        return "node118".equals(target) ? "118 环境" : "本地";
    }

    private static Object normalizeCell(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return "[BINARY]";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof java.util.Date date) {
            return date.toString();
        }
        return value;
    }
}
