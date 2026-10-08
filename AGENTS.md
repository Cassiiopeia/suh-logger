# AGENTS.md

Guidance for AI coding agents working **on** this repository. If you are an agent helping someone **use** suh-logger in their application, read [`llms.txt`](llms.txt) instead.

## What this is

suh-logger is a logging library for Spring, Java 17+. `@LogMonitor`, `@LogCall` and `@LogTime` log method calls. A servlet filter or WebFlux `WebFilter` logs HTTP responses. Everything goes through SLF4J. Sensitive values are masked by default.

## Commands

```bash
./gradlew build                          # everything: unit tests, ArchUnit rules, examples E2E
./gradlew :suh-logger-core:test          # one module
./gradlew :examples:servlet-boot4:test   # one environment end to end
./gradlew build -PjavaToolchain=21       # run on another JDK (bytecode stays 17)
./gradlew apiCompat -PapiBaseline=3.0.0  # binary compatibility against a release
```

Run `./gradlew build` before saying a change is done. A change is not done while any example fails.

## Module map and dependency direction

```
suh-logger-core                 slf4j-api only. annotation/, config/, spi/, util/, internal/{mask,serialize,http,json}
  ↑
suh-logger-spring               AOP aspects (aspect/), SuhLoggerConfiguration (spring/), internal/spring
suh-logger-servlet              filter/SuhLoggingFilter, servlet/ServletRequestContextAccessor
suh-logger-webflux              webflux/SuhReactiveLoggingWebFilter
suh-logger-json-jackson2|3      JsonCodec implementations
  ↑
suh-logger-spring-boot-autoconfigure   boot/autoconfigure/* (conditional on classpath)
  ↑
suh-logger-spring-boot-starter  what users add;  suh-logger-legacy = 2.x coordinate "suh-logger"
suh-logger-test-kit             LogCapture + contract tests (published)
suh-logger-architecture-tests   ArchUnit rules (not published)
examples/*                      one consumer app per supported environment (not published)
```

Hard rules (`suh-logger-architecture-tests` fails the build when they are broken):

- core never imports `org.springframework`, `com.fasterxml`, `tools.jackson`, `jakarta`, `reactor`.
- `spi` depends only on `java.*` and `annotation`.
- Servlet types only in `filter`/`servlet`/`boot`. Reactor/WebFlux types only in `webflux`/`boot`. Jackson types only in `json`/`boot`. Spring Boot types only in `boot`.
- Aspects never reference web types directly. `ResponseEntity` handling lives in `internal/spring` behind a class-name check.

## Public API boundary

- **Public:** `annotation.*`, `config.SuhLoggerProperties` (+ nested config classes and enums), `spi.*`, `util.SuhLogger`, `util.SuhTimeUtil`, `util.CommonUtil`, `spring.SuhLoggerConfiguration`, the auto-configuration classes, and every `suh-logger.*` property key.
- **Do not break public API** without a major release. To evolve it, add an overload or a new type, and mark the old one `@Deprecated(since = "x.y.z", forRemoval = true)`.
- New extension points start as `@Incubating`.
- Put implementation details in `*.internal.*`.
- New properties need a default that keeps current behaviour, a Javadoc comment on the field (it becomes IDE help text), and rows in `docs/en/configuration.md` and `docs/ko/configuration.md`.

## Conventions

- Comments explain *why*, concisely. The existing code uses Korean comments; keep the language of the file you edit.
- Tests use `LogCapture` (`kr.suhsaechan.suhlogger.testkit`) and assert on log text. Do not just print it.
- A new environment needs an adapter module, a conditional auto-configuration, an `examples/<name>` app with an E2E test, and a row in the README support table.
- Never log a value before masking it. Masking happens before pretty printing and before any custom `HttpLogFormatter`.
- Commit format: `<issue title> : <type>[!] : <summary> <issue URL>`. `!` marks a breaking change and triggers a major release.
- Docs: `README.md` (English) is canonical. Mirror user-facing changes in `README.ko.md`. Write dependency versions as `x.x.x` with the comment `// 최신 버전으로 변경하세요`.

## Decisions already made

Read `docs/adr/` before proposing structural changes: framework-free core with adapters (0001), no Jackson in core (0002), masking on by default (0003), javax deferred (0004).
