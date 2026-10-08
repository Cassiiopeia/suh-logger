package kr.suhsaechan.suhlogger.internal.http;

import java.nio.charset.StandardCharsets;
import java.util.List;
import kr.suhsaechan.suhlogger.config.ResponseBodyMode;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import kr.suhsaechan.suhlogger.internal.mask.Masker;
import kr.suhsaechan.suhlogger.util.SuhLogger;

/**
 * HTTP 응답 로그 규칙 한 곳. Servlet 필터·WebFlux WebFilter가 같은 출력과 같은 제외 규칙을 쓰게 하려고 모았다.
 * 3.0부터 4xx·5xx 응답도 기록한다 (2.x는 2xx만). 본문 범위는 response-body 설정으로 고른다.
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
            SuhLogger.lineLog("RESPONSE LOGGING");
            SuhLogger.info("URI: " + uri);
            SuhLogger.info("Method: " + method);
            SuhLogger.info("Status: " + status);
            if (shouldLogBody(status) && body != null && totalBytes > 0) {
                SuhLogger.info("Response Body: " + bodyText(body, totalBytes));
            }
            SuhLogger.lineLog(null);
        } catch (RuntimeException e) {
            // 로깅 실패가 응답을 깨면 안 된다
            SuhLogger.error("Response 로깅 중 에러 발생", e);
        }
    }

    /** response-body 설정: none은 본문 없음, error-only는 4xx·5xx만 */
    boolean shouldLogBody(int status) {
        ResponseBodyMode mode = properties.getResponseBody() != null ? properties.getResponseBody() : ResponseBodyMode.ALL;
        switch (mode) {
            case NONE:
                return false;
            case ERROR_ONLY:
                return status >= 400;
            default:
                return true;
        }
    }

    private String bodyText(byte[] body, long totalBytes) {
        int maxSize = properties.getMaxResponseBodySize();
        if (totalBytes > body.length) {
            return "[Too large to log - " + totalBytes + " bytes, max: " + maxSize + "]";
        }
        String formatted = format(new String(body, StandardCharsets.UTF_8));
        if (formatted.length() > maxSize) {
            return "[Too large to log - " + formatted.length() + " bytes, max: " + maxSize + "]";
        }
        return formatted;
    }

    /** 마스킹을 pretty print보다 먼저 적용한다 — 토큰이 들어간 본문이 어떤 경로로도 원문 그대로 나가지 않게 */
    private String format(String body) {
        SuhLoggerProperties.MaskingConfig masking = properties.getMasking();
        if (masking != null && masking.isEnabled()) {
            return new Masker(masking.effectiveMaskFields(), masking.getMaskValue())
                    .maskJson(body, properties.isPrettyPrintJson());
        }
        return properties.isPrettyPrintJson() ? JsonCodecs.prettyOrRaw(body) : body;
    }
}
