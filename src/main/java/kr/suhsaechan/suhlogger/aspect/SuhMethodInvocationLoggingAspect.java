package kr.suhsaechan.suhlogger.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import kr.suhsaechan.suhlogger.util.SuhLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.annotation.LogCall;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.annotation.TriState;
import kr.suhsaechan.suhlogger.util.CommonUtil;

import java.util.ArrayList;
import java.util.Arrays;

@Aspect
@Component
public class SuhMethodInvocationLoggingAspect {

  @Autowired
  private SuhLoggerProperties properties;

  /**
   * LogMethodInvocation, LogMonitoringInvocation 어노테이션이 붙은 메서드 호출 정보 로깅
   */
  @Around("@annotation(kr.suhsaechan.suhlogger.annotation.LogCall) || @annotation(kr.suhsaechan.suhlogger.annotation.LogMonitor)")
  public Object logMethodInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
    // 로깅이 비활성화된 경우 로깅 없이 메서드만 실행
    if (properties != null && !properties.isEnabled()) {
      return joinPoint.proceed();
    }
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String methodName = signature.getMethod().getName();
    String className = signature.getDeclaringType().getSimpleName();
    String fullMethodName = className + "." + methodName;

    // 어노테이션 옵션 확인
    boolean shouldLogParams = shouldLogParams(joinPoint);
    boolean shouldLogResult = shouldLogResult(joinPoint);
    boolean shouldLogHeaders = shouldLogHeaders(joinPoint);

    // 메서드 호출 전 로깅
    SuhLogger.lineLog("[" + fullMethodName + "] CALL");

    // 파라미터 로깅 (params = true 인 경우만)
    if (shouldLogParams) {
      Map<String, Object> parameterMap = extractParameters(joinPoint);
      // 마스킹 적용
      if (shouldMask(joinPoint)) {
        List<String> maskFields = collectMaskFields(joinPoint);
        String maskValue = CommonUtil.getMaskValue(properties != null ? properties.getMasking() : null);
        parameterMap = CommonUtil.maskParameters(parameterMap, maskFields, maskValue);
      }
      if (!parameterMap.isEmpty()) {
        SuhLogger.lineLog("CALL PARAMETER");
        SuhLogger.superLog(parameterMap, false);
      }
    }

    // HTTP 정보 로깅 (header = ON 또는 전역 설정 true 인 경우)
    if (shouldLogHeaders) {
      Map<String, Object> httpInfo = extractHttpRequestInfo();
      if (!httpInfo.isEmpty()) {
        SuhLogger.lineLog("HTTP REQUEST INFO");
        SuhLogger.superLog(httpInfo, false);
      }
    }

    try {
      // 메서드 실행
      Object result = joinPoint.proceed();

      // 결과 로깅 (result = true 인 경우만)
      if (shouldLogResult) {
        SuhLogger.lineLog("[" + fullMethodName + "] RESULT");
        if (result != null) {
          logResultSafely(result, fullMethodName);
        }
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
   * 파라미터 로깅 여부를 결정하는 메서드
   * 어노테이션의 params 속성 확인 (기본값: true)
   */
  private boolean shouldLogParams(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();

    // @LogCall 어노테이션 확인
    LogCall logCall = signature.getMethod().getAnnotation(LogCall.class);
    if (logCall != null) {
      return logCall.params();
    }

    // @LogMonitor 어노테이션 확인
    LogMonitor logMonitor = signature.getMethod().getAnnotation(LogMonitor.class);
    if (logMonitor != null) {
      return logMonitor.params();
    }

    // 기본값 true
    return true;
  }

  /**
   * 결과 로깅 여부를 결정하는 메서드
   * 어노테이션의 result 속성 확인 (기본값: true)
   */
  private boolean shouldLogResult(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();

    // @LogCall 어노테이션 확인
    LogCall logCall = signature.getMethod().getAnnotation(LogCall.class);
    if (logCall != null) {
      return logCall.result();
    }

    // @LogMonitor 어노테이션 확인
    LogMonitor logMonitor = signature.getMethod().getAnnotation(LogMonitor.class);
    if (logMonitor != null) {
      return logMonitor.result();
    }

    // 기본값 true
    return true;
  }

  /**
   * 헤더 출력 여부를 결정하는 메서드
   * - ON: 헤더 출력
   * - OFF: 헤더 출력 안함
   * - DEFAULT: 전역 설정(properties.header.enabled)에 따라 결정
   */
  private boolean shouldLogHeaders(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();

    // @LogCall 어노테이션 확인
    LogCall logCall = signature.getMethod().getAnnotation(LogCall.class);
    if (logCall != null) {
      TriState headerState = logCall.header();
      if (headerState == TriState.ON) {
        return true;
      } else if (headerState == TriState.OFF) {
        return false;
      }
      // DEFAULT인 경우 전역 설정으로 넘어감
    }

    // @LogMonitor 어노테이션 확인
    LogMonitor logMonitor = signature.getMethod().getAnnotation(LogMonitor.class);
    if (logMonitor != null) {
      TriState headerState = logMonitor.header();
      if (headerState == TriState.ON) {
        return true;
      } else if (headerState == TriState.OFF) {
        return false;
      }
      // DEFAULT인 경우 전역 설정으로 넘어감
    }

    // DEFAULT이거나 어노테이션이 없으면 전역 설정 사용
    return properties != null && properties.getHeader() != null && properties.getHeader().isEnabled();
  }

  /**
   * 마스킹 활성화 여부를 결정하는 메서드
   * - ON: 마스킹 활성화
   * - OFF: 마스킹 비활성화
   * - DEFAULT: 전역 설정(properties.masking.enabled)에 따라 결정
   */
  private boolean shouldMask(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();

    // @LogCall 어노테이션 확인
    LogCall logCall = signature.getMethod().getAnnotation(LogCall.class);
    if (logCall != null) {
      TriState maskState = logCall.mask();
      if (maskState == TriState.ON) {
        return true;
      } else if (maskState == TriState.OFF) {
        return false;
      }
      // DEFAULT인 경우 전역 설정으로 넘어감
    }

    // @LogMonitor 어노테이션 확인
    LogMonitor logMonitor = signature.getMethod().getAnnotation(LogMonitor.class);
    if (logMonitor != null) {
      TriState maskState = logMonitor.mask();
      if (maskState == TriState.ON) {
        return true;
      } else if (maskState == TriState.OFF) {
        return false;
      }
      // DEFAULT인 경우 전역 설정으로 넘어감
    }

    // DEFAULT이거나 어노테이션이 없으면 전역 설정 사용
    return properties != null && properties.getMasking() != null && properties.getMasking().isEnabled();
  }

  /**
   * 마스킹할 필드 목록 수집 (전역 설정 + 어노테이션 병합)
   */
  private List<String> collectMaskFields(ProceedingJoinPoint joinPoint) {
    List<String> fields = new ArrayList<>();

    // 전역 설정 필드
    if (properties != null && properties.getMasking() != null) {
      List<String> globalFields = properties.getMasking().getMaskFields();
      if (globalFields != null) {
        fields.addAll(globalFields);
      }
    }

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();

    // @LogCall 어노테이션의 maskFields 추가
    LogCall logCall = signature.getMethod().getAnnotation(LogCall.class);
    if (logCall != null && logCall.maskFields().length > 0) {
      fields.addAll(Arrays.asList(logCall.maskFields()));
    }

    // @LogMonitor 어노테이션의 maskFields 추가
    LogMonitor logMonitor = signature.getMethod().getAnnotation(LogMonitor.class);
    if (logMonitor != null && logMonitor.maskFields().length > 0) {
      fields.addAll(Arrays.asList(logMonitor.maskFields()));
    }

    return fields;
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

          // 요청 헤더 수집
          Map<String, String> requestHeaders = new HashMap<>();
          java.util.Enumeration<String> headerNames = request.getHeaderNames();

          if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
              String headerName = headerNames.nextElement();
              String headerValue = request.getHeader(headerName);
              requestHeaders.put(headerName, headerValue);
            }

            // 헤더 필터링 및 마스킹 처리
            Map<String, String> filteredHeaders = filterHeaders(requestHeaders);
            if (!filteredHeaders.isEmpty()) {
              httpInfo.put("headers", filteredHeaders);
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
   * 헤더 필터링 - 설정에 따라 출력할 헤더를 선택
   * @param headers 원본 헤더 맵
   * @return 필터링된 헤더 맵
   */
  private Map<String, String> filterHeaders(Map<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return Collections.emptyMap();
    }

    SuhLoggerProperties.HeaderConfig headerConfig = properties != null ? properties.getHeader() : null;

    // 헤더 설정이 없거나 비활성화된 경우 빈 맵 반환
    if (headerConfig == null || !headerConfig.isEnabled()) {
      return Collections.emptyMap();
    }

    // 모든 헤더 출력인 경우
    if (headerConfig.isIncludeAll()) {
      return maskSensitiveHeaders(headers);
    }

    // 특정 헤더만 출력하는 경우
    List<String> includeHeaders = headerConfig.getIncludeHeaders();
    if (includeHeaders == null || includeHeaders.isEmpty()) {
      return Collections.emptyMap();
    }

    // includeHeaders 목록에 있는 헤더만 필터링
    Map<String, String> filtered = headers.entrySet().stream()
        .filter(entry -> includeHeaders.stream()
            .anyMatch(h -> entry.getKey().equalsIgnoreCase(h)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return maskSensitiveHeaders(filtered);
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
   * commonUtil.maskHeaders()를 사용하여 중복 로직 제거
   * @param headers 원본 헤더 맵
   * @return 마스킹 처리된 헤더 맵
   */
  private Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
    if (headers == null) {
      return new HashMap<>();
    }

    SuhLoggerProperties.MaskingConfig masking = properties != null ? properties.getMasking() : null;

    // 마스킹이 비활성화되었거나 마스킹할 헤더 키워드가 없는 경우 원본 반환
    if (masking == null || !masking.isEnabled()) {
      return headers;
    }

    List<String> maskHeaders = masking.getMaskHeaders();
    if (maskHeaders == null || maskHeaders.isEmpty()) {
      return headers;
    }

    // commonUtil 공통 메서드 사용
    String maskValue = CommonUtil.getMaskValue(masking);
    return CommonUtil.maskHeaders(headers, maskHeaders, maskValue);
  }
}