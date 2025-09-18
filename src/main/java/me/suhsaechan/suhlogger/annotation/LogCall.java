package me.suhsaechan.suhlogger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogCall {
    /**
     * HTTP 헤더 로깅 제어
     * BASIC: 전역 설정(suh-logger.header.enabled)을 따름 (기본값)
     * ENABLED: 헤더 정보를 로그에 포함 (전역 설정보다 우선)
     * DISABLED: 헤더 정보를 로그에 포함하지 않음 (전역 설정보다 우선)
     */
    HeaderLogOption header() default HeaderLogOption.BASIC;
}
