package kr.suhsaechan.suhlogger.webflux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.testkit.LogCapture;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class SuhReactiveLoggingWebFilterTest {

    private MockServerWebExchange run(SuhLoggerProperties props, String path, String body) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        WebFilterChain chain = ex -> {
            ex.getResponse().setStatusCode(HttpStatus.OK);
            DataBuffer buf = DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(StandardCharsets.UTF_8));
            return ex.getResponse().writeWith(Mono.just(buf));
        };
        new SuhReactiveLoggingWebFilter(props).filter(exchange, chain).block();
        return exchange;
    }

    @Test
    void logsResponseBody() {
        try (LogCapture capture = LogCapture.start()) {
            MockServerWebExchange ex = run(new SuhLoggerProperties(), "/api/r", "{\"a\":1}");
            assertEquals("{\"a\":1}", ex.getResponse().getBodyAsString().block());
            assertTrue(capture.text().contains("URI: /api/r"), capture.text());
            assertTrue(capture.text().contains("Response Body: {\"a\":1}"), capture.text());
        }
    }

    @Test
    void excludedPathSkipsLogging() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setExcludePatterns(List.of("/health"));
        try (LogCapture capture = LogCapture.start()) {
            run(p, "/health", "{}");
            assertFalse(capture.text().contains("RESPONSE LOGGING"), capture.text());
        }
    }

    @Test
    void largeBodyIsTruncatedInLogOnly() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setMaxResponseBodySize(5);
        String big = "x".repeat(100);
        try (LogCapture capture = LogCapture.start()) {
            MockServerWebExchange ex = run(p, "/big", big);
            assertEquals(big, ex.getResponse().getBodyAsString().block(), "client gets the full body");
            assertTrue(capture.text().contains("[Too large to log - 100 bytes, max: 5]"), capture.text());
        }
    }
}
