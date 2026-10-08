<div align="center">

# suh-logger

**Readable, masked logs of method calls and HTTP responses for Spring — one dependency, one annotation.**

<!-- 수정하지마세요 자동으로 동기화 됩니다 -->
<!-- AUTO-VERSION-SECTION: DO NOT EDIT MANUALLY -->
## Current Version : v2.0.4 (2026-07-26)

[![Compatibility matrix](https://github.com/Cassiiopeia/suh-logger/actions/workflows/SUH-LOGGER-COMPATIBILITY-MATRIX.yml/badge.svg)](https://github.com/Cassiiopeia/suh-logger/actions/workflows/SUH-LOGGER-COMPATIBILITY-MATRIX.yml)
[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x%20%7C%204.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

**English** · [한국어](README.ko.md)

</div>

```java
@LogMonitor
public LoginResponse login(LoginRequest request) { ... }
```

```text
================= [AuthService.login] CALL =================
====================== CALL PARAMETER ======================
============================================================
{
  "request": {
    "username": "suh",
    "password": "****"
  }
}
============================================================
================ [AuthService.login] RESULT ================
============================================================
{
  "username": "suh",
  "accessToken": "****",
  "refreshToken": "****"
}
============================================================
============= [TIME]: AuthService.login : 4 ms =============
```

Parameters, return values and timing are logged through **your own SLF4J/Logback setup**. Tokens and passwords are masked **by default**, including inside nested DTOs, records and JSON response bodies.

## Why suh-logger

| Without it | With suh-logger |
|---|---|
| `System.out.println` while debugging, removed later | `@LogMonitor` on the method or the class |
| DTOs printed as `LoginResponse@7ef27d7f` or with secrets via `toString()` | Field-level JSON tree, secrets replaced with `****` |
| Request logs split over many lines, mixed under concurrency | `suh-logger.format=line` → `POST /api/orders -> 201 (12ms) rid=...` |
| Health checks flooding logs | `/actuator/**` excluded by default |
| Different setup per stack | Same library on Boot 3, Boot 4, WebFlux, Kotlin, XML Spring and plain Java |

## Quick start

**Gradle**

```groovy
repositories {
    mavenCentral()
    // Until Maven Central publishing is live (see #59), artifacts are served from SUH Nexus
    maven { url "https://nexus.suhsaechan.kr/repository/maven-releases/" }
}

dependencies {
    implementation 'kr.suhsaechan:suh-logger-spring-boot-starter:x.x.x' // 최신 버전으로 변경하세요
}
```

**Maven**

```xml
<repositories>
    <repository>
        <id>suh-nexus</id>
        <url>https://nexus.suhsaechan.kr/repository/maven-releases/</url>
    </repository>
</repositories>

<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>suh-logger-spring-boot-starter</artifactId>
    <version>x.x.x</version> <!-- 최신 버전으로 변경하세요 -->
</dependency>
```

The 2.x coordinate `kr.suhsaechan:suh-logger` still works and pulls in the same starter.

**Use it**

```java
@Service
@LogMonitor                      // every public method: parameters + result + time
public class OrderService {

    @LogCall(result = false)     // method settings win over the class annotation
    public void cancel(Long orderId) { ... }

    @LogTime                     // timing only
    public Report build() { ... }
}
```

No configuration is required. HTTP responses are logged by a servlet filter or WebFlux `WebFilter` that is registered automatically for your web stack.

## Supported environments

Every Tier 1 row is an application under [`examples/`](examples) that CI starts and calls over HTTP on Java 17 and 21.

| Environment | Tier | Example |
|---|---|---|
| Spring Boot 3.x, servlet (Spring MVC) | 1 | [`servlet-boot3`](examples/servlet-boot3) |
| Spring Boot 4.x, servlet (Spring 7, Jackson 3) | 1 | [`servlet-boot4`](examples/servlet-boot4) |
| Spring Boot WebFlux | 1 | [`webflux-boot3`](examples/webflux-boot3) |
| Spring Boot without a web stack (batch, workers) | 1 | [`batch-nonweb`](examples/batch-nonweb) |
| Kotlin + Spring Boot (incl. `suspend` functions) | 1 | [`kotlin-boot`](examples/kotlin-boot) |
| Spring Framework 6 without Boot (XML or JavaConfig) | 2 | [`spring-xml`](examples/spring-xml) |
| Plain Java (no Spring), `suh-logger-core` only | 2 | [`plain-java`](examples/plain-java) |
| `javax.*` legacy (Spring 5 / Boot 2 / Java 8) | planned | — |

Known limits:

- **Kotlin:** classes are `final` by default. Use the `kotlin-spring` plugin, otherwise suh-logger warns at startup because proxies cannot be applied. For `suspend` functions, the time reported is not the full completion time.
- **WebFlux:** `@LogMonitor` cannot read request headers in reactive code, so header logging is servlet-only. For methods that return `Mono`/`Flux`, `@LogTime` measures assembly time.

## Configuration

All keys are optional. Full reference: [docs/en/configuration.md](docs/en/configuration.md).

```yaml
suh-logger:
  enabled: true
  format: block                  # block | line
  response-body: all             # none | error-only | all
  max-response-body-size: 4096
  pretty-print-json: false
  slow-threshold-ms: 0           # > 0: slower requests are logged at WARN (5xx are always WARN)
  filter-order: 2147483647       # last by default, after Spring Security
  exclude-patterns:
    - /actuator/**               # Ant patterns; replaces the default list when set
  request-id:
    enabled: false               # MDC "requestId" + X-Request-Id response header
  masking:
    enabled: true                # default since 3.0
    use-defaults: true           # password, token, secret, authorization, cookie, csrf, api key ...
    mask-fields: [ ssn ]         # added to the defaults
    presets: [ pii ]             # email, phone, address ...
  header:
    enabled: false
```

Recommended for production:

```yaml
suh-logger:
  format: line
  response-body: error-only
  slow-threshold-ms: 500
  request-id:
    enabled: true
```

## Masking

Masking is **on by default since 3.0**. A key matches when its name contains a sensitive word, ignoring case. The whole value is replaced, even when it is a nested object. Masking applies to:

- method parameters and return values, including fields inside DTOs, records, maps and lists
- HTTP response bodies (JSON), before pretty printing or custom formatting
- request headers (`Authorization`, `Cookie`, ...) when header logging is enabled

Turning masking off while bodies are logged prints a startup warning. Details: [docs/en/masking.md](docs/en/masking.md).

## Extending

Extension points live in `kr.suhsaechan.suhlogger.spi` and are marked `@Incubating` until they are stable:

| SPI | Use it to |
|---|---|
| `TypeHandler` | log your own types safely (e.g. `Money`, geometry, file handles) |
| `HttpLogFormatter` | emit HTTP logs in your own one-line format (e.g. JSON for a log shipper) |
| `JsonCodec` | plug in a JSON library other than Jackson |
| `RequestContextAccessor` | provide request information on a new web stack |

In Spring Boot, declaring a bean is enough. Contract tests for your implementation come with `kr.suhsaechan:suh-logger-test-kit`. Guide: [docs/en/extending.md](docs/en/extending.md).

## Modules

| Artifact | Contents |
|---|---|
| `suh-logger-spring-boot-starter` | what applications add (pulls in everything below that applies) |
| `suh-logger-core` | `SuhLogger`, annotations, masking, SPI — depends only on `slf4j-api` |
| `suh-logger-spring` | AOP aspects, `SuhLoggerConfiguration` for Spring without Boot |
| `suh-logger-servlet` / `suh-logger-webflux` | HTTP response logging per web stack |
| `suh-logger-json-jackson2` / `suh-logger-json-jackson3` | JSON support for Boot 3 / Boot 4 |
| `suh-logger-spring-boot-autoconfigure` | conditional auto-configuration |
| `suh-logger-bom` | version alignment |
| `suh-logger-test-kit` | `LogCapture` and contract tests for extension authors |

## Upgrading from 2.x

Code using 2.x keeps compiling: the coordinate, the annotation and utility packages, and the property keys are unchanged. The visible differences are safer defaults (masking on, `/actuator/**` excluded, 4xx/5xx responses logged) and the new location of the auto-configuration class. Read [docs/en/migration-3.0.md](docs/en/migration-3.0.md) before upgrading.

## Contributing

Issues and pull requests are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md); [AGENTS.md](AGENTS.md) has the module map and rules for AI coding agents. Report security issues privately as described in [SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE)
