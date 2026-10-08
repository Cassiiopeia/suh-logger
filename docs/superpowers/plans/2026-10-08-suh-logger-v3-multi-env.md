# suh-logger v3 다중 환경 지원 (#58) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Boot 3.x·4.x, Servlet·WebFlux·non-web, Java·Kotlin, Boot 없는 Spring(XML), 순수 Java에서 suh-logger가 동작하고, 각 환경을 예제 앱 E2E로 검증한다.

**Architecture:** JSON 처리를 core `JsonCodec` SPI로 빼고 Jackson 2·3 구현 모듈을 둔다(앱 ObjectMapper/JsonMapper 우선). HTTP 응답 로그 출력 규칙을 core `HttpExchangeLogger`로 모아 Servlet 필터와 WebFlux WebFilter가 공유한다. 어노테이션을 클래스 레벨로도 쓸 수 있게 하고, 프록시가 걸리지 않는 final 대상은 기동 시 WARN을 남긴다. `examples/*`는 실제 소비자 앱으로 각 환경을 E2E 검증한다.

**Tech Stack:** Java 17, Spring Boot 3.4.4(컴파일 하한)·3.5.16·4.1.1(E2E), Jackson 2.18·3.2, Reactor 3.7, Kotlin 2.4.20, Gradle 8.13

**Spec:** `docs/superpowers/specs/2026-10-08-suh-logger-v3-architecture-design.md`

## Global Constraints

- #57 Global Constraints 전부 유지 (core는 slf4j만, 사용자 import 경로 유지, 하위 모듈에 `version =` 금지, `internal` 패키지).
- Jackson·Reactor·Kotlin은 라이브러리 모듈에서 `compileOnly`. starter는 json 모듈을 포함하되 Jackson 자체는 끌어오지 않는다.
- 프로퍼티 기본값은 바꾸지 않는다 (#55·#56).
- 예제 앱(`examples/*`)은 배포하지 않는다 (`maven-publish` 미적용).
- ServiceLoader로 찾는 provider 클래스는 Jackson 등 선택 의존 타입을 시그니처·필드에 두지 않는다 (없을 때 provider 로딩 자체가 깨진다).

## Review Focus

1. **Boot 4 앱에 Jackson 2가 없음**: pretty print·JSON 처리가 Jackson 3로 동작해야 하고 `NoClassDefFoundError`가 나면 안 된다 → Task 6 `servlet-boot4` E2E.
2. **Jackson이 전혀 없는 앱(non-web 배치)**: `JsonCodecs.get()`이 null이어도 로깅이 원문으로 동작해야 한다 → Task 1 `noCodecFallsBackToRaw`.
3. **응답 본문이 maxResponseBodySize보다 큰 WebFlux 응답**: 클라이언트에는 전체 본문이 가고 로그만 잘려야 한다 → Task 3 `largeBodyIsTruncatedInLogOnly`.
4. **클래스에 붙인 `@LogMonitor` + 메서드에 붙인 `@LogCall`**: 메서드 어노테이션 옵션이 우선해야 하고 로그가 두 번 찍히면 안 된다 → Task 4 `methodAnnotationOverridesClassAnnotation`.
5. **Kotlin final 메서드에 붙은 어노테이션**: 조용히 누락되지 않고 기동 시 WARN이 남아야 한다 → Task 4 `finalMethodIsWarned`.

---

### Task 1: JsonCodec SPI와 Jackson 2·3 모듈

**Files:**
- Create: `suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/spi/JsonCodec.java`, `spi/JsonCodecProvider.java`
- Create: `suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/internal/json/JsonCodecs.java`
- Create: `suh-logger-json-jackson2/` (`Jackson2JsonCodec`, `Jackson2JsonCodecProvider`, `META-INF/services/kr.suhsaechan.suhlogger.spi.JsonCodecProvider`)
- Create: `suh-logger-json-jackson3/` (`Jackson3JsonCodec`, `Jackson3JsonCodecProvider`, services 파일)
- Modify: `settings.gradle`, `gradle/libs.versions.toml`, `suh-logger-bom/build.gradle`, `suh-logger-spring-boot-starter/build.gradle`
- Test: `suh-logger-core/.../internal/json/JsonCodecsTest.java`, 각 json 모듈 `*JsonCodecTest.java`

**Interfaces:**
- Produces:
  - `public interface JsonCodec { Object parse(String json) throws Exception; String write(Object value, boolean pretty) throws Exception; }`
  - `public interface JsonCodecProvider { boolean isAvailable(); JsonCodec create(); default int order() { return 0; } }`
  - `public final class JsonCodecs { static JsonCodec get() /* null 가능 */; static void set(JsonCodec codec); static void reset(); static String prettyOrRaw(String body) }`
  - `Jackson2JsonCodec(com.fasterxml.jackson.databind.ObjectMapper)`, `Jackson3JsonCodec(tools.jackson.databind.ObjectMapper)`
  - provider order: jackson3 = 10, jackson2 = 20 (둘 다 있으면 Boot 4 기본인 3 우선)

- [ ] **Step 1: core 테스트** — `JsonCodecsTest`
  - `noCodecFallsBackToRaw`: `JsonCodecs.set(null)` 후(ServiceLoader에 provider 없음) `prettyOrRaw("{\"a\":1}")` == 원문
  - `explicitCodecWins`: 가짜 codec(`write` → `"PRETTY"`) set 후 `prettyOrRaw` == `"PRETTY"`
  - `codecFailureFallsBackToRaw`: `parse`가 예외를 던지는 codec이면 원문
- [ ] **Step 2: 실패 확인** — `./gradlew :suh-logger-core:test --tests '*JsonCodecsTest'` → 컴파일 실패
- [ ] **Step 3: 구현**
  - `JsonCodecs.get()`: 명시 set 값 → ServiceLoader `JsonCodecProvider`를 order 순으로 돌며 `isAvailable()`이 true인 첫 provider의 `create()` (iterator `next()`의 `ServiceConfigurationError`·`LinkageError`는 건너뜀) → 없으면 null. 결과 캐시.
  - `prettyOrRaw(body)`: codec null이면 body, 아니면 `write(parse(body), true)`, 예외면 body.
  - Jackson2 provider `isAvailable()`: `Class.forName("com.fasterxml.jackson.databind.ObjectMapper", false, loader)` 성공 여부. `create()`는 `new Jackson2JsonCodec(new ObjectMapper())` — provider 본문에서만 Jackson 타입을 쓴다.
  - Jackson3 provider: `tools.jackson.databind.json.JsonMapper` 확인, `create()`는 `new Jackson3JsonCodec(JsonMapper.builder().build())`.
- [ ] **Step 4: json 모듈 테스트** — 각 모듈에서 `write(parse("{\"a\":1}"), true)`가 줄바꿈을 포함하고, ServiceLoader로 `JsonCodecs.get()`이 자기 구현을 돌려주는지
- [ ] **Step 5: servlet 필터 교체** — `SuhLoggingFilter.formatResponseBody`가 `JsonCodecs.prettyOrRaw` 사용, `PrettyJson` holder와 servlet 모듈의 jackson2 `compileOnly` 제거. 기존 `prettyPrintUsesJacksonWhenPresent` 테스트는 servlet 테스트에 `testImplementation project(':suh-logger-json-jackson2')` 추가 후 그대로 통과해야 한다.
- [ ] **Step 6: 통과 확인** — `./gradlew :suh-logger-core:test :suh-logger-json-jackson2:test :suh-logger-json-jackson3:test :suh-logger-servlet:test`
- [ ] **Step 7: 커밋**

### Task 2: HTTP 응답 로그 규칙을 core로 모으기

**Files:**
- Create: `suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/internal/http/HttpExchangeLogger.java`
- Modify: `suh-logger-servlet/.../filter/SuhLoggingFilter.java` (`shouldExcludeFromLogging`·`logResponseSafely` 본문을 위임)
- Test: `suh-logger-core/.../internal/http/HttpExchangeLoggerTest.java`

**Interfaces:**
- Produces: `public final class HttpExchangeLogger { HttpExchangeLogger(SuhLoggerProperties p); boolean isExcluded(String uri); void logResponse(String method, String uri, int status, byte[] body); }`
  - 동작은 2.x 필터와 같다: 2xx이고 본문이 있을 때만, `RESPONSE LOGGING` 구분선 → `URI:` → `Method:` → `Status:` → `Response Body:`(pretty 옵션·크기 제한) → 구분선. `contains` 기반 제외.
- [ ] **Step 1: 테스트** — 2xx 로깅, 4xx 미로깅, 크기 초과 시 `[Too large to log`, 제외 패턴, 빈 본문 미로깅
- [ ] **Step 2~4: 구현·위임·통과** — 기존 `SuhLoggingFilterTest` 3개가 변경 없이 통과해야 한다
- [ ] **Step 5: 커밋**

### Task 3: WebFlux 모듈

**Files:**
- Create: `suh-logger-webflux/build.gradle`, `.../webflux/SuhReactiveLoggingWebFilter.java`
- Create: `suh-logger-spring-boot-autoconfigure/.../SuhLoggerReactiveAutoConfiguration.java` (imports 파일에 등록)
- Test: `suh-logger-webflux/.../SuhReactiveLoggingWebFilterTest.java`, autoconfigure `reactiveContextRegistersWebFilter`

**Interfaces:**
- Produces: `public class SuhReactiveLoggingWebFilter implements WebFilter, Ordered { SuhReactiveLoggingWebFilter(SuhLoggerProperties p) }`
  - 응답을 `ServerHttpResponseDecorator`로 감싸 `writeWith`의 DataBuffer를 클라이언트로 그대로 흘리면서 `maxResponseBodySize + 1` 바이트까지만 복사해 둔다. `doFinally`(완료·에러·취소)에서 `HttpExchangeLogger.logResponse` 호출.
  - order `Ordered.LOWEST_PRECEDENCE`.
- [ ] **Step 1: 테스트** (`WebTestClient.bindToWebHandler` 대신 `MockServerWebExchange` + 가짜 `WebFilterChain`)
  - `logsResponseBody`, `excludedPathSkipsLogging`, `largeBodyIsTruncatedInLogOnly`(클라이언트 본문은 전체, 로그는 `[Too large to log`)
- [ ] **Step 2~4: 구현·통과**
- [ ] **Step 5: 자동설정** — `@ConditionalOnWebApplication(type = REACTIVE)`, `@ConditionalOnClass(name = {"org.springframework.web.server.WebFilter", "kr.suhsaechan.suhlogger.webflux.SuhReactiveLoggingWebFilter"})`, `@ConditionalOnBean(SuhLoggerProperties.class)`, `after = SuhLoggerAutoConfiguration.class`. `ReactiveWebApplicationContextRunner` 테스트.
- [ ] **Step 6: 커밋**

### Task 4: 클래스 레벨 어노테이션, final 대상 WARN, Kotlin suspend 파라미터, TypeHandler 빈

**Files:**
- Modify: `annotation/{LogMonitor,LogCall,LogTime}.java` — `@Target({ElementType.METHOD, ElementType.TYPE})`
- Modify: 두 aspect — pointcut에 `@within(...)` 추가, 어노테이션 조회를 `AnnotationLookup.find(joinPoint, type)`(메서드 → 대상 클래스 순)로 통일, `kotlin.coroutines.Continuation` 타입 파라미터 제외
- Create: `suh-logger-spring/.../internal/spring/AnnotationLookup.java`, `.../spring/ProxyEligibilityChecker.java`
- Modify: `SuhLoggerConfiguration` — `ProxyEligibilityChecker` 빈, `ObjectProvider<TypeHandler>`를 `TypeHandlers.register`
- Test: `suh-logger-spring/.../aspect/ClassLevelAnnotationTest.java`, `.../spring/ProxyEligibilityCheckerTest.java`

**Interfaces:**
- Produces: `AnnotationLookup.find(ProceedingJoinPoint jp, Class<A> type): A`(null 가능), `ProxyEligibilityChecker implements SmartInitializingSingleton`
- 규칙: 한 메서드에 `@LogCall`(메서드)과 `@LogMonitor`(클래스)가 함께 걸리면 메서드 쪽 옵션을 쓰고, aspect는 한 번만 로그를 남긴다(`@Around` 하나가 두 pointcut을 OR로 받으므로 중복 없음 — 테스트로 고정).
- [ ] **Step 1: 테스트**
  - `classLevelMonitorLogsEveryPublicMethod`
  - `methodAnnotationOverridesClassAnnotation` (`@LogMonitor` 클래스 + `@LogCall(result=false)` 메서드 → RESULT 줄 없음, CALL 줄 1회)
  - `continuationParameterIsNotLogged` (마지막 파라미터 타입 이름이 `kotlin.coroutines.Continuation`인 가짜 인터페이스로 시뮬레이션 — 테스트 소스에 `package kotlin.coroutines; public interface Continuation<T> {}` 정의)
  - `finalMethodIsWarned` (open 클래스의 final `@LogMonitor` 메서드 → WARN에 클래스·메서드 이름)
  - `typeHandlerBeanIsRegistered`
- [ ] **Step 2~4: 구현·통과**
- [ ] **Step 5: 커밋**

### Task 5: Boot 없는 Spring(XML)·순수 Java 지원 확인

**Files:**
- Test: `suh-logger-spring/src/test/resources/suh-logger-context.xml`, `.../spring/XmlConfigurationTest.java`
- XML 내용: `<aop:aspectj-autoproxy/>` 없이 `<context:annotation-config/>` + `<bean class="kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration"/>` + 대상 빈 + `SuhLoggerProperties` 빈(프로퍼티 setter로 마스킹 설정)
- [ ] **Step 1: 테스트** — `ClassPathXmlApplicationContext`로 띄우고 마스킹·CALL 로그 확인
- [ ] **Step 2: 통과 확인 후 커밋**

### Task 6: 예제 앱 E2E

**Files:**
- Modify: `settings.gradle` (`include 'examples:servlet-boot3'` 등)
- Create: `examples/{servlet-boot3,servlet-boot4,webflux-boot3,batch-nonweb,kotlin-boot,spring-xml,plain-java}/build.gradle` + 앱 + 테스트

공통: 각 예제는 `implementation platform("org.springframework.boot:spring-boot-dependencies:<ver>")` + `implementation project(':suh-logger-spring-boot-starter')`(spring-xml·plain-java는 각각 `suh-logger-spring`·`suh-logger-core`). 테스트는 Boot 버전 간 위치가 바뀐 테스트 유틸(TestRestTemplate·LocalServerPort)을 피하고 `java.net.http.HttpClient` + `Environment.getProperty("local.server.port")`로 호출, 출력은 `OutputCaptureExtension`.

| 예제 | Boot | 검증 |
|---|---|---|
| servlet-boot3 | 3.5.16 | POST → `@LogMonitor` CALL·TIME, `RESPONSE LOGGING`, pretty print(Jackson 2) |
| servlet-boot4 | 4.1.1 | 위와 같음 + pretty print가 Jackson 3로 동작 |
| webflux-boot3 | 3.5.16 | GET → `RESPONSE LOGGING`, 클라이언트 본문 온전 |
| batch-nonweb | 3.5.16 | `spring.main.web-application-type=none`, `CommandLineRunner`의 `@LogMonitor` 서비스 호출 로그, 필터 빈 없음 |
| kotlin-boot | 3.5.16 | `kotlin-spring` 플러그인, `@Service` Kotlin 클래스의 `@LogMonitor` CALL 로그, data class 파라미터 |
| spring-xml | Spring 6.2 | XML 컨텍스트에서 aspect 동작 |
| plain-java | — | `SuhLogger.superLog`·`timeLog` 출력 |

- [ ] **Step 1~7: 예제별 작성·실행** — `./gradlew :examples:<name>:test`
- [ ] **Step 8: 전체** — `./gradlew build`
- [ ] **Step 9: 커밋**

## Self-Review 기록

- Spec §7 Kotlin suspend 실행 시간: Spring 6.1+ 코루틴 AOP 동작은 kotlin-boot 예제로 확인한다. 실행 시간 측정이 정확하지 않으면 README 지원표에서 suspend 실행 시간을 Tier2로 표기한다 (spec 합의 사항).
- Spec §8 WebFlux 한계: aspect의 헤더 로깅은 WebFlux에서 지원하지 않는다(`RequestContextAccessor.NONE`). `Mono`/`Flux` 반환 메서드의 `@LogTime`은 조립 시간만 잰다 — 문서에 명시(#60).
