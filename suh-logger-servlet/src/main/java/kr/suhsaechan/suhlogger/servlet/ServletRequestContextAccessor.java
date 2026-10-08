package kr.suhsaechan.suhlogger.servlet;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.RequestSnapshot;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Servlet 환경의 현재 요청을 RequestSnapshot으로 변환한다 (Spring MVC가 바인딩한 RequestContextHolder 사용) */
public class ServletRequestContextAccessor implements RequestContextAccessor {

    @Override
    public RequestSnapshot current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        Object requestId = request.getAttribute("RequestID");
        return new RequestSnapshot(request.getMethod(), request.getRequestURI(), headers,
                requestId != null ? requestId.toString() : null);
    }
}
