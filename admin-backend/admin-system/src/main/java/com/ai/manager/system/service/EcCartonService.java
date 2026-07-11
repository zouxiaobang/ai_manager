package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.EcCartonSaveRequest;
import com.ai.manager.system.domain.entity.EcCarton;
import com.ai.manager.system.domain.vo.EcCartonBackfillTaskVO;
import com.ai.manager.system.domain.vo.EcCartonCalculateResultVO;
import com.ai.manager.system.domain.vo.EcCartonListItemVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 电商纸箱服务接口
 *
 * <p>提供电商纸箱的分页查询、详情查看、创建、更新、删除等基础CRUD功能，
 * 同时支持纸箱匹配、SKU纸箱回填、纸箱用量计算、异步回填任务管理等纸箱相关业务操作。</p>
 */
public interface EcCartonService extends IService<EcCarton> {

    /**
     * 分页查询纸箱列表
     *
     * @param keyword  关键词（纸箱名称、规格等）
     * @param page     页码
     * @param pageSize 每页条数
     * @return 纸箱分页结果
     */
    PageResult<EcCartonListItemVO> pageCartons(String keyword, Long page, Long pageSize);

    /**
     * 获取纸箱详情
     *
     * @param id 纸箱ID
     * @return 纸箱详情信息
     */
    EcCartonListItemVO getCartonDetail(Long id);

    /**
     * 创建纸箱
     *
     * @param request 纸箱保存请求参数
     * @return 创建后的纸箱信息
     */
    EcCartonListItemVO createCarton(EcCartonSaveRequest request);

    /**
     * 更新纸箱
     *
     * @param id      纸箱ID
     * @param request 纸箱保存请求参数
     * @return 更新后的纸箱信息
     */
    EcCartonListItemVO updateCarton(Long id, EcCartonSaveRequest request);

    /**
     * 更新纸箱预览图片
     *
     * @param id         纸箱ID
     * @param file       图片文件
     * @param cartonName 纸箱名称
     * @return 更新后的纸箱信息
     */
    EcCartonListItemVO updateCartonPreviewImage(Long id, MultipartFile file, String cartonName);

    /**
     * 删除纸箱
     *
     * @param id 纸箱ID
     */
    void deleteCarton(Long id);

    /**
     * 匹配纸箱
     *
     * @param lengthCm 长度（厘米）
     * @param widthCm  宽度（厘米）
     * @param heightCm 高度（厘米）
     * @param factoryId 工厂ID
     * @return 匹配到的纸箱信息
     */
    EcCartonListItemVO matchCarton(java.math.BigDecimal lengthCm,
                                   java.math.BigDecimal widthCm,
                                   java.math.BigDecimal heightCm,
                                   Long factoryId);

    /**
     * 为全部SKU按单品尺寸回填carton_id
     *
     * <p>与match规则一致，含旋转匹配</p>
     *
     * @return 回填成功的SKU数量
     */
    int backfillSkuCartons();

    /**
     * 计算纸箱用量
     *
     * @param lengthCm  长度（厘米）
     * @param widthCm   宽度（厘米）
     * @param heightCm  高度（厘米）
     * @param factoryId 工厂ID
     * @return 纸箱计算结果
     */
    EcCartonCalculateResultVO calculateCartons(java.math.BigDecimal lengthCm,
                                               java.math.BigDecimal widthCm,
                                               java.math.BigDecimal heightCm,
                                               Long factoryId);

    /**
     * 异步启动SKU纸箱回填任务
     *
     * @return 任务ID
     */
    String startBackfillSkuCartonsAsync();

    /**
     * 获取回填任务状态
     *
     * @param taskId 任务ID
     * @return 回填任务信息
     */
    EcCartonBackfillTaskVO getBackfillTask(String taskId);
}
