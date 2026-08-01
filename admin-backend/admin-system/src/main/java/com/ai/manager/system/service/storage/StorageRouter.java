package com.ai.manager.system.service.storage;

import com.ai.manager.system.config.NoteStorageProperties;
import com.ai.manager.system.domain.vo.StorageCenterConfigVO;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.StorageCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储路由器 — 根据配置和授权状态，决定使用哪种 {@link StorageChain}。
 *
 * <p>职责：将"选择存储链"的逻辑从业务服务中剥离，集中管理。</p>
 *
 * <p>路由逻辑：</p>
 * <ol>
 *   <li>如果双写开启 → {@code [本地, 网盘]}（网盘未授权则仅 {@code [本地]}）</li>
 *   <li>如果双写关闭 → 按 {@code noteStorageProperties.type} 决定</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageRouter {

    private final LocalFileNoteContentStorage localStorage;
    private final BaiduPanNoteContentStorage baiduPanStorage;
    private final StorageCenterService storageCenterService;
    private final BaiduPanAuthService baiduPanAuthService;
    private final NoteStorageProperties noteStorageProperties;

    /**
     * 解析当前可用的存储链（用于双写场景）。
     */
    public StorageChain resolve() {
        StorageCenterConfigVO config = storageCenterService.getConfig();
        boolean authorized = baiduPanAuthService.isAuthorized();
        boolean dualEnabled = Boolean.TRUE.equals(config.getDualStorageEnabled());

        if (dualEnabled) {
            List<NoteContentStorage> storages = new ArrayList<>();
            storages.add(localStorage);
            if (authorized) {
                storages.add(baiduPanStorage);
            } else {
                log.warn("双写已开启但百度网盘未授权，仅使用本地存储");
            }
            return new OrderedChain(storages);
        }

        // 旧版模式：按配置选择
        return resolveLegacy(config, authorized);
    }

    /**
     * 解析旧版模式的存储链（单存储，不使用双写）。
     */
    public StorageChain resolveLegacy() {
        return resolveLegacy(storageCenterService.getConfig(), baiduPanAuthService.isAuthorized());
    }

    private StorageChain resolveLegacy(StorageCenterConfigVO config, boolean authorized) {
        String type = noteStorageProperties.getType();
        if ("BAIDU_PAN".equalsIgnoreCase(type) && authorized) {
            return new OrderedChain(List.of(baiduPanStorage));
        }
        if ("BAIDU_PAN".equalsIgnoreCase(type) && !authorized) {
            log.warn("百度网盘授权不可用，降级到本地存储");
        }
        return new OrderedChain(List.of(localStorage));
    }

    /**
     * 双写是否可用（已开启且已授权）。
     */
    public boolean isDualWriteAvailable() {
        return Boolean.TRUE.equals(storageCenterService.getConfig().getDualStorageEnabled())
                && baiduPanAuthService.isAuthorized();
    }
}
