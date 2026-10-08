package kr.suhsaechan.suhlogger.internal.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.Test;

class HttpExchangeLoggerTest {

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void logsSuccessfulResponse() {
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(new SuhLoggerProperties()).logResponse("GET", "/a", 200, b("{\"한\":1}"), 9);
            assertTrue(capture.text().contains("RESPONSE LOGGING"), capture.text());
            assertTrue(capture.text().contains("Response Body: {\"한\":1}"), "UTF-8로 복원\n" + capture.text());
        }
    }

    @Test
    void skipsErrorResponse() {
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(new SuhLoggerProperties()).logResponse("GET", "/a", 500, b("x"), 1);
            assertFalse(capture.text().contains("RESPONSE LOGGING"), capture.text());
        }
    }

    @Test
    void skipsEmptyBody() {
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(new SuhLoggerProperties()).logResponse("GET", "/a", 204, new byte[0], 0);
            assertFalse(capture.text().contains("RESPONSE LOGGING"), capture.text());
        }
    }

    @Test
    void partialCaptureIsReportedAsTooLarge() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setMaxResponseBodySize(3);
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(p).logResponse("GET", "/a", 200, b("abcd"), 10_000);
            assertTrue(capture.text().contains("[Too large to log - 10000 bytes, max: 3]"), capture.text());
            assertFalse(capture.text().contains("abcd"), capture.text());
        }
    }

    @Test
    void excludeUsesContains() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setExcludePatterns(List.of("/health"));
        HttpExchangeLogger logger = new HttpExchangeLogger(p);
        assertTrue(logger.isExcluded("/actuator/health"));
        assertFalse(logger.isExcluded("/api"));
    }
}
