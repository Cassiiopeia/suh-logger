package kr.suhsaechan.suhlogger.config;

/** 응답 본문을 로그에 남기는 범위 (suh-logger.response-body) */
public enum ResponseBodyMode {
    /** 본문을 남기지 않는다 — 상태·URI만 */
    NONE,
    /** 4xx·5xx 응답만 본문을 남긴다 (운영 권장) */
    ERROR_ONLY,
    /** 모든 응답의 본문을 남긴다 (기본값, 2.x 호환) */
    ALL
}
