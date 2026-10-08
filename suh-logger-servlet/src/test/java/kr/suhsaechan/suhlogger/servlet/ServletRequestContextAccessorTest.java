package kr.suhsaechan.suhlogger.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import kr.suhsaechan.suhlogger.spi.RequestSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ServletRequestContextAccessorTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsNullOutsideRequest() {
        assertNull(new ServletRequestContextAccessor().current());
    }

    @Test
    void readsCurrentRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/login");
        req.addHeader("X-Trace", "t1");
        req.setAttribute("RequestID", "r-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        RequestSnapshot snap = new ServletRequestContextAccessor().current();
        assertEquals("POST", snap.getMethod());
        assertEquals("/login", snap.getUri());
        assertEquals("t1", snap.getHeaders().get("X-Trace"));
        assertEquals("r-1", snap.getRequestId());
    }
}
