package kr.suhsaechan.suhlogger.spi;

import kr.suhsaechan.suhlogger.annotation.Incubating;

/** 한 번의 HTTP 요청·응답 요약. 본문은 이미 마스킹·크기 제한이 적용된 값이다 (없으면 null) */
@Incubating(since = "3.0.0")
public final class HttpExchangeRecord {

    private final String method;
    private final String uri;
    private final int status;
    private final long durationMs;
    private final String requestId;
    private final String body;
    private final boolean slow;

    public HttpExchangeRecord(String method, String uri, int status, long durationMs, String requestId, String body,
                              boolean slow) {
        this.method = method;
        this.uri = uri;
        this.status = status;
        this.durationMs = durationMs;
        this.requestId = requestId;
        this.body = body;
        this.slow = slow;
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public int getStatus() {
        return status;
    }

    /** 측정하지 못했으면 -1 */
    public long getDurationMs() {
        return durationMs;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getBody() {
        return body;
    }

    /** slow-threshold-ms를 넘었는지 */
    public boolean isSlow() {
        return slow;
    }
}
