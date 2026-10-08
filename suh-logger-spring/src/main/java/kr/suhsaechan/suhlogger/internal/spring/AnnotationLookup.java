package kr.suhsaechan.suhlogger.internal.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import kr.suhsaechan.suhlogger.annotation.LogCall;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.annotation.LogTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * 메서드 → 대상 클래스 순으로 suh-logger 어노테이션을 찾는다.
 * 메서드에 어노테이션이 하나라도 있으면 메서드 설정만 쓴다 — 클래스 기본값과 섞이면 어떤 옵션이 적용됐는지 알기 어렵다.
 */
public final class AnnotationLookup {

    private AnnotationLookup() {
    }

    public static <A extends Annotation> A find(ProceedingJoinPoint joinPoint, Class<A> type) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (hasAny(method)) {
            return AnnotationUtils.findAnnotation(method, type);
        }
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass()
                : method.getDeclaringClass();
        return AnnotationUtils.findAnnotation(targetClass, type);
    }

    public static boolean hasAny(Method method) {
        return method.isAnnotationPresent(LogCall.class) || method.isAnnotationPresent(LogMonitor.class)
                || method.isAnnotationPresent(LogTime.class);
    }

    public static boolean hasAny(Class<?> type) {
        return type.isAnnotationPresent(LogCall.class) || type.isAnnotationPresent(LogMonitor.class)
                || type.isAnnotationPresent(LogTime.class);
    }
}
