package kr.suhsaechan.suhlogger.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 어댑터(Servlet·WebFlux)가 넘겨주는 현재 요청 정보. core가 프레임워크 타입을 모르게 하려는 값 객체 */
public final class RequestSnapshot {

    private final String method;
    private final String uri;
    private final Map<String, String> headers;
    private final String requestId;

    public RequestSnapshot(String method, String uri, Map<String, String> headers, String requestId) {
        this.method = method;
        this.uri = uri;
        this.headers = headers == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.requestId = requestId;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestId() {
        return requestId;
    }
}
