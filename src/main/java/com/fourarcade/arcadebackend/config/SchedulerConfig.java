package com.fourarcade.arcadebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig {

    @Bean
    @Primary
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // 동시에 여러 방에서 타이머가 돌아갈 수 있으므로 스레드 풀 사이즈 넉넉히 설정
        scheduler.setPoolSize(10);

        // 로그 찍힐 때 보기 편하게 스레드 이름에 접두사 붙임
        scheduler.setThreadNamePrefix("game-timer-");
        scheduler.initialize();
        return scheduler;
    }
}
