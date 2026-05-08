package com.andrew.smartielts.writing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class WritingAsyncConfig {

    @Bean(name = "writingScoringExecutor")
    public Executor writingScoringExecutor() {
        ThreadPoolExecutor delegate = new ThreadPoolExecutor(
                2,
                8,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("writing-scoring-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return new DelegatingSecurityContextExecutor(delegate);
    }
}
