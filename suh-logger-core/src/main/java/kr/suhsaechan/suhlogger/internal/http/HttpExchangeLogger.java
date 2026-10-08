package kr.suhsaechan.suhlogger.internal.http;

import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import kr.suhsaechan.suhlogger.util.SuhLogger;

/**
 * HTTP 응답 로그 규칙 한 곳. Servlet 필터·WebFlux WebFilter가 같은 출력과 같은 제외 규칙을 쓰게 하려고 모았다.
 * 출력 형식은 2.x 필터와 동일하다 (형식 변경은 #56에서 Formatter로 분리).
 */
public final class HttpExchangeLogger {

    private final SuhLoggerProperties properties;

    public HttpExchangeLogger(SuhLoggerProperties properties) {
        this.properties = properties != null ? properties : new SuhLoggerProperties();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public int maxBodySize() {
        return properties.getMaxResponseBodySize();
    }

    /** 2.x 동작 유지: 패턴 문자열이 URI에 포함되면 제외 */
    public boolean isExcluded(String uri) {
        if (uri == null) {
            return false;
        }
        List<String> patterns = properties.getExcludePatterns();
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().filter(p -> p != null).anyMatch(uri::contains);
    }

    /**
     * @param body       로그용으로 확보한 본문 (전체이거나 앞부분)
     * @param totalBytes 실제 응답 본문 전체 크기 — 일부만 확보한 경우 크기 초과 판단에 쓴다
     */
    public void logResponse(String method, String uri, int status, byte[] body, long totalBytes) {
        try {
            if (!properties.isEnabled()) {
                return;
            }
            // 성공 응답(2xx)만 로깅 — 에러 응답 경로에서 추가 문제를 만들지 않으려는 2.x 정책
            if (status < 200 || status >= 300 || body == null || totalBytes <= 0) {
                return;
            }
            int maxSize = properties.getMaxResponseBodySize();
            SuhLogger.lineLog("RESPONSE LOGGING");
            SuhLogger.info("URI: " + uri);
            SuhLogger.info("Method: " + method);
            SuhLogger.info("Status: " + status);
            if (totalBytes > body.length) {
                SuhLogger.info("Response Body: [Too large to log - " + totalBytes + " bytes, max: " + maxSize + "]");
            } else {
                String formatted = format(new String(body, StandardCharsets.UTF_8));
                if (formatted.length() <= maxSize) {
                    SuhLogger.info("Response Body: " + formatted);
                } else {
                    SuhLogger.info("Response Body: [Too large to log - " + formatted.length() + " bytes, max: " + maxSize + "]");
                }
            }
            SuhLogger.lineLog(null);
        } catch (RuntimeException e) {
            // 로깅 실패가 응답을 깨면 안 된다
            SuhLogger.error("Response 로깅 중 에러 발생", e);
        }
    }

    private String format(String body) {
        return properties.isPrettyPrintJson() ? JsonCodecs.prettyOrRaw(body) : body;
    }
}
