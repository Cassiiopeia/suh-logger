package me.suhsaechan.suhlogger.config;

import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemFactory;
import org.springframework.core.Ordered;

/**
 * SuhLogger에서 Spring Boot의 로깅 시스템 간섭을 방지하기 위한 팩토리 클래스
 * 이 클래스는 Spring Boot의 로깅 시스템을 비활성화하여 SuhLogger가 독립적으로 작동하도록 함
 */
public class NoOpLoggingSystemFactory implements LoggingSystemFactory, Ordered {

    /**
     * 로깅 시스템 생성을 요청받으면 null을 반환하여 Spring Boot의 로깅 시스템 초기화를 방지
     */
    @Override
    public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
        // Spring Boot의 로깅 시스템 초기화를 방지하기 위해 null 반환
        return null;
    }
    
    /**
     * 우선순위를 최상위로 설정하여 다른 로깅 시스템보다 먼저 적용되도록 함
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
} 