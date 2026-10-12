package kr.suhsaechan.suhlogger.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import kr.suhsaechan.suhlogger.testkit.LogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 2.x부터 사용자가 직접 부르는 공개 정적 API가 3.0에서도 같은 동작을 하는지 고정한다 */
class SuhLoggerPublicApiTest {

    @AfterEach
    void reset() {
        SuhLogger.setProperties(null);
    }

    private static Level levelOf(LogCapture c, String contains) {
        return c.events().stream().filter(e -> e.getFormattedMessage().contains(contains))
                .map(ILoggingEvent::getLevel).findFirst().orElse(null);
    }

    @Test
    void levelMethodsEmitAtTheirLevel() {
        try (LogCapture c = LogCapture.start()) {
            SuhLogger.debug("d-msg");
            SuhLogger.info("i-msg");
            SuhLogger.warn("w-msg");
            SuhLogger.error("e-msg");
            SuhLogger.error("e-ex", new IllegalStateException("boom"));
            assertEquals(Level.INFO, levelOf(c, "i-msg"));
            assertEquals(Level.WARN, levelOf(c, "w-msg"));
            assertEquals(Level.ERROR, levelOf(c, "e-msg"));
            assertEquals(Level.ERROR, levelOf(c, "e-ex"));
            assertTrue(c.events().stream().anyMatch(e -> e.getThrowableProxy() != null
                    && e.getThrowableProxy().getMessage().equals("boom")), "예외가 함께 기록돼야 한다");
        }
    }

    @Test
    void superLogVariantsKeepTheirLevelAndClassName() {
        try (LogCapture c = LogCapture.start()) {
            SuhLogger.superLogWarn(Map.of("k", "warn-value"));
            SuhLogger.superLogError(Map.of("k", "error-value"), true);
            SuhLogger.superLogDebug(Map.of("k", "debug-value"));
            assertEquals(Level.WARN, levelOf(c, "warn-value"));
            assertEquals(Level.ERROR, levelOf(c, "error-value"));
            assertTrue(c.text().contains("Map") || c.text().contains("ImmutableCollections"), "showClassName=true는 클래스명을 표시");
        }
    }

    @Test
    void superLogHandlesNullWithoutThrowing() {
        try (LogCapture c = LogCapture.start()) {
            assertDoesNotThrow(() -> SuhLogger.superLog(null));
            assertTrue(c.text().contains("NULL OBJECT"), c.text());
        }
    }

    @Test
    void infoJsonPrintsMessageAndBody() {
        try (LogCapture c = LogCapture.start()) {
            SuhLogger.infoJson("payload", Map.of("a", 1));
            assertTrue(c.text().contains("payload"), c.text());
            assertTrue(c.text().contains("\"a\": 1"), c.text());
        }
    }

    @Test
    void dividersAndHeadersAreWrittenAtInfo() {
        try (LogCapture c = LogCapture.start()) {
            SuhLogger.divider();
            SuhLogger.topDivider("TOP");
            SuhLogger.bottomDivider("BOTTOM");
            SuhLogger.logHeader("HEADER");
            SuhLogger.lineLogWarn("W-LINE");
            SuhLogger.lineLogError("E-LINE");
            SuhLogger.lineLogDebug("D-LINE");
            String text = c.text();
            assertTrue(text.contains("HEADER"), text);
            assertEquals(Level.WARN, levelOf(c, "W-LINE"));
            assertEquals(Level.ERROR, levelOf(c, "E-LINE"));
        }
    }

    @Test
    void lineLogCentersTitleAndSurvivesLongTitles() {
        try (LogCapture c = LogCapture.start()) {
            SuhLogger.lineLog("T");
            SuhLogger.lineLog("x".repeat(200));
            List<String> m = c.messages();
            assertTrue(m.get(0).contains(" T "), m.get(0));
            assertEquals("x".repeat(200), m.get(1), "제목이 구분선보다 길면 제목만 그대로 출력");
        }
    }

    @Test
    void logStreamPrintsContentAndReportsFailures() {
        try (LogCapture c = LogCapture.start()) {
            SuhLogger.logStream(new ByteArrayInputStream("stream-body".getBytes(StandardCharsets.UTF_8)));
            InputStream broken = new InputStream() {
                @Override
                public int read() throws java.io.IOException {
                    throw new java.io.IOException("broken pipe");
                }
            };
            SuhLogger.logStream(broken);
            assertTrue(c.text().contains("stream-body"), c.text());
            assertEquals(Level.ERROR, levelOf(c, "스트림 로깅 실패"));
        }
    }

    @Test
    void timeLogReportsFailureAndStillTimes() {
        try (LogCapture c = LogCapture.start()) {
            SuhLogger.timeLog(() -> {
                throw new IllegalArgumentException("task-failed");
            });
            assertTrue(c.text().contains("task-failed"), c.text());
            assertTrue(c.text().contains("실행 시간"), "실패해도 시간은 기록돼야 한다\n" + c.text());
        }
    }

    @Test
    void serverInitDurationIsReadable() {
        try (LogCapture c = LogCapture.start()) {
            SuhLogger.logServerInitDuration(LocalDateTime.now().minusSeconds(75));
            assertTrue(c.text().contains("총 소요 시간"), c.text());
            assertTrue(c.text().contains("1 분"), c.text());
        }
    }

    @Test
    void deprecatedLevelAndFileApisAreSafeNoOps() {
        try (LogCapture c = LogCapture.start()) {
            assertDoesNotThrow(() -> SuhLogger.setLogLevel(SuhLogger.LogLevel.DEBUG));
            assertDoesNotThrow(() -> SuhLogger.setLogLevel(java.util.logging.Level.FINE));
            assertDoesNotThrow(() -> SuhLogger.addFileLogger("/tmp/ignored.log"));
            assertFalse(c.messages().isEmpty(), "호출하면 안내 경고를 남긴다");
        }
    }

    @Test
    void instanceLoggerLevelsAndGuards() {
        SuhLogger log = SuhLogger.getLogger("custom-name");
        assertEquals(SuhLogger.getLogger("custom-name"), log, "같은 이름은 캐시된 같은 인스턴스");
        try (LogCapture c = LogCapture.start()) {
            log.infoMsg("hello {}", "x");
            log.warnMsg("warn {}", 1);
            log.errorMsg("err", new RuntimeException("inst-ex"));
            log.debugMsg("dbg");
            assertTrue(c.text().contains("hello x"), c.text());
            assertEquals(Level.WARN, levelOf(c, "warn 1"));
            assertTrue(log.isInfoEnabled());
            assertTrue(log.isErrorEnabled());
            assertTrue(log.isWarnEnabled());
        }
    }
}
