package com.ai.manager.framework.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PG 数据源启动探针
 *
 * <p>防止 P0-1 的静默错绑再次发生：历史上 pgVectorDataSource 曾被 @Primary 的 MySQL
 * DataSourceProperties 抢占注入，向量 SQL 打到主业务库而运行期才报错。此探针在启动期
 * 校验注入的 {@code pgVectorDataSource}：</p>
 * <ol>
 *   <li>jdbcUrl 必须包含 {@code postgresql}——确定性抓错绑（MySQL 连接串不匹配即失败）；</li>
 *   <li>可连接且 {@code rag_vectors} 表存在——连接不上/未建表即失败。</li>
 * </ol>
 *
 * <p>校验失败抛 {@link IllegalStateException}，上下文刷新即终止，启动失败并一眼可见原因；
 * 校验通过仅打印日志，零副作用。无 PG 环境可通过
 * {@code ai-manager.rag.datasource-probe-enabled=false} 关闭。</p>
 *
 * <p>触发时机选 {@link SmartInitializingSingleton}：所有单例 bean（含两个数据源）就绪后、
 * 容器刷新完成前执行，此时数据源可用；抛异常会中断 refresh 实现启动即失败。</p>
 */
@Component
public class PgVectorDataSourceProbe implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(PgVectorDataSourceProbe.class);

    /** 校验失败统一前缀，便于从启动日志/异常信息一眼定位 */
    private static final String PROBE_FAIL_PREFIX = "PG 数据源探针失败：";

    /** 探针开关：无 PG 环境可置 false 关闭，默认开启 */
    private final boolean enabled;

    /** 被探针校验的 RAG 向量数据源（必须显式 @Qualifier，防错绑） */
    private final DataSource pgVectorDataSource;

    public PgVectorDataSourceProbe(
            @Qualifier("pgVectorDataSource") DataSource pgVectorDataSource,
            @Value("${ai-manager.rag.datasource-probe-enabled:true}") boolean enabled) {
        this.pgVectorDataSource = pgVectorDataSource;
        this.enabled = enabled;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!enabled) {
            log.info("PG 数据源启动探针已关闭（ai-manager.rag.datasource-probe-enabled=false），跳过校验");
            return;
        }
        String jdbcUrl = resolveJdbcUrl();
        checkJdbcUrl(jdbcUrl);
        checkTableExists();
        log.info("PG 数据源启动探针通过：jdbcUrl={}，rag_vectors 表存在", jdbcUrl);
    }

    /**
     * 解析数据源 jdbcUrl。HikariDataSource 直接暴露 getJdbcUrl（生产即此类型）；
     * 其他实现退回连接元数据获取，保证探针不绑定具体连接池。
     */
    private String resolveJdbcUrl() {
        if (pgVectorDataSource instanceof HikariDataSource hikari) {
            return hikari.getJdbcUrl();
        }
        try (Connection connection = pgVectorDataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            if (!StringUtils.hasText(url)) {
                throw new IllegalStateException(PROBE_FAIL_PREFIX
                        + "无法从连接元数据获取 jdbcUrl（getMetaData().getURL() 为空），数据源类型="
                        + pgVectorDataSource.getClass().getName());
            }
            return url;
        } catch (SQLException e) {
            throw new IllegalStateException(PROBE_FAIL_PREFIX
                    + "获取 jdbcUrl 时连接失败，原因：" + e.getMessage(), e);
        }
    }

    /**
     * 校验 jdbcUrl 必须包含 postgresql，否则视为被 MySQL 数据源错绑。
     */
    private void checkJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            throw new IllegalStateException(PROBE_FAIL_PREFIX + "jdbcUrl 为空，无法校验");
        }
        if (!jdbcUrl.toLowerCase().contains("postgresql")) {
            throw new IllegalStateException(PROBE_FAIL_PREFIX
                    + "jdbcUrl 不含 postgresql，疑似被 MySQL 数据源错绑（jdbcUrl=" + jdbcUrl
                    + "）。请检查 PgVectorDataSourceConfig 中 pgVectorDataSource 的 @Qualifier 绑定");
        }
    }

    /**
     * 校验数据源可连接且 rag_vectors 表存在（含 schema 判断，避免命中同名表）。
     */
    private void checkTableExists() {
        String sql = "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema = current_schema() AND table_name = 'rag_vectors'";
        try (Connection connection = pgVectorDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            boolean tableExists = resultSet.next() && resultSet.getLong(1) > 0;
            if (!tableExists) {
                throw new IllegalStateException(PROBE_FAIL_PREFIX
                        + "rag_vectors 表不存在，请先在 PG 库执行 sql/rag_pgvector.sql 建表");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(PROBE_FAIL_PREFIX
                    + "连接 PG 或查询 rag_vectors 表失败，原因：" + e.getMessage(), e);
        }
    }
}
