package kr.suhsaechan.suhlogger.internal.http;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import org.junit.jupiter.api.Test;

class PathPatternsTest {

    @Test
    void doubleStarMatchesZeroOrMoreSegments() {
        assertTrue(PathPatterns.matches("/actuator/**", "/actuator"));
        assertTrue(PathPatterns.matches("/actuator/**", "/actuator/health"));
        assertTrue(PathPatterns.matches("/actuator/**", "/actuator/health/liveness"));
        assertFalse(PathPatterns.matches("/actuator/**", "/api/actuator"));
    }

    @Test
    void singleStarAndQuestionStayInOneSegment() {
        assertTrue(PathPatterns.matches("/api/*/users", "/api/v1/users"));
        assertFalse(PathPatterns.matches("/api/*/users", "/api/v1/x/users"));
        assertTrue(PathPatterns.matches("/files/*.png", "/files/a.png"));
        assertTrue(PathPatterns.matches("/v?/ping", "/v2/ping"));
    }

    @Test
    void antPatternDoesNotOverMatchLikeContains() {
        // 2.x contains 방식의 문제: auth/login 이 login-history까지 제외
        assertTrue(PathPatterns.matches("auth/login", "/api/auth/login-history"));
        // 세그먼트 단위 Ant 패턴은 다른 경로를 잡지 않는다 — WARN 메시지가 안내하는 형태
        assertFalse(PathPatterns.matches("/api/auth/login/**", "/api/auth/login-history"));
        assertTrue(PathPatterns.matches("/api/auth/login/**", "/api/auth/login"));
    }

    @Test
    void actuatorIsExcludedByDefault() {
        HttpExchangeLogger logger = new HttpExchangeLogger(new SuhLoggerProperties());
        assertTrue(logger.isExcluded("/actuator/health"));
        assertFalse(logger.isExcluded("/api/orders"));
    }

    @Test
    void userListReplacesDefault() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setExcludePatterns(List.of("/internal/**"));
        HttpExchangeLogger logger = new HttpExchangeLogger(p);
        assertTrue(logger.isExcluded("/internal/x"));
        assertFalse(logger.isExcluded("/actuator/health"), "사용자 목록이 기본값을 덮어쓴다");
    }

    @Test
    void uriVariablesAndRegexCharactersAreSafe() {
        // 리뷰 C1: {id}가 정규식 반복자로 해석돼 PatternSyntaxException → 요청 500
        assertTrue(PathPatterns.matches("/api/{id}/**", "/api/5/x"));
        assertFalse(PathPatterns.matches("/api/{id}/**", "/other/5"));
        assertTrue(PathPatterns.matches("/files/(v1)+*.json", "/files/(v1)+a.json"));
        assertFalse(PathPatterns.matches("/files/(v1)+*.json", "/files/v1a.json"));
    }

    @Test
    void brokenPatternNeverFailsTheRequest() {
        SuhLoggerProperties p = new SuhLoggerProperties();
        p.setExcludePatterns(List.of("/api/{unclosed/**"));
        assertFalse(new HttpExchangeLogger(p).isExcluded("/api/x"));
    }
}
