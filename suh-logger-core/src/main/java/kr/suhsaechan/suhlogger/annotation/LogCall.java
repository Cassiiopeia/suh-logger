package kr.suhsaechan.suhlogger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 호출 로깅 어노테이션
 *
 * 사용 예시:
 * <pre>
 * import static kr.suhsaechan.suhlogger.annotation.TriState.*;
 *
 * // 기본 - 전역 설정 따름
 * &#64;LogCall
 * public void basicMethod() {}
 *
 * // 헤더 강제 출력
 * &#64;LogCall(header = ON)
 * public void withHeader() {}
 *
 * // 헤더 끔, 결과 로깅 안함
 * &#64;LogCall(header = OFF, result = false)
 * public void minimal() {}
 *
 * // 마스킹 강제 + 추가 필드
 * &#64;LogCall(mask = ON, maskFields = {"ssn", "creditCard"})
 * public void sensitiveData() {}
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogCall {

    /**
     * 헤더 로깅 제어
     * - DEFAULT: 전역 설정(suh-logger.header.enabled) 따름
     * - ON: 헤더 정보를 강제로 로그에 포함
     * - OFF: 헤더 정보를 강제로 로그에서 제외
     */
    TriState header() default TriState.DEFAULT;

    /**
     * 파라미터 로깅 여부 (기본값: true)
     * - true: 메서드 파라미터를 로그에 포함
     * - false: 메서드 파라미터를 로그에서 제외
     */
    boolean params() default true;

    /**
     * 결과 로깅 여부 (기본값: true)
     * - true: 메서드 반환값을 로그에 포함
     * - false: 메서드 반환값을 로그에서 제외
     */
    boolean result() default true;

    /**
     * 마스킹 제어
     * - DEFAULT: 전역 설정(suh-logger.masking.enabled) 따름
     * - ON: 마스킹을 강제로 활성화
     * - OFF: 마스킹을 강제로 비활성화
     */
    TriState mask() default TriState.DEFAULT;

    /**
     * 메서드별 추가 마스킹 필드 (전역 설정에 추가됨)
     * 지정된 필드명이 포함된 파라미터/필드는 마스킹 처리됨
     *
     * 예: maskFields = {"password", "ssn"}
     */
    String[] maskFields() default {};
}
