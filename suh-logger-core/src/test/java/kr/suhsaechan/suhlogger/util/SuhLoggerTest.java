package kr.suhsaechan.suhlogger.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.testkit.LogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SuhLoggerTest {

    @AfterEach
    void resetProperties() {
        SuhLogger.setProperties(null);
    }

    @Test
    void staticInfoDelegatesToSlf4j() {
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.info("hello");
            assertTrue(capture.messages().contains("hello"), capture.text());
        }
    }

    @Test
    void instanceLoggerUsesSlf4jPlaceholders() {
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.getLogger(SuhLoggerTest.class).infoMsg("{}-{}", "a", "b");
            assertTrue(capture.text().contains("a-b"), capture.text());
        }
    }

    @Test
    void superLogPrintsMapAsJson() {
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.superLog(Map.of("k", "v"));
            assertTrue(capture.text().contains("\"k\": \"v\""), capture.text());
        }
    }

    @Test
    void excludedClassesFromPropertiesAreMarked() {
        SuhLoggerProperties props = new SuhLoggerProperties();
        props.setExcludedClasses(List.of(StringBuilder.class.getName()));
        SuhLogger.setProperties(props);
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.superLog(new StringBuilder("secret-content"));
            assertTrue(capture.text().contains("EXCLUDED_CLASS"), capture.text());
            // #39: 제외한 클래스의 내용이 마커를 통해 새어 나오면 안 된다
            assertFalse(capture.text().contains("secret-content"), capture.text());
        }
    }

    @Test
    void timeLogReportsDuration() {
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.timeLog(() -> { });
            assertTrue(capture.text().contains("실행 시간"), capture.text());
        }
    }
}
