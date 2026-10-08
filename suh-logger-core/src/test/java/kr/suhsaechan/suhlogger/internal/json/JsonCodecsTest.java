package kr.suhsaechan.suhlogger.internal.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import kr.suhsaechan.suhlogger.spi.JsonCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class JsonCodecsTest {

    @AfterEach
    void reset() {
        JsonCodecs.reset();
    }

    @Test
    void noCodecFallsBackToRaw() {
        // core 단독 classpath에는 공급자가 없다
        assertNull(JsonCodecs.get());
        assertEquals("{\"a\":1}", JsonCodecs.prettyOrRaw("{\"a\":1}"));
    }

    @Test
    void explicitCodecWins() {
        JsonCodecs.set(new JsonCodec() {
            public Object parse(String json) { return json; }
            public String write(Object value, boolean pretty) { return "PRETTY"; }
        });
        assertEquals("PRETTY", JsonCodecs.prettyOrRaw("{}"));
    }

    @Test
    void codecFailureFallsBackToRaw() {
        JsonCodecs.set(new JsonCodec() {
            public Object parse(String json) throws Exception { throw new Exception("not json"); }
            public String write(Object value, boolean pretty) { return "never"; }
        });
        assertEquals("plain text", JsonCodecs.prettyOrRaw("plain text"));
    }
}
