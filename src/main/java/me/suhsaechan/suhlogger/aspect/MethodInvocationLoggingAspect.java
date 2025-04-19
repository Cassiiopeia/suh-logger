package me.suhsaechan.suhlogger.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import me.suhsaechan.suhlogger.util.SuhLogger;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class MethodInvocationLoggingAspect {

  /**
   * LogMethodInvocation, LogMonitoringInvocation 어노테이션이 붙은 메서드 호출 정보 로깅
   */
  @Around("@annotation(me.suhsaechan.suhlogger.annotation.LogMethodInvocation) || @annotation(me.suhsaechan.suhlogger.annotation.LogMonitoringInvocation)")
  public Object logMethodInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String methodName = signature.getMethod().getName();
    String className = signature.getDeclaringType().getSimpleName();
    String fullMethodName = className + "." + methodName;

    // 메소드 파라미터 정보 수집
    Map<String, Object> parameterMap = extractParameters(joinPoint);

    // HTTP 요청 정보 추출
    Map<String, Object> httpInfo = extractHttpRequestInfo();

    // 메서드 호출 전 로깅
    SuhLogger.lineLog("[" + fullMethodName + "] 호출");

    // 파라미터 로깅
    if (!parameterMap.isEmpty()) {
      SuhLogger.lineLog("파라미터 MAP");
      SuhLogger.superLog(parameterMap, false);
    }

    // HTTP 정보 로깅 (있는 경우)
    if (!httpInfo.isEmpty()) {
      SuhLogger.lineLog("HTTP 정보");
      SuhLogger.superLog(httpInfo, false);
    }

    try {
      // 메서드 실행
      Object result = joinPoint.proceed();

      // 메서드 호출 결과 로깅
      SuhLogger.lineLog("[" + fullMethodName + "] 결과");

      // 결과가 있으면 JSON 으로 표시
      if (result != null) {
        SuhLogger.superLog(result, false);
      }

      return result;
    } catch (Exception e) {
      // 예외 발생 시 로깅
      SuhLogger.lineLogError("[ERROR][X]" + fullMethodName + " 예외 발생");
      SuhLogger.lineLog("예외 유형: " + e.getClass().getName());
      SuhLogger.lineLog("예외 메시지: " + e.getMessage());

      throw e;
    }
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

          // 인증 헤더 존재시 마스킹해서 표시
          String authHeader = request.getHeader("Authorization");
          if (authHeader != null && !authHeader.isEmpty()) {
            if (authHeader.startsWith("Bearer ")) {
              httpInfo.put("auth", "Bearer ****" + authHeader.substring(authHeader.length() - 4));
            } else {
              httpInfo.put("auth", "**** (masked)");
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
}