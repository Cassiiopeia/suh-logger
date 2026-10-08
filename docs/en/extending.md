# Extending suh-logger

Extension points are interfaces in `kr.suhsaechan.suhlogger.spi`. They are marked `@Incubating` while the 3.x line settles: they may change in a minor release, which is announced in the release notes. Anything in a `*.internal.*` package is not an extension point.

| SPI | Called for | Register with Spring (Boot or not) | Register in plain Java |
|---|---|---|---|
| `TypeHandler` | each value before it is logged | a bean | `META-INF/services/kr.suhsaechan.suhlogger.spi.TypeHandler` |
| `HttpLogFormatter` | each HTTP response log | a bean | — (Spring only for now) |
| `JsonCodec` | parsing and writing JSON | a bean (replaces the Jackson one) | `META-INF/services/kr.suhsaechan.suhlogger.spi.JsonCodecProvider` |
| `RequestContextAccessor` | reading the current request in aspects | a bean | — (aspects need Spring) |

## Example: a type handler

```java
public class MoneyHandler implements TypeHandler {
    @Override public boolean supports(Object value) { return value instanceof Money; }
    @Override public Object toSafe(Object value) {
        Money m = (Money) value;
        return Map.of("amount", m.amount().toPlainString(), "currency", m.currency());
    }
    @Override public int order() { return 10; }   // before built-ins (100+)
}
```

```java
@Bean
TypeHandler moneyHandler() { return new MoneyHandler(); }
```

A handler that throws is skipped and logging continues. A failing handler never breaks the business call.

## Example: JSON lines for a log shipper

```java
@Bean
HttpLogFormatter jsonLines(ObjectMapper mapper) {
    return r -> mapper.writeValueAsString(Map.of(
            "method", r.getMethod(), "uri", r.getUri(), "status", r.getStatus(),
            "ms", r.getDurationMs(), "rid", String.valueOf(r.getRequestId())));
}
```

The body passed in `HttpExchangeRecord` is already masked and size-limited. The log level is still chosen by suh-logger: WARN for 5xx and slow requests.

## Testing your extension

```groovy
testImplementation 'kr.suhsaechan:suh-logger-test-kit:x.x.x' // 최신 버전으로 변경하세요
```

```java
class MoneyHandlerContractTest extends TypeHandlerContract {
    protected TypeHandler handler() { return new MoneyHandler(); }
    protected Object supportedValue() { return new Money(BigDecimal.TEN, "KRW"); }
    protected Object unrelatedValue() { return "text"; }
}
```

`HttpLogFormatterContract` works the same way. `LogCapture` collects what was logged:

```java
try (LogCapture capture = LogCapture.start()) {
    service.login(request);
    assertFalse(capture.text().contains("p@ss"));
}
```

## Adding a new environment

Adding support for another web stack (for example `javax.servlet`, planned) means adding one adapter module:

1. Measure the request, capture the response bytes, and call `HttpExchangeLogger.logResponse(...)`. Masking, exclusion, formatting and levels are shared.
2. Optionally provide a `RequestContextAccessor` so aspects can read headers.
3. Add a conditional auto-configuration in `suh-logger-spring-boot-autoconfigure`.
4. Add an `examples/<name>` application with an end-to-end test.
5. Keep framework types inside the adapter's package. `suh-logger-architecture-tests` enforces this.

Use `suh-logger-servlet` as the reference adapter.
