package com.ai.manager.system.service.support;

import com.ai.manager.system.client.BaiduPanClient;
import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.StorageCenterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageDualWriteSupport {

    private final BaiduPanAuthService baiduPanAuthService;
    private final BaiduPanClient baiduPanClient;
    private final BaiduPanProperties baiduPanProperties;
    private final StorageCenterService storageCenterService;

    private final Map<String, Boolean> ensuredDirs = new ConcurrentHashMap<>();

    public Long syncBytes(String cloudDir, String fileName, byte[] bytes) {
        if (!storageCenterService.isDualStorageEnabled()) {
            return null;
        }
        if (!baiduPanAuthService.isAuthorized()) {
            log.warn("百度网盘未授权，文件仅保存到本地: {}/{}", cloudDir, fileName);
            return null;
        }
        try {
            String accessToken = baiduPanAuthService.requireAccessToken();
            ensureDir(accessToken, baiduPanProperties.rootPath());
            ensureDir(accessToken, cloudDir);
            String panPath = cloudDir + "/" + fileName;
            BaiduPanClient.BaiduUploadResponse response = baiduPanClient.upload(accessToken, panPath, bytes);
            return response.getFsId();
        } catch (Exception e) {
            log.error("文件同步到百度网盘失败: {}/{}", cloudDir, fileName, e);
            return null;
        }
    }

    private void ensureDir(String accessToken, String dirPath) throws IOException, InterruptedException {
        if (Boolean.TRUE.equals(ensuredDirs.get(dirPath))) {
            return;
        }
        synchronized (ensuredDirs) {
            if (Boolean.TRUE.equals(ensuredDirs.get(dirPath))) {
                return;
            }
            baiduPanClient.ensureDir(accessToken, dirPath);
            ensuredDirs.put(dirPath, true);
        }
    }
}
