# suh-logger SLF4J 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** suh-logger의 커스텀 JUL+System.out 로깅 인프라를 걷어내고 SLF4J로 위임하여, 상위 스프링 프로젝트의 로그 패턴·레벨·Appender(외부 전송 포함)를 그대로 따르게 한다.

**Architecture:** `SuhLogger`의 내부 구현을 `java.util.logging` → SLF4J(`LoggerFactory.getLogger`)로 교체하되 공개 API 시그니처는 100% 유지한다. 상위 프로젝트의 Logback 초기화를 막던 `NoOpLoggingSystemFactory`와 자체 포맷터 `SuhLoggerConfig`를 삭제하고, `build.gradle`의 로깅 exclude를 제거한다.

**Tech Stack:** Java 17, Spring Boot 3.4.4, SLF4J, Gradle, JUnit 5.

## Global Constraints

- **git 명령어 절대 금지** (프로젝트 CLAUDE.md). 이 계획의 어떤 스텝도 `git add`/`git commit`/`git push`를 실행하지 않는다. 각 태스크는 커밋 대신 "빌드/테스트 통과 확인"으로 마무리한다.
- **파일 삭제 시 사용자 허락 필수** (프로젝트 CLAUDE.md). 파일 삭제가 포함된 Task 1은 실행 전 사용자 승인을 받는다.
- **공개 API 시그니처 무변경**: `SuhLogger`의 모든 public 메서드 시그니처를 바꾸지 않는다. 상위 프로젝트는 버전만 올리면 동작해야 한다.
- **코드 주석**: 실무 수준의 간결한 한국어 주석(WHY 중심).
- **버전**: 최종 2.1.0. 단 버전 bump는 별도 릴리스 프로세스이므로 이 계획 범위 밖(문서에만 명시).
- **의존성 버전 하드코딩 금지** (문서): README 등에 버전 표기 시 `x.x.x` + 주석.

---

## File Structure

- **삭제**
  - `src/main/java/kr/suhsaechan/suhlogger/config/NoOpLoggingSystemFactory.java`
  - `src/main/java/kr/suhsaechan/suhlogger/config/SuhLoggerConfig.java`
- **수정**
  - `src/main/java/kr/suhsaechan/suhlogger/util/SuhLogger.java` — JUL→SLF4J 전환 (핵심)
  - `src/main/java/kr/suhsaechan/suhlogger/config/SuhLoggerAutoConfiguration.java` — JUL import·SLF4J 차단 잔재 제거
  - `src/main/resources/META-INF/spring.factories` — LoggingSystemFactory 라인 제거
  - `build.gradle` — slf4j-api 추가, 로깅 exclude 제거 (main+test)
- **변경 없음**: `annotation/*`, `aspect/*`, `filter/SuhLoggingFilter.java`, `config/SuhLoggerProperties.java`, `util/CommonUtil.java`, `util/SuhTimeUtil.java`

---

## Task 1: build.gradle 로깅 의존성 정리 + SuhLogger SLF4J 전환

**근거:** `SuhLogger.java`가 SLF4J로 컴파일되려면 `build.gradle`에 slf4j-api가 있어야 하고 로깅 exclude가 없어야 한다. 이 둘은 함께 변경·검증되어야 하므로 한 태스크로 묶는다. 삭제 대상 파일(`SuhLoggerConfig`, `NoOpLoggingSystemFactory`)은 아직 참조가 남아 컴파일이 깨지므로, 이 태스크에서는 **삭제하지 않고** `SuhLogger`가 그들을 더 이상 참조하지 않게 만드는 데 집중한다. 실제 파일 삭제는 Task 3.

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/java/kr/suhsaechan/suhlogger/util/SuhLogger.java`
- Test: `src/test/java/kr/suhsaechan/suhlogger/util/SuhLoggerTest.java` (기존)

**Interfaces:**
- Consumes: 없음 (진입 태스크)
- Produces: `SuhLogger`의 모든 public 정적 메서드(`info/warn/error/debug`, `infoJson`, `superLog[Debug|Warn|Error]`, `lineLog[Debug|Warn|Error]`, `logHeader`, `topDivider`/`bottomDivider`/`divider`, `logStream`, `timeLog`, `logServerInitDuration`, `setLogLevel`, `addFileLogger`)과 인스턴스 메서드(`getLogger`, `infoMsg/debugMsg/warnMsg/errorMsg`, `isXxxEnabled`) — 시그니처 불변, 내부는 SLF4J. 정적 로거 이름은 `kr.suhsaechan.suhlogger.util.SuhLogger`.

- [ ] **Step 1: build.gradle 수정**

`configurations { ... }` 블록(로깅 exclude 전체)을 삭제하고, `dependencies`에 slf4j-api를 추가한다. `compileOnly` starter들의 개별 `exclude` 도 제거한다.

변경 후 `build.gradle`의 `configurations`/`dependencies` 관련부는 다음과 같아야 한다:

```groovy
repositories {
    mavenCentral()
}

// (기존 configurations { all*.exclude ... 로깅 } 블록 전체 삭제)

dependencies {
    // ---- 런타임/컴파일 ----
    // Spring Boot 3.x 및 4.x 호환: compileOnly로 상위 애플리케이션이 버전 제공
    compileOnly 'org.springframework.boot:spring-boot-starter:3.4.4'
    compileOnly 'org.springframework.boot:spring-boot-starter-aop:3.4.4'

    // SLF4J API: 로그를 상위 프로젝트 로깅 파이프라인으로 위임 (구현체는 상위가 제공)
    compileOnly 'org.slf4j:slf4j-api'

    compileOnly 'jakarta.servlet:jakarta.servlet-api:6.0.0'
    compileOnly 'org.springframework:spring-web:6.1.12'

    api 'com.fasterxml.jackson.core:jackson-databind:2.18.3'
    api 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3'

    // ---- 테스트 ----
    // 테스트에서는 logback이 실제로 바인딩되도록 logging exclude 하지 않음 (SLF4J 백엔드 검증)
    testImplementation 'org.springframework.boot:spring-boot-starter-test:3.4.4'
    testImplementation 'org.springframework.boot:spring-boot-starter:3.4.4'
    testImplementation 'org.springframework.boot:spring-boot-starter-aop:3.4.4'
    testImplementation 'org.springframework:spring-web:6.1.12'
    testImplementation 'jakarta.servlet:jakarta.servlet-api:6.0.0'
}
```

- [ ] **Step 2: SuhLogger.java 상단 import 및 로거 필드 교체**

`import java.util.logging.Level;` / `import java.util.logging.Logger;` 및 `SuhLoggerConfig` import를 제거하고 SLF4J로 교체한다.

기존:
```java
import java.util.logging.Level;
import java.util.logging.Logger;

import kr.suhsaechan.suhlogger.config.SuhLoggerConfig;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
```
변경:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
```

로거 필드 및 인스턴스 필드 교체.

기존:
```java
// 로거 인스턴스
private static final Logger logger = SuhLoggerConfig.getLogger();
```
```java
// 인스턴스 필드
private final Logger julLogger;
private final String loggerName;

private SuhLogger(String name) {
    this.loggerName = name;
    this.julLogger = SuhLoggerConfig.getLogger();
}
```
변경:
```java
// SLF4J 로거: 정적 메서드 출력은 이 고정 이름으로 찍힘 (상위 프로젝트 패턴이 포맷 담당)
private static final Logger logger = LoggerFactory.getLogger(SuhLogger.class);
```
```java
// 인스턴스 필드: getLogger(clazz)로 생성된 클래스별 SLF4J 로거
private final Logger slf4jLogger;
private final String loggerName;

private SuhLogger(String name) {
    this.loggerName = name;
    // 호출자 클래스 이름으로 SLF4J 로거 생성 → 상위 프로젝트에서 %logger 로 정상 표시
    this.slf4jLogger = LoggerFactory.getLogger(name);
}
```

- [ ] **Step 3: 인스턴스 메서드(SLF4J 스타일) 교체**

`infoMsg/debugMsg/warnMsg/errorMsg` 계열은 SLF4J의 `{}` 플레이스홀더와 레벨 가드를 네이티브로 위임한다. 직접 짠 `formatMessage`/`isLoggable` 체크는 제거한다.

기존 (예: debug 3종):
```java
public void debugMsg(String message) {
    if (julLogger.isLoggable(Level.FINE)) {
        julLogger.log(Level.FINE, message);
    }
}
public void debugMsg(String message, Object arg) {
    if (julLogger.isLoggable(Level.FINE)) {
        julLogger.log(Level.FINE, formatMessage(message, arg));
    }
}
public void debugMsg(String message, Object... args) {
    if (julLogger.isLoggable(Level.FINE)) {
        julLogger.log(Level.FINE, formatMessage(message, args));
    }
}
```
변경:
```java
public void debugMsg(String message) {
    slf4jLogger.debug(message);
}
public void debugMsg(String message, Object arg) {
    slf4jLogger.debug(message, arg);   // SLF4J가 {} 치환·레벨가드 처리
}
public void debugMsg(String message, Object... args) {
    slf4jLogger.debug(message, args);
}
```

동일 패턴으로:
- `infoMsg(...)` 3종 → `slf4jLogger.info(...)`
- `warnMsg(...)` 3종 → `slf4jLogger.warn(...)`
- `errorMsg(...)` 3종 → `slf4jLogger.error(...)`
- `errorMsg(String, Throwable)` → `slf4jLogger.error(message, throwable)`

레벨 체크 메서드:
```java
public boolean isDebugEnabled() { return slf4jLogger.isDebugEnabled(); }
public boolean isInfoEnabled()  { return slf4jLogger.isInfoEnabled(); }
public boolean isWarnEnabled()  { return slf4jLogger.isWarnEnabled(); }
public boolean isErrorEnabled() { return slf4jLogger.isErrorEnabled(); }
```

- [ ] **Step 4: 정적 로그 출력 메서드 교체 (`logAtLevel` 및 직접 호출부)**

핵심은 `logger.log(Level.X, ...)` 호출을 전부 SLF4J로 바꾸는 것. `logAtLevel`이 대부분을 담당하므로 여기가 관건이다.

기존:
```java
private static void logAtLevel(LogLevel level, String message, Object... args) {
    switch (level) {
        case DEBUG: logger.log(Level.FINE, message, args); break;
        case INFO:  logger.log(Level.INFO, message, args); break;
        case WARN:  logger.log(Level.WARNING, message, args); break;
        case ERROR: logger.log(Level.SEVERE, message, args); break;
        default:    logger.log(Level.INFO, message, args);
    }
}
```
변경 (JUL의 `{0}` 플레이스홀더 → SLF4J `{}` 로 정규화):
```java
// 내부 메시지 포맷은 JUL 스타일 "{0}" 을 써왔으므로 SLF4J "{}" 로 변환 후 위임
private static void logAtLevel(LogLevel level, String message, Object... args) {
    String slf4jMsg = message.replace("{0}", "{}");
    switch (level) {
        case DEBUG: logger.debug(slf4jMsg, args); break;
        case INFO:  logger.info(slf4jMsg, args); break;
        case WARN:  logger.warn(slf4jMsg, args); break;
        case ERROR: logger.error(slf4jMsg, args); break;
        default:    logger.info(slf4jMsg, args);
    }
}
```

> WHY: 기존 코드는 `logAtLevel(LogLevel.INFO, "{0}", json)` 처럼 JUL의 `{0}` 인덱스 플레이스홀더를 쓴다. SLF4J는 `{}` 를 쓰므로 위임 직전에 치환해야 값이 정상 출력된다. `lineLogImpl`, `superLogImpl`, `logServerInitDuration` 등이 모두 `logAtLevel`을 통해 `{0}` 을 넘기므로 이 한 곳에서 처리하면 일괄 해결된다.

정적 단순 메서드들의 직접 호출부도 교체:
```java
public static void info(String message)  { logger.info(message); }
public static void warn(String message)  { logger.warn(message); }
public static void error(String message) { logger.error(message); }
public static void debug(String message) { logger.debug(message); }
public static void error(String message, Throwable throwable) { logger.error(message, throwable); }
```

`infoJson`:
```java
public static void infoJson(String message, Object object) {
    try {
        String jsonString = toSimpleJson(object);
        logger.info("{}\n{}", message, jsonString);
    } catch (Exception e) {
        error("JSON 변환 실패", e);
    }
}
```

`topDivider`/`bottomDivider`/`divider`/`logHeader`/`logStream` 내부의 `logger.log(Level.INFO, x)` → `logger.info(x)`, `logger.log(Level.SEVERE, msg, e)` → `logger.error(msg, e)` 로 각각 교체. (문자열 리터럴에 `{}` 가 없으므로 `{0}` 치환 불필요, 단 `logHeader`의 `padding + title` 등도 그대로 `logger.info(...)` 로 넘기면 됨)

- [ ] **Step 5: setLogLevel/addFileLogger를 @Deprecated no-op 처리**

이 메서드들은 SLF4J 위임 후 상위 프로젝트 설정 소관이 되므로 무동작 + 안내.

기존 `setLogLevel(LogLevel)` / `setLogLevel(Level)` / `addFileLogger(String)` 를 아래로 교체. `Level` 파라미터 시그니처는 하위호환 위해 유지하되, JUL import를 지웠으므로 **`java.util.logging.Level` 을 FQN으로** 받는다.

```java
/**
 * @deprecated SLF4J 위임 후 로그 레벨은 상위 프로젝트 설정(logging.level.kr.suhsaechan.suhlogger)으로 제어한다.
 * 하위호환 위해 시그니처만 유지하며 아무 동작도 하지 않는다.
 */
@Deprecated
public static void setLogLevel(LogLevel level) {
    warnDeprecatedLevelControl();
}

/**
 * @deprecated {@link #setLogLevel(LogLevel)} 참고. JUL Level 파라미터는 하위호환용이며 무시된다.
 */
@Deprecated
public static void setLogLevel(java.util.logging.Level level) {
    warnDeprecatedLevelControl();
}

/**
 * @deprecated SLF4J 위임 후 파일 출력은 상위 프로젝트의 logback appender로 설정한다.
 * 하위호환 위해 시그니처만 유지하며 아무 동작도 하지 않는다.
 */
@Deprecated
public static void addFileLogger(String logFilePath) {
    logger.warn("SuhLogger.addFileLogger()는 더 이상 동작하지 않습니다. "
        + "파일 로그는 상위 프로젝트의 logback appender로 설정하세요.");
}

// 중복 경고 로직 분리 (레벨 제어 계열 공통)
private static void warnDeprecatedLevelControl() {
    logger.warn("SuhLogger.setLogLevel()은 더 이상 동작하지 않습니다. "
        + "로그 레벨은 상위 프로젝트 설정(logging.level.kr.suhsaechan.suhlogger=...)으로 제어하세요.");
}
```

- [ ] **Step 6: 미사용 private 메서드 정리 (`formatMessage`)**

Step 3에서 `formatMessage` 호출이 모두 사라졌다. `formatMessage(String, Object...)` private 메서드를 삭제한다. (`repeat`, `toSimpleJson`, `objectToJsonString` 등 다른 private 메서드는 계속 사용되므로 유지)

> 확인: 삭제 전 파일 내 `formatMessage` 참조가 0인지 확인. Step 3 이후여야 함.

- [ ] **Step 7: 컴파일 확인 (아직 SuhLoggerConfig/NoOp 파일은 존재)**

Run: `./gradlew compileJava`
Expected: **BUILD SUCCESSFUL**. `SuhLogger.java`가 더 이상 `SuhLoggerConfig`·JUL을 참조하지 않고 SLF4J로 컴파일됨. (SuhLoggerConfig.java 파일 자체는 아직 남아있지만 아무도 참조 안 함 → 컴파일은 통과)

만약 `SuhLoggerConfig` 관련 "unused"가 아닌 **참조 에러**가 나면, `SuhLogger` 내 잔존 참조를 찾아 제거한다.

- [ ] **Step 8: SuhLogger 단위 테스트 실행**

Run: `./gradlew test --tests "kr.suhsaechan.suhlogger.util.SuhLoggerTest"`
Expected: PASS. (기존 테스트가 public API 시그니처만 쓰므로 통과해야 함)

만약 테스트가 JUL 특정 동작(예: `setLogLevel` 후 출력 억제)에 의존한다면, 해당 테스트를 열어 확인 후 이 계획의 "Task 1 노트"에 기록하고 사용자에게 보고한다 (deprecated no-op가 되었으므로 동작이 바뀜).

**검증 지점 (커밋 대신):** `./gradlew compileJava test --tests "*SuhLoggerTest"` 통과.

---

## Task 2: AutoConfiguration의 SLF4J 차단 잔재 제거

**근거:** `SuhLoggerAutoConfiguration`의 `SuhLoggerInitializer`가 삭제 예정인 `SuhLoggerConfig.getLogger()`를 호출하고, SLF4J를 끄는 `System.setProperty`를 실행한다. Task 3(파일 삭제) 전에 이 참조를 끊어야 삭제 시 컴파일이 안 깨진다.

**Files:**
- Modify: `src/main/java/kr/suhsaechan/suhlogger/config/SuhLoggerAutoConfiguration.java`
- Test: `src/test/java/kr/suhsaechan/suhlogger/config/AutoConfigurationCompatibilityTest.java` (기존)

**Interfaces:**
- Consumes: `SuhLogger.setProperties(SuhLoggerProperties)` (Task 1에서 시그니처 유지됨)
- Produces: `SuhLoggerInitializer` — 생성자에서 `SuhLogger.setProperties(properties)`만 수행. JUL/SuhLoggerConfig 참조 없음.

- [ ] **Step 1: 미사용 import 및 SuhLoggerInitializer 본문 정리**

`import java.util.logging.Logger;` 제거.

`SuhLoggerInitializer` 생성자를 아래로 교체:

기존:
```java
public SuhLoggerInitializer(SuhLoggerProperties properties) {
  Logger suhLogger = SuhLoggerConfig.getLogger();
  if (!suhLogger.getName().equals("kr.suhsaechan.suhlogger")) {
    throw new IllegalStateException("SuhLogger must use 'kr.suhsaechan.suhlogger' namespace");
  }
  System.setProperty("org.slf4j.simpleLogger.log.kr.suhsaechan.suhlogger", "off");
  SuhLogger.setProperties(properties);
}
```
변경:
```java
public SuhLoggerInitializer(SuhLoggerProperties properties) {
  // SLF4J 위임 구조: 로거 초기화/차단 로직 없이 마스킹 등 설정만 SuhLogger에 주입
  SuhLogger.setProperties(properties);
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL. 이제 `SuhLoggerConfig`·`NoOpLoggingSystemFactory`를 참조하는 소스가 남아있는지 확인:

Run: `grep -rn "SuhLoggerConfig\|NoOpLoggingSystemFactory" src/main src/test`
Expected: `spring.factories`(Task 3에서 처리)를 제외하고 `.java` 참조는 0건이어야 함.

- [ ] **Step 3: 호환성 테스트 실행**

Run: `./gradlew test --tests "kr.suhsaechan.suhlogger.config.AutoConfigurationCompatibilityTest"`
Expected: PASS.

**검증 지점:** `grep` 결과 .java 참조 0건 + 호환성 테스트 통과.

---

## Task 3: 커스텀 로깅 인프라 파일 삭제 + spring.factories 정리

**근거:** Task 1·2에서 모든 자바 참조를 끊었으므로 이제 안전하게 파일을 삭제하고 spring.factories에서 LoggingSystemFactory 등록을 제거할 수 있다. **파일 삭제 포함 → 실행 전 사용자 승인 필수.**

**Files:**
- Delete: `src/main/java/kr/suhsaechan/suhlogger/config/NoOpLoggingSystemFactory.java`
- Delete: `src/main/java/kr/suhsaechan/suhlogger/config/SuhLoggerConfig.java`
- Modify: `src/main/resources/META-INF/spring.factories`

**Interfaces:**
- Consumes: 없음
- Produces: 없음 (인프라 제거)

- [ ] **Step 1: 사용자에게 삭제 승인 요청**

두 파일 삭제 예정임을 사용자에게 알리고 명시적 허락을 받는다 (프로젝트 규칙). 승인 없이는 다음 스텝 진행 금지.

- [ ] **Step 2: spring.factories에서 LoggingSystemFactory 라인 제거**

기존:
```
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  kr.suhsaechan.suhlogger.config.SuhLoggerAutoConfiguration

# Disable Spring Boot Logging
org.springframework.boot.logging.LoggingSystemFactory=\
  kr.suhsaechan.suhlogger.config.NoOpLoggingSystemFactory
```
변경 (LoggingSystemFactory 블록 전체 삭제, EnableAutoConfiguration은 유지):
```
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  kr.suhsaechan.suhlogger.config.SuhLoggerAutoConfiguration
```

- [ ] **Step 3: 두 파일 삭제 (승인 후)**

Run: `rm src/main/java/kr/suhsaechan/suhlogger/config/NoOpLoggingSystemFactory.java src/main/java/kr/suhsaechan/suhlogger/config/SuhLoggerConfig.java`

- [ ] **Step 4: 잔존 참조 최종 확인**

Run: `grep -rn "SuhLoggerConfig\|NoOpLoggingSystemFactory" src/`
Expected: **0건** (spring.factories 라인도 제거됐으므로 전무).

**검증 지점:** grep 0건.

---

## Task 4: 전체 빌드 및 테스트 검증

**근거:** 모든 변경을 통합해 라이브러리가 정상 빌드되고 SLF4J 백엔드(logback)가 테스트에서 바인딩되는지 확인한다.

**Files:**
- Test: 전체 (`src/test/**`)

**Interfaces:**
- Consumes: Task 1~3의 모든 변경
- Produces: 없음 (검증)

- [ ] **Step 1: 클린 빌드**

Run: `./gradlew clean build`
Expected: **BUILD SUCCESSFUL**. 모든 테스트(`SuhLoggerTest`, `AutoConfigurationCompatibilityTest`, `SuhLoggerApplicationTests`) 통과.

- [ ] **Step 2: SLF4J 바인딩 확인 (로그 육안 검증)**

`./gradlew test` 출력 로그에서, suh-logger가 찍는 로그 줄이 **logback 기본 포맷**(예: `HH:mm:ss.SSS [thread] INFO k.s.s.util.SuhLogger - ...`)으로 나오는지 확인. JUL의 커스텀 색상 포맷(ANSI+PID)이 **더 이상 나오지 않아야** 함.

Expected: logback 포맷으로 출력. "SLF4J: No providers were found" 경고가 **없어야** 함 (test에 logback 포함됨).

- [ ] **Step 3: deprecated 경고 동작 확인 (선택)**

`SuhLoggerTest`에 `setLogLevel`/`addFileLogger` 호출이 있다면, 그 호출이 예외 없이 통과하고 경고 로그가 찍히는지 확인. (동작은 no-op)

**검증 지점:** `./gradlew clean build` 전체 통과 + SLF4J 바인딩 로그 확인.

---

## Self-Review (작성자 체크)

**1. Spec coverage:**
- spec §4.1 삭제 → Task 3 (+ build.gradle exclude는 Task 1) ✅
- spec §4.2 SuhLogger 전환 → Task 1 (Step 2~6) ✅
- spec §4.3 AutoConfiguration 정리 → Task 2 ✅
- spec §4.4 build.gradle → Task 1 Step 1 ✅
- spec §4.5 변경 없음 → 계획에서 건드리지 않음 (명시) ✅
- spec §6 검증 → Task 4 ✅
- setLogLevel/addFileLogger @Deprecated no-op → Task 1 Step 5 ✅

**2. Placeholder scan:** "TBD/TODO/적절히" 없음. 모든 코드 스텝에 실제 코드 존재. ✅

**3. Type consistency:**
- 인스턴스 필드명: `slf4jLogger` (Task 1 Step 2에서 정의, Step 3에서 사용) — 일관 ✅
- 정적 로거: `logger` (기존명 유지, 타입만 SLF4J) — Step 4에서 `logger.info` 등으로 사용 일관 ✅
- `warnDeprecatedLevelControl()` (Step 5에서 정의·사용) ✅
- `SuhLogger.setProperties` (Task 1 유지 → Task 2 Step 1 사용) ✅

**주의 사항 (실행자에게):**
- Task 순서 엄수: 1(SuhLogger 참조 제거) → 2(AutoConfig 참조 제거) → 3(파일 삭제). 순서 바꾸면 컴파일 깨짐.
- Task 1 Step 8 / Task 4 Step 2에서 기존 테스트가 JUL 특정 동작에 의존하면 발견 즉시 사용자 보고 (동작 변경 지점).
