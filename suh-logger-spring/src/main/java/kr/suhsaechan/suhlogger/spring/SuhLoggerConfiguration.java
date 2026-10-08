package kr.suhsaechan.suhlogger.spring;

import kr.suhsaechan.suhlogger.aspect.SuhExecutionTimeLoggingAspect;
import kr.suhsaechan.suhlogger.aspect.SuhMethodInvocationLoggingAspect;
import kr.suhsaechan.suhlogger.config.ResponseBodyMode;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import java.util.List;
import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import kr.suhsaechan.suhlogger.internal.serialize.TypeHandlers;
import kr.suhsaechan.suhlogger.internal.http.HttpLogFormatters;
import kr.suhsaechan.suhlogger.spi.HttpLogFormatter;
import kr.suhsaechan.suhlogger.spi.JsonCodec;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.TypeHandler;
import kr.suhsaechan.suhlogger.util.SuhLogger;
import org.slf4j.LoggerFactory;
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

    /** static: BeanFactoryPostProcessor는 일반 빈보다 먼저 생성돼야 하므로 설정 클래스 인스턴스에 의존하지 않게 한다 */
    @Bean
    public static ProxyEligibilityChecker suhLoggerProxyEligibilityChecker() {
        return new ProxyEligibilityChecker();
    }

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
    public SuhLoggerInitializer suhLoggerInitializer(ObjectProvider<SuhLoggerProperties> properties,
                                                     ObjectProvider<JsonCodec> jsonCodec,
                                                     ObjectProvider<TypeHandler> typeHandlers,
                                                     ObjectProvider<HttpLogFormatter> httpLogFormatter) {
        SuhLoggerInitializer initializer = new SuhLoggerInitializer(properties.getIfAvailable(SuhLoggerProperties::new),
                jsonCodec.getIfAvailable(), typeHandlers.orderedStream().toList());
        HttpLogFormatters.set(httpLogFormatter.getIfAvailable());
        return initializer;
    }

    /**
     * 정적 SuhLogger API·필터가 컨텍스트 설정을 쓰도록 주입한다.
     * 정적 저장소에 넣는 이유: SuhLogger 정적 메서드는 빈이 아니라서 컨텍스트를 직접 볼 수 없다.
     */
    public static class SuhLoggerInitializer {

        public SuhLoggerInitializer(SuhLoggerProperties properties) {
            this(properties, null, List.of());
        }

        public SuhLoggerInitializer(SuhLoggerProperties properties, JsonCodec jsonCodec, List<TypeHandler> typeHandlers) {
            SuhLogger.setProperties(properties);
            warnIfUnmasked(properties);
            if (jsonCodec != null) {
                JsonCodecs.set(jsonCodec);
            }
            typeHandlers.forEach(TypeHandlers::register);
        }

        /** 마스킹을 끈 채 본문을 남기면 토큰·개인정보가 평문으로 남는다 (#55) — 명시적으로 끈 경우에만 생기므로 한 번 알린다 */
        private static void warnIfUnmasked(SuhLoggerProperties properties) {
            boolean maskingOff = properties.getMasking() == null || !properties.getMasking().isEnabled();
            boolean bodyLogged = properties.isEnabled() && properties.getResponseBody() != ResponseBodyMode.NONE;
            if (maskingOff && bodyLogged) {
                LoggerFactory.getLogger(SuhLoggerConfiguration.class).warn(
                        "[suh-logger] masking is disabled while response bodies are logged; tokens and personal data "
                                + "may appear in plain text (set suh-logger.masking.enabled=true or suh-logger.response-body=none)");
            }
        }
    }
}
