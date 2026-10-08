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

import java.io.IOException;
import java.util.UUID;
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
        
        String uri = request.getRequestURI();
        
        // 로깅이 비활성화된 경우 통과
        if (properties == null || !properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 제외 패턴에 해당하는 경우 로깅 없이 통과
        if (shouldExcludeFromLogging(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 요청 ID: 들어온 헤더를 우선 쓰고 없으면 만든다. MDC에 넣으면 앱 로그 패턴의 %X{requestId}로 이어진다
        SuhLoggerProperties.RequestIdConfig rid = exchangeLogger.requestIdConfig();
        String requestId = null;
        if (rid.isEnabled()) {
            requestId = request.getHeader(rid.getHeader());
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            MDC.put(rid.getMdcKey(), requestId);
            // 응답이 커밋되기 전에 넣어야 하므로 체인 실행 전에 설정한다
            response.setHeader(rid.getHeader(), requestId);
        }

        // ContentCachingResponseWrapper로 안전하게 Response 캐싱
        ContentCachingResponseWrapper responseWrapper =
            new ContentCachingResponseWrapper(response);
        long start = System.nanoTime();

        try {
            // 다음 필터 체인 실행
            filterChain.doFilter(request, responseWrapper);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            // 응답 처리 후 안전하게 로깅
            logResponseSafely(request, responseWrapper, durationMs, requestId);
            // 중요! 실제 response로 내용 복사
            responseWrapper.copyBodyToResponse();
            if (requestId != null) {
                // 스레드 풀 재사용 시 다음 요청에 섞이지 않게 제거
                MDC.remove(rid.getMdcKey());
            }
        }
    }

    /** 제외 규칙은 WebFlux와 공유하는 HttpExchangeLogger에 위임 */
    private boolean shouldExcludeFromLogging(String uri) {
        return exchangeLogger.isExcluded(uri);
    }

    /** 출력 규칙은 WebFlux와 공유하는 HttpExchangeLogger에 위임 */
    private void logResponseSafely(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper,
                                   long durationMs, String requestId) {
        byte[] content = responseWrapper.getContentAsByteArray();
        exchangeLogger.logResponse(request.getMethod(), request.getRequestURI(), responseWrapper.getStatus(),
                content, content.length, durationMs, requestId);
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
