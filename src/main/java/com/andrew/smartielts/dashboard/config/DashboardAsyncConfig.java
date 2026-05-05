package com.andrew.smartielts.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class DashboardAsyncConfig {

    @Bean(name = "dashboardSseExecutor")
    public Executor dashboardSseExecutor() {
        int corePoolSize = 4;
        int maxPoolSize = 16;
        int queueCapacity = 200;
        long keepAliveSeconds = 60L;

        ThreadPoolExecutor delegate = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("dashboard-sse-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return new DelegatingSecurityContextExecutor(delegate);
    }
}