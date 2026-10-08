package kr.suhsaechan.suhlogger.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 아직 안정화되지 않은 API 표시. minor 버전에서 바뀔 수 있다.
 * 표시가 없는 공개 API는 major 버전에서만 깨진다 (Gradle·Micrometer의 @Incubating과 같은 규칙).
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Incubating {

    /** 처음 도입된 버전 */
    String since() default "";
}
