package kr.suhsaechan.suhlogger.internal.http;

import java.nio.charset.StandardCharsets;
import kr.suhsaechan.suhlogger.config.LogFormat;
import kr.suhsaechan.suhlogger.config.ResponseBodyMode;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import kr.suhsaechan.suhlogger.internal.mask.Masker;
import kr.suhsaechan.suhlogger.spi.HttpExchangeRecord;
import kr.suhsaechan.suhlogger.spi.HttpLogFormatter;
import kr.suhsaechan.suhlogger.util.SuhLogger;

/**
 * HTTP 응답 로그 규칙 한 곳. Servlet 필터·WebFlux WebFilter가 같은 출력과 같은 제외 규칙을 쓰게 하려고 모았다.
 * 3.0부터 4xx·5xx 응답도 기록하고, 5xx·느린 요청은 WARN으로 올린다.
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

    public SuhLoggerProperties.RequestIdConfig requestIdConfig() {
        SuhLoggerProperties.RequestIdConfig c = properties.getRequestId();
        return c != null ? c : new SuhLoggerProperties.RequestIdConfig();
    }

    public int filterOrder() {
        return properties.getFilterOrder();
    }

    /** Ant 패턴(/actuator/**) 우선, 패턴 문자가 없는 2.x 값은 contains */
    public boolean isExcluded(String uri) {
        return PathPatterns.anyMatch(properties.getExcludePatterns(), uri);
    }

    /** 처리 시간·요청 ID 없이 호출하던 2.x 경로용 */
    public void logResponse(String method, String uri, int status, byte[] body, long totalBytes) {
        logResponse(method, uri, status, body, totalBytes, -1, null);
    }

    /**
     * @param body       로그용으로 확보한 본문 (전체이거나 앞부분)
     * @param totalBytes 실제 응답 본문 전체 크기 — 일부만 확보한 경우 크기 초과 판단에 쓴다
     * @param durationMs 처리 시간, 모르면 -1
     */
    public void logResponse(String method, String uri, int status, byte[] body, long totalBytes, long durationMs,
                            String requestId) {
        try {
            if (!properties.isEnabled()) {
                return;
            }
            String bodyText = shouldLogBody(status) && body != null && totalBytes > 0 ? bodyText(body, totalBytes) : null;
            boolean slow = properties.getSlowThresholdMs() > 0 && durationMs > properties.getSlowThresholdMs();
            HttpExchangeRecord record = new HttpExchangeRecord(method, uri, status, durationMs, requestId, bodyText, slow);
            boolean warn = slow || status >= 500;

            HttpLogFormatter custom = HttpLogFormatters.get();
            if (custom != null) {
                emit(warn, custom.format(record));
            } else if (properties.getFormat() == LogFormat.LINE) {
                emit(warn, line(record));
            } else {
                block(record, warn);
            }
        } catch (RuntimeException e) {
            // 로깅 실패가 응답을 깨면 안 된다
            SuhLogger.error("Response 로깅 중 에러 발생", e);
        }
    }

    /** METHOD URI -> STATUS (Nms) [SLOW] rid=... body=... — 요청당 한 줄이라 동시 요청이 섞여도 짝이 맞는다 */
    static String line(HttpExchangeRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getMethod()).append(' ').append(r.getUri()).append(" -> ").append(r.getStatus());
        if (r.getDurationMs() >= 0) {
            sb.append(" (").append(r.getDurationMs()).append("ms)");
        }
        if (r.isSlow()) {
            sb.append(" [SLOW]");
        }
        if (r.getRequestId() != null) {
            sb.append(" rid=").append(r.getRequestId());
        }
        if (r.getBody() != null) {
            // 한 줄 유지를 위해 줄바꿈을 접는다 (pretty print와 함께 써도 한 줄)
            sb.append(" body=").append(r.getBody().replaceAll("\\s*\\R\\s*", " "));
        }
        return sb.toString();
    }

    private void block(HttpExchangeRecord r, boolean warn) {
        lineAt(warn, "RESPONSE LOGGING");
        emit(warn, "URI: " + r.getUri());
        emit(warn, "Method: " + r.getMethod());
        emit(warn, "Status: " + r.getStatus());
        if (r.getDurationMs() >= 0) {
            emit(warn, "Duration: " + r.getDurationMs() + " ms" + (r.isSlow() ? " [SLOW]" : ""));
        }
        if (r.getRequestId() != null) {
            emit(warn, "Request-Id: " + r.getRequestId());
        }
        if (r.getBody() != null) {
            emit(warn, "Response Body: " + r.getBody());
        }
        lineAt(warn, null);
    }

    private static void emit(boolean warn, String message) {
        if (warn) {
            SuhLogger.warn(message);
        } else {
            SuhLogger.info(message);
        }
    }

    private static void lineAt(boolean warn, String title) {
        if (warn) {
            SuhLogger.lineLogWarn(title);
        } else {
            SuhLogger.lineLog(title);
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
