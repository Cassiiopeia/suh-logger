package kr.suhsaechan.suhlogger.aspect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kotlin.coroutines.Continuation;
import kr.suhsaechan.suhlogger.annotation.LogCall;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration;
import kr.suhsaechan.suhlogger.testkit.LogCapture;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class ClassLevelAnnotationTest {

    @LogMonitor
    public static class MonitoredService {

        public String first(String a) {
            return "1" + a;
        }

        public String second() {
            return "2";
        }

        @LogCall(result = false)
        public String quiet(String q) {
            return "secret-result";
        }

        /** suspend fun find(id: Long): String 이 컴파일된 모양 */
        public Object suspendLike(long id, Continuation<? super String> continuation) {
            return "found";
        }
    }

    private AnnotationConfigApplicationContext context() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SuhLoggerConfiguration.class);
        ctx.registerBean(MonitoredService.class);
        ctx.refresh();
        return ctx;
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    @Test
    void classLevelMonitorLogsEveryPublicMethod() {
        try (AnnotationConfigApplicationContext ctx = context(); LogCapture capture = LogCapture.start()) {
            MonitoredService service = ctx.getBean(MonitoredService.class);
            service.first("x");
            service.second();
            String text = capture.text();
            assertTrue(text.contains("[MonitoredService.first] CALL"), text);
            assertTrue(text.contains("[MonitoredService.second] CALL"), text);
            assertTrue(text.contains("[TIME]: MonitoredService.second"), text);
        }
    }

    @Test
    void methodAnnotationOverridesClassAnnotation() {
        try (AnnotationConfigApplicationContext ctx = context(); LogCapture capture = LogCapture.start()) {
            ctx.getBean(MonitoredService.class).quiet("q");
            String text = capture.text();
            assertEquals(1, count(text, "[MonitoredService.quiet] CALL"), text);
            assertFalse(text.contains("secret-result"), "@LogCall(result=false) must win\n" + text);
            assertFalse(text.contains("[TIME]: MonitoredService.quiet"), "@LogCall has no timing\n" + text);
        }
    }

    @Test
    void continuationParameterIsNotLogged() {
        try (AnnotationConfigApplicationContext ctx = context(); LogCapture capture = LogCapture.start()) {
            ctx.getBean(MonitoredService.class).suspendLike(7L, new Continuation<String>() { });
            String text = capture.text();
            assertTrue(text.contains("\"id\": 7"), text);
            assertFalse(text.contains("continuation"), text);
        }
    }
}
