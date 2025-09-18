package me.suhsaechan.suhlogger.config;

import me.suhsaechan.suhlogger.filter.SuhLoggingFilter;
import me.suhsaechan.suhlogger.util.SuhLogger;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;

import java.util.logging.Logger;

/**
 * SuhLogger 자동 설정 클래스
 * Spring Boot 환경에서 AOP 및 로깅 설정을 자동화
 * 
 * 우선순위 설정:
 * 1. Security Filters (인증/인가) - 최우선  
 * 2. Logging/Monitoring - Security 이후 실행
 * 3. Error Handling - 마지막
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan("me.suhsaechan.suhlogger")
@EnableConfigurationProperties(SuhLoggerProperties.class)
@AutoConfigureAfter(SecurityAutoConfiguration.class)
@AutoConfigureBefore(ErrorMvcAutoConfiguration.class)
public class SuhLoggerAutoConfiguration {

  /**
   * SuhLogger 설정 초기화
   * Spring Boot 시작 시 로깅 설정을 초기화
   */
  @Bean
  public SuhLoggerInitializer suhLoggerInitializer(SuhLoggerProperties properties) {
    return new SuhLoggerInitializer(properties);
  }


  /**
   * SuhLoggingFilter 빈 등록
   * 생성자 주입을 통해 SuhLoggerProperties를 주입받음
   */
  @Bean
  public SuhLoggingFilter suhLoggingFilter(SuhLoggerProperties properties) {
    return new SuhLoggingFilter(properties);
  }

  /**
   * 안전한 Response 처리를 위한 로깅 필터 등록
   * Spring Security 이후 실행되도록 설정
   */
  @Bean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  public FilterRegistrationBean<SuhLoggingFilter> suhLoggingFilterRegistration(SuhLoggingFilter filter) {
    FilterRegistrationBean<SuhLoggingFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.addUrlPatterns("/*");
    registration.setName("suhLoggingFilter");
    registration.setOrder(Ordered.LOWEST_PRECEDENCE); // 가장 낮은 우선순위
    return registration;
  }

  /**
   * SuhLogger 초기화 클래스
   * 로깅 시스템을 초기화하고 기본 설정을 적용
   */
  public static class SuhLoggerInitializer {

    public SuhLoggerInitializer(SuhLoggerProperties properties) {
      // SuhLogger 전용 설정 초기화 : 전역 로거 설정 변경하지 않음
      // me.suhsaechan.suhlogger 네임스페이스의 독립 로거만 설정
      Logger suhLogger = SuhLoggerConfig.getLogger();
      
      // setUseParentHandlers(false)
      if (!suhLogger.getName().equals("me.suhsaechan.suhlogger")) {
        throw new IllegalStateException("SuhLogger must use 'me.suhsaechan.suhlogger' namespace");
      }
      
      // SLF4J 연결 해제
      System.setProperty("org.slf4j.simpleLogger.log.me.suhsaechan.suhlogger", "off");
      
      // SuhLogger에 properties 주입
      SuhLogger.setProperties(properties);
    }
  }
}