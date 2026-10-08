package kr.suhsaechan.suhlogger.spi;

import kr.suhsaechan.suhlogger.annotation.Incubating;

/**
 * 직렬화가 위험한 타입(스트림, 파일, 지오메트리 등)을 로그에 안전한 값으로 바꾸는 확장 지점.
 * core를 고치지 않고 자기 도메인 타입을 등록할 수 있게 하려고 분리했다.
 * 등록 방법: {@code META-INF/services/kr.suhsaechan.suhlogger.spi.TypeHandler}(ServiceLoader),
 * Boot에서는 TypeHandler 빈 등록.
 */
@Incubating(since = "3.0.0")
public interface TypeHandler {

    /** 이 핸들러가 처리할 값인지 */
    boolean supports(Object value);

    /** 로그에 남길 안전한 값 (Map·String·Number 권장) */
    Object toSafe(Object value);

    /** 작을수록 먼저 검사한다. 사용자 핸들러가 기본 핸들러보다 앞서도록 기본 구현은 100 이상을 쓴다 */
    default int order() {
        return 0;
    }
}
