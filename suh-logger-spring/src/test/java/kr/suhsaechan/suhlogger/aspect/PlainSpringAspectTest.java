package kr.suhsaechan.suhlogger.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.RequestSnapshot;
import kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration;
import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Boot 없이 순수 Spring(JavaConfig)에서 import 한 줄로 동작하는지 — XML 사용자와 같은 경로 */
class PlainSpringAspectTest {

    public static class Greeter {

        @LogMonitor
        public String greet(String name, String password) {
            return "hi " + name;
        }

        @LogMonitor
        public Object returnsMap() {
            return Map.of("ok", 1);
        }
    }

    @Configuration
    @Import(SuhLoggerConfiguration.class)
    static class AppConfig {

        @Bean
        Greeter greeter() {
            return new Greeter();
        }

        @Bean
        SuhLoggerProperties suhLoggerProperties() {
            SuhLoggerProperties p = new SuhLoggerProperties();
            p.getMasking().setEnabled(true);
            p.getMasking().setMaskFields(List.of("password"));
            p.getHeader().setEnabled(true);
            p.getHeader().setIncludeAll(true);
            return p;
        }

        @Bean
        RequestContextAccessor accessor() {
            return () -> new RequestSnapshot("GET", "/hello", Map.of("X-Trace", "t1"), null);
        }
    }

    @Test
    void annotatedMethodIsLoggedAndMasked() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
             LogCapture capture = LogCapture.start()) {
            assertEquals("hi suh", ctx.getBean(Greeter.class).greet("suh", "p@ss"));
            String text = capture.text();
            assertTrue(text.contains("[Greeter.greet] CALL"), text);
            assertTrue(text.contains("suh"), text);
            assertFalse(text.contains("p@ss"), "password must be masked\n" + text);
            assertTrue(text.contains("/hello"), "request info from accessor\n" + text);
            assertTrue(text.contains("[TIME]: Greeter.greet"), text);
        }
    }

    @Test
    void resultLoggingWorksWithoutResponseEntityCheck() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
             LogCapture capture = LogCapture.start()) {
            ctx.getBean(Greeter.class).returnsMap();
            assertTrue(capture.text().contains("\"ok\": 1"), capture.text());
        }
    }

    @Test
    void worksWithoutPropertiesOrAccessorBeans() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(SuhLoggerConfiguration.class);
            ctx.registerBean(Greeter.class);
            ctx.refresh();
            try (LogCapture capture = LogCapture.start()) {
                ctx.getBean(Greeter.class).greet("a", "b");
                assertTrue(capture.text().contains("[Greeter.greet] CALL"), capture.text());
            }
        }
    }
}
