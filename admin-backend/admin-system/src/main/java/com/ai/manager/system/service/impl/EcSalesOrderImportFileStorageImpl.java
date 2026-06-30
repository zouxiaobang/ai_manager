package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.client.BaiduPanClient;
import com.ai.manager.system.config.BaiduPanProperties;
import com.ai.manager.system.domain.entity.SysImportBatch;
import com.ai.manager.system.service.BaiduPanAuthService;
import com.ai.manager.system.service.EcSalesOrderImportFileStorage;
import com.ai.manager.system.service.StorageCenterService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcSalesOrderImportFileStorageImpl implements EcSalesOrderImportFileStorage {

    private final BaiduPanAuthService baiduPanAuthService;
    private final BaiduPanClient baiduPanClient;
    private final BaiduPanProperties baiduPanProperties;
    private final ObjectMapper objectMapper;
    private final StorageCenterService storageCenterService;

    @Value("${ai-manager.upload.ecommerce-path:uploads/ecommerce}")
    private String ecommerceUploadPath;

    private Path importDir;
    private volatile boolean panImportsDirEnsured;

    @PostConstruct
    public void init() throws IOException {
        Path uploadBase = resolveUploadBasePath(ecommerceUploadPath);
        importDir = uploadBase.resolve("imports").resolve("sales-orders").toAbsolutePath().normalize();
        Files.createDirectories(importDir);
        log.info("销售订单导入文件目录: {}", importDir);
    }

    /**
     * 统一解析上传根目录：支持绝对路径；相对路径时兼容从 admin-backend 或 admin-server 模块启动。
     */
    private static Path resolveUploadBasePath(String configuredPath) {
        Path configured = Paths.get(configuredPath);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if ("admin-server".equals(userDir.getFileName().toString())) {
            Path parent = userDir.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                return parent.resolve(configuredPath).normalize();
            }
        }
        return userDir.resolve(configuredPath).normalize();
    }

    @Override
    public SaveResult save(String batchNo, String originalFilename, byte[] bytes)
            throws IOException, InterruptedException {
        storageCenterService.assertWritable(StorageCenterServiceImpl.ZONE_IMPORT_FILES, bytes.length);
        String storedName = batchNo + "_" + sanitizeFilename(originalFilename);
        Path target = importDir.resolve(storedName).normalize();
        if (!target.startsWith(importDir)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法文件名");
        }
        Files.write(target, bytes);
        storageCenterService.onFileWritten(StorageCenterServiceImpl.ZONE_IMPORT_FILES, bytes.length);
        String panPath = baiduPanProperties.salesOrderImportsDir() + "/" + storedName;
        Long fsId = syncToBaiduPan(panPath, bytes);
        return new SaveResult(storedName, panPath, fsId, bytes.length);
    }

    @Override
    public byte[] load(SysImportBatch batch) throws IOException, InterruptedException {
        byte[] localBytes = tryLoadLocal(batch);
        if (localBytes != null && localBytes.length > 0) {
            return localBytes;
        }
        String panPath = batch.getFilePath();
        Long fsId = readStorageFsId(batch);
        if (StringUtils.hasText(panPath) && baiduPanAuthService.isAuthorized()) {
            try {
                String accessToken = baiduPanAuthService.requireAccessToken();
                byte[] panBytes = baiduPanClient.downloadBytes(accessToken, panPath, fsId);
                if (panBytes != null && panBytes.length > 0) {
                    return panBytes;
                }
            } catch (Exception ex) {
                log.warn("从百度网盘读取销售订单导入文件失败 batchNo={} path={}: {}",
                        batch.getBatchNo(), panPath, ex.getMessage());
            }
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "导入文件不存在或无法读取，请重新上传");
    }

    @Override
    public boolean exists(SysImportBatch batch) {
        try {
            if (hasLocalFile(batch)) {
                return true;
            }
        } catch (IOException ex) {
            log.debug("检查销售订单导入本地文件失败 batchNo={}: {}", batch.getBatchNo(), ex.getMessage());
        }
        String panPath = batch.getFilePath();
        Long fsId = readStorageFsId(batch);
        return StringUtils.hasText(panPath) && fsId != null && baiduPanAuthService.isAuthorized();
    }

    private boolean hasLocalFile(SysImportBatch batch) throws IOException {
        for (String candidate : resolveLocalFileCandidates(batch)) {
            Path local = importDir.resolve(candidate).normalize();
            if (local.startsWith(importDir) && Files.isRegularFile(local)) {
                return true;
            }
        }
        return false;
    }

    private byte[] tryLoadLocal(SysImportBatch batch) throws IOException {
        for (String candidate : resolveLocalFileCandidates(batch)) {
            Path local = importDir.resolve(candidate).normalize();
            if (!local.startsWith(importDir) || !Files.isRegularFile(local)) {
                continue;
            }
            log.debug("读取销售订单导入本地文件: {}", local);
            return Files.readAllBytes(local);
        }
        return null;
    }

    private List<String> resolveLocalFileCandidates(SysImportBatch batch) throws IOException {
        Set<String> candidates = new LinkedHashSet<>();
        String fromContext = readLocalStoredName(batch);
        if (StringUtils.hasText(fromContext)) {
            candidates.add(fromContext.trim());
        }
        if (StringUtils.hasText(batch.getBatchNo()) && StringUtils.hasText(batch.getFileName())) {
            candidates.add(batch.getBatchNo() + "_" + sanitizeFilename(batch.getFileName()));
        }
        if (StringUtils.hasText(batch.getBatchNo()) && Files.isDirectory(importDir)) {
            String prefix = batch.getBatchNo() + "_";
            try (Stream<Path> stream = Files.list(importDir)) {
                stream.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> name.startsWith(prefix))
                        .forEach(candidates::add);
            }
        }
        return new ArrayList<>(candidates);
    }

    private Long syncToBaiduPan(String panPath, byte[] bytes) {
        if (!storageCenterService.isDualStorageEnabled()) {
            return null;
        }
        if (!baiduPanAuthService.isAuthorized()) {
            log.warn("百度网盘未授权，销售订单导入文件仅保存到本地: {}", panPath);
            return null;
        }
        try {
            String accessToken = baiduPanAuthService.requireAccessToken();
            ensurePanImportsDir(accessToken);
            BaiduPanClient.BaiduUploadResponse response = baiduPanClient.upload(accessToken, panPath, bytes);
            return response.getFsId();
        } catch (Exception e) {
            log.error("销售订单导入文件同步到百度网盘失败: {}", panPath, e);
            return null;
        }
    }

    private void ensurePanImportsDir(String accessToken) throws IOException, InterruptedException {
        if (panImportsDirEnsured) {
            return;
        }
        synchronized (this) {
            if (panImportsDirEnsured) {
                return;
            }
            baiduPanClient.ensureDir(accessToken, baiduPanProperties.rootPath());
            baiduPanClient.ensureDir(accessToken, baiduPanProperties.salesOrderImportsDir());
            panImportsDirEnsured = true;
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String name = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "import.xlsx";
        name = name.replace('\\', '_').replace('/', '_');
        if (name.length() > 120) {
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, Math.min(dot, 100)) + name.substring(dot);
            } else {
                name = name.substring(0, 120);
            }
        }
        return name;
    }

    private String readLocalStoredName(SysImportBatch batch) {
        if (!StringUtils.hasText(batch.getBizContext())) {
            return null;
        }
        try {
            Map<String, Object> ctx = objectMapper.readValue(batch.getBizContext(), new TypeReference<>() {});
            Object localStoredName = ctx.get("localStoredName");
            return localStoredName == null ? null : String.valueOf(localStoredName);
        } catch (Exception ex) {
            return null;
        }
    }

    private Long readStorageFsId(SysImportBatch batch) {
        if (!StringUtils.hasText(batch.getBizContext())) {
            return null;
        }
        try {
            Map<String, Object> ctx = objectMapper.readValue(batch.getBizContext(), new TypeReference<>() {});
            Object fsId = ctx.get("storageFsId");
            if (fsId == null) {
                return null;
            }
            return Long.valueOf(String.valueOf(fsId));
        } catch (Exception ex) {
            return null;
        }
    }
}
