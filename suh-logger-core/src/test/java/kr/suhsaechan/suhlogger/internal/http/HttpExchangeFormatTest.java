package kr.suhsaechan.suhlogger.internal.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.suhsaechan.suhlogger.config.LogFormat;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.testkit.LogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** #56: 한 줄 형식, 처리 시간, 5xx·느린 요청 WARN, 사용자 포매터 */
class HttpExchangeFormatTest {

    @AfterEach
    void reset() {
        HttpLogFormatters.set(null);
    }

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static SuhLoggerProperties line() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setFormat(LogFormat.LINE);
        return p;
    }

    @Test
    void lineFormatIsOneEventPerRequest() {
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(line()).logResponse("GET", "/api/x", 200, b("{\"a\":1}"), 7, 12, "rid-1");
            List<String> messages = capture.messages();
            assertEquals(1, messages.size(), messages.toString());
            assertEquals("GET /api/x -> 200 (12ms) rid=rid-1 body={\"a\":1}", messages.get(0));
        }
    }

    @Test
    void lineFormatFoldsPrettyBodyIntoOneLine() {
        SuhLoggerProperties p = line();
        HttpLogFormatters.set(null);
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(p).logResponse("GET", "/x", 200, b("{\n  \"a\" : 1\n}"), 13, 1, null);
            assertFalse(capture.messages().get(0).contains("\n"), capture.text());
        }
    }

    @Test
    void blockFormatShowsDuration() {
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(new SuhLoggerProperties()).logResponse("GET", "/x", 200, b("{}"), 2, 33, null);
            assertTrue(capture.text().contains("Duration: 33 ms"), capture.text());
        }
    }

    @Test
    void serverErrorIsWarn() {
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(line()).logResponse("GET", "/x", 503, b("{}"), 2, 5, null);
            ILoggingEvent event = capture.events().get(0);
            assertEquals(Level.WARN, event.getLevel());
        }
    }

    @Test
    void slowRequestIsWarnAndMarked() {
        SuhLoggerProperties p = line();
        p.setSlowThresholdMs(100);
        try (LogCapture capture = LogCapture.start()) {
            HttpExchangeLogger logger = new HttpExchangeLogger(p);
            logger.logResponse("GET", "/fast", 200, b("{}"), 2, 50, null);
            logger.logResponse("GET", "/slow", 200, b("{}"), 2, 250, null);
            List<ILoggingEvent> events = capture.events();
            assertEquals(Level.INFO, events.get(0).getLevel());
            assertEquals(Level.WARN, events.get(1).getLevel());
            assertTrue(events.get(1).getFormattedMessage().contains("(250ms) [SLOW]"), capture.text());
        }
    }

    @Test
    void customFormatterWinsAndStillMasks() {
        HttpLogFormatters.set(r -> "{\"uri\":\"" + r.getUri() + "\",\"body\":" + r.getBody() + "}");
        try (LogCapture capture = LogCapture.start()) {
            new HttpExchangeLogger(new SuhLoggerProperties())
                    .logResponse("POST", "/login", 200, b("{\"token\":\"T-1\"}"), 13, 3, null);
            String out = capture.messages().get(0);
            assertTrue(out.startsWith("{\"uri\":\"/login\""), out);
            assertFalse(out.contains("T-1"), "masking happens before the formatter\n" + out);
        }
    }
}
