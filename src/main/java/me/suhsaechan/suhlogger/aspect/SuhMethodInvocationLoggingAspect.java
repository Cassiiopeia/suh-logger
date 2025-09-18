package me.suhsaechan.suhlogger.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import me.suhsaechan.suhlogger.util.SuhLogger;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import me.suhsaechan.suhlogger.config.SuhLoggerProperties;
import me.suhsaechan.suhlogger.annotation.LogCall;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.suhlogger.annotation.HeaderLogOption;

@Aspect
@Component
public class SuhMethodInvocationLoggingAspect {

  @Autowired
  private SuhLoggerProperties properties;

  /**
   * LogMethodInvocation, LogMonitoringInvocation 어노테이션이 붙은 메서드 호출 정보 로깅
   */
  @Around("@annotation(me.suhsaechan.suhlogger.annotation.LogCall) || @annotation(me.suhsaechan.suhlogger.annotation.LogMonitor)")
  public Object logMethodInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
    // 로깅이 비활성화된 경우 로깅 없이 메서드만 실행
    if (properties != null && !properties.isEnabled()) {
      return joinPoint.proceed();
    }
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String methodName = signature.getMethod().getName();
    String className = signature.getDeclaringType().getSimpleName();
    String fullMethodName = className + "." + methodName;

    // 메소드 파라미터 정보 수집
    Map<String, Object> parameterMap = extractParameters(joinPoint);

    // 헤더 출력 여부 확인
    boolean shouldLogHeaders = shouldLogHeaders(joinPoint);

    // HTTP 요청 정보 추출
    Map<String, Object> httpInfo = shouldLogHeaders ? extractHttpRequestInfo() : new HashMap<>();

    // 메서드 호출 전 로깅
    SuhLogger.lineLog("[" + fullMethodName + "] CALL");

    // 파라미터 로깅
    if (!parameterMap.isEmpty()) {
      SuhLogger.lineLog("CALL PARAMETER");
      SuhLogger.superLog(parameterMap, false);
    }

    // HTTP 정보 로깅 (있는 경우)
    if (!httpInfo.isEmpty()) {
      SuhLogger.lineLog("HTTP REQUEST INFO");
      SuhLogger.superLog(httpInfo, false);
    }

    try {
      // 메서드 실행
      Object result = joinPoint.proceed();

      // 메서드 호출 결과 로깅
      SuhLogger.lineLog("[" + fullMethodName + "] RESULT");

      // 결과가 있으면 안전하게 JSON 으로 표시
      if (result != null) {
        logResultSafely(result, fullMethodName);
      }

      return result;
    } catch (Exception e) {
      // 예외 발생 시 로깅
      SuhLogger.lineLogError("[ERROR][X]" + fullMethodName + " 예외 발생");
      SuhLogger.error("Exception Type: " + e.getClass().getSimpleName());
      SuhLogger.error("Exception Message: " + e.getMessage());

      throw e;
    }
  }

  /**
   * 헤더 출력 여부를 결정하는 메서드
   * 1. ENABLED: 헤더 출력
   * 2. DISABLED: 헤더 출력 안함
   * 3. BASIC: 전역 설정(properties.header.enabled)에 따라 결정
   */
  private boolean shouldLogHeaders(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    
    // @LogCall 어노테이션 확인
    LogCall logCall = signature.getMethod().getAnnotation(LogCall.class);
    if (logCall != null) {
      HeaderLogOption option = logCall.header();
      if (option == HeaderLogOption.ENABLED) {
        return true;
      } else if (option == HeaderLogOption.DISABLED) {
        return false;
      }
      // BASIC인 경우 전역 설정으로 넘어감
    }
    
    // @LogMonitor 어노테이션 확인
    LogMonitor logMonitor = signature.getMethod().getAnnotation(LogMonitor.class);
    if (logMonitor != null) {
      HeaderLogOption option = logMonitor.header();
      if (option == HeaderLogOption.ENABLED) {
        return true;
      } else if (option == HeaderLogOption.DISABLED) {
        return false;
      }
      // BASIC인 경우 전역 설정으로 넘어감
    }
    
    // BASIC이거나 어노테이션이 없으면 전역 설정 사용
    return properties != null && properties.getHeader() != null && properties.getHeader().isEnabled();
  }

  /**
   * 메소드 파라미터 이름과 값 추출
   */
  private Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint) {
    Map<String, Object> params = new HashMap<>();
    CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
    String[] parameterNames = codeSignature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    if (parameterNames != null) {
      for (int i = 0; i < parameterNames.length; i++) {
        if (i < args.length) {
          params.put(parameterNames[i], args[i]);
        }
      }
    }

    return params;
  }

  /**
   * HTTP 요청 관련 정보 추출 (웹 환경일 때만)
   */
  private Map<String, Object> extractHttpRequestInfo() {
    Map<String, Object> httpInfo = new HashMap<>();

    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        if (request != null) {
          // 주요 HTTP 정보만 수집
          httpInfo.put("method", request.getMethod());
          httpInfo.put("URI", request.getRequestURI());

          // 요청 헤더 마스킹 처리
          Map<String, String> requestHeaders = new HashMap<>();
          java.util.Enumeration<String> headerNames = request.getHeaderNames();
          
          if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
              String headerName = headerNames.nextElement();
              String headerValue = request.getHeader(headerName);
              requestHeaders.put(headerName, headerValue);
            }
            
            // 마스킹 처리된 헤더만 로깅에 포함
            Map<String, String> maskedRequestHeaders = maskSensitiveHeaders(requestHeaders);
            if (!maskedRequestHeaders.isEmpty()) {
              httpInfo.put("headers", maskedRequestHeaders);
            }
          }

          // 요청 ID가 있으면 추가
          String requestId = (String) request.getAttribute("RequestID");
          if (requestId != null) {
            httpInfo.put("requestId", requestId);
          }
        }
      }
    } catch (Exception e) {
      // 웹 환경이 아닌 경우 무시
    }

    return httpInfo;
  }

  /**
   * 결과 객체를 안전하게 로깅
   * ResponseEntity의 경우 특별 처리하여 response 충돌 방지
   */
  private void logResultSafely(Object result, String methodName) {
    try {
      // ResponseEntity인 경우 안전하게 처리
      if (result instanceof ResponseEntity) {
        ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
        
        // ResponseEntity의 안전한 정보만 로깅
        Map<String, Object> safeResponse = new HashMap<>();
        safeResponse.put("statusCode", responseEntity.getStatusCode().toString());
        safeResponse.put("statusCodeValue", responseEntity.getStatusCode().value());
        
        // 헤더 마스킹 처리
        Map<String, String> headers = responseEntity.getHeaders().toSingleValueMap();
        Map<String, String> maskedHeaders = maskSensitiveHeaders(headers);
        safeResponse.put("headers", maskedHeaders);
        
        // Body는 안전하게 처리
        Object body = responseEntity.getBody();
        if (body != null) {
          // Body가 복잡한 객체인 경우 타입 정보만 로깅
          if (isComplexObject(body)) {
            safeResponse.put("bodyType", body.getClass().getSimpleName());
            safeResponse.put("bodyInfo", "Complex object - logged separately by filter");
          } else {
            safeResponse.put("body", body);
          }
        }
        
        SuhLogger.superLog(safeResponse, false);
      } else {
        // 일반 객체는 기존 방식으로 로깅
        SuhLogger.superLog(result, false);
      }
    } catch (Exception e) {
      // 로깅 중 에러가 발생해도 원본 결과에는 영향을 주지 않음
      SuhLogger.warn("결과 로깅 중 에러 발생: " + e.getMessage());
      SuhLogger.info("결과 타입: " + result.getClass().getSimpleName());
    }
  }

  /**
   * 복잡한 객체인지 판단 (직렬화 시 문제가 될 수 있는 객체들)
   */
  private boolean isComplexObject(Object obj) {
    if (obj == null) return false;
    
    String className = obj.getClass().getName();
    
    // Spring 관련 복잡한 객체들
    return className.startsWith("org.springframework.") ||
           className.startsWith("jakarta.servlet.") ||
           className.startsWith("javax.servlet.") ||
           className.contains("$Proxy") ||
           className.contains("CGLIB");
  }

  /**
   * 헤더 맵에서 민감한 헤더를 마스킹 처리
   * @param headers 원본 헤더 맵
   * @return 마스킹 처리된 헤더 맵
   */
  private Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
    // 입력 헤더가 null인 경우 빈 맵 반환
    if (headers == null) {
      return new HashMap<>();
    }
    
    // 마스킹이 비활성화된 경우 원본 반환
    if (properties == null || properties.getMasking() == null || !properties.getMasking().isHeader()) {
      return headers;
    }

    Map<String, String> maskedHeaders = new HashMap<>();
    
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      String headerName = entry.getKey();
      String headerValue = entry.getValue();
      
      if (headerName != null) {
        String lowerHeaderName = headerName.toLowerCase();
        
        // 민감한 헤더들을 마스킹 처리
        if (isSensitiveHeader(lowerHeaderName)) {
          maskedHeaders.put(headerName, "****");
        } else {
          maskedHeaders.put(headerName, headerValue);
        }
      } else {
        maskedHeaders.put(headerName, headerValue);
      }
    }
    
    return maskedHeaders;
  }

  /**
   * 민감한 헤더인지 확인
   * @param lowerHeaderName 소문자로 변환된 헤더명
   * @return 민감한 헤더 여부
   */
  private boolean isSensitiveHeader(String lowerHeaderName) {
    return lowerHeaderName.equals("authorization") ||
           lowerHeaderName.equals("cookie") ||
           lowerHeaderName.equals("set-cookie") ||
           lowerHeaderName.equals("x-auth-token") ||
           lowerHeaderName.equals("x-api-key") ||
           lowerHeaderName.contains("token") ||
           lowerHeaderName.contains("auth");
  }
}