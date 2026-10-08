package kr.suhsaechan.suhlogger.json.jackson3;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.suhsaechan.suhlogger.internal.json.JsonCodecs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Jackson3JsonCodecTest {

    @AfterEach
    void reset() {
        JsonCodecs.reset();
    }

    @Test
    void discoveredThroughServiceLoader() {
        assertInstanceOf(Jackson3JsonCodec.class, JsonCodecs.get());
    }

    @Test
    void prettyPrints() {
        String pretty = JsonCodecs.prettyOrRaw("{\"a\":1}");
        assertTrue(pretty.contains("\n"), pretty);
        assertTrue(pretty.contains("\"a\" : 1"), pretty);
    }
}
