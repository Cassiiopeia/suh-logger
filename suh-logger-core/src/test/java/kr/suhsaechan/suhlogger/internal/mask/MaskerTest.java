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
}
