package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.vo.EcCartonBackfillTaskVO;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class EcCartonBackfillTaskManager {

    private static final int MAX_TASKS = 20;

    private final ConcurrentHashMap<String, EcCartonBackfillTaskVO> tasks = new ConcurrentHashMap<>();

    public String startTask(Consumer<EcCartonBackfillTaskVO> worker) {
        EcCartonBackfillTaskVO running = findRunningTask();
        if (running != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已有 SKU 纸箱回填任务正在进行");
        }

        String taskId = UUID.randomUUID().toString();
        EcCartonBackfillTaskVO task = new EcCartonBackfillTaskVO();
        task.setTaskId(taskId);
        task.setStatus("PENDING");
        tasks.put(taskId, task);
        trimOldTasks();

        CompletableFuture.runAsync(() -> {
            task.setStatus("RUNNING");
            try {
                worker.accept(task);
                if (!"FAILED".equals(task.getStatus())) {
                    task.setStatus("COMPLETED");
                }
            } catch (Exception ex) {
                task.setStatus("FAILED");
                task.setMessage(ex.getMessage() != null ? ex.getMessage() : "回填失败");
            }
        });

        return taskId;
    }

    public EcCartonBackfillTaskVO getTask(String taskId) {
        EcCartonBackfillTaskVO task = tasks.get(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "任务不存在或已过期");
        }
        return task;
    }

    private EcCartonBackfillTaskVO findRunningTask() {
        for (EcCartonBackfillTaskVO task : tasks.values()) {
            if ("PENDING".equals(task.getStatus()) || "RUNNING".equals(task.getStatus())) {
                return task;
            }
        }
        return null;
    }

    private void trimOldTasks() {
        if (tasks.size() <= MAX_TASKS) {
            return;
        }
        tasks.entrySet().removeIf(entry -> {
            String status = entry.getValue().getStatus();
            return "COMPLETED".equals(status) || "FAILED".equals(status);
        });
    }
}
