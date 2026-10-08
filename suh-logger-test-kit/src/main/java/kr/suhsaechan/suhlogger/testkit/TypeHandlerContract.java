package kr.suhsaechan.suhlogger.testkit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.suhsaechan.suhlogger.spi.TypeHandler;
import org.junit.jupiter.api.Test;

/**
 * TypeHandler 구현이 지켜야 할 계약. 상속해서 세 메서드만 채우면 된다.
 * <pre>
 * class MoneyHandlerTest extends TypeHandlerContract {
 *     protected TypeHandler handler() { return new MoneyHandler(); }
 *     protected Object supportedValue() { return new Money(100); }
 *     protected Object unrelatedValue() { return "text"; }
 * }
 * </pre>
 */
public abstract class TypeHandlerContract {

    protected abstract TypeHandler handler();

    /** 이 핸들러가 처리해야 하는 값 */
    protected abstract Object supportedValue();

    /** 이 핸들러가 건드리면 안 되는 값 */
    protected abstract Object unrelatedValue();

    @Test
    void supportsItsOwnType() {
        assertTrue(handler().supports(supportedValue()));
    }

    @Test
    void ignoresUnrelatedTypes() {
        assertFalse(handler().supports(unrelatedValue()));
    }

    @Test
    void producesSafeReplacement() {
        Object safe = assertDoesNotThrow(() -> handler().toSafe(supportedValue()));
        assertNotNull(safe, "null을 돌려주면 기본 처리로 넘어가 원본이 노출될 수 있다");
        assertNotSame(supportedValue(), safe);
    }

    @Test
    void supportsDoesNotThrowOnAnyObject() {
        assertDoesNotThrow(() -> handler().supports(new Object()));
    }
}
