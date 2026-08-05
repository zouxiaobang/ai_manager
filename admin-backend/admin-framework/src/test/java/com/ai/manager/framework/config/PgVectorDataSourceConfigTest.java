package com.ai.manager.framework.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PgVectorDataSourceConfig 多数据源绑定测试
 *
 * <p>验证两个数据源分别绑定到正确的连接串：
 * 主业务库 primaryDataSource 必须绑定 MySQL（spring.datasource），
 * RAG 向量库 pgVectorDataSource 必须绑定 PostgreSQL（spring.datasource-pg）。
 * 防止因 @Primary 的 DataSourceProperties 抢占注入导致向量 SQL 打到 MySQL。</p>
 */
class PgVectorDataSourceConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(PgVectorDataSourceConfig.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/ai_manager_admin",
                    "spring.datasource.username=root",
                    "spring.datasource.password=123456",
                    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                    "spring.datasource-pg.url=jdbc:postgresql://192.168.0.118:5432/ai_manager_rag",
                    "spring.datasource-pg.username=ai_manager",
                    "spring.datasource-pg.password=123456",
                    "spring.datasource-pg.driver-class-name=org.postgresql.Driver");

    @Test
    void primaryDataSource应绑定MySQL() {
        contextRunner.run(ctx -> {
            DataSource ds = ctx.getBean("primaryDataSource", DataSource.class);
            assertThat(((HikariDataSource) ds).getJdbcUrl()).startsWith("jdbc:mysql://");
        });
    }

    @Test
    void pgVectorDataSource应绑定PostgreSQL() {
        contextRunner.run(ctx -> {
            DataSource ds = ctx.getBean("pgVectorDataSource", DataSource.class);
            assertThat(((HikariDataSource) ds).getJdbcUrl()).startsWith("jdbc:postgresql://");
        });
    }

    @Test
    void 两个数据源bean应相互独立() {
        contextRunner.run(ctx -> {
            DataSource primary = ctx.getBean("primaryDataSource", DataSource.class);
            DataSource pg = ctx.getBean("pgVectorDataSource", DataSource.class);
            assertThat(primary).isNotSameAs(pg);
        });
    }
}
