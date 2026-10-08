package kr.suhsaechan.suhlogger.boot.autoconfigure;

import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.filter.SuhLoggingFilter;
import kr.suhsaechan.suhlogger.servlet.ServletRequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/** Servlet 웹 앱에서만 필터와 요청 정보 어댑터를 등록한다 (non-web·WebFlux에서는 건너뜀) */
@AutoConfiguration(after = SuhLoggerAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {"jakarta.servlet.Filter", "kr.suhsaechan.suhlogger.filter.SuhLoggingFilter"})
@ConditionalOnBean(SuhLoggerProperties.class)
public class SuhLoggerServletAutoConfiguration {

    // aspect는 ObjectProvider로 조회하므로 이 빈이 aspect보다 늦게 정의돼도 런타임에 찾는다
    @Bean
    @ConditionalOnMissingBean(RequestContextAccessor.class)
    public RequestContextAccessor servletRequestContextAccessor() {
        return new ServletRequestContextAccessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public SuhLoggingFilter suhLoggingFilter(SuhLoggerProperties properties) {
        return new SuhLoggingFilter(properties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "suhLoggingFilterRegistration")
    public FilterRegistrationBean<SuhLoggingFilter> suhLoggingFilterRegistration(SuhLoggingFilter filter,
                                                                                 SuhLoggerProperties properties) {
        FilterRegistrationBean<SuhLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setName("suhLoggingFilter");
        // suh-logger.filter-order (기본 가장 마지막) — Security 401/403 요청을 기록할지 사용자가 정할 수 있게
        registration.setOrder(properties.getFilterOrder());
        return registration;
    }
}
