# ADR 0002: Keep Jackson out of core

- Status: accepted (3.0.0)
- Issue: #58

## Context

Spring Boot 3 uses Jackson 2 (`com.fasterxml.jackson`). Spring Boot 4 uses Jackson 3 (`tools.jackson`). 2.x exported `jackson-databind` 2.18.3 as an `api` dependency, which forced a Jackson 2 version onto every consumer.

## Decision

- Core defines `JsonCodec` and `JsonCodecProvider`. `suh-logger-json-jackson2` and `-jackson3` implement them with `compileOnly` Jackson.
- In Boot, the codec wraps the application's own `ObjectMapper` (Boot 3) or `JsonMapper` (Boot 4), so date formats, `@JsonIgnore` and the Kotlin module apply to logs too.
- Without a codec, logging still works: bodies are printed raw, and masking falls back to pattern matching.

## Consequences

- No Jackson version conflict on either Boot line. Verified by the `servlet-boot4` example on Spring 7 / Jackson 3.
- ServiceLoader providers must not reference Jackson types in fields or signatures, or loading them fails when Jackson is absent.
