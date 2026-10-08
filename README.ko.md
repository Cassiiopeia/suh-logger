<div align="center">

# suh-logger

**메서드 호출과 HTTP 응답을 읽기 좋게, 민감 정보는 가려서 — 의존성 하나, 어노테이션 하나.**

[![Compatibility matrix](https://github.com/Cassiiopeia/suh-logger/actions/workflows/SUH-LOGGER-COMPATIBILITY-MATRIX.yml/badge.svg)](https://github.com/Cassiiopeia/suh-logger/actions/workflows/SUH-LOGGER-COMPATIBILITY-MATRIX.yml)
[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x%20%7C%204.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

[English](README.md) · **한국어**

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

파라미터·반환값·실행 시간이 **프로젝트의 SLF4J/Logback 설정 그대로** 남습니다. 토큰과 비밀번호는 **기본으로 가려지며**, 중첩 DTO·record·JSON 응답 본문 안에 있어도 마찬가지입니다.

## 왜 suh-logger인가

| 직접 하면 | suh-logger |
|---|---|
| `System.out.println`으로 디버깅하고 나중에 지움 | 메서드나 클래스에 `@LogMonitor` |
| DTO가 `LoginResponse@7ef27d7f`로 찍히거나 `toString()`으로 토큰이 노출됨 | 필드 단위 JSON 트리, 민감 값은 `****` |
| 요청 로그가 여러 줄이라 동시 요청끼리 섞임 | `suh-logger.format=line` → `POST /api/orders -> 201 (12ms) rid=...` |
| 헬스체크가 로그를 채움 | `/actuator/**` 기본 제외 |
| 스택마다 설정이 다름 | Boot 3·Boot 4·WebFlux·Kotlin·XML Spring·순수 Java에서 같은 라이브러리 |

## 빠른 시작

**Gradle**

```groovy
repositories {
    mavenCentral()
    // Maven Central 배포 전까지는 SUH Nexus에서 받는다 (#59)
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

2.x 좌표 `kr.suhsaechan:suh-logger`도 그대로 동작하며 같은 starter를 가져옵니다.

**사용**

```java
@Service
@LogMonitor                      // 모든 public 메서드: 파라미터 + 결과 + 시간
public class OrderService {

    @LogCall(result = false)     // 메서드 설정이 클래스 어노테이션보다 우선
    public void cancel(Long orderId) { ... }

    @LogTime                     // 시간만
    public Report build() { ... }
}
```

설정은 필요 없습니다. HTTP 응답은 웹 스택에 맞춰 자동 등록되는 Servlet 필터 또는 WebFlux `WebFilter`가 남깁니다.

## 지원 환경

Tier 1은 [`examples/`](examples)의 실제 앱을 CI가 Java 17·21에서 띄우고 HTTP로 호출해 검증합니다.

| 환경 | 등급 | 예제 |
|---|---|---|
| Spring Boot 3.x Servlet(Spring MVC) | 1 | [`servlet-boot3`](examples/servlet-boot3) |
| Spring Boot 4.x Servlet(Spring 7·Jackson 3) | 1 | [`servlet-boot4`](examples/servlet-boot4) |
| Spring Boot WebFlux | 1 | [`webflux-boot3`](examples/webflux-boot3) |
| 웹 스택 없는 Spring Boot(배치·워커) | 1 | [`batch-nonweb`](examples/batch-nonweb) |
| Kotlin + Spring Boot(`suspend` 포함) | 1 | [`kotlin-boot`](examples/kotlin-boot) |
| Boot 없는 Spring Framework 6(XML·JavaConfig) | 2 | [`spring-xml`](examples/spring-xml) |
| 순수 Java(Spring 없음), `suh-logger-core`만 | 2 | [`plain-java`](examples/plain-java) |
| `javax.*` 레거시(Spring 5 / Boot 2 / Java 8) | 예정 | — |

알려진 한계:

- **Kotlin:** 클래스가 기본 `final`이라 `kotlin-spring` 플러그인이 필요합니다. 없으면 프록시를 걸 수 없어 기동 시 경고를 남깁니다. `suspend` 함수의 실행 시간은 실제 완료까지의 시간이 아닙니다.
- **WebFlux:** 리액티브 코드에서는 `@LogMonitor`가 요청 헤더를 읽을 수 없어 헤더 로깅은 Servlet 전용입니다. `Mono`/`Flux`를 반환하는 메서드의 `@LogTime`은 조립 시간을 잽니다.

## 설정

모든 키는 선택입니다. 전체 설명: [docs/ko/configuration.md](docs/ko/configuration.md)

```yaml
suh-logger:
  enabled: true
  format: block                  # block | line
  response-body: all             # none | error-only | all
  max-response-body-size: 4096
  pretty-print-json: false
  slow-threshold-ms: 0           # 0보다 크면 이보다 느린 요청을 WARN으로 (5xx는 항상 WARN)
  filter-order: 2147483647       # 기본은 가장 마지막 (Spring Security 이후)
  exclude-patterns:
    - /actuator/**               # Ant 패턴, 설정하면 기본 목록을 대체
  request-id:
    enabled: false               # MDC "requestId" + X-Request-Id 응답 헤더
  masking:
    enabled: true                # 3.0부터 기본값
    use-defaults: true           # password, token, secret, authorization, cookie, csrf, api key ...
    mask-fields: [ ssn ]         # 기본 목록에 추가
    presets: [ pii ]             # email, phone, address ...
  header:
    enabled: false
```

운영 권장:

```yaml
suh-logger:
  format: line
  response-body: error-only
  slow-threshold-ms: 500
  request-id:
    enabled: true
```

## 마스킹

**3.0부터 기본으로 켜져 있습니다.** 키 이름에 민감 단어가 포함되면(대소문자 무시) 값 전체를 가립니다. 값이 중첩 객체여도 통째로 가립니다. 적용 대상은 다음과 같습니다.

- 메서드 파라미터와 반환값 (DTO·record·Map·List 안의 필드까지)
- HTTP 응답 본문(JSON) — pretty print·사용자 포매터보다 먼저 적용
- 요청 헤더(`Authorization`, `Cookie` 등) — 헤더 로깅을 켰을 때

본문을 남기면서 마스킹을 끄면 기동 시 경고가 남습니다. 자세히: [docs/ko/masking.md](docs/ko/masking.md)

## 확장

확장 지점은 `kr.suhsaechan.suhlogger.spi`에 있으며 안정화 전까지 `@Incubating`으로 표시됩니다.

| SPI | 용도 |
|---|---|
| `TypeHandler` | 내 도메인 타입을 안전하게 로깅 (예: `Money`, 지오메트리, 파일 핸들) |
| `HttpLogFormatter` | HTTP 로그를 원하는 한 줄 형식으로 (예: 로그 수집기용 JSON) |
| `JsonCodec` | Jackson 외 JSON 라이브러리 연결 |
| `RequestContextAccessor` | 새 웹 스택에서 요청 정보 제공 |

Spring Boot에서는 빈으로 등록하면 끝입니다. 구현 검증용 계약 테스트는 `kr.suhsaechan:suh-logger-test-kit`에 있습니다. 가이드: [docs/en/extending.md](docs/en/extending.md)

## 모듈

| 아티팩트 | 내용 |
|---|---|
| `suh-logger-spring-boot-starter` | 앱이 추가하는 것 (아래에서 필요한 것을 모두 가져옴) |
| `suh-logger-core` | `SuhLogger`, 어노테이션, 마스킹, SPI — `slf4j-api`에만 의존 |
| `suh-logger-spring` | AOP aspect, Boot 없는 Spring용 `SuhLoggerConfiguration` |
| `suh-logger-servlet` / `suh-logger-webflux` | 웹 스택별 HTTP 응답 로깅 |
| `suh-logger-json-jackson2` / `suh-logger-json-jackson3` | Boot 3 / Boot 4 JSON 지원 |
| `suh-logger-spring-boot-autoconfigure` | 조건부 자동설정 |
| `suh-logger-bom` | 버전 정렬 |
| `suh-logger-test-kit` | 확장 구현자용 `LogCapture`와 계약 테스트 |

## 2.x에서 올리기

2.x 코드는 그대로 컴파일됩니다. 좌표, 어노테이션·유틸 패키지, 프로퍼티 키가 같습니다. 눈에 띄는 차이는 더 안전해진 기본값(마스킹 켜짐, `/actuator/**` 제외, 4xx·5xx 응답 기록)과 자동설정 클래스 위치입니다. 올리기 전에 [docs/ko/migration-3.0.md](docs/ko/migration-3.0.md)를 읽어 주세요.

## 기여

이슈와 PR을 환영합니다. [CONTRIBUTING.md](CONTRIBUTING.md)부터 보세요. AI 코딩 에이전트용 모듈 지도와 규칙은 [AGENTS.md](AGENTS.md)에 있습니다. 보안 문제는 [SECURITY.md](SECURITY.md) 절차대로 비공개로 알려 주세요.

## 라이선스

[MIT](LICENSE)
