package me.suhsaechan.suhlogger.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.suhsaechan.suhlogger.config.SuhLoggerProperties;
import me.suhsaechan.suhlogger.util.SuhLogger;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;

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
    private final ObjectMapper objectMapper;

    public SuhLoggingFilter(SuhLoggerProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
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

    /**
     * 로깅에서 제외할 URI인지 확인
     */
    private boolean shouldExcludeFromLogging(String uri) {
        if (uri == null || properties == null) {
            return false;
        }
        
        List<String> excludePatterns = properties.getExcludePatterns();
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return false;
        }
        
        return excludePatterns.stream()
            .filter(pattern -> pattern != null)
            .anyMatch(uri::contains);
    }

    /**
     * Response를 안전하게 로깅
     */
    private void logResponseSafely(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper) {
        try {
            // 로깅이 비활성화된 경우 스킵 (이미 위에서 체크했지만 안전을 위해)
            if (properties == null || !properties.isEnabled()) {
                return;
            }
            
            int status = responseWrapper.getStatus();
            
            // 성공 응답(2xx)만 로깅하여 에러 상황에서의 추가 문제 방지
            if (status >= 200 && status < 300) {
                byte[] content = responseWrapper.getContentAsByteArray();
                
                if (content.length > 0) {
                    String responseBody = new String(content);
                    
                    // Response 로깅 (구분선과 함께)
                    SuhLogger.lineLog("RESPONSE LOGGING");
                    SuhLogger.info("URI: " + request.getRequestURI());
                    SuhLogger.info("Method: " + request.getMethod());
                    SuhLogger.info("Status: " + status);
                    
                    // Response Body 크기 제한 확인 (포맷팅 후 크기 고려)
                    int maxSize = properties.getMaxResponseBodySize();
                    String formattedBody = formatResponseBody(responseBody);
                    
                    if (formattedBody.length() <= maxSize) {
                        SuhLogger.info("Response Body: " + formattedBody);
                    } else {
                        SuhLogger.info("Response Body: [Too large to log - " + formattedBody.length() + " bytes, max: " + maxSize + "]");
                    }
                    
                    SuhLogger.lineLog(null);
                }
            }
        } catch (Exception e) {
            // 로깅 중 에러가 발생해도 원본 응답에는 영향을 주지 않음
            SuhLogger.error("Response 로깅 중 에러 발생", e);
        }
    }

    /**
     * Response Body를 설정에 따라 포맷팅
     */
    private String formatResponseBody(String responseBody) {
        if (properties == null || !properties.isPrettyPrintJson()) {
            return responseBody;
        }

        try {
            // JSON인지 확인하고 pretty print 적용
            Object jsonObject = objectMapper.readValue(responseBody, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (Exception e) {
            // JSON이 아니거나 파싱 실패시 원본 반환
            return responseBody;
        }
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
