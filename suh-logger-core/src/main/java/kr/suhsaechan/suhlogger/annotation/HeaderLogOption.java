package kr.suhsaechan.suhlogger.annotation;

/**
 * 헤더 로깅 제어 옵션
 *
 * @deprecated TriState enum 사용을 권장합니다.
 * <ul>
 *   <li>BASIC → TriState.DEFAULT</li>
 *   <li>ENABLED → TriState.ON</li>
 *   <li>DISABLED → TriState.OFF</li>
 * </ul>
 *
 * @see TriState
 */
@Deprecated
public enum HeaderLogOption {
    /**
     * 전역 설정(suh-logger.header.enabled)을 따름 (기본값)
     * @deprecated TriState.DEFAULT 사용 권장
     */
    @Deprecated
    BASIC,

    /**
     * 헤더 정보를 로그에 포함 (전역 설정보다 우선)
     * @deprecated TriState.ON 사용 권장
     */
    @Deprecated
    ENABLED,

    /**
     * 헤더 정보를 로그에 포함하지 않음 (전역 설정보다 우선)
     * @deprecated TriState.OFF 사용 권장
     */
    @Deprecated
    DISABLED;

    /**
     * HeaderLogOption을 TriState로 변환
     * @return 대응하는 TriState 값
     */
    public TriState toTriState() {
        switch (this) {
            case ENABLED:
                return TriState.ON;
            case DISABLED:
                return TriState.OFF;
            default:
                return TriState.DEFAULT;
        }
    }
}
