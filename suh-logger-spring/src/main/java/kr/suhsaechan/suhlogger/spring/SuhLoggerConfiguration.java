package kr.suhsaechan.suhlogger.spring;

import kr.suhsaechan.suhlogger.aspect.SuhExecutionTimeLoggingAspect;
import kr.suhsaechan.suhlogger.aspect.SuhMethodInvocationLoggingAspect;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import java.util.List;
import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import kr.suhsaechan.suhlogger.internal.serialize.TypeHandlers;
import kr.suhsaechan.suhlogger.spi.JsonCodec;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.TypeHandler;
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
    public SuhLoggerInitializer suhLoggerInitializer(ObjectProvider<SuhLoggerProperties> properties,
                                                     ObjectProvider<JsonCodec> jsonCodec,
                                                     ObjectProvider<TypeHandler> typeHandlers) {
        return new SuhLoggerInitializer(properties.getIfAvailable(SuhLoggerProperties::new),
                jsonCodec.getIfAvailable(), typeHandlers.orderedStream().toList());
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
            if (jsonCodec != null) {
                JsonCodecs.set(jsonCodec);
            }
            typeHandlers.forEach(TypeHandlers::register);
        }
    }
}
