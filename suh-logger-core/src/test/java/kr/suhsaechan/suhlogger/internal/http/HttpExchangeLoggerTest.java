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
    void errorResponseIsLoggedSince3() {
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(new SuhLoggerProperties()).logResponse("GET", "/a", 500, b("{\"error\":\"x\"}"), 13);
            assertTrue(capture.text().contains("Status: 500"), capture.text());
            assertTrue(capture.text().contains("\"error\""), capture.text());
        }
    }

    @Test
    void emptyBodyLogsStatusWithoutBodyLine() {
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(new SuhLoggerProperties()).logResponse("GET", "/a", 204, new byte[0], 0);
            assertTrue(capture.text().contains("Status: 204"), capture.text());
            assertFalse(capture.text().contains("Response Body"), capture.text());
        }
    }

    @Test
    void tokensInBodyAreMaskedByDefault() {
        String body = "{\"user\":{\"name\":\"suh\",\"accessToken\":\"AT-1\"},\"refreshToken\":\"RT-2\",\"csrf\":\"C-3\"}";
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(new SuhLoggerProperties()).logResponse("POST", "/auth/login", 200, b(body), body.length());
            String text = capture.text();
            assertTrue(text.contains("suh"), text);
            assertFalse(text.contains("AT-1") || text.contains("RT-2") || text.contains("C-3"), text);
            assertTrue(text.contains("****"), text);
        }
    }

    @Test
    void disabledMaskingKeepsBody() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.getMasking().setEnabled(false);
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(p).logResponse("POST", "/x", 200, b("{\"token\":\"T\"}"), 11);
            assertTrue(capture.text().contains("\"token\":\"T\""), capture.text());
        }
    }

    @Test
    void errorOnlyModeSkipsSuccessBody() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setResponseBody(kr.suhsaechan.suhlogger.config.ResponseBodyMode.ERROR_ONLY);
        try (LogCapture capture = LogCapture.start()) {
            HttpExchangeLogger logger = new HttpExchangeLogger(p);
            logger.logResponse("GET", "/ok", 200, b("{\"ok\":1}"), 8);
            logger.logResponse("GET", "/bad", 400, b("{\"why\":\"bad\"}"), 13);
            String text = capture.text();
            assertFalse(text.contains("\"ok\""), text);
            assertTrue(text.contains("\"why\""), text);
        }
    }

    @Test
    void noneModeNeverLogsBody() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setResponseBody(kr.suhsaechan.suhlogger.config.ResponseBodyMode.NONE);
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(p).logResponse("GET", "/bad", 500, b("{\"x\":1}"), 7);
            assertTrue(capture.text().contains("Status: 500"), capture.text());
            assertFalse(capture.text().contains("Response Body"), capture.text());
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
