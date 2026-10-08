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

        // ContentCachingResponseWrapper로 안전하게 Response 캐싱
        ContentCachingResponseWrapper responseWrapper = 
            new ContentCachingResponseWrapper(response);

        try {
            // 다음 필터 체인 실행
            filterChain.doFilter(request, responseWrapper);
        } finally {
            // 응답 처리 후 안전하게 로깅
            logResponseSafely(request, responseWrapper);
            
            // 중요! 실제 response로 내용 복사
            responseWrapper.copyBodyToResponse();
        }
    }

    /** 제외 규칙은 WebFlux와 공유하는 HttpExchangeLogger에 위임 */
    private boolean shouldExcludeFromLogging(String uri) {
        return exchangeLogger.isExcluded(uri);
    }

    /** 출력 규칙은 WebFlux와 공유하는 HttpExchangeLogger에 위임 */
    private void logResponseSafely(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper) {
        byte[] content = responseWrapper.getContentAsByteArray();
        exchangeLogger.logResponse(request.getMethod(), request.getRequestURI(), responseWrapper.getStatus(),
                content, content.length);
    }

    @Override
    public int getOrder() {
        // 가장 낮은 우선순위로 설정하여 모든 다른 필터들이 실행된 후 로깅
        return Ordered.LOWEST_PRECEDENCE;
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
