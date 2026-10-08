package kr.suhsaechan.suhlogger.spring;

import kr.suhsaechan.suhlogger.aspect.SuhExecutionTimeLoggingAspect;
import kr.suhsaechan.suhlogger.aspect.SuhMethodInvocationLoggingAspect;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.util.SuhLogger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Boot 없는 Spring(JavaConfig·XML)용 진입점. {@code @Import(SuhLoggerConfiguration.class)} 또는
 * {@code <bean class="kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration"/>} 한 줄로 켠다.
 * Boot 자동설정도 이 클래스를 재사용해 등록 규칙이 한 곳에만 있게 한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy
public class SuhLoggerConfiguration {

    @Bean
    public SuhExecutionTimeLoggingAspect suhExecutionTimeLoggingAspect(ObjectProvider<SuhLoggerProperties> properties) {
        return new SuhExecutionTimeLoggingAspect(properties.getIfAvailable(SuhLoggerProperties::new));
    }

    @Bean
    public SuhMethodInvocationLoggingAspect suhMethodInvocationLoggingAspect(
            ObjectProvider<SuhLoggerProperties> properties, ObjectProvider<RequestContextAccessor> accessor) {
        return new SuhMethodInvocationLoggingAspect(properties.getIfAvailable(SuhLoggerProperties::new),
                accessor.getIfAvailable(() -> RequestContextAccessor.NONE));
    }

    @Bean
    public SuhLoggerInitializer suhLoggerInitializer(ObjectProvider<SuhLoggerProperties> properties) {
        return new SuhLoggerInitializer(properties.getIfAvailable(SuhLoggerProperties::new));
    }

    /** 정적 SuhLogger API가 컨텍스트 설정(마스킹·제외 클래스)을 쓰도록 주입 */
    public static class SuhLoggerInitializer {

        public SuhLoggerInitializer(SuhLoggerProperties properties) {
            SuhLogger.setProperties(properties);
        }
    }
}
