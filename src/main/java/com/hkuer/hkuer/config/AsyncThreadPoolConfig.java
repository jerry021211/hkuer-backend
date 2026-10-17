package com.hkuer.hkuer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置
 * 用于浏览量统计等异步任务
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncThreadPoolConfig {

    /**
     * 浏览量统计专用线程池
     * 
     * 设计原则：
     * - 核心线程数：CPU核心数（IO密集型任务可适当增加）
     * - 最大线程数：核心线程数 * 2
     * - 队列容量：较大，因为浏览量统计是轻量级任务
     * - 拒绝策略：CallerRunsPolicy，由调用线程执行，保证数据不丢失
     */
    @Bean("viewCountExecutor")
    public Executor viewCountExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 获取CPU核心数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        // 核心线程数：CPU核心数
        executor.setCorePoolSize(cpuCores);
        
        // 最大线程数：CPU核心数 * 2
        executor.setMaxPoolSize(cpuCores * 2);
        
        // 队列容量：5000，浏览量统计任务轻量，可设置较大队列
        executor.setQueueCapacity(5000);
        
        // 线程名前缀
        executor.setThreadNamePrefix("view-count-");
        
        // 空闲线程存活时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 拒绝策略：由调用线程执行，保证数据不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("浏览量统计线程池初始化完成，核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                cpuCores, cpuCores * 2, 5000);
        
        return executor;
    }
}
