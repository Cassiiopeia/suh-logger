package kr.suhsaechan.suhlogger.boot.autoconfigure;

import kr.suhsaechan.suhlogger.json.jackson2.Jackson2JsonCodec;
import kr.suhsaechan.suhlogger.json.jackson3.Jackson3JsonCodec;
import kr.suhsaechan.suhlogger.spi.JsonCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 앱이 이미 구성한 Jackson 빈으로 JsonCodec을 만든다 — 날짜 형식·Kotlin 모듈 같은 앱 설정이 로그에도 그대로 반영되게.
 * Boot 4(Jackson 3)를 먼저 본다. 빈이 없으면 ServiceLoader 기본 codec으로 넘어간다.
 */
@AutoConfiguration(
        afterName = {
                "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration", // Boot 3.x
                "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration"  // Boot 4.x
        },
        beforeName = "kr.suhsaechan.suhlogger.boot.autoconfigure.SuhLoggerAutoConfiguration")
public class SuhLoggerJsonAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {"tools.jackson.databind.json.JsonMapper",
            "kr.suhsaechan.suhlogger.json.jackson3.Jackson3JsonCodec"})
    @ConditionalOnBean(type = "tools.jackson.databind.json.JsonMapper")
    static class Jackson3Config {

        @Bean
        @ConditionalOnMissingBean(JsonCodec.class)
        JsonCodec suhLoggerJsonCodec(tools.jackson.databind.json.JsonMapper mapper) {
            return new Jackson3JsonCodec(mapper);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {"com.fasterxml.jackson.databind.ObjectMapper",
            "kr.suhsaechan.suhlogger.json.jackson2.Jackson2JsonCodec"})
    @ConditionalOnBean(type = "com.fasterxml.jackson.databind.ObjectMapper")
    static class Jackson2Config {

        @Bean
        @ConditionalOnMissingBean(JsonCodec.class)
        JsonCodec suhLoggerJsonCodec(com.fasterxml.jackson.databind.ObjectMapper mapper) {
            return new Jackson2JsonCodec(mapper);
        }
    }
}
