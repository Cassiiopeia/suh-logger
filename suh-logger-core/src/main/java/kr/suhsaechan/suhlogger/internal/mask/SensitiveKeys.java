package kr.suhsaechan.suhlogger.internal.mask;

import java.util.List;

/**
 * 기본 민감 키 목록. 필드·헤더 이름에 이 문자열이 포함되면(대소문자 무시) 마스킹한다.
 * 포함 비교라 accessToken·refreshToken·idToken은 "token" 하나로 잡힌다.
 */
public final class SensitiveKeys {

    public static final List<String> DEFAULT_FIELDS = List.of(
            "password", "passwd", "pwd", "secret", "token", "authorization", "credential",
            "apikey", "api_key", "api-key", "csrf", "cookie", "privatekey", "private_key");

    public static final List<String> DEFAULT_HEADERS = List.of(
            "authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token", "x-csrf-token", "x-xsrf-token");

    /** 개인정보 프리셋 — 디버깅에 쓰는 경우가 많아 기본값에서는 빼고 suh-logger.masking.presets=pii로 켠다 */
    public static final List<String> PII = List.of("email", "phone", "mobile", "ssn", "residentnumber", "address");

    private SensitiveKeys() {
    }

    public static List<String> preset(String name) {
        return "pii".equalsIgnoreCase(name) ? PII : List.of();
    }
}
