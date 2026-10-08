<div align="center">

# suh-logger

**Readable, masked logs for Spring — one dependency, one annotation.**

[![Compatibility](https://img.shields.io/github/actions/workflow/status/Cassiiopeia/suh-logger/SUH-LOGGER-COMPATIBILITY-MATRIX.yml?branch=develop&label=compatibility&style=flat-square)](https://github.com/Cassiiopeia/suh-logger/actions/workflows/SUH-LOGGER-COMPATIBILITY-MATRIX.yml)
[![Release](https://img.shields.io/github/v/release/Cassiiopeia/suh-logger?style=flat-square&color=blue)](https://github.com/Cassiiopeia/suh-logger/releases)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3%20%7C%204-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)](LICENSE)

**English** · [한국어](README.ko.md) · [Docs](docs/README.md) · [Examples](examples)

</div>

<br/>

```java
@LogMonitor
public LoginResponse login(LoginRequest request) { ... }
```

```text
POST /api/login -> 200 (12ms) rid=3f9a1c2e body={"username":"suh","accessToken":"****"}
```

Parameters, return values, timing and HTTP responses go to **your own SLF4J/Logback setup**. Tokens and passwords are masked **by default**, even inside nested DTOs and JSON bodies.

<br/>

## Features

- **One annotation** — put `@LogMonitor` on a method or a whole class. No configuration needed.
- **Safe by default** — passwords, tokens, secrets and auth headers become `****`, at any depth.
- **Production friendly** — one line per request, durations, WARN for slow and 5xx requests, `/actuator/**` skipped, request id in MDC.
- **Runs everywhere** — Spring Boot 3 and 4, WebFlux, Kotlin, Spring without Boot, plain Java. Each is tested end to end in CI.
- **Extensible** — plug in your own type handlers, log formats and JSON library.

<br/>

<!-- 수정하지마세요 자동으로 동기화 됩니다 -->
<!-- AUTO-VERSION-SECTION: DO NOT EDIT MANUALLY -->
## Current Version : v3.0.0 (2026-10-08)

```groovy
repositories {
    mavenCentral()
    maven { url "https://nexus.suhsaechan.kr/repository/maven-releases/" }
}

dependencies {
    implementation 'kr.suhsaechan:suh-logger-spring-boot-starter:x.x.x' // 최신 버전으로 변경하세요
}
```

<details>
<summary><b>Maven</b></summary>

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

</details>

Upgrading from 2.x? The old coordinate `kr.suhsaechan:suh-logger` still works. Read the [migration guide](docs/en/migration-3.0.md) first.

<br/>

## Usage

```java
@Service
@LogMonitor                       // every public method: parameters + result + time
public class OrderService {

    @LogCall(result = false)      // a method annotation overrides the class one
    public void cancel(Long orderId) { ... }

    @LogTime                      // timing only
    public Report build() { ... }
}
```

<details>
<summary><b>What the default (block) output looks like</b></summary>

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

For one line per request, set `suh-logger.format: line`.

</details>

<br/>

## Configuration

Everything is optional. A good production setup:

```yaml
suh-logger:
  format: line                 # one line per request
  response-body: error-only    # bodies only for 4xx/5xx
  slow-threshold-ms: 500       # slower requests → WARN
  request-id:
    enabled: true              # MDC requestId + X-Request-Id header
```

<details>
<summary><b>All options</b></summary>

```yaml
suh-logger:
  enabled: true
  format: block                  # block | line
  response-body: all             # none | error-only | all
  max-response-body-size: 4096
  pretty-print-json: false
  slow-threshold-ms: 0
  filter-order: 2147483647       # last, after Spring Security
  exclude-patterns: [ /actuator/** ]   # Ant patterns; setting this replaces the default
  request-id:
    enabled: false
    header: X-Request-Id
  masking:
    enabled: true                # default since 3.0
    use-defaults: true           # password, token, secret, authorization, cookie, csrf, api key …
    mask-fields: [ ssn ]         # added to the defaults
    presets: [ pii ]             # email, phone, address …
  header:
    enabled: false
```

Reference: [configuration](docs/en/configuration.md) · [masking](docs/en/masking.md)

</details>

<br/>

## Supported environments

| | Environment | Example |
|:-:|---|---|
| ✅ | Spring Boot 3 · Spring MVC | [`servlet-boot3`](examples/servlet-boot3) |
| ✅ | Spring Boot 4 · Spring 7 · Jackson 3 | [`servlet-boot4`](examples/servlet-boot4) |
| ✅ | Spring WebFlux | [`webflux-boot3`](examples/webflux-boot3) |
| ✅ | Batch / workers (no web) | [`batch-nonweb`](examples/batch-nonweb) |
| ✅ | Kotlin, including `suspend` | [`kotlin-boot`](examples/kotlin-boot) |
| ☑️ | Spring without Boot (XML / JavaConfig) | [`spring-xml`](examples/spring-xml) |
| ☑️ | Plain Java (`suh-logger-core`) | [`plain-java`](examples/plain-java) |
| 🗓️ | `javax` legacy (Boot 2 / Java 8) | planned |

✅ tested on every pull request (Java 17 and 21) · ☑️ documented with an example · 🗓️ planned

<details>
<summary><b>Known limits</b></summary>

- **Kotlin:** apply the `kotlin-spring` plugin. Without it, classes are final and cannot be proxied, and suh-logger warns at startup. Timing of `suspend` functions is approximate.
- **WebFlux:** method logs cannot include request headers. For methods that return `Mono`/`Flux`, `@LogTime` measures assembly time only.

</details>

<br/>

## Extending

Declare one of these as a bean and suh-logger picks it up.

| Extension point | Use it to |
|---|---|
| `TypeHandler` | log your own types safely |
| `HttpLogFormatter` | write HTTP logs in your own format (e.g. JSON lines) |
| `JsonCodec` | use a JSON library other than Jackson |
| `RequestContextAccessor` | support another web stack |

`suh-logger-test-kit` includes contract tests for your implementation. Guide: [docs/en/extending.md](docs/en/extending.md)

<details>
<summary><b>Modules</b></summary>

| Artifact | Contents |
|---|---|
| `suh-logger-spring-boot-starter` | what applications add |
| `suh-logger-core` | annotations, masking, SPI — depends only on `slf4j-api` |
| `suh-logger-spring` | AOP aspects, `SuhLoggerConfiguration` for Spring without Boot |
| `suh-logger-servlet` / `-webflux` | HTTP response logging |
| `suh-logger-json-jackson2` / `-jackson3` | JSON for Boot 3 / Boot 4 |
| `suh-logger-spring-boot-autoconfigure` | conditional auto-configuration |
| `suh-logger-bom` | version alignment |
| `suh-logger-test-kit` | `LogCapture` and contract tests |

</details>

<br/>

## Contributing

Issues and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md). AI coding agents should read [AGENTS.md](AGENTS.md). Report security issues as described in [SECURITY.md](SECURITY.md).

<div align="center">
<sub>MIT License · made by <a href="https://github.com/Cassiiopeia">@Cassiiopeia</a></sub>
</div>
