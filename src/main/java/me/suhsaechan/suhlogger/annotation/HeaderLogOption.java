package me.suhsaechan.suhlogger.annotation;

/**
 * 헤더 로깅 제어 옵션
 */
public enum HeaderLogOption {
    /**
     * 전역 설정(suh-logger.header.enabled)을 따름 (기본값)
     */
    BASIC,
    
    /**
     * 헤더 정보를 로그에 포함 (전역 설정보다 우선)
     */
    ENABLED,
    
    /**
     * 헤더 정보를 로그에 포함하지 않음 (전역 설정보다 우선)
     */
    DISABLED
}