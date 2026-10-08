<div align="center">

# suh-logger

**Spring 로그를 읽기 좋게, 민감 정보는 가려서 — 의존성 하나, 어노테이션 하나.**

[![Compatibility](https://img.shields.io/github/actions/workflow/status/Cassiiopeia/suh-logger/SUH-LOGGER-COMPATIBILITY-MATRIX.yml?branch=develop&label=compatibility&style=flat-square)](https://github.com/Cassiiopeia/suh-logger/actions/workflows/SUH-LOGGER-COMPATIBILITY-MATRIX.yml)
[![Release](https://img.shields.io/github/v/release/Cassiiopeia/suh-logger?style=flat-square&color=blue)](https://github.com/Cassiiopeia/suh-logger/releases)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3%20%7C%204-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)](LICENSE)

[English](README.md) · **한국어** · [문서](docs/README.md) · [예제](examples)

</div>

<br/>

```java
@LogMonitor
public LoginResponse login(LoginRequest request) { ... }
```

```text
POST /api/login -> 200 (12ms) rid=3f9a1c2e body={"username":"suh","accessToken":"****"}
```

파라미터·반환값·실행 시간·HTTP 응답이 **프로젝트의 SLF4J/Logback 설정 그대로** 남습니다. 토큰과 비밀번호는 **기본으로 가려지며**, 중첩 DTO나 JSON 본문 안에 있어도 마찬가지입니다.

<br/>

## 특징

- **어노테이션 하나** — 메서드나 클래스에 `@LogMonitor`만 붙이면 됩니다. 설정이 필요 없습니다.
- **기본이 안전** — 비밀번호·토큰·시크릿·인증 헤더는 어느 깊이에 있든 `****`로 가려집니다.
- **운영 친화** — 요청당 한 줄 로그, 처리 시간, 느린 요청·5xx는 WARN, `/actuator/**` 제외, MDC 요청 ID를 지원합니다.
- **어디서나** — Spring Boot 3·4, WebFlux, Kotlin, Boot 없는 Spring, 순수 Java에서 동작하며, 환경마다 CI에서 E2E로 검증합니다.
- **확장 가능** — 타입 처리, 로그 형식, JSON 라이브러리를 직접 바꿀 수 있습니다.

<br/>

## 설치

최신 버전은 [Releases](https://github.com/Cassiiopeia/suh-logger/releases)에서 확인하세요.

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

2.x에서 올리시나요? 기존 좌표 `kr.suhsaechan:suh-logger`도 그대로 동작합니다. 먼저 [마이그레이션 가이드](docs/ko/migration-3.0.md)를 읽어 주세요.

<br/>

## 사용

```java
@Service
@LogMonitor                       // 모든 public 메서드: 파라미터 + 결과 + 시간
public class OrderService {

    @LogCall(result = false)      // 메서드 설정이 클래스 설정보다 우선
    public void cancel(Long orderId) { ... }

    @LogTime                      // 시간만
    public Report build() { ... }
}
```

<details>
<summary><b>기본(block) 출력 예시</b></summary>

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

요청당 한 줄로 보려면 `suh-logger.format: line`으로 설정하세요.

</details>

<br/>

## 설정

모두 선택입니다. 운영 권장 설정:

```yaml
suh-logger:
  format: line                 # 요청당 한 줄
  response-body: error-only    # 4xx·5xx만 본문 기록
  slow-threshold-ms: 500       # 이보다 느리면 WARN
  request-id:
    enabled: true              # MDC requestId + X-Request-Id 헤더
```

<details>
<summary><b>전체 옵션</b></summary>

```yaml
suh-logger:
  enabled: true
  format: block                  # block | line
  response-body: all             # none | error-only | all
  max-response-body-size: 4096
  pretty-print-json: false
  slow-threshold-ms: 0
  filter-order: 2147483647       # 가장 마지막 (Spring Security 이후)
  exclude-patterns: [ /actuator/** ]   # Ant 패턴, 지정하면 기본값을 대체
  request-id:
    enabled: false
    header: X-Request-Id
  masking:
    enabled: true                # 3.0부터 기본값
    use-defaults: true           # password, token, secret, authorization, cookie, csrf, api key …
    mask-fields: [ ssn ]         # 기본 목록에 추가
    presets: [ pii ]             # email, phone, address …
  header:
    enabled: false
```

자세히: [설정](docs/ko/configuration.md) · [마스킹](docs/ko/masking.md)

</details>

<br/>

## 지원 환경

| | 환경 | 예제 |
|:-:|---|---|
| ✅ | Spring Boot 3 · Spring MVC | [`servlet-boot3`](examples/servlet-boot3) |
| ✅ | Spring Boot 4 · Spring 7 · Jackson 3 | [`servlet-boot4`](examples/servlet-boot4) |
| ✅ | Spring WebFlux | [`webflux-boot3`](examples/webflux-boot3) |
| ✅ | 배치·워커 (웹 없음) | [`batch-nonweb`](examples/batch-nonweb) |
| ✅ | Kotlin (`suspend` 포함) | [`kotlin-boot`](examples/kotlin-boot) |
| ☑️ | Boot 없는 Spring (XML·JavaConfig) | [`spring-xml`](examples/spring-xml) |
| ☑️ | 순수 Java (`suh-logger-core`) | [`plain-java`](examples/plain-java) |
| 🗓️ | `javax` 레거시 (Boot 2 / Java 8) | 예정 |

✅ 매 PR마다 검증 (Java 17·21) · ☑️ 예제와 문서 제공 · 🗓️ 예정

<details>
<summary><b>알려진 한계</b></summary>

- **Kotlin:** `kotlin-spring` 플러그인을 적용하세요. 없으면 클래스가 final이라 프록시를 걸 수 없고, 기동 시 경고가 남습니다. `suspend` 함수의 실행 시간은 근사값입니다.
- **WebFlux:** 메서드 로그에 요청 헤더를 넣을 수 없습니다. `Mono`/`Flux`를 반환하는 메서드의 `@LogTime`은 조립 시간만 잽니다.

</details>

<br/>

## 확장

아래 중 하나를 빈으로 등록하면 적용됩니다.

| 확장 지점 | 용도 |
|---|---|
| `TypeHandler` | 내 타입을 안전하게 로깅 |
| `HttpLogFormatter` | HTTP 로그를 원하는 형식으로 (예: JSON lines) |
| `JsonCodec` | Jackson 외 JSON 라이브러리 사용 |
| `RequestContextAccessor` | 다른 웹 스택 지원 |

`suh-logger-test-kit`에 구현 검증용 계약 테스트가 들어 있습니다. 가이드: [docs/en/extending.md](docs/en/extending.md)

<details>
<summary><b>모듈</b></summary>

| 아티팩트 | 내용 |
|---|---|
| `suh-logger-spring-boot-starter` | 앱이 추가하는 것 |
| `suh-logger-core` | 어노테이션, 마스킹, SPI — `slf4j-api`에만 의존 |
| `suh-logger-spring` | AOP aspect, Boot 없는 Spring용 `SuhLoggerConfiguration` |
| `suh-logger-servlet` / `-webflux` | HTTP 응답 로깅 |
| `suh-logger-json-jackson2` / `-jackson3` | Boot 3 / Boot 4 JSON |
| `suh-logger-spring-boot-autoconfigure` | 조건부 자동설정 |
| `suh-logger-bom` | 버전 정렬 |
| `suh-logger-test-kit` | `LogCapture`와 계약 테스트 |

</details>

<br/>

## 기여

이슈와 PR을 환영합니다 — [CONTRIBUTING.md](CONTRIBUTING.md)를 보세요. AI 코딩 에이전트는 [AGENTS.md](AGENTS.md)를 읽어 주세요. 보안 문제는 [SECURITY.md](SECURITY.md) 절차대로 알려 주세요.

<div align="center">
<sub>MIT License · made by <a href="https://github.com/Cassiiopeia">@Cassiiopeia</a></sub>
</div>
