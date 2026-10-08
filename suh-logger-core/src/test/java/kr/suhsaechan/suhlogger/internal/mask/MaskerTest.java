package kr.suhsaechan.suhlogger.internal.mask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import org.junit.jupiter.api.Test;

class MaskerTest {

    private final Masker masker = new Masker(new SuhLoggerProperties().getMasking().effectiveMaskFields(), "****");

    @Test
    void defaultsAreOnAndCaseInsensitive() {
        SuhLoggerProperties.MaskingConfig m = new SuhLoggerProperties().getMasking();
        assertTrue(m.isEnabled());
        assertTrue(masker.isSensitive("AccessToken"));
        assertTrue(masker.isSensitive("X-CSRF"));
        assertFalse(masker.isSensitive("email"), "PII는 프리셋으로만");
    }

    @Test
    void userFieldsAreAddedToDefaults() {
        SuhLoggerProperties.MaskingConfig m = new SuhLoggerProperties().getMasking();
        m.setMaskFields(List.of("ssn"));
        assertTrue(m.effectiveMaskFields().containsAll(List.of("password", "token", "ssn")));
        m.setUseDefaults(false);
        assertEquals(List.of("ssn"), m.effectiveMaskFields());
    }

    @Test
    void piiPresetAddsEmail() {
        SuhLoggerProperties.MaskingConfig m = new SuhLoggerProperties().getMasking();
        m.setPresets(List.of("pii"));
        assertTrue(new Masker(m.effectiveMaskFields(), "*").isSensitive("userEmail"));
    }

    @Test
    void nestedMapsAndListsAreMasked() {
        Object tree = Map.of("user", Map.of("name", "a", "password", "p"),
                "devices", List.of(Map.of("pushToken", "t1"), Map.of("pushToken", "t2")),
                "credentials", Map.of("key", "whole-subtree"));
        String out = String.valueOf(masker.mask(tree));
        assertFalse(out.contains("=p,") || out.contains("=p}"), out);
        assertFalse(out.contains("t1") || out.contains("t2") || out.contains("whole-subtree"), out);
        assertTrue(out.contains("name=a"), out);
    }

    @Test
    void regexFallbackMasksJsonWithoutCodec() {
        String json = "{\"a\":1,\"nested\":{\"refreshToken\":\"R\\\"x\",\"ok\":true},\"list\":[{\"password\":123}]}";
        String out = masker.maskByPattern(json);
        assertFalse(out.contains("R\\\"x") || out.contains("123"), out);
        assertTrue(out.contains("\"a\":1") && out.contains("\"ok\":true"), out);
    }

    @Test
    void regexFallbackMasksArrayAndObjectValues() {
        // 리뷰 Minor: codec 없을 때 배열 값이 원문으로 남던 문제
        String json = "{\"refreshTokens\":[\"abc\",\"def\"],\"secret\":{\"k\":\"v\"},\"ok\":[1,2],"
                + "\"nested\":{\"password\":\"p\",\"name\":\"n\"}}";
        String out = masker.maskByPattern(json);
        assertFalse(out.contains("abc") || out.contains("def") || out.contains("\"v\"") || out.contains("\"p\""), out);
        assertTrue(out.contains("\"ok\":[1,2]") && out.contains("\"name\":\"n\""), out);
        assertEquals("{\"refreshTokens\":\"****\",\"secret\":\"****\",\"ok\":[1,2],\"nested\":{\"password\":\"****\",\"name\":\"n\"}}", out);
    }

    @Test
    void regexFallbackLeavesNonJsonAlone() {
        assertEquals("plain text, no json", masker.maskByPattern("plain text, no json"));
    }
}
