package kr.suhsaechan.suhlogger.aspect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration;
import kr.suhsaechan.suhlogger.testkit.LogCapture;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** #55: 설정 없이도 DTO·record 안의 토큰·비밀번호가 가려져야 한다 */
class DefaultMaskingTest {

    public record LoginRequest(String username, String password) { }

    public record LoginResponse(String username, String accessToken, String refreshToken) { }

    public static class SignupService {

        @kr.suhsaechan.suhlogger.annotation.LogCall(mask = kr.suhsaechan.suhlogger.annotation.TriState.ON,
                header = kr.suhsaechan.suhlogger.annotation.TriState.ON)
        public void signup(String name) {
            throw new IllegalArgumentException("rejected value [p@ss-in-msg] for field password");
        }
    }

    public static class AuthService {

        @LogMonitor
        public LoginResponse login(LoginRequest request) {
            return new LoginResponse(request.username(), "AT-xyz", "RT-xyz");
        }
    }

    @Test
    void recordParamsAndResultAreMaskedByDefault() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(SuhLoggerConfiguration.class);
            ctx.registerBean(AuthService.class);
            ctx.refresh();
            try (LogCapture capture = LogCapture.start()) {
                ctx.getBean(AuthService.class).login(new LoginRequest("suh", "p@ss"));
                String text = capture.text();
                assertTrue(text.contains("\"username\": \"suh\""), text);
                assertFalse(text.contains("p@ss"), text);
                assertFalse(text.contains("AT-xyz") || text.contains("RT-xyz"), text);
            }
        }
    }

    @Test
    void warnsWhenMaskingIsDisabledButBodiesAreLogged() {
        SuhLoggerProperties props = new SuhLoggerProperties();
        props.getMasking().setEnabled(false);
        try (LogCapture capture = LogCapture.start()) {
            new SuhLoggerConfiguration.SuhLoggerInitializer(props);
            assertTrue(capture.text().contains("masking is disabled while response bodies are logged"), capture.text());
        }
        kr.suhsaechan.suhlogger.util.SuhLogger.setProperties(null);
    }

    @Test
    void noWarnByDefault() {
        try (LogCapture capture = LogCapture.start()) {
            new SuhLoggerConfiguration.SuhLoggerInitializer(new SuhLoggerProperties());
            assertFalse(capture.text().contains("masking is disabled"), capture.text());
        }
        kr.suhsaechan.suhlogger.util.SuhLogger.setProperties(null);
    }

    @Test
    void annotationMaskOnHidesHeadersAndSensitiveExceptionMessagesEvenWhenGlobalOff() {
        SuhLoggerProperties props = new SuhLoggerProperties();
        props.getMasking().setEnabled(false);
        props.getHeader().setEnabled(true);
        props.getHeader().setIncludeAll(true);
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(SuhLoggerConfiguration.class);
            ctx.registerBean(SuhLoggerProperties.class, () -> props);
            ctx.registerBean(SignupService.class);
            ctx.registerBean(kr.suhsaechan.suhlogger.spi.RequestContextAccessor.class,
                    () -> () -> new kr.suhsaechan.suhlogger.spi.RequestSnapshot("POST", "/signup",
                            java.util.Map.of("Authorization", "Bearer JWT-SECRET"), null));
            ctx.refresh();
            try (LogCapture capture = LogCapture.start()) {
                try {
                    ctx.getBean(SignupService.class).signup("suh");
                } catch (IllegalArgumentException expected) {
                    // 예외는 그대로 전파된다
                }
                String text = capture.text();
                assertFalse(text.contains("JWT-SECRET"), text);
                assertFalse(text.contains("p@ss-in-msg"), text);
                assertTrue(text.contains("IllegalArgumentException"), text);
            }
        }
        kr.suhsaechan.suhlogger.util.SuhLogger.setProperties(null);
    }
}
