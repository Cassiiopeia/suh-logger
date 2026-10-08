package kr.suhsaechan.suhlogger.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import kr.suhsaechan.suhlogger.aspect.SuhMethodInvocationLoggingAspect;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import java.util.UUID;
import kr.suhsaechan.suhlogger.filter.SuhLoggingFilter;
import kr.suhsaechan.suhlogger.internal.serialize.TypeHandlers;
import kr.suhsaechan.suhlogger.json.jackson2.Jackson2JsonCodec;
import kr.suhsaechan.suhlogger.json.jackson3.Jackson3JsonCodec;
import kr.suhsaechan.suhlogger.spi.JsonCodec;
import kr.suhsaechan.suhlogger.spi.TypeHandler;
import kr.suhsaechan.suhlogger.util.CommonUtil;
import kr.suhsaechan.suhlogger.servlet.ServletRequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class SuhLoggerAutoConfigurationTest {

    private final AutoConfigurations configs = AutoConfigurations.of(SuhLoggerJsonAutoConfiguration.class,
            SuhLoggerAutoConfiguration.class, SuhLoggerServletAutoConfiguration.class,
            SuhLoggerReactiveAutoConfiguration.class);

    @Test
    void servletWebContextRegistersFilterAndAccessor() {
        new WebApplicationContextRunner().withConfiguration(configs).run(ctx -> {
            assertThat(ctx).hasSingleBean(SuhLoggerProperties.class);
            assertThat(ctx).hasSingleBean(SuhMethodInvocationLoggingAspect.class);
            assertThat(ctx).hasSingleBean(SuhLoggingFilter.class);
            assertThat(ctx).hasBean("suhLoggingFilterRegistration");
            assertThat(ctx.getBean(RequestContextAccessor.class)).isInstanceOf(ServletRequestContextAccessor.class);
        });
    }

    @Test
    void nonWebContextHasAspectsButNoFilter() {
        new ApplicationContextRunner().withConfiguration(configs).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(SuhMethodInvocationLoggingAspect.class);
            assertThat(ctx).doesNotHaveBean(SuhLoggingFilter.class);
            assertThat(ctx).doesNotHaveBean(FilterRegistrationBean.class);
        });
    }

    @Test
    void contextStartsWhenServletApiIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("jakarta.servlet", "kr.suhsaechan.suhlogger.filter",
                        "kr.suhsaechan.suhlogger.servlet"))
                .withConfiguration(configs)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(SuhMethodInvocationLoggingAspect.class);
                });
    }

    @Test
    void propertiesAreBound() {
        new ApplicationContextRunner().withConfiguration(configs)
                .withPropertyValues("suh-logger.masking.enabled=true", "suh-logger.masking.mask-fields=password,token",
                        "suh-logger.exclude-patterns=/health")
                .run(ctx -> {
                    SuhLoggerProperties p = ctx.getBean(SuhLoggerProperties.class);
                    assertThat(p.getMasking().isEnabled()).isTrue();
                    assertThat(p.getMasking().getMaskFields()).containsExactly("password", "token");
                    assertThat(p.getExcludePatterns()).containsExactly("/health");
                });
    }

    @Test
    void userDefinedPropertiesBeanWins() {
        new ApplicationContextRunner().withConfiguration(configs)
                .withBean("myProps", SuhLoggerProperties.class, () -> {
                    SuhLoggerProperties p = new SuhLoggerProperties();
                    p.setMaxResponseBodySize(7);
                    return p;
                })
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(SuhLoggerProperties.class);
                    assertThat(ctx.getBean(SuhLoggerProperties.class).getMaxResponseBodySize()).isEqualTo(7);
                });
    }

    @Test
    void disabledFlagStillStartsContext() {
        new WebApplicationContextRunner().withConfiguration(configs)
                .withPropertyValues("suh-logger.enabled=false")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void appObjectMapperBecomesJsonCodec() {
        new ApplicationContextRunner().withConfiguration(configs)
                .withBean(com.fasterxml.jackson.databind.ObjectMapper.class, com.fasterxml.jackson.databind.ObjectMapper::new)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(JsonCodec.class);
                    assertThat(ctx.getBean(JsonCodec.class)).isInstanceOf(Jackson2JsonCodec.class);
                });
    }

    @Test
    void appJsonMapperBecomesJackson3Codec() {
        new ApplicationContextRunner().withConfiguration(configs)
                .withBean(tools.jackson.databind.json.JsonMapper.class,
                        () -> tools.jackson.databind.json.JsonMapper.builder().build())
                .run(ctx -> assertThat(ctx.getBean(JsonCodec.class)).isInstanceOf(Jackson3JsonCodec.class));
    }

    @Test
    void typeHandlerBeanIsRegistered() {
        new ApplicationContextRunner().withConfiguration(configs)
                .withBean(TypeHandler.class, () -> new TypeHandler() {
                    public boolean supports(Object value) { return value instanceof UUID; }
                    public Object toSafe(Object value) { return "UUID-HIDDEN"; }
                })
                .run(ctx -> {
                    try {
                        assertThat(CommonUtil.makeSafeForSerialization(UUID.randomUUID())).isEqualTo("UUID-HIDDEN");
                    } finally {
                        TypeHandlers.reset();
                    }
                });
    }

    @Test
    void reactiveContextRegistersWebFilterOnly() {
        new org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner().withConfiguration(configs)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(kr.suhsaechan.suhlogger.webflux.SuhReactiveLoggingWebFilter.class);
                    assertThat(ctx).doesNotHaveBean(SuhLoggingFilter.class);
                    assertThat(ctx).doesNotHaveBean(RequestContextAccessor.class);
                });
    }

    @Test
    void servletContextDoesNotRegisterWebFilter() {
        new WebApplicationContextRunner().withConfiguration(configs)
                .run(ctx -> assertThat(ctx).doesNotHaveBean(kr.suhsaechan.suhlogger.webflux.SuhReactiveLoggingWebFilter.class));
    }
}
