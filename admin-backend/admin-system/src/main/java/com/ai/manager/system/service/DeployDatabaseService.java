package com.ai.manager.system.service;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.time.DisplayTime;
import com.ai.manager.system.domain.vo.DeployDatabaseColumnVO;
import com.ai.manager.system.domain.vo.DeployDatabaseSnapshotVO;
import com.ai.manager.system.domain.vo.DeployDatabaseTableVO;
import com.ai.manager.system.util.MysqlCommentEncodingFix;
import com.ai.manager.system.util.MysqlCharsetUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DeployDatabaseService {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final Path snapshotFile;

    public DeployDatabaseService(
            DataSource dataSource,
            ObjectMapper objectMapper,
            @Value("${ai-manager.deploy.backend-dir:/opt/ai-manager/backend}") String backendDir) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.snapshotFile = Path.of(backendDir).resolve("deploy-database-snapshot.json");
    }

    public DeployDatabaseSnapshotVO getSnapshot() {
        DeployDatabaseSnapshotVO snapshot = loadFromFile();
        if (snapshot == null) {
            return emptySnapshot();
        }
        return snapshot;
    }

    public synchronized DeployDatabaseSnapshotVO sync() {
        try (Connection connection = dataSource.getConnection()) {
            MysqlCharsetUtils.ensureUtf8mb4Session(connection);
            String schema = connection.getCatalog();
            if (schema == null || schema.isBlank()) {
                schema = connection.getSchema();
            }
            if (schema == null || schema.isBlank()) {
                throw new BusinessException(500, "无法识别当前数据库名称");
            }

            Map<String, DeployDatabaseTableVO> tables = loadTables(connection, schema);
            loadColumns(connection, schema, tables);

            List<DeployDatabaseTableVO> tableList = new ArrayList<>(tables.values());
            tableList.sort(Comparator.comparing(DeployDatabaseTableVO::getTableName, String.CASE_INSENSITIVE_ORDER));

            for (DeployDatabaseTableVO table : tableList) {
                table.setRowCount(countRows(connection, table.getTableName()));
                table.setColumnCount(table.getColumns() == null ? 0 : table.getColumns().size());
                table.getColumns().sort(Comparator.comparingInt(DeployDatabaseColumnVO::getOrdinalPosition));
            }

            long now = System.currentTimeMillis();
            DeployDatabaseSnapshotVO snapshot = new DeployDatabaseSnapshotVO();
            snapshot.setDatabaseName(schema);
            snapshot.setSyncedAt(DisplayTime.formatMinute(Instant.ofEpochMilli(now)));
            snapshot.setSyncedAtEpochMs(now);
            snapshot.setTableCount(tableList.size());
            snapshot.setTables(tableList);

            persist(snapshot);
            return snapshot;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Sync database snapshot failed: {}", ex.getMessage());
            throw new BusinessException(500, "同步数据库信息失败: " + ex.getMessage());
        }
    }

    private Map<String, DeployDatabaseTableVO> loadTables(Connection connection, String schema) throws SQLException {
        String sql =
                """
                SELECT TABLE_NAME, TABLE_COMMENT, ENGINE, IFNULL(TABLE_ROWS, 0) AS TABLE_ROWS
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """;
        Map<String, DeployDatabaseTableVO> tables = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    DeployDatabaseTableVO table = new DeployDatabaseTableVO();
                    table.setTableName(resultSet.getString("TABLE_NAME"));
                    table.setTableComment(fixComment(resultSet.getString("TABLE_COMMENT")));
                    table.setEngine(nullToEmpty(resultSet.getString("ENGINE")));
                    table.setRowCount(resultSet.getLong("TABLE_ROWS"));
                    tables.put(table.getTableName(), table);
                }
            }
        }
        return tables;
    }

    private void loadColumns(Connection connection, String schema, Map<String, DeployDatabaseTableVO> tables)
            throws SQLException {
        if (tables.isEmpty()) {
            return;
        }
        String sql =
                """
                SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, COLUMN_COMMENT,
                       COLUMN_DEFAULT, ORDINAL_POSITION
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ?
                ORDER BY TABLE_NAME, ORDINAL_POSITION
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    DeployDatabaseTableVO table = tables.get(tableName);
                    if (table == null) {
                        continue;
                    }
                    DeployDatabaseColumnVO column = new DeployDatabaseColumnVO();
                    column.setColumnName(resultSet.getString("COLUMN_NAME"));
                    column.setColumnType(resultSet.getString("COLUMN_TYPE"));
                    column.setNullable("YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE")));
                    column.setColumnKey(nullToEmpty(resultSet.getString("COLUMN_KEY")));
                    column.setColumnComment(fixComment(resultSet.getString("COLUMN_COMMENT")));
                    column.setColumnDefault(formatColumnDefault(resultSet.getString("COLUMN_DEFAULT")));
                    column.setOrdinalPosition(resultSet.getInt("ORDINAL_POSITION"));
                    table.getColumns().add(column);
                }
            }
        }
    }

    private long countRows(Connection connection, String tableName) {
        String escaped = tableName.replace("`", "``");
        String sql = "SELECT COUNT(*) FROM `" + escaped + "`";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
        } catch (SQLException ex) {
            log.debug("Count rows failed for {}: {}", tableName, ex.getMessage());
        }
        return -1;
    }

    private DeployDatabaseSnapshotVO emptySnapshot() {
        DeployDatabaseSnapshotVO snapshot = new DeployDatabaseSnapshotVO();
        snapshot.setDatabaseName("");
        snapshot.setSyncedAt("");
        snapshot.setSyncedAtEpochMs(0);
        snapshot.setTableCount(0);
        snapshot.setTables(new ArrayList<>());
        return snapshot;
    }

    private synchronized DeployDatabaseSnapshotVO loadFromFile() {
        try {
            if (!Files.isRegularFile(snapshotFile)) {
                return null;
            }
            return objectMapper.readValue(
                    Files.newBufferedReader(snapshotFile, StandardCharsets.UTF_8),
                    DeployDatabaseSnapshotVO.class);
        } catch (Exception ex) {
            log.warn("Failed to read database snapshot: {}", ex.getMessage());
            return null;
        }
    }

    private synchronized void persist(DeployDatabaseSnapshotVO snapshot) {
        try {
            Files.createDirectories(snapshotFile.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(
                    Files.newBufferedWriter(snapshotFile, StandardCharsets.UTF_8), snapshot);
        } catch (Exception ex) {
            log.warn("Failed to persist database snapshot: {}", ex.getMessage());
        }
    }

    private static String fixComment(String value) {
        return MysqlCommentEncodingFix.fix(nullToEmpty(value));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String formatColumnDefault(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
