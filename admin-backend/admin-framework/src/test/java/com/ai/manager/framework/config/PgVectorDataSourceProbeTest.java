package com.ai.manager.framework.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PgVectorDataSourceProbe 启动探针单元测试
 *
 * <p>覆盖：错绑抓取（MySQL 连接串必须失败）、正常通过（postgresql + 表存在）、
 * 表缺失/连接失败必须失败、开关关闭跳过校验。均直接 mock DataSource，
 * 无需真实 PG 连接。</p>
 */
class PgVectorDataSourceProbeTest {

    /** Hikari 数据源 + 表存在（postgresql 正常路径） */
    private HikariDataSource hikariWithTable() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.getJdbcUrl()).thenReturn("jdbc:postgresql://192.168.0.118:5432/ai_manager_rag");
        Connection conn = connectionWithTableCount(1);
        when(ds.getConnection()).thenReturn(conn);
        return ds;
    }

    /** 造出返回指定行数的连接（模拟 rag_vectors 表存在/不存在） */
    private Connection connectionWithTableCount(long count) throws Exception {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(count);
        return conn;
    }

    @Test
    void jdbcUrl是MySQL时应抛错_抓住错绑() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.getJdbcUrl()).thenReturn("jdbc:mysql://127.0.0.1:3306/ai_manager_admin");

        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(ds, true);

        assertThatThrownBy(probe::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PG 数据源探针失败")
                .hasMessageContaining("不含 postgresql")
                .hasMessageContaining("错绑")
                .hasMessageContaining("jdbc:mysql://127.0.0.1:3306/ai_manager_admin");
    }

    @Test
    void jdbcUrl为null时应抛错() {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.getJdbcUrl()).thenReturn(null);

        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(ds, true);

        assertThatThrownBy(probe::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbcUrl 为空");
    }

    @Test
    void postgresql且表存在时应通过() throws Exception {
        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(hikariWithTable(), true);

        assertThatCode(probe::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    void 表不存在时应抛错() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.getJdbcUrl()).thenReturn("jdbc:postgresql://192.168.0.118:5432/ai_manager_rag");
        Connection conn = connectionWithTableCount(0);
        when(ds.getConnection()).thenReturn(conn);

        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(ds, true);

        assertThatThrownBy(probe::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PG 数据源探针失败")
                .hasMessageContaining("rag_vectors 表不存在");
    }

    @Test
    void 连接失败时应抛错() throws Exception {
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.getJdbcUrl()).thenReturn("jdbc:postgresql://192.168.0.118:5432/ai_manager_rag");
        when(ds.getConnection()).thenThrow(new SQLException("Connection refused: PG 不可达"));

        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(ds, true);

        assertThatThrownBy(probe::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("连接 PG 或查询 rag_vectors 表失败")
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    void 开关关闭时应跳过校验() {
        // 故意用 MySQL 连接串：若探针未关闭必然抛错，关闭后应静默通过
        HikariDataSource ds = mock(HikariDataSource.class);
        when(ds.getJdbcUrl()).thenReturn("jdbc:mysql://127.0.0.1:3306/ai_manager_admin");

        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(ds, false);

        assertThatCode(probe::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    void 非Hikari数据源_从连接元数据取URL_表存在时应通过() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = connectionWithTableCount(1);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getURL()).thenReturn("jdbc:postgresql://192.168.0.118:5432/ai_manager_rag");

        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(ds, true);

        assertThatCode(probe::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    void 非Hikari数据源_连接元数据URL为空时应抛错() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getURL()).thenReturn(null);

        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(ds, true);

        assertThatThrownBy(probe::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法从连接元数据获取 jdbcUrl");
    }

    @Test
    void 非Hikari数据源_取URL连接失败时应抛错() throws Exception {
        DataSource ds = mock(DataSource.class);
        when(ds.getConnection()).thenThrow(new SQLException("cannot open connection"));

        PgVectorDataSourceProbe probe = new PgVectorDataSourceProbe(ds, true);

        assertThatThrownBy(probe::afterSingletonsInstantiated)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("获取 jdbcUrl 时连接失败")
                .hasCauseInstanceOf(SQLException.class);
    }
}
