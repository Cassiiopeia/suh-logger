package kr.suhsaechan.suhlogger.spi;

import kr.suhsaechan.suhlogger.annotation.Incubating;

/**
 * 현재 스레드의 요청 정보를 꺼내는 확장 지점.
 * aspect가 jakarta.servlet을 직접 import하면 Servlet 없는 앱(WebFlux·배치)에서 클래스 로딩이 깨지므로 분리했다.
 */
@Incubating(since = "3.0.0")
@FunctionalInterface
public interface RequestContextAccessor {

    /** 요청이 없으면 null */
    RequestSnapshot current();

    /** 요청 개념이 없는 환경용 */
    RequestContextAccessor NONE = () -> null;
}
