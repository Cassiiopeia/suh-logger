# suh-logger v3 아키텍처 설계 — 오픈소스 확장 구조

- 작성일: 2026-10-08
- 대상 버전: 3.0.0 (major — 모듈 분리·마스킹 기본값 변경)
- 관련 이슈: #55(마스킹 기본값), #56(요청 로그 운영 추적성), #22·#20·#10(직렬화·마스킹 잔여)

## 1. 목표와 합의 사항

### 목표 (사용자 합의)
실사용자 확보·노출·기여자 모집·포트폴리오를 모두 만족하는 오픈소스 라이브러리로 재구성한다.
순서는 의존 관계로 정한다: 구조 → 기능(#55·#56) → 강제 장치·배포 → 문서·커뮤니티.

### 지원 범위
| 등급 | 대상 | 보장 방식 |
|---|---|---|
| Tier1 | Spring Boot 3.x(최신 2 minor)·4.x × Java 17/21 × (Servlet · WebFlux · non-web) × (Java · Kotlin) | CI 매트릭스로 매 PR 검증 |
| Tier2 | Boot 없는 Spring Framework 6 (XML·JavaConfig), 순수 Java(core만) | 문서와 예제 앱 |
| 2차 | javax 레거시(Spring 5 / Boot 2 / Java 8) | 이번 범위 밖. 어댑터 모듈 추가만으로 붙도록 구조를 남긴다 |
| 미확인 | GraalVM native | 구현 중 확인 전까지 보장하지 않는다 |

### 배포·문서
- Maven Central + 기존 Nexus 병행 배포
- 문서는 영문 기본(`README.md`) + 한글(`README.ko.md`)

## 2. 모듈 구성

```
suh-logger-core                       Java 17, 외부 의존: slf4j-api(compileOnly)
suh-logger-spring                     core + spring-context/aop (AOP 어노테이션 처리)
suh-logger-servlet                    core + jakarta.servlet + spring-web (요청/응답 필터)
suh-logger-webflux                    core + spring-webflux (WebFilter)
suh-logger-json-jackson2              JsonCodec 구현 (com.fasterxml.jackson)
suh-logger-json-jackson3              JsonCodec 구현 (tools.jackson)
suh-logger-spring-boot-autoconfigure  classpath 조건부 자동설정 (Boot 3/4)
suh-logger-spring-boot-starter        사용자가 추가하는 단일 의존성
suh-logger                            기존 좌표 — starter에 위임 (2.x 하위호환)
suh-logger-bom                        모듈 버전 정렬
suh-logger-test-kit                   확장 구현용 적합성 테스트
```

### 의존 방향 (한 방향만 허용)
`core ← spring ← servlet / webflux ← autoconfigure ← starter`
`core ← json-jackson2 / json-jackson3 ← autoconfigure`

- core는 Spring·Jackson·Servlet을 import하지 않는다.
- 외부 프레임워크 의존은 모두 `compileOnly` 또는 `optional`이다. 사용자 버전을 강제하지 않는다.
- 모듈 간 패키지는 겹치지 않는다(split package 금지). 단 사용자가 import하는 `annotation`·`util` 패키지 경로는 2.x와 같게 core에 둔다.

### 삭제 대상 (라이브러리 위생)
- `SuhLoggerApplication`(@SpringBootApplication) — 라이브러리 jar에 앱 진입점이 있으면 안 된다
- jar 내부 `application.properties` — 사용자 설정과 충돌한다
- 자동설정의 `@ComponentScan` — 명시적 `@Bean`만 둔다
- `META-INF/spring.factories` — Boot 3+는 `AutoConfiguration.imports`만 읽는다

## 3. 내부 처리 파이프라인

```
[수집 어댑터]                       [core 파이프라인]
@LogMonitor/@LogCall aspect ─┐
Servlet filter              ─┼→ LogEvent → Enricher* → Masker → Formatter → SLF4J
WebFlux WebFilter           ─┘
```

- 어댑터는 `LogEvent`(불변 값 객체)를 만드는 일까지만 한다. 이후 처리는 core가 공유한다.
- `LogEvent` 종류: `MethodInvocationEvent`(파라미터·반환값·예외·실행시간), `HttpExchangeEvent`(method·uri·status·처리시간·헤더·본문).
- `Enricher`: 요청 ID(MDC `requestId`), 처리 시간, 느린 요청 표시.
- `Masker`: 필드·헤더 마스킹. JSON 본문은 `JsonCodec`으로 트리를 만들어 중첩·배열까지 마스킹한다.
- `Formatter`: `block`(2.x 출력, 기본) / `line`(요청당 한 줄, #56).
- 새 환경은 어댑터 하나만 추가한다. 2차 javax도 같은 방식으로 붙는다.

## 4. 확장 지점 (SPI)

| SPI | 역할 | 기본 구현 |
|---|---|---|
| `TypeHandler` | 직렬화가 위험한 타입을 안전한 맵으로 변환 | MultipartFile·File·Vector·JTS Geometry·InputStream |
| `MaskingRule` | 마스킹 대상 판단 | 필드명(대소문자 무시) 목록 |
| `LogFormatter` | 이벤트를 문자열로 변환 | `BlockFormatter`, `LineFormatter` |
| `ExclusionMatcher` | 요청 경로 제외 판단 | Ant 패턴 매처 (contains는 deprecated 유지) |
| `JsonCodec` | JSON 파싱·직렬화·pretty print | Jackson2, Jackson3 |
| `RequestContextAccessor` | 현재 요청 헤더 조회 (aspect용) | Servlet 구현, 없으면 빈 구현 |

- Boot: 사용자 `@Bean`이 기본 구현을 대체한다(`@ConditionalOnMissingBean`). `TypeHandler`·`MaskingRule`은 여러 개를 모아 쓴다.
- XML·순수 Java: `SuhLoggerProperties`와 `ServiceLoader`로 주입한다.

## 5. 공개 API와 호환성 정책

라이브러리 업계에서 흔한 방식을 따른다 (Netty·OkHttp의 `internal` 패키지, Spring의 deprecation, Gradle·Micrometer의 `@Incubating`).

1. **공개 API**: `annotation.*`, `util.SuhLogger`, `util.SuhTimeUtil`, `spi.*`, `config.SuhLoggerProperties`, 프로퍼티 키.
2. **비공개**: `kr.suhsaechan.suhlogger.internal.*` — 이름으로 표시하고 보장하지 않는다.
3. **`@Incubating`**: 의존 없는 자체 어노테이션. 새 SPI에만 붙이고 minor에서 바뀔 수 있다.
4. **deprecation**: `@Deprecated(since, forRemoval = true)`로 minor에서 표시하고 다음 major에서만 제거한다.
   - 프로퍼티 이름 변경 시 옛 키도 읽고 기동 시 WARN을 남긴다. 설정 메타데이터에 대체 키를 기록한다.
5. **기계적 강제**
   - japicmp: 직전 릴리스와 바이너리 호환 비교. `internal`·`@Incubating` 제외. 깨지면 빌드 실패 → `feat!` 커밋 필요 → `semver_auto`가 major로 올린다.
   - ArchUnit: core의 Spring import, 모듈 간 `internal` 교차 참조를 금지한다.
6. **2.x 사용자 보장**: `kr.suhsaechan:suh-logger` 좌표, import 경로, 프로퍼티 키가 그대로 동작한다. `CommonUtil` 공개 static 메서드는 새 구현에 위임하는 deprecated 메서드로 남긴다.

## 6. 설정 모델

- 단일 설정 객체는 기존 `kr.suhsaechan.suhlogger.config.SuhLoggerProperties`(순수 JavaBean)를 core에 그대로 둔다.
  - 클래스에 붙어 있던 `@ConfigurationProperties`를 떼고, 자동설정의 `@Bean` 메서드에 `@ConfigurationProperties("suh-logger")`를 붙여 바인딩한다. core가 Boot에 의존하지 않으면서 2.x 사용자 코드(`SuhLoggerProperties` 주입, `SuhLogger.setProperties`)가 그대로 동작한다.
  - 별도 `SuhLoggerSettings` 클래스는 만들지 않는다 (같은 역할의 클래스가 둘이 되면 기여자가 어느 쪽을 고칠지 헷갈린다).
- Boot: `suh-logger.*` → 자동설정이 `SuhLoggerProperties` 빈으로 바인딩.
- Spring(XML·JavaConfig): `SuhLoggerConfiguration`을 import하거나 `<bean>` 등록. `SuhLoggerProperties` 빈이 있으면 그것을, 없으면 기본값을 쓴다.
- 순수 Java: `SuhLogger.setProperties(properties)`. 호출하지 않으면 기본값.
- 자동설정 클래스는 `kr.suhsaechan.suhlogger.boot.autoconfigure.SuhLoggerAutoConfiguration`으로 옮긴다 (core의 `config` 패키지와 겹치지 않게). 옛 FQN으로 exclude하던 사용자는 마이그레이션 가이드로 안내한다.

### 신규·변경 프로퍼티
| 키 | 기본값 | 비고 |
|---|---|---|
| `suh-logger.masking.enabled` | `true` | **변경(#55)**. 2.x는 `false` |
| `suh-logger.masking.mask-fields` | 기본 민감 필드 목록 | 사용자 값은 기본 목록에 **추가**된다. 기본 목록 제거는 `masking.use-defaults=false` |
| `suh-logger.masking.use-defaults` | `true` | 신규 |
| `suh-logger.format` | `block` | 신규(#56). `block` \| `line` |
| `suh-logger.response-body` | `all` | 신규(#55). `none` \| `error-only` \| `all` |
| `suh-logger.slow-threshold-ms` | `0`(끔) | 신규(#56). 넘거나 5xx면 WARN |
| `suh-logger.exclude-patterns` | `/actuator/**` | **변경(#56)**. Ant 패턴. 패턴 문자(`*`,`?`)가 없는 2.x 값은 contains로 해석하고 WARN |
| `suh-logger.filter-order` | `Ordered.LOWEST_PRECEDENCE` | 신규(#56) |
| `suh-logger.request-id.enabled` | `false` | 신규(#56). MDC `requestId` + `X-Request-Id` 응답 헤더 |

기본 민감 필드: `password`, `passwd`, `pwd`, `secret`, `token`, `accessToken`, `refreshToken`, `idToken`, `authorization`, `apiKey`, `csrf`, `cookie`, `set-cookie`.
개인정보(`email`, `phone`)는 디버깅 용도와 충돌해 기본 목록에서 빼고 `masking.presets=pii`로 켠다.
마스킹이 꺼진 상태에서 본문 로깅이 켜져 있으면 기동 시 WARN을 한 번 남긴다.

## 7. Kotlin

- 어노테이션 `@Target`에 `TYPE` 추가 — 클래스에 붙이면 공개 메서드 전체에 적용.
- 기동 시 어노테이션이 붙은 `final` 클래스·메서드를 감지하면 WARN ("프록시 불가 — kotlin-spring 플러그인 확인").
- `suspend` 함수: Spring 6.1+ 코루틴 AOP 위에서 실행 시간을 완료 시점 기준으로 재고 `Continuation` 파라미터는 로그에서 제외한다. 테스트로 확인되지 않으면 Tier2로 내린다.
- JSON은 앱 `ObjectMapper` 빈을 재사용한다 → `jackson-module-kotlin`이 반영된다.

## 8. WebFlux 한계

aspect의 헤더 로깅은 Reactor Context가 필요해 `Mono`/`Flux`를 반환하는 메서드에서만 지원한다. 그 외 메서드는 헤더 로깅을 건너뛴다. 요청·응답 로깅은 `WebFilter`가 전부 처리한다.

## 9. 빌드·CI·배포

- Gradle 멀티모듈, `gradle/libs.versions.toml`, `build-logic/` convention plugin.
- 컴파일은 지원 하한 버전 API, 테스트는 상한 버전까지.
- CI 매트릭스: Boot 3.x·4.x × Java 17/21 × examples 통합 테스트 + japicmp + ArchUnit.
- 배포: `com.vanniktech.maven.publish`로 Maven Central(GPG 서명) + Nexus(HTTPS 강제, `allowInsecureProtocol` 제거). projectops 릴리스 흐름(`develop → main`, `semver_auto`)에 연결.
- Dependabot으로 의존성 업데이트, 매트릭스 통과 시 머지.

### E2E 테스트 (모의 소비자 앱)
- `examples/*` 각 앱이 E2E 대상이다. 라이브러리를 실제 의존성으로 붙인 소비자 앱을 띄운다.
- Boot 앱: `@SpringBootTest(webEnvironment = RANDOM_PORT)`로 실제 서버를 띄우고 HTTP 요청을 보낸 뒤 `OutputCaptureExtension`으로 SLF4J 출력을 검증한다.
- 시나리오: 로그인 응답 토큰 마스킹(#55), `/actuator/health` 제외·한 줄 형식·처리 시간·`X-Request-Id`(#56), `@LogMonitor` 파라미터·반환값, 직렬화 위험 타입, 예외 경로.
- 2.x 호환 시나리오: 기존 좌표·import·프로퍼티만 쓴 앱이 그대로 동작하는지.
- non-web·plain-java·spring-xml은 `main` 실행 또는 컨텍스트 기동 후 출력 검증.

## 10. 문서·커뮤니티·AI agent

- `README.md`(영문) + `README.ko.md`: 첫 화면에 한 줄 가치·설치 3줄·출력 예시.
- `docs/`: 시작하기, 환경별 설정(Boot·WebFlux·Kotlin·XML·plain), 확장 가이드(`docs/extending/`), 마이그레이션 2.x→3.0, ADR(`docs/adr/`), 지원 정책.
- `CONTRIBUTING.md`(실제 빌드·테스트 명령·모듈 지도), `SECURITY.md`(신고 채널은 메인테이너 확인 후 기입), `CODE_OF_CONDUCT.md`.
- `AGENTS.md`(모듈 지도·의존 규칙·공개 API 경계·명령), `llms.txt`(사용자 agent용 요약).
- `examples/`: `servlet-boot3`, `servlet-boot4`, `webflux`, `batch-nonweb`, `kotlin-boot`, `spring-xml`, `plain-java`.
- GitHub 메타: topics, About, homepage(문서), LICENSE 인식, Release 생성. 한글 상태 라벨은 projectops 보드 동기화용으로 유지하고 `module: *` 라벨을 추가한다.

## 11. 하위 프로젝트와 완료 기준

| # | 범위 | 완료 기준 |
|---|---|---|
| 1 | 멀티모듈 골격, core 분리, servlet 모듈 이전, TypeHandler·RequestContextAccessor SPI, 하위호환 위임 | 기존 테스트 전부 통과, 2.x import·프로퍼티로 작성한 테스트 통과 |
| 2 | servlet·webflux·non-web 어댑터, Jackson 2/3, Boot 3/4, Kotlin | 각 환경 통합 테스트 통과 |
| 3 | #55·#56 (LogEvent 파이프라인·Formatter·Enricher는 이 단계에서 도입) | 이슈 작업 항목별 테스트 통과 |
| 4 | japicmp·ArchUnit·매트릭스 CI, Maven Central 설정 | CI 정의 완료, 로컬 `./gradlew check` 통과 |
| 5 | 문서·예제·커뮤니티 파일·GitHub 메타·이슈 정리 | 예제 앱 빌드 통과, 문서 링크 검증 |

## 12. 메인테이너가 직접 해야 하는 것

- Sonatype Central 계정, `kr.suhsaechan` 네임스페이스 도메인 인증, GPG 키 생성과 GitHub Secret 등록
- `SECURITY.md` 신고 채널(메일 등) 결정
- Nexus 계정 비밀번호 교체 권장 (`gradle.properties`는 git 미추적이라 유출은 아님)
