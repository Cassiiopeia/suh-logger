package kr.suhsaechan.suhlogger.boot.autoconfigure;

import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.webflux.SuhReactiveLoggingWebFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * WebFlux 앱에서만 응답 로깅 WebFilter를 등록한다.
 * aspect의 요청 헤더 로깅은 Reactor Context가 필요해 WebFlux에서는 지원하지 않는다 (RequestContextAccessor 미등록).
 */
@AutoConfiguration(after = SuhLoggerAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = {"org.springframework.web.server.WebFilter",
        "kr.suhsaechan.suhlogger.webflux.SuhReactiveLoggingWebFilter"})
@ConditionalOnBean(SuhLoggerProperties.class)
public class SuhLoggerReactiveAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SuhReactiveLoggingWebFilter suhReactiveLoggingWebFilter(SuhLoggerProperties properties) {
        return new SuhReactiveLoggingWebFilter(properties);
    }
}
