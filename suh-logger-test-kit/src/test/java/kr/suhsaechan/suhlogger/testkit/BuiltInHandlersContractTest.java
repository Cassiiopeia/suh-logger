package kr.suhsaechan.suhlogger.testkit;

import java.util.Map;
import kr.suhsaechan.suhlogger.spi.HttpLogFormatter;
import kr.suhsaechan.suhlogger.spi.TypeHandler;

/** test-kit 계약 자체가 동작하는지 — 예시 구현으로 검증 */
class BuiltInHandlersContractTest {

    static class Money {
        final long amount;

        Money(long amount) {
            this.amount = amount;
        }
    }

    static class MoneyHandlerContractTest extends TypeHandlerContract {
        @Override
        protected TypeHandler handler() {
            return new TypeHandler() {
                public boolean supports(Object value) { return value instanceof Money; }
                public Object toSafe(Object value) { return Map.of("amount", ((Money) value).amount); }
            };
        }

        @Override
        protected Object supportedValue() {
            return new Money(10);
        }

        @Override
        protected Object unrelatedValue() {
            return "text";
        }
    }

    static class JsonLineFormatterContractTest extends HttpLogFormatterContract {
        @Override
        protected HttpLogFormatter formatter() {
            return r -> "{\"uri\":\"" + r.getUri() + "\",\"status\":" + r.getStatus() + ",\"body\":"
                    + (r.getBody() == null ? "null" : r.getBody().replaceAll("\\s*\\R\\s*", " ")) + "}";
        }
    }
}
