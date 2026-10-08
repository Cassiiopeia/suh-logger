package kr.suhsaechan.suhlogger.config;

/** HTTP 요청 로그 형식 (suh-logger.format) */
public enum LogFormat {
    /** 구분선으로 둘러싼 여러 줄 (2.x 형식, 기본값) */
    BLOCK,
    /** 요청당 한 줄 — 동시 요청이 섞여도 짝이 맞고 grep하기 쉽다 (운영 권장) */
    LINE
}
