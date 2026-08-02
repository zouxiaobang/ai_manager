package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.domain.dto.NbNotebookSaveRequest;
import com.ai.manager.system.domain.entity.NbNote;
import com.ai.manager.system.domain.entity.NbNotebook;
import com.ai.manager.system.domain.vo.NbNotebookVO;
import com.ai.manager.system.mapper.NbNoteMapper;
import com.ai.manager.system.mapper.NbNotebookMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NbNotebookServiceImpl 服务层单元测试
 * Mock 掉 Mapper，覆盖创建/更新/删除与 VO 映射（剔除 deleted 字段）。
 */
@ExtendWith(MockitoExtension.class)
class NbNotebookServiceImplTest {

    @Mock
    private NbNotebookMapper nbNotebookMapper;

    @Mock
    private NbNoteMapper nbNoteMapper;

    private NbNotebookServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        // 无 Spring 容器时 LambdaQueryWrapper 的 lambda 解析需要元数据缓存，需手动初始化相关实体
        initTable(NbNotebook.class);
        initTable(NbNote.class);
    }

    private static void initTable(Class<?> clazz) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), clazz);
    }

    @BeforeEach
    void setUp() {
        service = new NbNotebookServiceImpl(nbNoteMapper);
        ReflectionTestUtils.setField(service, "baseMapper", nbNotebookMapper);
    }

    private NbNotebook entity(Long id, String name) {
        NbNotebook nb = new NbNotebook();
        nb.setId(id);
        nb.setParentId(null);
        nb.setName(name);
        nb.setIcon("📁");
        nb.setColor("#409EFF");
        nb.setSortOrder(1);
        nb.setDeleted(0);
        return nb;
    }

    @Test
    void createNotebook_shouldInsertAndReturnVO() {
        // nextSortOrder 的 getOne 内部调用 selectOne(wrapper, true) 双参重载
        when(nbNotebookMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        NbNotebookSaveRequest req = new NbNotebookSaveRequest();
        req.setName(" 新文件夹 ");

        NbNotebookVO vo = service.createNotebook(req);

        assertThat(vo.getName()).isEqualTo("新文件夹");        verify(nbNotebookMapper).insert(any(NbNotebook.class));
    }

    @Test
    void createNotebook_withMissingParent_shouldThrow() {
        when(nbNotebookMapper.selectById(5L)).thenReturn(null);

        NbNotebookSaveRequest req = new NbNotebookSaveRequest();
        req.setName("子文件夹");
        req.setParentId(5L);

        assertThatThrownBy(() -> service.createNotebook(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父文件夹不存在");
    }

    @Test
    void createNotebook_withBlankName_shouldThrow() {
        NbNotebookSaveRequest req = new NbNotebookSaveRequest();
        req.setName("   ");

        assertThatThrownBy(() -> service.createNotebook(req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateNotebook_shouldUpdateAndReturnVO() {
        when(nbNotebookMapper.selectById(1L)).thenReturn(entity(1L, "旧名"));

        NbNotebookSaveRequest req = new NbNotebookSaveRequest();
        req.setName("新名");

        NbNotebookVO vo = service.updateNotebook(1L, req);

        assertThat(vo.getName()).isEqualTo("新名");
        verify(nbNotebookMapper).updateById(any(NbNotebook.class));
    }

    @Test
    void updateNotebook_whenMissing_shouldThrow() {
        when(nbNotebookMapper.selectById(99L)).thenReturn(null);

        NbNotebookSaveRequest req = new NbNotebookSaveRequest();
        req.setName("文件夹");

        assertThatThrownBy(() -> service.updateNotebook(99L, req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateNotebook_moveToSelf_shouldThrow() {
        when(nbNotebookMapper.selectById(1L)).thenReturn(entity(1L, "文件夹"));

        NbNotebookSaveRequest req = new NbNotebookSaveRequest();
        req.setName("文件夹");
        req.setParentId(1L);

        assertThatThrownBy(() -> service.updateNotebook(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自身");
    }

    @Test
    void deleteNotebook_whenMissing_shouldThrow() {
        when(nbNotebookMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteNotebook(99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteNotebook_shouldDeleteDescendantFolderIds() {
        when(nbNotebookMapper.selectById(1L)).thenReturn(entity(1L, "根"));
        // 无子文件夹：selectList（collectDescendantFolderIds）返回空，deleteOrder 仅含自身
        when(nbNotebookMapper.selectList(any())).thenReturn(List.of());
        // selectBatchIds 为辅助 stub（让 listByIds 不 NPE），Mockito strict 对泛型方法偶发误报，用 lenient 豁免
        lenient().when(nbNotebookMapper.selectBatchIds(any())).thenReturn(List.of(entity(1L, "根")));

        service.deleteNotebook(1L);

        verify(nbNoteMapper).delete(any());
        verify(nbNotebookMapper).deleteById(1L);
    }
}
