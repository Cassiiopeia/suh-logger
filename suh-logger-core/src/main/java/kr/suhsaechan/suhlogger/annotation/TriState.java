package kr.suhsaechan.suhlogger.annotation;

/**
 * 3상태 옵션 enum
 * - DEFAULT: 전역 설정(application.yml) 따름
 * - ON: 강제 활성화
 * - OFF: 강제 비활성화
 *
 * 사용 예시:
 * <pre>
 * import static kr.suhsaechan.suhlogger.annotation.TriState.*;
 *
 * &#64;LogMonitor(header = ON)
 * public void myMethod() {}
 * </pre>
 */
public enum TriState {
    /**
     * 전역 설정(application.yml)을 따름 (기본값)
     */
    DEFAULT,

    /**
     * 강제 활성화 (전역 설정보다 우선)
     */
    ON,

    /**
     * 강제 비활성화 (전역 설정보다 우선)
     */
    OFF
}
