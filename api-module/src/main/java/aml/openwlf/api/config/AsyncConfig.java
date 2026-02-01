package aml.openwlf.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 *
 * 감사 로깅 등 비동기 작업을 위한 ThreadPool 설정을 담당합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 감사 로깅 전용 Executor
     *
     * 메인 요청 처리에 영향을 주지 않도록 별도의 스레드 풀에서 감사 로그를 비동기로 저장합니다.
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
