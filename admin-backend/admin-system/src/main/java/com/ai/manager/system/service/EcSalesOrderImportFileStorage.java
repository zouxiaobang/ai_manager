package com.ai.manager.system.service;

import com.ai.manager.system.domain.entity.SysImportBatch;

import java.io.IOException;

public interface EcSalesOrderImportFileStorage {

    record SaveResult(String localStoredName, String panPath, Long storageFsId, long fileSize) {}

    SaveResult save(String batchNo, String originalFilename, byte[] bytes) throws IOException, InterruptedException;

    byte[] load(SysImportBatch batch) throws IOException, InterruptedException;

    /** 原始导入文件是否仍可读取（重新解析依赖此项） */
    boolean exists(SysImportBatch batch);
}
