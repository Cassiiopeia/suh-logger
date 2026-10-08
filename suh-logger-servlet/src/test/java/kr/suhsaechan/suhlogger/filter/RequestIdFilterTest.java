package kr.suhsaechan.suhlogger.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.testkit.LogCapture;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** #56: 요청 ID MDC·응답 헤더, 필터 순서 설정 */
class RequestIdFilterTest {

    private SuhLoggerProperties withRequestId() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.getRequestId().setEnabled(true);
        return p;
    }

    private MockHttpServletResponse run(SuhLoggerProperties p, MockHttpServletRequest req, AtomicReference<String> seenMdc)
            throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        new SuhLoggingFilter(p).doFilter(req, res, new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest rq, HttpServletResponse rs) throws IOException {
                seenMdc.set(MDC.get("requestId"));
                rs.getWriter().write("{}");
            }
        }));
        return res;
    }

    @Test
    void generatesIdIntoMdcAndHeaderThenClears() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        try (LogCapture capture = LogCapture.start()) {
            MockHttpServletResponse res = run(withRequestId(), new MockHttpServletRequest("GET", "/api/x"), seen);
            String header = res.getHeader("X-Request-Id");
            assertNotNull(header);
            assertEquals(header, seen.get(), "app code sees the same id through MDC");
            assertTrue(capture.text().contains("Request-Id: " + header), capture.text());
        }
        assertNull(MDC.get("requestId"), "MDC must not leak to the next request on this thread");
    }

    @Test
    void reusesIncomingHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        req.addHeader("X-Request-Id", "from-gateway");
        AtomicReference<String> seen = new AtomicReference<>();
        MockHttpServletResponse res = run(withRequestId(), req, seen);
        assertEquals("from-gateway", res.getHeader("X-Request-Id"));
        assertEquals("from-gateway", seen.get());
    }

    @Test
    void disabledByDefault() throws Exception {
        MockHttpServletResponse res = run(new SuhLoggerProperties(), new MockHttpServletRequest("GET", "/api/x"),
                new AtomicReference<>());
        assertNull(res.getHeader("X-Request-Id"));
    }

    @Test
    void filterOrderComesFromProperties() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setFilterOrder(-100);
        assertEquals(-100, new SuhLoggingFilter(p).getOrder());
    }
}
