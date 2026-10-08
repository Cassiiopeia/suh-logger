package kr.suhsaechan.suhlogger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
// 클래스에 붙이면 그 클래스의 public 메서드 전체에 적용된다 (메서드에 붙은 설정이 우선)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LogTime {
}
