package kr.suhsaechan.suhlogger.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.internal.http.HttpExchangeLogger;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/**
 * SuhLogger 안전한 Response 처리 필터
 * 
 * 이 필터는 다음과 같은 문제를 해결합니다:
 * 1. Response 객체 중복 사용으로 인한 "getWriter() has already been called" 에러
 * 2. Spring Security와의 충돌 방지
 * 3. 안전한 Response Body 로깅
 * 
 * 실행 순서: Spring Security → Business Logic → SuhLoggingFilter (최하위 우선순위)
 */
public class SuhLoggingFilter extends OncePerRequestFilter implements Ordered {

    /** 요청 속성 키 — 비동기 디스패치 사이에 시작 시각을 넘긴다 */
    private static final String START_ATTRIBUTE = SuhLoggingFilter.class.getName() + ".start";
    /** aspect(ServletRequestContextAccessor)가 읽는 요청 ID 속성 — 2.x부터 쓰던 이름 */
    public static final String REQUEST_ID_ATTRIBUTE = "RequestID";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:\\-]{1,128}");

    private final SuhLoggerProperties properties;
    private final HttpExchangeLogger exchangeLogger;

    public SuhLoggingFilter(SuhLoggerProperties properties) {
        this.properties = properties;
        this.exchangeLogger = new HttpExchangeLogger(properties);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        // 로깅이 비활성화됐거나 제외 경로면 손대지 않고 통과
        if (properties == null || !properties.isEnabled() || exchangeLogger.isExcluded(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 비동기(DeferredResult·Callable) 요청은 첫 디스패치에서 시작 시각·요청 ID를 정하고, 마지막 디스패치에서 기록한다
        Object startAttr = request.getAttribute(START_ATTRIBUTE);
        long start = startAttr instanceof Long ? (Long) startAttr : System.nanoTime();
        request.setAttribute(START_ATTRIBUTE, start);
        SuhLoggerProperties.RequestIdConfig rid = exchangeLogger.requestIdConfig();
        String requestId = rid.isEnabled() ? resolveRequestId(request, response, rid) : null;

        // SSE는 본문을 모아 두면 이벤트가 끝날 때까지 클라이언트로 안 나간다 — 감싸지 않고 상태만 기록
        boolean streaming = isEventStream(request);
        ContentCachingResponseWrapper wrapper = streaming ? null
                : WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (!streaming && wrapper == null) {
            wrapper = new ContentCachingResponseWrapper(response);
        }

        Throwable failure = null;
        try {
            filterChain.doFilter(request, wrapper != null ? wrapper : response);
        } catch (IOException | ServletException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            try {
                if (!isAsyncStarted(request)) {
                    long durationMs = (System.nanoTime() - start) / 1_000_000;
                    int status = wrapper != null ? wrapper.getStatus() : response.getStatus();
                    // 체인 밖으로 나간 예외는 이 시점에 상태가 아직 200이다 — 컨테이너가 500으로 바꾸기 전이므로 직접 보정
                    if (failure != null && status < 400) {
                        status = 500;
                    }
                    byte[] content = wrapper != null ? wrapper.getContentAsByteArray() : new byte[0];
                    exchangeLogger.logResponse(request.getMethod(), request.getRequestURI(), status,
                            content, content.length, durationMs, requestId);
                    if (wrapper != null) {
                        // 중요! 실제 response로 내용 복사 (마지막 디스패치에서 한 번만)
                        wrapper.copyBodyToResponse();
                    }
                }
            } finally {
                if (requestId != null) {
                    // 복사 중 클라이언트가 끊겨도(IOException) 다음 요청에 섞이지 않게 반드시 제거
                    MDC.remove(rid.getMdcKey());
                }
            }
        }
    }

    /** 요청 ID: 들어온 헤더가 형식에 맞으면 쓰고, 아니면 새로 만든다 (로그 위조·헤더 오염 방지) */
    private static String resolveRequestId(HttpServletRequest request, HttpServletResponse response,
                                           SuhLoggerProperties.RequestIdConfig rid) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        String requestId = existing instanceof String ? (String) existing : null;
        if (requestId == null) {
            String incoming = request.getHeader(rid.getHeader());
            requestId = incoming != null && SAFE_REQUEST_ID.matcher(incoming).matches()
                    ? incoming : UUID.randomUUID().toString();
            // aspect(@LogMonitor) 로그의 requestId와 같은 값을 쓰도록 요청 속성에도 둔다
            request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
            // 응답이 커밋되기 전에 넣어야 하므로 체인 실행 전에 설정한다
            response.setHeader(rid.getHeader(), requestId);
        }
        // MDC는 스레드 로컬이라 디스패치마다 다시 넣는다
        MDC.put(rid.getMdcKey(), requestId);
        return requestId;
    }

    private static boolean isEventStream(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/event-stream");
    }

    /** 비동기 요청의 마지막 디스패치에서도 로깅·본문 복사를 해야 한다 (기본값 true면 본문이 래퍼에 남는다) */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    public int getOrder() {
        // 기본은 가장 마지막(보안 필터 이후). suh-logger.filter-order로 바꿀 수 있다
        return exchangeLogger.filterOrder();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 정적 리소스는 필터링하지 않음
        String uri = request.getRequestURI();
        return uri.startsWith("/static/") || 
               uri.startsWith("/css/") || 
               uri.startsWith("/js/") || 
               uri.startsWith("/images/") ||
               uri.endsWith(".ico") ||
               uri.endsWith(".png") ||
               uri.endsWith(".jpg") ||
               uri.endsWith(".css") ||
               uri.endsWith(".js");
    }
}
