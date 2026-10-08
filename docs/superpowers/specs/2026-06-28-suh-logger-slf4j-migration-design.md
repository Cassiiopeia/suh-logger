# suh-logger SLF4J 전환 설계

- **작성일**: 2026-06-28
- **대상 버전**: 2.0.2 → 2.1.0 (마이너 업, 공개 API 하위호환 유지)
- **상태**: 승인 대기

## 1. 배경 및 문제

`suh-logger`는 현재 **자체 커스텀 로깅 인프라**로 동작한다:

- `java.util.logging`(JUL) 로거를 만들어 `System.out`에 **직접 print**한다 (`SuhLoggerConfig.DirectConsoleHandler`).
- `SuhLogFormatter`가 ANSI 색·타임스탬프·PID·스레드명·축약 클래스명을 **직접 만들어** 붙인다.
- `NoOpLoggingSystemFactory`가 스프링부트의 `LoggingSystemFactory`로 등록되어, `getLoggingSystem()`에서 `null`을 반환해 **스프링부트의 Logback 초기화 자체를 막는다**.
- `build.gradle`이 `slf4j-api`, `logback-classic`, `spring-boot-starter-logging`을 **전부 exclude** 한다.

### 이로 인한 실제 문제

상위 스프링 프로젝트에서 `@Slf4j` 로그를 외부로 전송(Logback Appender 추가 등)하려 해도 **로그가 가지 않는다**. 원인은 `NoOpLoggingSystemFactory`가 상위 프로젝트의 Logback 초기화를 막아, 상위가 설정한 패턴·레벨·Appender(외부 전송 포함)가 **전부 무력화**되기 때문이다. 즉 이 라이브러리가 상위 프로젝트의 정상적인 SLF4J 로깅까지 깨뜨리고 있다.

부수적으로 suh-logger 자체 출력도 상위 프로젝트의 로그 컨벤션(`logging.pattern.console`, `logback-spring.xml` 등)을 전혀 따르지 않아, 포맷이 따로 논다.

### 과거 커스텀화의 추정 이유

주석들("전역 로거 설정에 영향 안 줌", "다른 로깅 프레임워크와 독립", "SLF4J 연결 해제")로 미루어, 라이브러리가 자체 로깅 의존성을 상위에 강제 주입해 **버전 충돌**이 났던 문제를 피하려고 모든 로깅 의존성을 끊고 JUL+System.out으로 독립시킨 것으로 보인다. 그 과정에서 상위 로깅까지 막는 부작용이 생겼다.

## 2. 목표

라이브러리의 로그 출력을 **상위 프로젝트의 SLF4J 파이프라인으로 위임**한다. 라이브러리는 메시지 텍스트만 생성하고, **색·타임스탬프·PID·클래스명·레벨 제어·Appender(외부 전송 포함)는 전부 상위 프로젝트가 담당**한다.

### 비목표 (YAGNI)

- 공개 API 시그니처 변경 — 안 한다. 상위 프로젝트 코드는 버전만 올리면 동작해야 한다.
- 어노테이션/마스킹/헤더 필터링/AOP/Filter 기능 변경 — 안 한다. 라이브러리의 핵심 가치이므로 그대로 유지한다.
- suh-logger만의 자체 색상 출력 모양 유지 — 포기한다. 상위 패턴에 위임하는 것이 목적이다.

## 3. 설계 결정 (확정)

| 항목 | 결정 |
|------|------|
| 전환 방식 | SLF4J 표준 위임. `compileOnly`로 `slf4j-api` 의존, 구현체는 상위 프로젝트가 제공 |
| 포맷팅 | 100% 상위 프로젝트에 위임. ANSI/PID/타임스탬프 직접 생성 코드 전부 삭제 |
| 정적 메서드 로거 이름 | 고정 이름 `kr.suhsaechan.suhlogger.SuhLogger` 사용 (스택트레이스 추적 안 함) |
| 공개 API | 시그니처 100% 유지. 내부 구현만 JUL → SLF4J 교체 |
| `setLogLevel()`/`addFileLogger()` | `@Deprecated` + no-op. 시그니처 유지로 하위호환, 호출 시 "상위 logging 설정으로 제어하라" 안내 로그 1회 |
| 버전 | 2.1.0 (마이너 업) |

### 상위 프로젝트 설정 위임 효과

전환 후 상위 프로젝트의 다음 설정들이 suh-logger 출력에 **그대로 적용**된다:

- `logging.pattern.console` / `logging.pattern.file` (콘솔/파일 패턴·정규식)
- `logging.level.kr.suhsaechan.suhlogger` (suh-logger 출력 레벨 제어)
- `logging.charset.*`, `logging.pattern.dateformat` 등
- `logback-spring.xml`의 `<appender>` (파일·외부 전송·JSON 등) — **suh-logger 로그도 동일 Appender를 탄다**
- 상위 `@Slf4j` 로그와 suh-logger 로그가 **동일 파이프라인·동일 패턴·동일 Appender**를 공유한다

## 4. 변경 상세

### 4.1 삭제 (커스텀 로깅 인프라)

| 대상 | 이유 |
|------|------|
| `config/NoOpLoggingSystemFactory.java` | 상위 Logback 초기화를 막던 주범. 로그 안 가던 근본 원인 |
| `config/SuhLoggerConfig.java` | JUL 로거 + `SuhLogFormatter`(ANSI/PID/타임스탬프) + `DirectConsoleHandler`(System.out print). 전부 상위 패턴이 대체 |
| `META-INF/spring.factories`의 `org.springframework.boot.logging.LoggingSystemFactory=...` 라인 | 차단 해제. (`EnableAutoConfiguration` 라인은 유지) |
| `build.gradle`의 `configurations { all*.exclude ... }` 로깅 exclude 블록 | slf4j-api/logback/starter-logging exclude 전부 제거 |

> 삭제 작업은 사용자 허락 후 진행 (CLAUDE.md 규칙).

### 4.2 전환: `util/SuhLogger.java`

- `import java.util.logging.*` 제거 → `org.slf4j.Logger`, `org.slf4j.LoggerFactory` 도입
- 정적 로거: `private static final Logger logger = LoggerFactory.getLogger(SuhLogger.class);`
- 레벨 매핑:
  - `logger.log(Level.INFO, msg)` → `log.info(msg)`
  - `Level.SEVERE` → `log.error(...)`
  - `Level.WARNING` → `log.warn(...)`
  - `Level.FINE` → `log.debug(...)`
- `getLogger(Class)` / `getLogger(String)`: 내부적으로 `LoggerFactory.getLogger(...)`를 감싸도록 변경. 인스턴스 메서드(`infoMsg`, `debugMsg` 등)는 SLF4J의 `{}` 플레이스홀더를 **네이티브로 위임** (직접 구현한 `formatMessage`/`isLoggable` 체크 제거 가능 — SLF4J가 레벨 가드와 포맷을 모두 처리)
- 레벨 체크 메서드(`isInfoEnabled` 등): SLF4J의 동명 메서드로 위임
- `setLogLevel(LogLevel)` / `setLogLevel(Level)` / `addFileLogger(String)`: `@Deprecated` 처리. 본문은 no-op + "상위 프로젝트의 logging 설정(logging.level.*, logback)으로 제어하세요" 경고 로그 1회. **JUL `Level` 파라미터 시그니처는 하위호환 위해 유지**하되 내부에서 무시.
- 메시지 생성 로직은 **그대로 유지**: `lineLogImpl`(구분선), `toSimpleJson`/`objectToJsonString`(JSON 직렬화), `superLogImpl`, `timeLog`, `logServerInitDuration`, 마스킹 연동(`CommonUtil`)
- `LogLevel` enum, `ThrowingRunnable` 인터페이스 유지

### 4.3 전환: `config/SuhLoggerAutoConfiguration.java`

- `import java.util.logging.Logger;` 제거 (미사용)
- `SuhLoggerInitializer`:
  - `SuhLoggerConfig.getLogger()` 호출 및 네임스페이스 검증 로직 제거 (SuhLoggerConfig 삭제됨)
  - `System.setProperty("org.slf4j.simpleLogger.log.kr.suhsaechan.suhlogger", "off")` **제거** (SLF4J 차단 잔재)
  - `SuhLogger.setProperties(properties)` 호출은 **유지** (마스킹 설정 주입에 필요)
- 나머지(`@EnableAspectJAutoProxy`, `@ComponentScan`, Filter 등록, 3.x/4.x 호환 `@AutoConfigureAfter/Before`)는 그대로 유지

### 4.4 정리: `build.gradle`

- 추가: `compileOnly 'org.slf4j:slf4j-api'` (버전은 Spring BOM 관리)
- 삭제: `configurations { all*.exclude group: ... 로깅 }` 블록 전체
- 테스트 의존성: `testImplementation`들의 `exclude group: ..., module: 'spring-boot-starter-logging'` **제거** → 테스트에서 logback이 실제로 잡혀 SLF4J 백엔드 동작을 검증할 수 있게 함

### 4.5 변경 없음 (유지)

- `annotation/` 전체 (`LogCall`, `LogMonitor`, `LogTime`, `HeaderLogOption`, `TriState`)
- `aspect/SuhMethodInvocationLoggingAspect.java`, `aspect/SuhExecutionTimeLoggingAspect.java` — `SuhLogger` public API만 사용하므로 내부 전환으로 자동 해결
- `filter/SuhLoggingFilter.java` — 동일
- `config/SuhLoggerProperties.java`, `util/CommonUtil.java`, `util/SuhTimeUtil.java`

## 5. 영향 분석 / 마이그레이션

- **상위 프로젝트**: 의존성 버전을 2.1.0으로 올리면 끝. 코드 수정 0.
  - 효과: 막혀 있던 `@Slf4j` 로그 외부 전송이 정상 동작. suh-logger 출력이 상위 패턴을 따름.
  - 주의: suh-logger 출력 레벨을 조정하려면 이제 `logging.level.kr.suhsaechan.suhlogger=...`를 상위 설정에 둬야 함 (기존 `SuhLogger.setLogLevel()`은 no-op).
- **API 하위호환**: 공개 메서드 시그니처 무변경. `setLogLevel`/`addFileLogger` 호출 코드도 컴파일·실행은 되나 deprecated 경고 + 무동작.

## 6. 검증

- `./gradlew build` 통과
- 기존 테스트 통과: `AutoConfigurationCompatibilityTest`, `SuhLoggerTest`, `SuhLoggerApplicationTests`
- 테스트 환경에서 SLF4J 백엔드(logback)가 정상 바인딩되는지 확인 (logging exclude 제거 효과)
- (수동) 샘플 스프링부트 앱에서 `logging.pattern.console` 변경 시 suh-logger 출력 포맷이 따라가는지 확인 — 선택

## 7. 리스크

- 상위 프로젝트가 SLF4J 구현체를 제공하지 않는 극단적 경우 → "SLF4J: No providers were found" 경고 후 무출력. 단 스프링부트 스타터를 쓰면 logback이 기본 포함되므로 사실상 발생 안 함.
- 기존에 `SuhLogger.setLogLevel()`로 런타임 레벨을 바꾸던 상위 프로젝트가 있으면 동작이 사라짐 → deprecated 안내 로그로 인지시키고, `logging.level.*`로 이전하도록 유도.
