package com.ai.manager.framework.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 多数据源配置
 *
 * <p>RAG 模块使用 PostgreSQL + pgvector 存储向量数据，
 * 主业务库使用 MySQL，故需要显式定义两个数据源。</p>
 *
 * <p>注意：一旦定义了自定义 DataSource bean，Spring Boot 将不再自动配置默认数据源，
 * 因此必须显式声明主数据源并用 @Primary 标记。</p>
 */
@Configuration
public class PgVectorDataSourceConfig {

    /**
     * 主数据源属性（MySQL）
     */
    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * 主数据源（MySQL），MyBatis-Plus 默认使用此数据源
     */
    @Primary
    @Bean
    public DataSource primaryDataSource(DataSourceProperties primaryDataSourceProperties) {
        return primaryDataSourceProperties.initializeDataSourceBuilder().build();
    }

    /**
     * pgvector 数据源属性（PostgreSQL）
     *
     * <p>同一类型的 DataSourceProperties 有两个 bean，必须显式打上 @Qualifier 区分。
     * 否则注入时 @Primary 的 primaryDataSourceProperties（MySQL）会抢占，
     * 导致 pgVectorDataSource 被构建成 MySQL 连接，RAG 向量 SQL 打到主业务库。</p>
     */
    @Bean
    @Qualifier("pgDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource-pg")
    public DataSourceProperties pgDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * pgvector 数据源（PostgreSQL），RAG 模块专用
     */
    @Bean
    public DataSource pgVectorDataSource(@Qualifier("pgDataSourceProperties") DataSourceProperties pgDataSourceProperties) {
        return pgDataSourceProperties.initializeDataSourceBuilder().build();
    }
}
