package com.ai.manager.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * RAG 文档异步处理线程池
 *
 * <p>文档上传后，解析/分块/嵌入/存向量在独立线程池执行，避免长时间占用 HTTP 线程与数据库事务。
 * 核心线程数取小值（嵌入调用外部 API 为 IO 密集，并发过高反而拖慢），
 * 队列缓冲突发上传，进程关闭时优雅等待进行中的任务完成。</p>
 */
@Configuration
public class RagAsyncConfig {

    @Bean(name = "ragProcessExecutor")
    public ThreadPoolTaskExecutor ragProcessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("rag-process-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
