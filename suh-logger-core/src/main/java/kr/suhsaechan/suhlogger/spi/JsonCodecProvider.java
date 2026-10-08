package kr.suhsaechan.suhlogger.spi;

import kr.suhsaechan.suhlogger.annotation.Incubating;

/**
 * ServiceLoader로 찾는 JsonCodec 공급자. 구현 클래스는 선택 의존(Jackson) 타입을 필드·시그니처에 두면 안 된다 —
 * 해당 라이브러리가 없을 때 공급자 로딩 자체가 실패하기 때문. 타입 사용은 create() 본문 안에서만 한다.
 */
@Incubating(since = "3.0.0")
public interface JsonCodecProvider {

    /** 필요한 라이브러리가 classpath에 있는지 */
    boolean isAvailable();

    JsonCodec create();

    /** 작을수록 우선 */
    default int order() {
        return 0;
    }
}
