package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcFactorySaveRequest;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.entity.EcFactory;
import com.ai.manager.system.domain.entity.EcProduct;
import com.ai.manager.system.domain.vo.EcFactoryStatsVO;
import com.ai.manager.system.domain.vo.EcFactoryVO;
import com.ai.manager.system.mapper.EcCartonMapper;
import com.ai.manager.system.mapper.EcFactoryMapper;
import com.ai.manager.system.mapper.EcProductMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EcFactoryServiceImpl 服务层单元测试
 * Mock 掉三个 Mapper，覆盖分页/选项/详情/创建/更新/删除与 VO 映射（剔除 deleted 字段）。
 */
@ExtendWith(MockitoExtension.class)
class EcFactoryServiceImplTest {

    @Mock
    private EcFactoryMapper ecFactoryMapper;

    @Mock
    private EcProductMapper ecProductMapper;

    @Mock
    private EcCartonMapper ecCartonMapper;

    private EcFactoryServiceImpl service;

    @BeforeAll
    static void initMybatisPlus() {
        // 无 Spring 容器时 LambdaQueryWrapper 的 lambda 解析需要元数据缓存，需手动初始化相关实体
        initTable(EcFactory.class);
        initTable(EcProduct.class);
        initTable(EcCarton.class);
    }

    private static void initTable(Class<?> clazz) {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), clazz);
    }

    @BeforeEach
    void setUp() {
        service = new EcFactoryServiceImpl(ecProductMapper, ecCartonMapper);
        // ServiceImpl 的 baseMapper 位于父类泛型字段，@InjectMocks 无法注入，需反射注入
        ReflectionTestUtils.setField(service, "baseMapper", ecFactoryMapper);
    }

    private EcFactory entity(Long id) {
        EcFactory f = new EcFactory();
        f.setId(id);
        f.setName("工厂" + id);
        f.setFactoryType("PRODUCTION");
        f.setContactName("张三");
        f.setStatus("ENABLED");
        f.setDeleted(0);
        return f;
    }

    @Test
    void pageFactories_shouldMapToVOWithoutDeleted() {
        Page<EcFactory> entityPage = new Page<>(1, 10);
        entityPage.setRecords(List.of(entity(1L)));
        entityPage.setTotal(1);
        when(ecFactoryMapper.selectPage(any(), any())).thenReturn(entityPage);

        PageResult<EcFactoryVO> result = service.pageFactories(null, null, null, 1L, 10L);

        assertThat(result.getRecords()).hasSize(1);
        EcFactoryVO vo = result.getRecords().get(0);
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getName()).isEqualTo("工厂1");
    }

    @Test
    void listFactoryOptions_shouldMapToVO() {
        when(ecFactoryMapper.selectList(any())).thenReturn(List.of(entity(2L)));

        List<EcFactoryVO> result = service.listFactoryOptions("PRODUCTION");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getFactory_whenExists_shouldReturnVO() {
        when(ecFactoryMapper.selectById(1L)).thenReturn(entity(1L));

        EcFactoryVO vo = service.getFactory(1L);

        assertThat(vo).isNotNull();
        assertThat(vo.getName()).isEqualTo("工厂1");
    }

    @Test
    void getFactory_whenMissing_shouldReturnNull() {
        when(ecFactoryMapper.selectById(99L)).thenReturn(null);

        assertThat(service.getFactory(99L)).isNull();
    }

    @Test
    void createFactory_shouldInsertAndReturnVO() {
        EcFactorySaveRequest req = new EcFactorySaveRequest();
        req.setName("  新工厂  ");
        req.setFactoryType("customer");
        req.setContactName("李四");

        EcFactoryVO vo = service.createFactory(req);

        assertThat(vo.getName()).isEqualTo("新工厂");
        assertThat(vo.getFactoryType()).isEqualTo("CUSTOMER");
        verify(ecFactoryMapper).insert(any(EcFactory.class));
    }

    @Test
    void createFactory_withInvalidType_shouldThrow() {
        EcFactorySaveRequest req = new EcFactorySaveRequest();
        req.setName("工厂");
        req.setFactoryType("UNKNOWN");

        assertThatThrownBy(() -> service.createFactory(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工厂类型无效");
        verify(ecFactoryMapper, never()).insert(any(EcFactory.class));
    }

    @Test
    void updateFactory_shouldUpdateAndReturnVO() {
        when(ecFactoryMapper.selectById(1L)).thenReturn(entity(1L));
        EcFactorySaveRequest req = new EcFactorySaveRequest();
        req.setName("改名后");
        req.setFactoryType("PRODUCTION");

        EcFactoryVO vo = service.updateFactory(1L, req);

        assertThat(vo.getName()).isEqualTo("改名后");
        verify(ecFactoryMapper).updateById(any(EcFactory.class));
    }

    @Test
    void updateFactory_whenMissing_shouldThrow() {
        when(ecFactoryMapper.selectById(99L)).thenReturn(null);
        EcFactorySaveRequest req = new EcFactorySaveRequest();
        req.setName("工厂");
        req.setFactoryType("PRODUCTION");

        assertThatThrownBy(() -> service.updateFactory(99L, req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteFactory_withBoundProduct_shouldReject() {
        when(ecFactoryMapper.selectById(1L)).thenReturn(entity(1L));
        when(ecProductMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.deleteFactory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仍有商品");
        verify(ecFactoryMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteFactory_whenBoundCarton_shouldReject() {
        when(ecFactoryMapper.selectById(1L)).thenReturn(entity(1L));
        when(ecProductMapper.selectCount(any())).thenReturn(0L);
        when(ecCartonMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteFactory(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仍有纸箱");
    }

    @Test
    void deleteFactory_whenUnbound_shouldRemove() {
        when(ecFactoryMapper.selectById(1L)).thenReturn(entity(1L));
        when(ecProductMapper.selectCount(any())).thenReturn(0L);
        when(ecCartonMapper.selectCount(any())).thenReturn(0L);

        service.deleteFactory(1L);

        verify(ecFactoryMapper).deleteById(any(Long.class));
    }

    @Test
    void getFactoryStats_shouldCountByTypeAndStatus() {
        when(ecFactoryMapper.selectCount(any())).thenReturn(3L);

        EcFactoryStatsVO stats = service.getFactoryStats();

        assertThat(stats.getProductionCount()).isEqualTo(3L);
        assertThat(stats.getCustomerCount()).isEqualTo(3L);
        assertThat(stats.getCartonCount()).isEqualTo(3L);
        assertThat(stats.getEnabledCount()).isEqualTo(3L);
        assertThat(stats.getDisabledCount()).isEqualTo(3L);
    }
}
