package com.ai.manager.system.service;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.domain.dto.StorageCenterConfigSaveRequest;
import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import com.ai.manager.system.domain.vo.StorageCenterOverviewVO;
import com.ai.manager.system.domain.vo.StorageCleanupResultVO;
import com.ai.manager.system.domain.vo.StorageImageItemVO;
import com.ai.manager.system.domain.vo.StorageOrphanPreviewVO;
import com.ai.manager.system.domain.vo.StorageOrphanZonePreviewVO;

/**
 * 存储中心服务接口
 *
 * <p>提供存储中心的概览统计、配置管理、孤儿文件清理、缓存清理、
 * 双存储状态判断、超限策略解析、缓存限制强制、可写性校验、
 * 文件写入回调和项目图片浏览等存储管理功能。</p>
 */
public interface StorageCenterService {

    /**
     * 获取存储中心概览
     *
     * @return 存储中心概览信息
     */
    StorageCenterOverviewVO getOverview();

    /**
     * 获取存储中心配置
     *
     * @return 存储中心配置信息
     */
    StorageCenterConfigVO getConfig();

    /**
     * 保存存储中心配置
     *
     * @param request 配置保存请求参数
     * @return 保存后的配置信息
     */
    StorageCenterConfigVO saveConfig(StorageCenterConfigSaveRequest request);

    /**
     * 清理指定区域的孤儿文件
     *
     * @param zone   区域
     * @param dryRun 是否试运行
     * @return 清理结果
     */
    StorageCleanupResultVO cleanupOrphans(String zone, boolean dryRun);

    /**
     * 预览所有孤儿文件
     *
     * @return 孤儿文件预览结果
     */
    StorageOrphanPreviewVO previewAllOrphans();

    /**
     * 清理所有孤儿文件
     *
     * @param dryRun 是否试运行
     * @return 清理结果
     */
    StorageOrphanPreviewVO cleanupAllOrphans(boolean dryRun);

    /**
     * 预览指定区域的孤儿文件
     *
     * @param zone 区域
     * @return 区域孤儿文件预览结果
     */
    StorageOrphanZonePreviewVO previewOrphanZone(String zone);

    /**
     * 删除孤儿文件
     *
     * @param zone         区域
     * @param relativePath 相对路径
     * @return 删除结果
     */
    StorageCleanupResultVO deleteOrphanFile(String zone, String relativePath);

    /**
     * 清理缓存
     *
     * @param dryRun 是否试运行
     * @return 清理结果
     */
    StorageCleanupResultVO cleanupCache(boolean dryRun);

    /**
     * 判断双存储是否启用
     *
     * @return 双存储是否启用
     */
    boolean isDualStorageEnabled();

    /**
     * 解析超限策略
     *
     * @param zoneKey 区域键
     * @return 超限策略
     */
    String resolveOverLimitStrategy(String zoneKey);

    /**
     * 强制缓存限制
     *
     * @param incomingBytes 传入字节数
     */
    void enforceCacheLimit(long incomingBytes);

    /**
     * 校验是否可写
     *
     * @param zoneKey        区域键
     * @param additionalBytes 额外字节数
     */
    void assertWritable(String zoneKey, long additionalBytes);

    /**
     * 文件写入回调
     *
     * @param zoneKey       区域键
     * @param bytesWritten  写入字节数
     */
    void onFileWritten(String zoneKey, long bytesWritten);

    /**
     * 浏览项目图片
     *
     * @param zone     区域
     * @param keyword  关键词
     * @param page     页码
     * @param pageSize 每页条数
     * @return 图片分页结果
     */
    PageResult<StorageImageItemVO> browseProjectImages(
            String zone,
            String keyword,
            Long page,
            Long pageSize
    );
}
