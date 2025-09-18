package me.suhsaechan.suhlogger.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import me.suhsaechan.suhlogger.config.SuhLoggerProperties;
import me.suhsaechan.suhlogger.util.SuhLogger;

@Aspect
@Component
public class SuhExecutionTimeLoggingAspect {

  @Autowired
  private SuhLoggerProperties properties;

  /**
   * LogTimeInvocation, LogMonitoringInvocation 어노테이션이 붙은 메서드 실행 시간 로깅
   */
  @Around("@annotation(me.suhsaechan.suhlogger.annotation.LogTime) || @annotation(me.suhsaechan.suhlogger.annotation.LogMonitor)")
  public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
    // 로깅이 비활성화된 경우 로깅 없이 메서드만 실행
    if (properties != null && !properties.isEnabled()) {
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