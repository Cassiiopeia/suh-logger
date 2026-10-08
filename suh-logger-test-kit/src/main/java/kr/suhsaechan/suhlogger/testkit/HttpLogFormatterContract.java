package kr.suhsaechan.suhlogger.testkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.suhsaechan.suhlogger.spi.HttpExchangeRecord;
import kr.suhsaechan.suhlogger.spi.HttpLogFormatter;
import org.junit.jupiter.api.Test;

/** HttpLogFormatter 구현이 지켜야 할 계약 — 상속해서 formatter()만 채운다 */
public abstract class HttpLogFormatterContract {

    protected abstract HttpLogFormatter formatter();

    private static HttpExchangeRecord sample(String body, long durationMs, String requestId) {
        return new HttpExchangeRecord("POST", "/api/orders", 201, durationMs, requestId, body, false);
    }

    @Test
    void includesMethodUriAndStatus() {
        String out = formatter().format(sample("{\"a\":1}", 12, "rid-1"));
        assertNotNull(out);
        assertTrue(out.contains("/api/orders"), out);
        assertTrue(out.contains("201"), out);
    }

    @Test
    void handlesMissingOptionalFields() {
        assertDoesNotThrow(() -> formatter().format(sample(null, -1, null)));
    }

    @Test
    void staysOnOneLine() {
        String out = formatter().format(sample("{\n  \"a\" : 1\n}", 5, null));
        assertFalse(out.contains("\n"), "한 줄이어야 로그 수집기가 한 이벤트로 받는다\n" + out);
    }
}
