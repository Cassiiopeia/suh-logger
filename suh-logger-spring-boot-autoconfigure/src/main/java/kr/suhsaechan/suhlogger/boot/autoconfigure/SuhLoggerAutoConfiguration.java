package kr.suhsaechan.suhlogger.boot.autoconfigure;

import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * suh-logger Boot 자동설정 (Boot 3.x·4.x).
 * 자동설정 순서는 클래스 직접 참조 대신 이름으로 지정한다 — Boot 4에서 패키지가 바뀌어도 로딩이 깨지지 않게.
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",   // Boot 3.x
        "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration"           // Boot 4.x
})
@AutoConfigureBefore(name = {
        "org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration", // Boot 3.x
        "org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration"       // Boot 4.x
})
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
// 인자 없이 켜서 아래 @Bean 메서드의 @ConfigurationProperties 바인딩 후처리기만 등록한다
@EnableConfigurationProperties
@Import(SuhLoggerConfiguration.class)
public class SuhLoggerAutoConfiguration {

    /** core의 POJO를 그대로 바인딩 — core가 Boot 어노테이션에 의존하지 않게 하려고 메서드에 붙인다 */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "suh-logger")
    public SuhLoggerProperties suhLoggerProperties() {
        return new SuhLoggerProperties();
    }
}
