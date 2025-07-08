package me.suhsaechan.suhlogger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * SuhLogger 자동 설정 클래스
 * Spring Boot 환경에서 AOP 및 로깅 설정을 자동화
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan("me.suhsaechan.suhlogger")
public class SuhLoggerAutoConfiguration {

    /**
     * SuhLogger 설정 초기화
     * Spring Boot 시작 시 로깅 설정을 초기화
     */
    @Bean
    public SuhLoggerInitializer suhLoggerInitializer() {
        return new SuhLoggerInitializer();
    }
    
    /**
     * SuhLogger 초기화 클래스
     * 로깅 시스템을 초기화하고 기본 설정을 적용
     */
    public static class SuhLoggerInitializer {
        public SuhLoggerInitializer() {
            // JUL 설정 초기화 (상위 프로젝트와 분리)
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            if (rootLogger != null) {
                // 루트 로거의 영향을 받지 않도록 설정
                Logger logger = SuhLoggerConfig.getLogger();
                logger.setUseParentHandlers(false);
            }
        }
    }
}