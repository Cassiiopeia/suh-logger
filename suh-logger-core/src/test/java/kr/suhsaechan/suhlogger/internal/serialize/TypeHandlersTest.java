package kr.suhsaechan.suhlogger.internal.serialize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import kr.suhsaechan.suhlogger.spi.TypeHandler;
import kr.suhsaechan.suhlogger.util.CommonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TypeHandlersTest {

    @AfterEach
    void reset() {
        TypeHandlers.reset();
    }

    /** 클래스 이름에 MultipartFile이 들어간 타입은 Spring 없이도 메타데이터만 남긴다 */
    public static class FakeMultipartFile {
        public String getOriginalFilename() { return "a.txt"; }
        public String getContentType() { return "text/plain"; }
        public long getSize() { return 3L; }
        public boolean isEmpty() { return false; }
    }

    static class Money {
        final long amount;

        Money(long amount) {
            this.amount = amount;
        }
    }

    @Test
    void inputStreamBecomesTypeInfo() {
        Object safe = CommonUtil.makeSafeForSerialization(new ByteArrayInputStream(new byte[]{1}));
        assertEquals("InputStream", ((Map<?, ?>) safe).get("_type"));
    }

    @Test
    void multipartFileByNameBecomesMetadata() {
        Map<?, ?> map = (Map<?, ?>) CommonUtil.makeSafeForSerialization(new FakeMultipartFile());
        assertEquals("MultipartFile", map.get("_type"));
        assertEquals("a.txt", map.get("fileName"));
    }

    @Test
    void vectorIsHandled() {
        Object safe = CommonUtil.makeSafeForSerialization(new Vector<>(List.of(1, 2)));
        assertInstanceOf(Map.class, safe);
        assertEquals("Vector", ((Map<?, ?>) safe).get("_type"));
    }

    @Test
    void excludedClassWinsOverHandlers() {
        Map<?, ?> map = (Map<?, ?>) CommonUtil.makeSafeForSerialization(new FakeMultipartFile(),
                List.of(FakeMultipartFile.class.getName()));
        assertEquals("EXCLUDED_CLASS", map.get("_type"));
    }

    @Test
    void customHandlerIsApplied() {
        TypeHandlers.register(new TypeHandler() {
            public boolean supports(Object value) { return value instanceof Money; }
            public Object toSafe(Object value) { return Map.of("amount", ((Money) value).amount); }
        });
        Map<?, ?> map = (Map<?, ?>) CommonUtil.makeSafeForSerialization(new Money(500));
        assertEquals(500L, map.get("amount"));
    }

    @Test
    void throwingHandlerFallsBackToDefault() {
        TypeHandlers.register(new TypeHandler() {
            public boolean supports(Object value) { return value instanceof Money; }
            public Object toSafe(Object value) { throw new IllegalStateException("boom"); }
        });
        Money money = new Money(1);
        // 핸들러 실패가 로깅 대상 호출을 깨면 안 된다 — 원본 객체로 폴백
        assertSame(money, CommonUtil.makeSafeForSerialization(money));
    }
}
