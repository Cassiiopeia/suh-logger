package me.suhsaechan.suhlogger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.boot.logging.LoggingSystem;
import jakarta.annotation.PostConstruct;

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
     * Spring Boot 로깅 시스템이 Java Util Logging을 간섭하지 않도록 설정
     * SLF4J가 JUL 핸들러를 가로채는 것 방지
     */
    @PostConstruct
    public void init() {
        // Spring Boot의 JUL-to-SLF4J 브릿지 비활성화
        System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
        
        // JUL 로깅 설정 초기화 
        System.setProperty("java.util.logging.config.file", "no-such-file");
    }
    
    /**
     * SuhLogger 초기화 클래스
     * 로깅 시스템을 초기화하고 기본 설정을 적용
     */
    public static class SuhLoggerInitializer {
        public SuhLoggerInitializer() {
            // JUL 설정 초기화 : 상위 프로젝트와 분리
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            if (rootLogger != null) {
                // 루트 로거의 영향을 받지 않도록 설정
                Logger logger = SuhLoggerConfig.getLogger();
                logger.setUseParentHandlers(false);
                
                // 추가: SLF4J 로깅과의 연결 해제
                System.setProperty("org.slf4j.simpleLogger.log.me.suhsaechan.suhlogger", "off");
            }
        }
    }
}