package kr.suhsaechan.suhlogger.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import kr.suhsaechan.suhlogger.util.SuhLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.annotation.LogCall;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.annotation.TriState;
import kr.suhsaechan.suhlogger.util.CommonUtil;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.RequestSnapshot;
import kr.suhsaechan.suhlogger.internal.mask.Masker;
import kr.suhsaechan.suhlogger.internal.mask.ObjectTrees;
import kr.suhsaechan.suhlogger.internal.spring.AnnotationLookup;
import kr.suhsaechan.suhlogger.internal.spring.ResponseEntityResults;

import java.util.ArrayList;
import java.util.Arrays;

@Aspect
public class SuhMethodInvocationLoggingAspect {

  private final SuhLoggerProperties properties;
  private final RequestContextAccessor requestContextAccessor;

  // 요청 정보는 어댑터(Servlet·WebFlux)가 RequestContextAccessor로 넘긴다 — aspect가 Servlet을 직접 몰라도 되게
  public SuhMethodInvocationLoggingAspect(SuhLoggerProperties properties, RequestContextAccessor requestContextAccessor) {
    this.properties = properties;
    this.requestContextAccessor = requestContextAccessor != null ? requestContextAccessor : RequestContextAccessor.NONE;
  }

  /**
   * LogMethodInvocation, LogMonitoringInvocation 어노테이션이 붙은 메서드 호출 정보 로깅
   */
  @Around("@annotation(kr.suhsaechan.suhlogger.annotation.LogCall) || @annotation(kr.suhsaechan.suhlogger.annotation.LogMonitor)"
      + " || @within(kr.suhsaechan.suhlogger.annotation.LogCall) || @within(kr.suhsaechan.suhlogger.annotation.LogMonitor)")
  public Object logMethodInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
    // 로깅이 비활성화된 경우 로깅 없이 메서드만 실행
    if (properties != null && !properties.isEnabled()) {
      return joinPoint.proceed();
    }
    // 메서드 설정 우선: 클래스 @LogMonitor 안에서 메서드가 @LogTime만 달고 있으면 호출 로그는 남기지 않는다
    if (AnnotationLookup.find(joinPoint, LogCall.class) == null && AnnotationLookup.find(joinPoint, LogMonitor.class) == null) {
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

    // 마스킹 대상이면 파라미터·결과 모두 같은 규칙으로 가린다 (null이면 마스킹 안 함)
    Masker masker = shouldMask(joinPoint)
        ? new Masker(collectMaskFields(joinPoint), CommonUtil.getMaskValue(properties != null ? properties.getMasking() : null))
        : null;

    // 파라미터 로깅 (params = true 인 경우만)
    if (shouldLogParams) {
      Map<String, Object> parameterMap = extractParameters(joinPoint);
      if (!parameterMap.isEmpty()) {
        // DTO·record를 필드 트리로 바꿔야 중첩 필드(body.password 등)까지 가릴 수 있다
        Object tree = ObjectTrees.toTree(parameterMap);
        SuhLogger.lineLog("CALL PARAMETER");
        SuhLogger.superLog(masker != null ? masker.mask(tree) : tree, false);
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
          logResultSafely(result, fullMethodName, masker);
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
    LogCall logCall = AnnotationLookup.find(joinPoint, LogCall.class);
    if (logCall != null) {
      return logCall.params();
    }

    // @LogMonitor 어노테이션 확인
    LogMonitor logMonitor = AnnotationLookup.find(joinPoint, LogMonitor.class);
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
    LogCall logCall = AnnotationLookup.find(joinPoint, LogCall.class);
    if (logCall != null) {
      return logCall.result();
    }

    // @LogMonitor 어노테이션 확인
    LogMonitor logMonitor = AnnotationLookup.find(joinPoint, LogMonitor.class);
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
    LogCall logCall = AnnotationLookup.find(joinPoint, LogCall.class);
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
    LogMonitor logMonitor = AnnotationLookup.find(joinPoint, LogMonitor.class);
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
    LogCall logCall = AnnotationLookup.find(joinPoint, LogCall.class);
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
    LogMonitor logMonitor = AnnotationLookup.find(joinPoint, LogMonitor.class);
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
      // 기본 민감 키 + 프리셋 + 사용자 지정 (#55)
      List<String> globalFields = properties.getMasking().effectiveMaskFields();
      if (globalFields != null) {
        fields.addAll(globalFields);
      }
    }

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();

    // @LogCall 어노테이션의 maskFields 추가
    LogCall logCall = AnnotationLookup.find(joinPoint, LogCall.class);
    if (logCall != null && logCall.maskFields().length > 0) {
      fields.addAll(Arrays.asList(logCall.maskFields()));
    }

    // @LogMonitor 어노테이션의 maskFields 추가
    LogMonitor logMonitor = AnnotationLookup.find(joinPoint, LogMonitor.class);
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
    Class<?>[] parameterTypes = codeSignature.getParameterTypes();
    Object[] args = joinPoint.getArgs();

    if (parameterNames != null) {
      for (int i = 0; i < parameterNames.length; i++) {
        // Kotlin suspend 함수는 컴파일러가 Continuation 파라미터를 끝에 추가한다 — 사용자 파라미터가 아니므로 제외
        if (parameterTypes != null && i < parameterTypes.length
            && KOTLIN_CONTINUATION.equals(parameterTypes[i].getName())) {
          continue;
        }
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
    RequestSnapshot request;
    try {
      request = requestContextAccessor.current();
    } catch (RuntimeException e) {
      // 요청 정보 조회 실패는 로깅 생략 사유일 뿐 호출을 막지 않는다
      return httpInfo;
    }
    if (request == null) {
      return httpInfo;
    }
    httpInfo.put("method", request.getMethod());
    httpInfo.put("URI", request.getUri());
    Map<String, String> filteredHeaders = filterHeaders(request.getHeaders());
    if (!filteredHeaders.isEmpty()) {
      httpInfo.put("headers", filteredHeaders);
    }
    if (request.getRequestId() != null) {
      httpInfo.put("requestId", request.getRequestId());
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
  private void logResultSafely(Object result, String methodName, Masker masker) {
    try {
      // spring-web이 없는 앱(배치)에서도 클래스 로딩이 깨지지 않도록 이름으로 먼저 판별한다
      if (isResponseEntity(result)) {
        Map<String, Object> safeResponse = ResponseEntityResults.toSafeMap(result, this::maskSensitiveHeaders,
            this::isComplexObject);
        Object tree = ObjectTrees.toTree(safeResponse);
        SuhLogger.superLog(masker != null ? masker.mask(tree) : tree, false);
      } else {
        // DTO 반환값(로그인 응답의 토큰 등)도 필드 단위로 마스킹
        Object tree = ObjectTrees.toTree(result);
        SuhLogger.superLog(masker != null ? masker.mask(tree) : tree, false);
      }
    } catch (Exception e) {
      // 로깅 중 에러가 발생해도 원본 결과에는 영향을 주지 않음
      SuhLogger.warn("결과 로깅 중 에러 발생: " + e.getMessage());
      SuhLogger.info("결과 타입: " + result.getClass().getSimpleName());
    }
  }

  private static final String RESPONSE_ENTITY = "org.springframework.http.ResponseEntity";
  private static final String KOTLIN_CONTINUATION = "kotlin.coroutines.Continuation";

  /** spring-web 클래스를 로드하지 않고 이름만으로 판별 — ResponseEntityResults는 이 검사를 통과한 뒤에만 로드된다 */
  private static boolean isResponseEntity(Object result) {
    for (Class<?> c = result.getClass(); c != null; c = c.getSuperclass()) {
      if (RESPONSE_ENTITY.equals(c.getName())) {
        return true;
      }
    }
    return false;
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

    List<String> maskHeaders = masking.effectiveMaskHeaders();
    if (maskHeaders == null || maskHeaders.isEmpty()) {
      return headers;
    }

    // commonUtil 공통 메서드 사용
    String maskValue = CommonUtil.getMaskValue(masking);
    return CommonUtil.maskHeaders(headers, maskHeaders, maskValue);
  }
}