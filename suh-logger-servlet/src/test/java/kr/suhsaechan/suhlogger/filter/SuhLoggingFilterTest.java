package kr.suhsaechan.suhlogger.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.testkit.LogCapture;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SuhLoggingFilterTest {

    private MockHttpServletResponse run(SuhLoggerProperties props, String uri, String body) throws Exception {
        SuhLoggingFilter filter = new SuhLoggingFilter(props);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest rq, HttpServletResponse rs) throws IOException {
                rs.setStatus(200);
                rs.setContentType("application/json");
                rs.getWriter().write(body);
            }
        }));
        return res;
    }

    @Test
    void logsResponseAndKeepsBodyForClient() throws Exception {
        try (LogCapture capture = LogCapture.start()) {
            MockHttpServletResponse res = run(new SuhLoggerProperties(), "/api/x", "{\"a\":1}");
            assertEquals("{\"a\":1}", res.getContentAsString(), "body must still reach the client");
            assertTrue(capture.text().contains("URI: /api/x"), capture.text());
            assertTrue(capture.text().contains("Response Body: {\"a\":1}"), capture.text());
        }
    }

    @Test
    void excludePatternSkipsLogging() throws Exception {
        SuhLoggerProperties props = new SuhLoggerProperties();
        props.setExcludePatterns(List.of("/health"));
        try (LogCapture capture = LogCapture.start()) {
            run(props, "/health", "{}");
            assertFalse(capture.text().contains("RESPONSE LOGGING"), capture.text());
        }
    }

    @Test
    void prettyPrintUsesJacksonWhenPresent() throws Exception {
        SuhLoggerProperties props = new SuhLoggerProperties();
        props.setPrettyPrintJson(true);
        try (LogCapture capture = LogCapture.start()) {
            run(props, "/api/x", "{\"a\":1}");
            assertTrue(capture.text().contains("\"a\" : 1"), capture.text());
        }
    }
}
