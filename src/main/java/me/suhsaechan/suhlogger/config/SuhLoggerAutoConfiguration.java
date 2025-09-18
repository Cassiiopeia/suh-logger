package me.suhsaechan.suhlogger.config;

import me.suhsaechan.suhlogger.aspect.SuhExecutionTimeLoggingAspect;
import me.suhsaechan.suhlogger.aspect.SuhMethodInvocationLoggingAspect;
import me.suhsaechan.suhlogger.filter.SuhLoggingFilter;
import me.suhsaechan.suhlogger.util.SuhLogger;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * SuhLogger 자동 설정 클래스
 * Spring Boot 환경에서 AOP 및 로깅 설정을 자동화
 * 
 * 우선순위 설정:
 * 1. Security Filters (인증/인가) - 최우선
 * 2. Business Logic (컨트롤러, 서비스) - 핵심
 * 3. Logging/Monitoring - 마지막 (낮은 우선순위)
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan("me.suhsaechan.suhlogger")
@EnableConfigurationProperties(SuhLoggerProperties.class)
@AutoConfigureAfter({
    WebMvcAutoConfiguration.class,
    SecurityAutoConfiguration.class  // Security 이후 실행
})
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
      // JUL 설정 초기화 : 상위 프로젝트와 분리
      Logger rootLogger = LogManager.getLogManager().getLogger("");
      if (rootLogger != null) {
        // 루트 로거의 영향을 받지 않도록 설정
        Logger logger = SuhLoggerConfig.getLogger();
        logger.setUseParentHandlers(false);

        // 추가: SLF4J 로깅과의 연결 해제
        System.setProperty("org.slf4j.simpleLogger.log.me.suhsaechan.suhlogger", "off");
      }
      
      // SuhLogger에 properties 주입
      SuhLogger.setProperties(properties);
    }
  }
}