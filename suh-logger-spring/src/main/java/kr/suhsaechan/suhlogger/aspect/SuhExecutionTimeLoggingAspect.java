package kr.suhsaechan.suhlogger.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.annotation.LogTime;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.internal.spring.AnnotationLookup;
import kr.suhsaechan.suhlogger.util.SuhLogger;

@Aspect
public class SuhExecutionTimeLoggingAspect {

  private final SuhLoggerProperties properties;

  // 컴포넌트 스캔 대신 설정 클래스가 명시적으로 등록한다 (사용자 패키지 스캔 오염 방지)
  public SuhExecutionTimeLoggingAspect(SuhLoggerProperties properties) {
    this.properties = properties;
  }

  /**
   * LogTimeInvocation, LogMonitoringInvocation 어노테이션이 붙은 메서드 실행 시간 로깅
   */
  @Around("@annotation(kr.suhsaechan.suhlogger.annotation.LogTime) || @annotation(kr.suhsaechan.suhlogger.annotation.LogMonitor)"
      + " || @within(kr.suhsaechan.suhlogger.annotation.LogTime) || @within(kr.suhsaechan.suhlogger.annotation.LogMonitor)")
  public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
    // 로깅이 비활성화된 경우 로깅 없이 메서드만 실행
    if (properties != null && !properties.isEnabled()) {
      return joinPoint.proceed();
    }
    // 메서드에 붙은 설정 우선: 메서드가 @LogCall만 달고 있으면 클래스의 @LogMonitor가 있어도 시간은 남기지 않는다
    if (AnnotationLookup.find(joinPoint, LogTime.class) == null && AnnotationLookup.find(joinPoint, LogMonitor.class) == null) {
      return joinPoint.proceed();
    }

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String methodName = signature.getMethod().getName();
    String className = signature.getDeclaringType().getSimpleName();
    String fullMethodName = className + "." + methodName;

    // 시작 시간 기록
    long startTime = System.currentTimeMillis();

    try {
      // 메서드 실행
      return joinPoint.proceed();
    } finally {
      // 종료 시간 및 실행 시간 계산
      long executionTime = System.currentTimeMillis() - startTime;

      // 실행 시간 로깅
      SuhLogger.lineLog("[TIME]: " + fullMethodName + " : " + executionTime + " ms");
    }
  }
}