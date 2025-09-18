package me.suhsaechan.suhlogger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogMonitor {
    /**
     * HTTP 헤더 정보 포함 여부
     * true: 헤더 정보를 로그에 포함 (전역 설정보다 우선)
     * false: 헤더 정보를 로그에 포함하지 않음 (기본값)
     */
    boolean header() default false;
}
