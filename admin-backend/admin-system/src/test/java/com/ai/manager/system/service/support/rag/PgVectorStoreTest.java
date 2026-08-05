package com.ai.manager.system.service.support.rag;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PgVectorStore 单元测试
 *
 * <p>验证向量存储的 insert / delete / 相似度查询都基于构造器注入的 DataSource 执行，
 * 且使用 pgvector 语法（?::vector）。数据源路由是否正确由
 * {@code PgVectorDataSourceConfigTest} 验证（pgVectorDataSource 必须绑定 PostgreSQL）。</p>
 */
class PgVectorStoreTest {

    /** 构造 DataSource → Connection → PreparedStatement → ResultSet 的 mock 链，并让 batchUpdate 通过 supportsBatchUpdates 检查 */
    private PreparedStatement mockStatement(DataSource ds, Connection conn, PreparedStatement ps,
                                            DatabaseMetaData dbmd) throws Exception {
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(conn.getMetaData()).thenReturn(dbmd);
        when(dbmd.supportsBatchUpdates()).thenReturn(true);
        when(ps.getConnection()).thenReturn(conn);
        return ps;
    }

    @Test
    void storeBatch_应在注入的数据源上执行pgvector批量插入() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
        when(ps.executeBatch()).thenReturn(new int[]{1});
        mockStatement(ds, conn, ps, dbmd);

        PgVectorStore store = new PgVectorStore(ds);
        store.storeBatch(List.of(PgVectorStore.VectorRecord.builder()
                .chunkId(1L)
                .docId(2L)
                .embedding(new float[]{0.1f, 0.2f})
                .content("内容")
                .build()));

        verify(conn).prepareStatement(contains("?::vector"));
        verify(ps).setLong(1, 1L);
        verify(ps).setLong(2, 2L);
        verify(ps).setString(3, "[0.1,0.2]");
        verify(ps).setString(4, "内容");
        verify(ps).executeBatch();
    }

    @Test
    void store_应在注入的数据源上执行pgvector单条插入() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
        when(ps.executeUpdate()).thenReturn(1);
        mockStatement(ds, conn, ps, dbmd);

        PgVectorStore store = new PgVectorStore(ds);
        store.store(3L, 4L, new float[]{0.5f}, "片段");

        verify(conn).prepareStatement(contains("?::vector"));
        verify(ps).setObject(1, 3L);
        verify(ps).setObject(2, 4L);
        verify(ps).setString(3, "[0.5]");
        verify(ps).setString(4, "片段");
        verify(ps).executeUpdate();
    }

    @Test
    void deleteByDocId_应在注入的数据源上按文档删除() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
        when(ps.executeUpdate()).thenReturn(2);
        mockStatement(ds, conn, ps, dbmd);

        PgVectorStore store = new PgVectorStore(ds);
        store.deleteByDocId(9L);

        verify(conn).prepareStatement("DELETE FROM rag_vectors WHERE doc_id = ?");
        verify(ps).setObject(1, 9L);
        verify(ps).executeUpdate();
    }

    @Test
    void deleteByChunkIds_应在注入的数据源上按chunk批量删除() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
        when(ps.executeUpdate()).thenReturn(1);
        mockStatement(ds, conn, ps, dbmd);

        PgVectorStore store = new PgVectorStore(ds);
        store.deleteByChunkIds(List.of(10L, 11L));

        verify(conn).prepareStatement("DELETE FROM rag_vectors WHERE chunk_id IN (?,?)");
        verify(ps).setObject(1, 10L);
        verify(ps).setObject(2, 11L);
        verify(ps).executeUpdate();
    }

    @Test
    void deleteByChunkIds_空列表直接返回不执行SQL() {
        DataSource ds = mock(DataSource.class);

        PgVectorStore store = new PgVectorStore(ds);
        store.deleteByChunkIds(List.of());

        verifyNoInteractions(ds);
    }

    @Test
    void similaritySearch_应在注入的数据源上执行余弦相似度查询() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
        ResultSet rs = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(rs);
        mockStatement(ds, conn, ps, dbmd);
        when(rs.next()).thenReturn(true, false);
        when(rs.getLong("chunk_id")).thenReturn(5L);
        when(rs.getLong("doc_id")).thenReturn(6L);
        when(rs.getString("content")).thenReturn("匹配片段");
        when(rs.getDouble("score")).thenReturn(0.85);

        PgVectorStore store = new PgVectorStore(ds);
        List<PgVectorStore.SearchResult> results =
                store.similaritySearch(new float[]{0.1f, 0.2f}, 5, 0.65);

        verify(conn).prepareStatement(contains("<=> ?::vector"));
        verify(ps).executeQuery();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChunkId()).isEqualTo(5L);
        assertThat(results.get(0).getDocId()).isEqualTo(6L);
        assertThat(results.get(0).getContent()).isEqualTo("匹配片段");
        assertThat(results.get(0).getScore()).isEqualTo(0.85);
    }
}
