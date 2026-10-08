# suh-logger v3 멀티모듈 전환 (#57) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 단일 모듈 suh-logger를 core / spring / servlet / boot-autoconfigure / starter / legacy / bom 멀티모듈로 나누고, 2.x 사용자 코드(좌표·import·프로퍼티)가 그대로 동작하게 한다.

**Architecture:** core는 slf4j-api만 보고 Spring을 모른다. 직렬화 위험 타입 처리는 `TypeHandler` SPI로, 현재 요청 정보 조회는 `RequestContextAccessor` SPI로 빼서 aspect가 Servlet을 직접 import하지 않게 한다. 자동설정은 `@Bean @ConfigurationProperties`로 core의 `SuhLoggerProperties`를 바인딩한다.

**Tech Stack:** Java 17, Gradle 8.13(wrapper 유지), Spring Boot 3.4.4(컴파일 하한), JUnit 5, logback-classic(테스트 출력 검증)

**Spec:** `docs/superpowers/specs/2026-10-08-suh-logger-v3-architecture-design.md`

## Global Constraints

- core 모듈의 컴파일 classpath에는 `org.slf4j:slf4j-api`만 둔다 (Spring·Jackson·Servlet 금지).
- 외부 프레임워크 의존은 전부 `compileOnly`(라이브러리 모듈) 또는 starter의 `api`(사용자 편의)로만 둔다. 버전은 `gradle/libs.versions.toml` 한 곳에서 관리한다.
- 사용자 import 경로 유지: `kr.suhsaechan.suhlogger.annotation.*`, `kr.suhsaechan.suhlogger.util.*`, `kr.suhsaechan.suhlogger.config.SuhLoggerProperties`, `kr.suhsaechan.suhlogger.aspect.*`, `kr.suhsaechan.suhlogger.filter.SuhLoggingFilter`.
- 자동설정 클래스 새 FQN: `kr.suhsaechan.suhlogger.boot.autoconfigure.SuhLoggerAutoConfiguration`.
- 기존 좌표 `kr.suhsaechan:suh-logger`는 legacy 모듈이 artifactId로 유지한다.
- 하위 모듈 `build.gradle`에 줄 시작 `version =` 대입을 두지 않는다 (version_manager가 덮어쓴다). 버전은 루트 `build.gradle`의 `version = "x.y.z"` 한 줄이 원천이다.
- 프로퍼티 기본값은 이 계획에서 바꾸지 않는다 (#55·#56에서 바꾼다).
- 비공개 구현은 `kr.suhsaechan.suhlogger.internal.*` 패키지에 둔다.
- 코드 주석은 한국어, WHY 중심으로 간결하게.

## Review Focus

1. **Servlet 없는 앱(non-web, 배치)**: starter를 넣고 servlet 클래스가 없어도 컨텍스트가 뜨고 `@LogMonitor`가 동작해야 한다 → Task 5 `nonWebContextHasAspectsButNoFilter`.
2. **spring-web 없는 앱에서 aspect 결과 로깅**: `ResponseEntity` 클래스가 없어도 `logResultSafely`가 `NoClassDefFoundError` 없이 일반 객체로 처리해야 한다 → Task 3 `resultLoggingWorksWithoutResponseEntityCheck` (클래스 이름 기반 판별 검증).
3. **사용자가 직접 `SuhLoggerProperties` 빈을 정의한 경우**: 자동설정이 두 번째 빈을 만들어 충돌하면 안 된다 → Task 5 `userDefinedPropertiesBeanWins`.
4. **2.x 사용법 그대로 쓴 소비자 앱**: 기존 좌표만 의존하고 `suh-logger.masking.*` 프로퍼티로 실제 HTTP 요청을 보냈을 때 마스킹·응답 로깅이 동작해야 한다 → Task 6 E2E.
5. **사용자 정의 `TypeHandler`가 예외를 던지는 경우**: 로깅이 원본 호출을 깨면 안 되고 기본 처리로 넘어가야 한다 → Task 2 `throwingHandlerFallsBackToDefault`.

---

## 파일 구조

```
settings.gradle                         (수정) 모듈 include, 루트 이름 suh-logger-parent
build.gradle                            (수정) version 원천 + 공통 group
gradle/libs.versions.toml               (생성) 버전 카탈로그
build-logic/settings.gradle             (생성)
build-logic/build.gradle                (생성)
build-logic/src/main/groovy/suh.java-library.gradle   (생성) 공통 java/test/publish 규칙

suh-logger-core/build.gradle
suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/
  annotation/{HeaderLogOption,LogCall,LogMonitor,LogTime,TriState,Incubating}.java
  config/SuhLoggerProperties.java       (@ConfigurationProperties 제거)
  util/{SuhLogger,SuhTimeUtil,CommonUtil}.java
  spi/TypeHandler.java
  spi/RequestContextAccessor.java
  spi/RequestSnapshot.java
  internal/serialize/TypeHandlers.java  (기본 핸들러 + 레지스트리)
suh-logger-core/src/main/resources/META-INF/services/  (없음 — 사용자 확장용 진입점만 문서화)
suh-logger-core/src/test/java/kr/suhsaechan/suhlogger/
  testsupport/LogCapture.java
  util/SuhLoggerTest.java               (재작성: 출력 검증)
  internal/serialize/TypeHandlersTest.java

suh-logger-spring/build.gradle
suh-logger-spring/src/main/java/kr/suhsaechan/suhlogger/
  aspect/SuhExecutionTimeLoggingAspect.java     (생성자 주입)
  aspect/SuhMethodInvocationLoggingAspect.java  (servlet import 제거)
  spring/SuhLoggerConfiguration.java            (Boot 없는 Spring용)
  internal/spring/ResponseEntityResults.java    (spring-web 있을 때만 로드)
suh-logger-spring/src/test/java/...  aspect/PlainSpringAspectTest.java

suh-logger-servlet/build.gradle
suh-logger-servlet/src/main/java/kr/suhsaechan/suhlogger/
  filter/SuhLoggingFilter.java
  servlet/ServletRequestContextAccessor.java
suh-logger-servlet/src/test/java/...  filter/SuhLoggingFilterTest.java

suh-logger-spring-boot-autoconfigure/build.gradle
suh-logger-spring-boot-autoconfigure/src/main/java/kr/suhsaechan/suhlogger/boot/autoconfigure/
  SuhLoggerAutoConfiguration.java
  SuhLoggerServletAutoConfiguration.java
suh-logger-spring-boot-autoconfigure/src/main/resources/META-INF/spring/
  org.springframework.boot.autoconfigure.AutoConfiguration.imports
suh-logger-spring-boot-autoconfigure/src/test/java/.../boot/autoconfigure/
  SuhLoggerAutoConfigurationTest.java
  AutoConfigurationCompatibilityTest.java       (이전 + 새 FQN)

suh-logger-spring-boot-starter/build.gradle
suh-logger-legacy/build.gradle                  (artifactId suh-logger)
suh-logger-legacy/src/test/java/.../e2e/Legacy2xCompatibilityE2ETest.java
suh-logger-bom/build.gradle

삭제: src/ (루트 단일 모듈 소스 전체 — 위 모듈로 이동 후)
      src/main/java/.../SuhLoggerApplication.java
      src/main/resources/application.properties
      src/main/resources/META-INF/spring.factories
      src/test/java/.../SuhLoggerApplicationTests.java
```

---

### Task 1: 멀티모듈 골격

**Files:**
- Create: `gradle/libs.versions.toml`, `build-logic/settings.gradle`, `build-logic/build.gradle`, `build-logic/src/main/groovy/suh.java-library.gradle`
- Create: `suh-logger-{core,spring,servlet,spring-boot-autoconfigure,spring-boot-starter,legacy,bom}/build.gradle`
- Modify: `settings.gradle`, `build.gradle`

**Interfaces:**
- Produces: 플러그인 id `suh.java-library` (Java 17 toolchain, JUnit Platform, maven-publish 공통 POM, Nexus HTTPS 저장소). 카탈로그 alias는 아래 toml 그대로.

- [ ] **Step 1: 버전 카탈로그 작성** — `gradle/libs.versions.toml`

```toml
[versions]
spring-boot = "3.4.4"
spring-framework = "6.2.5"
slf4j = "2.0.17"
jackson2 = "2.18.3"
servlet = "6.0.0"
aspectj = "1.9.22.1"
junit = "5.11.4"
logback = "1.5.18"

[libraries]
slf4j-api = { module = "org.slf4j:slf4j-api", version.ref = "slf4j" }
spring-context = { module = "org.springframework:spring-context", version.ref = "spring-framework" }
spring-aop = { module = "org.springframework:spring-aop", version.ref = "spring-framework" }
spring-web = { module = "org.springframework:spring-web", version.ref = "spring-framework" }
spring-test = { module = "org.springframework:spring-test", version.ref = "spring-framework" }
spring-boot-autoconfigure = { module = "org.springframework.boot:spring-boot-autoconfigure", version.ref = "spring-boot" }
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test", version.ref = "spring-boot" }
spring-boot-configuration-processor = { module = "org.springframework.boot:spring-boot-configuration-processor", version.ref = "spring-boot" }
jackson2-databind = { module = "com.fasterxml.jackson.core:jackson-databind", version.ref = "jackson2" }
servlet-api = { module = "jakarta.servlet:jakarta.servlet-api", version.ref = "servlet" }
aspectj-weaver = { module = "org.aspectj:aspectjweaver", version.ref = "aspectj" }
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit" }
logback-classic = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }
```

- [ ] **Step 2: build-logic 포함 빌드** — `build-logic/settings.gradle`

```groovy
// 루트 빌드의 버전 카탈로그를 convention plugin에서도 같이 쓴다
dependencyResolutionManagement {
    versionCatalogs {
        libs { from(files('../gradle/libs.versions.toml')) }
    }
}
rootProject.name = 'build-logic'
```

`build-logic/build.gradle`

```groovy
plugins { id 'groovy-gradle-plugin' }
repositories { gradlePluginPortal() }
```

`build-logic/src/main/groovy/suh.java-library.gradle`

```groovy
// 모든 배포 모듈이 공유하는 규칙 — 모듈마다 복붙하지 않도록 한 곳에서 관리한다
plugins {
    id 'java-library'
    id 'maven-publish'
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    // aspect가 파라미터 이름을 읽으므로 -parameters 유지
    options.compilerArgs << '-parameters'
}

tasks.withType(Javadoc).configureEach {
    options.encoding = 'UTF-8'
    // 한글 javadoc·누락 태그로 배포가 막히지 않게 doclint 경고를 끈다
    options.addStringOption('Xdoclint:none', '-quiet')
}

repositories { mavenCentral() }

dependencies {
    testImplementation platform(libs.findLibrary('junit-bom').get())
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') { useJUnitPlatform() }

publishing {
    publications {
        mavenJava(MavenPublication) {
            from components.java
            pom {
                name = project.name
                description = project.description ?: 'Annotation-based structured logging for Spring Boot'
                url = 'https://github.com/Cassiiopeia/suh-logger'
                licenses { license { name = 'MIT License'; url = 'https://opensource.org/licenses/MIT' } }
                developers { developer { id = 'Cassiiopeia'; name = 'SUH SAECHAN' } }
                scm {
                    url = 'https://github.com/Cassiiopeia/suh-logger'
                    connection = 'scm:git:https://github.com/Cassiiopeia/suh-logger.git'
                }
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = 'SUH-NEXUS'
            // HTTPS만 허용한다 (기존 allowInsecureProtocol 제거)
            url = uri(version.toString().endsWith('SNAPSHOT')
                    ? 'https://nexus.suhsaechan.kr/repository/maven-snapshots/'
                    : 'https://nexus.suhsaechan.kr/repository/maven-releases/')
            credentials {
                username = project.findProperty('nexusUsername')
                password = project.findProperty('nexusPassword')
            }
        }
    }
}
```

- [ ] **Step 3: 루트 설정** — `settings.gradle` 전체 교체

```groovy
pluginManagement {
    includeBuild('build-logic')
}

rootProject.name = 'suh-logger-parent'

include 'suh-logger-core'
include 'suh-logger-spring'
include 'suh-logger-servlet'
include 'suh-logger-spring-boot-autoconfigure'
include 'suh-logger-spring-boot-starter'
include 'suh-logger-legacy'
include 'suh-logger-bom'
```

`build.gradle` 전체 교체

```groovy
// version.yml ↔ 이 줄이 버전 원천이다 (version_manager가 줄 시작 version = 만 동기화)
version = "2.0.3"

allprojects {
    group = 'kr.suhsaechan'
    version = rootProject.version
}
```

- [ ] **Step 4: 모듈 build.gradle 7개**

`suh-logger-core/build.gradle`
```groovy
plugins { id 'suh.java-library' }
description = 'suh-logger core: framework-free structured logging over SLF4J'

dependencies {
    compileOnly libs.slf4j.api
    testImplementation libs.slf4j.api
    testImplementation libs.logback.classic
}
```

`suh-logger-spring/build.gradle`
```groovy
plugins { id 'suh.java-library' }
description = 'suh-logger Spring AOP support (@LogMonitor, @LogCall, @LogTime)'

dependencies {
    api project(':suh-logger-core')
    compileOnly libs.slf4j.api
    compileOnly libs.spring.context
    compileOnly libs.spring.aop
    compileOnly libs.aspectj.weaver
    // ResponseEntity 특수 처리용 — 런타임엔 있을 때만 로드한다
    compileOnly libs.spring.web

    testImplementation libs.spring.context
    testImplementation libs.spring.aop
    testImplementation libs.aspectj.weaver
    testImplementation libs.logback.classic
    testImplementation testFixtures(project(':suh-logger-core'))
}
```

`suh-logger-servlet/build.gradle`
```groovy
plugins { id 'suh.java-library' }
description = 'suh-logger Servlet request/response logging filter'

dependencies {
    api project(':suh-logger-core')
    compileOnly libs.slf4j.api
    compileOnly libs.servlet.api
    compileOnly libs.spring.web
    compileOnly libs.spring.context
    // pretty print 전용 — 없으면 원문 그대로 출력한다 (#58에서 JsonCodec으로 교체)
    compileOnly libs.jackson2.databind

    testImplementation libs.servlet.api
    testImplementation libs.spring.web
    testImplementation libs.spring.test
    testImplementation libs.jackson2.databind
    testImplementation libs.logback.classic
    testImplementation testFixtures(project(':suh-logger-core'))
}
```

`suh-logger-spring-boot-autoconfigure/build.gradle`
```groovy
plugins { id 'suh.java-library' }
description = 'suh-logger Spring Boot auto-configuration'

dependencies {
    api project(':suh-logger-core')
    compileOnly project(':suh-logger-spring')
    compileOnly project(':suh-logger-servlet')
    compileOnly libs.spring.boot.autoconfigure
    compileOnly libs.servlet.api
    compileOnly libs.spring.web
    compileOnly libs.aspectj.weaver
    annotationProcessor libs.spring.boot.configuration.processor

    testImplementation project(':suh-logger-spring')
    testImplementation project(':suh-logger-servlet')
    testImplementation libs.spring.boot.starter.test
    testImplementation libs.spring.boot.starter.web
    testImplementation libs.aspectj.weaver
    testImplementation testFixtures(project(':suh-logger-core'))
}
```

`suh-logger-spring-boot-starter/build.gradle`
```groovy
plugins { id 'suh.java-library' }
description = 'suh-logger Spring Boot starter — add this one dependency'

dependencies {
    api project(':suh-logger-core')
    api project(':suh-logger-spring')
    api project(':suh-logger-servlet')
    api project(':suh-logger-spring-boot-autoconfigure')
    // AOP 실행에 필요 — 사용자 Boot의 의존성 관리가 버전을 맞춘다
    api libs.aspectj.weaver
}
```

`suh-logger-legacy/build.gradle`
```groovy
plugins { id 'suh.java-library' }
description = 'suh-logger (2.x coordinate) — delegates to suh-logger-spring-boot-starter'

// 기존 좌표 kr.suhsaechan:suh-logger 유지
base { archivesName = 'suh-logger' }
publishing { publications { mavenJava(MavenPublication) { artifactId = 'suh-logger' } } }

dependencies {
    api project(':suh-logger-spring-boot-starter')

    testImplementation libs.spring.boot.starter.test
    testImplementation libs.spring.boot.starter.web
    testImplementation testFixtures(project(':suh-logger-core'))
}
```

`suh-logger-bom/build.gradle`
```groovy
plugins {
    id 'java-platform'
    id 'maven-publish'
}
description = 'suh-logger BOM'

dependencies {
    constraints {
        api project(':suh-logger-core')
        api project(':suh-logger-spring')
        api project(':suh-logger-servlet')
        api project(':suh-logger-spring-boot-autoconfigure')
        api project(':suh-logger-spring-boot-starter')
        api project(':suh-logger-legacy')
    }
}

publishing {
    publications { mavenBom(MavenPublication) { from components.javaPlatform } }
    repositories {
        mavenLocal()
        maven {
            name = 'SUH-NEXUS'
            url = uri(version.toString().endsWith('SNAPSHOT')
                    ? 'https://nexus.suhsaechan.kr/repository/maven-snapshots/'
                    : 'https://nexus.suhsaechan.kr/repository/maven-releases/')
            credentials {
                username = project.findProperty('nexusUsername')
                password = project.findProperty('nexusPassword')
            }
        }
    }
}
```

core에 `java-test-fixtures` 플러그인을 추가한다 (`LogCapture`를 다른 모듈 테스트가 공유):
```groovy
plugins {
    id 'suh.java-library'
    id 'java-test-fixtures'
}
...
dependencies {
    compileOnly libs.slf4j.api
    testFixturesApi libs.slf4j.api
    testFixturesApi libs.logback.classic
}
```

- [ ] **Step 5: 골격 확인**

Run: `./gradlew projects`
Expected: 7개 하위 프로젝트 나열, BUILD SUCCESSFUL

(루트 `src/`는 Task 2~5에서 옮기므로 이 시점엔 루트에 java 플러그인이 없어 컴파일되지 않는다.)

---

### Task 2: core 모듈 — 이전, TypeHandler SPI, 출력 검증 테스트

**Files:**
- Move: `src/main/java/kr/suhsaechan/suhlogger/{annotation,util}/*` → `suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/{annotation,util}/`
- Move+Modify: `config/SuhLoggerProperties.java` → core (`@ConfigurationProperties`·import 제거)
- Create: `annotation/Incubating.java`, `spi/TypeHandler.java`, `spi/RequestContextAccessor.java`, `spi/RequestSnapshot.java`, `internal/serialize/TypeHandlers.java`
- Modify: `util/CommonUtil.java` (`makeSafeForSerialization`이 `TypeHandlers`에 위임)
- Create: `suh-logger-core/src/testFixtures/java/kr/suhsaechan/suhlogger/testsupport/LogCapture.java`
- Test: `suh-logger-core/src/test/java/kr/suhsaechan/suhlogger/util/SuhLoggerTest.java`, `.../internal/serialize/TypeHandlersTest.java`

**Interfaces:**
- Produces:
  - `public interface TypeHandler { boolean supports(Object value); Object toSafe(Object value); default int order() { return 0; } }`
  - `public final class TypeHandlers { static TypeHandlers defaults(); static void register(TypeHandler h); static void reset(); Object apply(Object value) /* null이면 처리 안 함 */; static TypeHandlers global(); }`
  - `public interface RequestContextAccessor { RequestSnapshot current(); RequestContextAccessor NONE = () -> null; }`
  - `public final class RequestSnapshot { RequestSnapshot(String method, String uri, Map<String,String> headers, String requestId); getters }`
  - `@Incubating` (`@Documented @Retention(CLASS) @Target({TYPE, METHOD, CONSTRUCTOR, FIELD})`, 속성 `String since() default ""`)
  - testFixtures `LogCapture implements AutoCloseable { static LogCapture start(); String text(); List<String> messages(); void close(); }`

- [ ] **Step 1: 파일 이동**

```bash
mkdir -p suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/{config,spi,internal/serialize}
mv src/main/java/kr/suhsaechan/suhlogger/annotation suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/
mv src/main/java/kr/suhsaechan/suhlogger/util suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/
mv src/main/java/kr/suhsaechan/suhlogger/config/SuhLoggerProperties.java suh-logger-core/src/main/java/kr/suhsaechan/suhlogger/config/
```

`SuhLoggerProperties.java`에서 `import org.springframework.boot.context.properties.ConfigurationProperties;`와 `@ConfigurationProperties(prefix = "suh-logger")` 두 줄을 지우고 클래스 javadoc에 한 줄 추가:
```java
 * Boot에서는 자동설정이 {@code @Bean @ConfigurationProperties("suh-logger")}로 바인딩한다.
 * core는 Boot에 의존하지 않으므로 이 클래스에는 바인딩 어노테이션을 두지 않는다.
```

- [ ] **Step 2: LogCapture 테스트 지원 작성** — `suh-logger-core/src/testFixtures/java/kr/suhsaechan/suhlogger/testsupport/LogCapture.java`

```java
package kr.suhsaechan.suhlogger.testsupport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 * SuhLogger 출력 검증용 캡처.
 * 콘솔 문자열 비교 대신 logback 이벤트를 모아 SLF4J 위임이 실제로 일어났는지 본다.
 */
public final class LogCapture implements AutoCloseable {

    private final Logger root;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private LogCapture() {
        root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appender.start();
        root.addAppender(appender);
    }

    public static LogCapture start() {
        return new LogCapture();
    }

    public List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }

    public List<ILoggingEvent> events() {
        return List.copyOf(appender.list);
    }

    public String text() {
        return String.join("\n", messages());
    }

    @Override
    public void close() {
        root.detachAppender(appender);
        appender.stop();
    }
}
```

- [ ] **Step 3: 실패하는 테스트 작성** — `suh-logger-core/src/test/java/kr/suhsaechan/suhlogger/internal/serialize/TypeHandlersTest.java`

```java
package kr.suhsaechan.suhlogger.internal.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import kr.suhsaechan.suhlogger.spi.TypeHandler;
import kr.suhsaechan.suhlogger.util.CommonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TypeHandlersTest {

    @AfterEach
    void reset() {
        TypeHandlers.reset();
    }

    /** 클래스 이름에 MultipartFile이 들어간 타입은 Spring 없이도 메타데이터만 남긴다 */
    static class FakeMultipartFile {
        public String getOriginalFilename() { return "a.txt"; }
        public String getContentType() { return "text/plain"; }
        public long getSize() { return 3L; }
        public String getName() { return "file"; }
        public boolean isEmpty() { return false; }
    }

    static class Money {
        final long amount;
        Money(long amount) { this.amount = amount; }
    }

    @Test
    void inputStreamBecomesTypeInfo() {
        Object safe = CommonUtil.makeSafeForSerialization(new ByteArrayInputStream(new byte[]{1}));
        assertEquals("InputStream", ((Map<?, ?>) safe).get("_type"));
    }

    @Test
    void multipartFileByNameBecomesMetadata() {
        Object safe = CommonUtil.makeSafeForSerialization(new FakeMultipartFile());
        Map<?, ?> map = (Map<?, ?>) safe;
        assertEquals("MultipartFile", map.get("_type"));
        assertEquals("a.txt", map.get("originalFilename"));
    }

    @Test
    void vectorIsHandled() {
        Vector<Integer> v = new Vector<>(List.of(1, 2));
        Object safe = CommonUtil.makeSafeForSerialization(v);
        assertInstanceOf(Map.class, safe);
    }

    @Test
    void excludedClassWinsOverHandlers() {
        Object safe = CommonUtil.makeSafeForSerialization(new FakeMultipartFile(),
                List.of(FakeMultipartFile.class.getName()));
        assertEquals(Boolean.TRUE, ((Map<?, ?>) safe).get("_excluded"));
    }

    @Test
    void customHandlerIsApplied() {
        TypeHandlers.register(new TypeHandler() {
            public boolean supports(Object value) { return value instanceof Money; }
            public Object toSafe(Object value) { return Map.of("amount", ((Money) value).amount); }
        });
        Object safe = CommonUtil.makeSafeForSerialization(new Money(500));
        assertEquals(500L, ((Map<?, ?>) safe).get("amount"));
    }

    @Test
    void throwingHandlerFallsBackToDefault() {
        TypeHandlers.register(new TypeHandler() {
            public boolean supports(Object value) { return value instanceof Money; }
            public Object toSafe(Object value) { throw new IllegalStateException("boom"); }
        });
        Money money = new Money(1);
        // 핸들러 실패가 로깅 대상 호출을 깨면 안 된다 — 원본 객체로 폴백
        assertSame(money, CommonUtil.makeSafeForSerialization(money));
    }
}
```

`createExcludedClassInfo`가 `_excluded` 키를 쓰는지 기존 코드(`CommonUtil.java:353`)를 확인하고, 다른 키면 테스트의 키를 그 이름에 맞춘다.

- [ ] **Step 4: 실패 확인**

Run: `./gradlew :suh-logger-core:test --tests '*TypeHandlersTest'`
Expected: 컴파일 실패 (`TypeHandler`, `TypeHandlers` 없음)

- [ ] **Step 5: SPI·레지스트리 구현**

`spi/TypeHandler.java`
```java
package kr.suhsaechan.suhlogger.spi;

import kr.suhsaechan.suhlogger.annotation.Incubating;

/**
 * 직렬화가 위험한 타입(스트림, 파일, 지오메트리 등)을 로그에 안전한 값으로 바꾸는 확장 지점.
 * core를 고치지 않고 자기 도메인 타입을 등록할 수 있게 하려고 분리했다.
 * Boot에서는 TypeHandler 빈을 등록하면 자동으로 반영된다.
 */
@Incubating(since = "3.0.0")
public interface TypeHandler {

    /** 이 핸들러가 처리할 값인지 */
    boolean supports(Object value);

    /** 로그에 남길 안전한 값 (Map·String·Number 권장) */
    Object toSafe(Object value);

    /** 작을수록 먼저 검사한다. 사용자 핸들러가 기본 핸들러보다 앞서도록 기본 구현은 100 이상을 쓴다 */
    default int order() {
        return 0;
    }
}
```

`annotation/Incubating.java`
```java
package kr.suhsaechan.suhlogger.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 아직 안정화되지 않은 API 표시. minor 버전에서 바뀔 수 있다.
 * 표시가 없는 공개 API는 major 버전에서만 깨진다 (Gradle·Micrometer의 @Incubating과 같은 규칙).
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Incubating {
    /** 처음 도입된 버전 */
    String since() default "";
}
```

`spi/RequestSnapshot.java`
```java
package kr.suhsaechan.suhlogger.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 어댑터(Servlet·WebFlux)가 넘겨주는 현재 요청 정보. core가 프레임워크 타입을 모르게 하려는 값 객체 */
public final class RequestSnapshot {

    private final String method;
    private final String uri;
    private final Map<String, String> headers;
    private final String requestId;

    public RequestSnapshot(String method, String uri, Map<String, String> headers, String requestId) {
        this.method = method;
        this.uri = uri;
        this.headers = headers == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.requestId = requestId;
    }

    public String getMethod() { return method; }
    public String getUri() { return uri; }
    public Map<String, String> getHeaders() { return headers; }
    public String getRequestId() { return requestId; }
}
```

`spi/RequestContextAccessor.java`
```java
package kr.suhsaechan.suhlogger.spi;

import kr.suhsaechan.suhlogger.annotation.Incubating;

/**
 * 현재 스레드의 요청 정보를 꺼내는 확장 지점.
 * aspect가 jakarta.servlet을 직접 import하면 Servlet 없는 앱(WebFlux·배치)에서 클래스 로딩이 깨지므로 분리했다.
 */
@Incubating(since = "3.0.0")
@FunctionalInterface
public interface RequestContextAccessor {

    /** 요청이 없으면 null */
    RequestSnapshot current();

    /** 요청 개념이 없는 환경용 */
    RequestContextAccessor NONE = () -> null;
}
```

`internal/serialize/TypeHandlers.java`
```java
package kr.suhsaechan.suhlogger.internal.serialize;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import kr.suhsaechan.suhlogger.spi.TypeHandler;
import kr.suhsaechan.suhlogger.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TypeHandler 레지스트리. 기본 핸들러 + ServiceLoader 등록분 + 런타임 등록분을 order 순으로 검사한다.
 * 정적 상태인 이유: SuhLogger 정적 API가 Spring 컨텍스트 없이도 같은 규칙을 써야 하기 때문.
 */
public final class TypeHandlers {

    private static final Logger log = LoggerFactory.getLogger(TypeHandlers.class);
    private static final List<TypeHandler> registered = new CopyOnWriteArrayList<>();
    private static volatile List<TypeHandler> cache;

    private TypeHandlers() {
    }

    public static void register(TypeHandler handler) {
        if (handler != null) {
            registered.add(handler);
            cache = null;
        }
    }

    /** 테스트·컨텍스트 재시작용 */
    public static void reset() {
        registered.clear();
        cache = null;
    }

    /** 처리할 핸들러가 없으면 null을 돌려 호출부가 기존 분기(Map·Collection 등)로 진행하게 한다 */
    public static Object apply(Object value) {
        for (TypeHandler handler : handlers()) {
            try {
                if (handler.supports(value)) {
                    return handler.toSafe(value);
                }
            } catch (RuntimeException e) {
                // 사용자 핸들러 실패가 비즈니스 호출을 깨면 안 된다
                log.debug("TypeHandler {} failed, falling back: {}", handler.getClass().getName(), e.toString());
                return null;
            }
        }
        return null;
    }

    private static List<TypeHandler> handlers() {
        List<TypeHandler> local = cache;
        if (local == null) {
            List<TypeHandler> all = new ArrayList<>(registered);
            for (TypeHandler h : ServiceLoader.load(TypeHandler.class, TypeHandlers.class.getClassLoader())) {
                all.add(h);
            }
            all.addAll(defaults());
            all.sort(Comparator.comparingInt(TypeHandler::order));
            cache = local = List.copyOf(all);
        }
        return local;
    }

    /** 2.x CommonUtil에 하드코딩돼 있던 처리 순서를 그대로 옮긴 기본 핸들러 */
    static List<TypeHandler> defaults() {
        return List.of(
                handler(100, v -> v instanceof InputStream, v -> {
                    java.util.Map<String, Object> m = new java.util.HashMap<>();
                    m.put("_type", "InputStream");
                    m.put("_class", v.getClass().getName());
                    return m;
                }),
                handler(110, v -> v.getClass().getName().contains("MultipartFile"), CommonUtil::extractMultipartFileInfo),
                handler(120, v -> v instanceof Vector, v -> CommonUtil.extractVectorInfo((Vector<?>) v)),
                handler(130, v -> v instanceof File, v -> CommonUtil.extractFileInfo((File) v)),
                handler(140, v -> v.getClass().getName().contains("org.locationtech.jts.geom") || CommonUtil.isJTSGeometryType(v),
                        CommonUtil::extractJTSGeometryInfo)
        );
    }

    private static TypeHandler handler(int order, java.util.function.Predicate<Object> supports,
                                       java.util.function.Function<Object, Object> toSafe) {
        return new TypeHandler() {
            public boolean supports(Object value) { return supports.test(value); }
            public Object toSafe(Object value) { return toSafe.apply(value); }
            public int order() { return order; }
        };
    }
}
```

`util/CommonUtil.java`의 `makeSafeForSerialization(Object, List<String>)` 본문에서 "InputStream / MultipartFile / Vector / File / JTS" 다섯 분기를 지우고, 제외 클래스 체크 직후에 다음을 넣는다:
```java
        // 타입별 안전 변환은 TypeHandler 레지스트리에 위임 (사용자 확장 가능)
        Object handled = TypeHandlers.apply(obj);
        if (handled != null) {
            return handled;
        }
```
import `kr.suhsaechan.suhlogger.internal.serialize.TypeHandlers;` 추가. `extract*`·`isJTSGeometryType` 공개 static 메서드는 시그니처 그대로 둔다.

`Money` 같은 일반 객체는 기존 마지막 분기(`toString()`에 MultipartFile 포함 여부)까지 그대로 내려가 원본이 반환된다 — `throwingHandlerFallsBackToDefault`가 이를 검증한다.

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :suh-logger-core:test --tests '*TypeHandlersTest'`
Expected: 6 tests PASS

- [ ] **Step 7: SuhLogger 출력 테스트 재작성** — `suh-logger-core/src/test/java/kr/suhsaechan/suhlogger/util/SuhLoggerTest.java` (기존 `src/test/.../util/SuhLoggerTest.java`는 삭제)

```java
package kr.suhsaechan.suhlogger.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SuhLoggerTest {

    @AfterEach
    void resetProperties() {
        SuhLogger.setProperties(null);
    }

    @Test
    void staticInfoDelegatesToSlf4j() {
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.info("hello");
            assertTrue(capture.messages().contains("hello"));
        }
    }

    @Test
    void indexedPlaceholdersAreAllReplaced() {
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.getLogger(SuhLoggerTest.class).infoMsg("{0}-{1}", "a", "b");
            assertTrue(capture.text().contains("a-b"), capture.text());
        }
    }

    @Test
    void superLogPrintsMapAsJson() {
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.superLog(Map.of("k", "v"));
            assertTrue(capture.text().contains("\"k\": \"v\""), capture.text());
        }
    }

    @Test
    void excludedClassesFromPropertiesAreRespected() {
        SuhLoggerProperties props = new SuhLoggerProperties();
        props.setExcludedClasses(List.of(StringBuilder.class.getName()));
        SuhLogger.setProperties(props);
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.superLog(new StringBuilder("secret-content"));
            assertFalse(capture.text().contains("secret-content"), capture.text());
        }
    }

    @Test
    void timeLogReportsDuration() {
        try (LogCapture capture = LogCapture.start()) {
            SuhLogger.timeLog(() -> { });
            assertTrue(capture.text().contains("실행 시간"), capture.text());
        }
    }
}
```

`excludedClassesFromPropertiesAreRespected`: `isExcludedClass`가 `Class.forName`으로 비교하므로 `java.lang.StringBuilder`가 잡힌다. 실패하면 `CommonUtil.isExcludedClass` 동작(`CommonUtil.java:334`)을 읽고 테스트 대상 클래스를 바꾼다 — 구현을 테스트에 맞춰 고치지 않는다.

- [ ] **Step 8: core 전체 통과**

Run: `./gradlew :suh-logger-core:test`
Expected: PASS

- [ ] **Step 9: 커밋** — `/pro-commit` (경로: `gradle/ build-logic/ settings.gradle build.gradle suh-logger-*/build.gradle suh-logger-core/`, 삭제된 `src/main/java/.../{annotation,util}` 경로)

---

### Task 3: spring 모듈 — aspect 이전, Servlet 직접 의존 제거, Boot 없는 Spring 설정

**Files:**
- Move+Modify: `src/main/java/kr/suhsaechan/suhlogger/aspect/*.java` → `suh-logger-spring/src/main/java/kr/suhsaechan/suhlogger/aspect/`
- Create: `suh-logger-spring/src/main/java/kr/suhsaechan/suhlogger/spring/SuhLoggerConfiguration.java`
- Create: `suh-logger-spring/src/main/java/kr/suhsaechan/suhlogger/internal/spring/ResponseEntityResults.java`
- Test: `suh-logger-spring/src/test/java/kr/suhsaechan/suhlogger/aspect/PlainSpringAspectTest.java`

**Interfaces:**
- Consumes: `SuhLoggerProperties`, `RequestContextAccessor`, `RequestSnapshot`, `LogCapture`(testFixtures)
- Produces:
  - `SuhMethodInvocationLoggingAspect(SuhLoggerProperties properties, RequestContextAccessor accessor)`
  - `SuhExecutionTimeLoggingAspect(SuhLoggerProperties properties)`
  - `@Configuration @EnableAspectJAutoProxy public class SuhLoggerConfiguration` — 빈: `suhExecutionTimeLoggingAspect`, `suhMethodInvocationLoggingAspect`, `suhLoggerInitializer`. 생성자 인자 `ObjectProvider<SuhLoggerProperties>`, `ObjectProvider<RequestContextAccessor>`
  - `ResponseEntityResults.isResponseEntity(Object)`, `ResponseEntityResults.toSafeMap(Object, Function<Map<String,String>,Map<String,String>> headerMasker)`

- [ ] **Step 1: 실패하는 테스트** — `PlainSpringAspectTest.java`

```java
package kr.suhsaechan.suhlogger.aspect;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.RequestSnapshot;
import kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration;
import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Boot 없이 순수 Spring(JavaConfig)에서 import 한 줄로 동작하는지 — XML 사용자와 같은 경로 */
class PlainSpringAspectTest {

    public static class Greeter {
        @LogMonitor
        public String greet(String name, String password) {
            return "hi " + name;
        }

        public Object plainObject() {
            return Map.of("plain", true);
        }

        @LogMonitor
        public Object returnsMap() {
            return Map.of("ok", 1);
        }
    }

    @Configuration
    @Import(SuhLoggerConfiguration.class)
    static class AppConfig {
        @Bean Greeter greeter() { return new Greeter(); }

        @Bean SuhLoggerProperties suhLoggerProperties() {
            SuhLoggerProperties p = new SuhLoggerProperties();
            p.getMasking().setEnabled(true);
            p.getMasking().setMaskFields(List.of("password"));
            p.getHeader().setEnabled(true);
            p.getHeader().setIncludeAll(true);
            return p;
        }

        @Bean RequestContextAccessor accessor() {
            return () -> new RequestSnapshot("GET", "/hello", Map.of("X-Trace", "t1"), null);
        }
    }

    @Test
    void annotatedMethodIsLoggedAndMasked() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
             LogCapture capture = LogCapture.start()) {
            String result = ctx.getBean(Greeter.class).greet("suh", "p@ss");
            assertEquals("hi suh", result);
            String text = capture.text();
            assertTrue(text.contains("[Greeter.greet] CALL"), text);
            assertTrue(text.contains("suh"), text);
            assertFalse(text.contains("p@ss"), "password must be masked\n" + text);
            assertTrue(text.contains("/hello"), "request info from accessor\n" + text);
            assertTrue(text.contains("[TIME]: Greeter.greet"), text);
        }
    }

    @Test
    void resultLoggingWorksWithoutResponseEntityCheck() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class);
             LogCapture capture = LogCapture.start()) {
            ctx.getBean(Greeter.class).returnsMap();
            assertTrue(capture.text().contains("\"ok\": 1"), capture.text());
        }
    }

    @Test
    void worksWithoutPropertiesOrAccessorBeans() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(SuhLoggerConfiguration.class);
            ctx.registerBean(Greeter.class);
            ctx.refresh();
            try (LogCapture capture = LogCapture.start()) {
                ctx.getBean(Greeter.class).greet("a", "b");
                assertTrue(capture.text().contains("[Greeter.greet] CALL"), capture.text());
            }
        }
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :suh-logger-spring:test`
Expected: 컴파일 실패 (`SuhLoggerConfiguration` 없음)

- [ ] **Step 3: aspect 이전과 수정**

```bash
mkdir -p suh-logger-spring/src/main/java/kr/suhsaechan/suhlogger/{aspect,spring,internal/spring}
mv src/main/java/kr/suhsaechan/suhlogger/aspect/*.java suh-logger-spring/src/main/java/kr/suhsaechan/suhlogger/aspect/
```

`SuhExecutionTimeLoggingAspect.java`: `@Component`, `@Autowired` 필드와 해당 import 제거, 생성자 추가:
```java
  private final SuhLoggerProperties properties;

  // 컴포넌트 스캔 대신 설정 클래스가 명시적으로 등록한다 (사용자 패키지 스캔 오염 방지)
  public SuhExecutionTimeLoggingAspect(SuhLoggerProperties properties) {
    this.properties = properties;
  }
```

`SuhMethodInvocationLoggingAspect.java`:
1. `import jakarta.servlet.http.HttpServletRequest;`, `RequestContextHolder`, `ServletRequestAttributes`, `ResponseEntity`, `Autowired`, `Component` import 제거, `@Component` 제거.
2. 필드·생성자:
```java
  private final SuhLoggerProperties properties;
  private final RequestContextAccessor requestContextAccessor;

  public SuhMethodInvocationLoggingAspect(SuhLoggerProperties properties, RequestContextAccessor requestContextAccessor) {
    this.properties = properties;
    this.requestContextAccessor = requestContextAccessor != null ? requestContextAccessor : RequestContextAccessor.NONE;
  }
```
3. `extractHttpRequestInfo()` 본문 교체:
```java
  private Map<String, Object> extractHttpRequestInfo() {
    Map<String, Object> httpInfo = new HashMap<>();
    RequestSnapshot request;
    try {
      request = requestContextAccessor.current();
    } catch (RuntimeException e) {
      // 요청 정보 조회 실패는 로깅 생략 사유일 뿐 호출을 막지 않는다
      return httpInfo;
    }
    if (request == null) {
      return httpInfo;
    }
    httpInfo.put("method", request.getMethod());
    httpInfo.put("URI", request.getUri());
    Map<String, String> filteredHeaders = filterHeaders(request.getHeaders());
    if (!filteredHeaders.isEmpty()) {
      httpInfo.put("headers", filteredHeaders);
    }
    if (request.getRequestId() != null) {
      httpInfo.put("requestId", request.getRequestId());
    }
    return httpInfo;
  }
```
4. `logResultSafely`의 `if (result instanceof ResponseEntity)` 블록을 교체:
```java
      // spring-web이 없는 앱(배치)에서도 클래스 로딩이 깨지지 않도록 이름으로 먼저 판별한다
      if (ResponseEntityResults.isResponseEntity(result)) {
        Map<String, Object> safeResponse = ResponseEntityResults.toSafeMap(result, this::maskSensitiveHeaders,
            this::isComplexObject);
        SuhLogger.superLog(safeResponse, false);
      } else {
```

`internal/spring/ResponseEntityResults.java`
```java
package kr.suhsaechan.suhlogger.internal.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.http.ResponseEntity;

/**
 * ResponseEntity 전용 처리. spring-web이 classpath에 있을 때만 이 클래스가 로드되도록
 * 호출부는 isResponseEntity(이름 비교)를 먼저 거친다.
 */
public final class ResponseEntityResults {

    private static final String RESPONSE_ENTITY = "org.springframework.http.ResponseEntity";

    private ResponseEntityResults() {
    }

    public static boolean isResponseEntity(Object result) {
        for (Class<?> c = result.getClass(); c != null; c = c.getSuperclass()) {
            if (RESPONSE_ENTITY.equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, Object> toSafeMap(Object result,
                                                Function<Map<String, String>, Map<String, String>> headerMasker,
                                                Predicate<Object> isComplex) {
        ResponseEntity<?> entity = (ResponseEntity<?>) result;
        Map<String, Object> safe = new HashMap<>();
        safe.put("statusCode", entity.getStatusCode().toString());
        safe.put("statusCodeValue", entity.getStatusCode().value());
        safe.put("headers", headerMasker.apply(entity.getHeaders().toSingleValueMap()));
        Object body = entity.getBody();
        if (body != null) {
            if (isComplex.test(body)) {
                safe.put("bodyType", body.getClass().getSimpleName());
                safe.put("bodyInfo", "Complex object - logged separately by filter");
            } else {
                safe.put("body", body);
            }
        }
        return safe;
    }
}
```

`maskSensitiveHeaders`·`isComplexObject`는 private → 메서드 참조가 같은 클래스 안이라 그대로 쓴다.

`spring/SuhLoggerConfiguration.java`
```java
package kr.suhsaechan.suhlogger.spring;

import kr.suhsaechan.suhlogger.aspect.SuhExecutionTimeLoggingAspect;
import kr.suhsaechan.suhlogger.aspect.SuhMethodInvocationLoggingAspect;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.util.SuhLogger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Boot 없는 Spring(JavaConfig·XML)용 진입점. {@code @Import(SuhLoggerConfiguration.class)} 또는
 * {@code <bean class="kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration"/>} 한 줄로 켠다.
 * Boot 자동설정도 이 클래스를 재사용해 등록 규칙이 한 곳에만 있게 한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy
public class SuhLoggerConfiguration {

    @Bean
    public SuhExecutionTimeLoggingAspect suhExecutionTimeLoggingAspect(ObjectProvider<SuhLoggerProperties> properties) {
        return new SuhExecutionTimeLoggingAspect(properties.getIfAvailable(SuhLoggerProperties::new));
    }

    @Bean
    public SuhMethodInvocationLoggingAspect suhMethodInvocationLoggingAspect(
            ObjectProvider<SuhLoggerProperties> properties, ObjectProvider<RequestContextAccessor> accessor) {
        return new SuhMethodInvocationLoggingAspect(properties.getIfAvailable(SuhLoggerProperties::new),
                accessor.getIfAvailable(() -> RequestContextAccessor.NONE));
    }

    @Bean
    public SuhLoggerInitializer suhLoggerInitializer(ObjectProvider<SuhLoggerProperties> properties) {
        return new SuhLoggerInitializer(properties.getIfAvailable(SuhLoggerProperties::new));
    }

    /** 정적 SuhLogger API가 컨텍스트 설정(마스킹·제외 클래스)을 쓰도록 주입 */
    public static class SuhLoggerInitializer {
        public SuhLoggerInitializer(SuhLoggerProperties properties) {
            SuhLogger.setProperties(properties);
        }
    }
}
```

주의: 같은 컨텍스트에서 `SuhLoggerProperties` 빈이 없으면 aspect마다 `new SuhLoggerProperties()`가 따로 생긴다. 기본값만 쓰는 경우라 동작 차이는 없다.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :suh-logger-spring:test`
Expected: 3 tests PASS

- [ ] **Step 5: 커밋** — `/pro-commit` (경로: `suh-logger-spring/`, 삭제된 `src/main/java/.../aspect`)

---

### Task 4: servlet 모듈 — 필터 이전과 요청 정보 어댑터

**Files:**
- Move+Modify: `src/main/java/kr/suhsaechan/suhlogger/filter/SuhLoggingFilter.java` → `suh-logger-servlet/.../filter/`
- Create: `suh-logger-servlet/src/main/java/kr/suhsaechan/suhlogger/servlet/ServletRequestContextAccessor.java`
- Test: `suh-logger-servlet/src/test/java/kr/suhsaechan/suhlogger/filter/SuhLoggingFilterTest.java`, `.../servlet/ServletRequestContextAccessorTest.java`

**Interfaces:**
- Consumes: `SuhLoggerProperties`, `RequestContextAccessor`, `RequestSnapshot`
- Produces: `SuhLoggingFilter(SuhLoggerProperties)` (기존 시그니처 유지), `public class ServletRequestContextAccessor implements RequestContextAccessor` (기본 생성자)

- [ ] **Step 1: 실패하는 테스트**

`SuhLoggingFilterTest.java`
```java
package kr.suhsaechan.suhlogger.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SuhLoggingFilterTest {

    private MockHttpServletResponse run(SuhLoggerProperties props, String uri, String body) throws Exception {
        SuhLoggingFilter filter = new SuhLoggingFilter(props);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void service(jakarta.servlet.http.HttpServletRequest rq, jakarta.servlet.http.HttpServletResponse rs)
                    throws java.io.IOException {
                rs.setStatus(200);
                rs.setContentType("application/json");
                rs.getWriter().write(body);
            }
        }));
        return res;
    }

    @Test
    void logsResponseAndKeepsBodyForClient() throws Exception {
        try (LogCapture capture = LogCapture.start()) {
            MockHttpServletResponse res = run(new SuhLoggerProperties(), "/api/x", "{\"a\":1}");
            assertEquals("{\"a\":1}", res.getContentAsString(), "body must still reach the client");
            assertTrue(capture.text().contains("URI: /api/x"), capture.text());
            assertTrue(capture.text().contains("Response Body: {\"a\":1}"), capture.text());
        }
    }

    @Test
    void excludePatternSkipsLogging() throws Exception {
        SuhLoggerProperties props = new SuhLoggerProperties();
        props.setExcludePatterns(List.of("/health"));
        try (LogCapture capture = LogCapture.start()) {
            run(props, "/health", "{}");
            assertFalse(capture.text().contains("RESPONSE LOGGING"), capture.text());
        }
    }

    @Test
    void prettyPrintUsesJacksonWhenPresent() throws Exception {
        SuhLoggerProperties props = new SuhLoggerProperties();
        props.setPrettyPrintJson(true);
        try (LogCapture capture = LogCapture.start()) {
            run(props, "/api/x", "{\"a\":1}");
            assertTrue(capture.text().contains("\"a\" : 1"), capture.text());
        }
    }
}
```

`ServletRequestContextAccessorTest.java`
```java
package kr.suhsaechan.suhlogger.servlet;

import static org.junit.jupiter.api.Assertions.*;

import kr.suhsaechan.suhlogger.spi.RequestSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ServletRequestContextAccessorTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsNullOutsideRequest() {
        assertNull(new ServletRequestContextAccessor().current());
    }

    @Test
    void readsCurrentRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/login");
        req.addHeader("X-Trace", "t1");
        req.setAttribute("RequestID", "r-1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        RequestSnapshot snap = new ServletRequestContextAccessor().current();
        assertEquals("POST", snap.getMethod());
        assertEquals("/login", snap.getUri());
        assertEquals("t1", snap.getHeaders().get("X-Trace"));
        assertEquals("r-1", snap.getRequestId());
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :suh-logger-servlet:test`
Expected: 컴파일 실패

- [ ] **Step 3: 이전과 구현**

```bash
mkdir -p suh-logger-servlet/src/main/java/kr/suhsaechan/suhlogger/{filter,servlet}
mv src/main/java/kr/suhsaechan/suhlogger/filter/SuhLoggingFilter.java suh-logger-servlet/src/main/java/kr/suhsaechan/suhlogger/filter/
```

`SuhLoggingFilter.java` 수정 — Jackson을 선택 의존으로:
1. `import com.fasterxml.jackson.databind.ObjectMapper;` 제거, 필드 `private final ObjectMapper objectMapper;`와 생성자 안 `new ObjectMapper()` 제거.
2. `formatResponseBody`의 try 블록 교체:
```java
        try {
            return PrettyJson.format(responseBody);
        } catch (Exception | LinkageError e) {
            // JSON이 아니거나 Jackson이 classpath에 없으면 원본 그대로
            return responseBody;
        }
```
3. 파일 하단에 private static nested class 추가:
```java
    /** Jackson이 있을 때만 로드되는 holder — 없으면 NoClassDefFoundError를 위에서 잡는다 */
    private static final class PrettyJson {
        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
                new com.fasterxml.jackson.databind.ObjectMapper();

        static String format(String json) throws Exception {
            Object tree = MAPPER.readValue(json, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        }
    }
```

`servlet/ServletRequestContextAccessor.java`
```java
package kr.suhsaechan.suhlogger.servlet;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.RequestSnapshot;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Servlet 환경의 현재 요청을 RequestSnapshot으로 변환한다 (Spring MVC가 바인딩한 RequestContextHolder 사용) */
public class ServletRequestContextAccessor implements RequestContextAccessor {

    @Override
    public RequestSnapshot current() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        Object requestId = request.getAttribute("RequestID");
        return new RequestSnapshot(request.getMethod(), request.getRequestURI(), headers,
                requestId != null ? requestId.toString() : null);
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :suh-logger-servlet:test`
Expected: 5 tests PASS

- [ ] **Step 5: 커밋** — `/pro-commit` (경로: `suh-logger-servlet/`, 삭제된 `src/main/java/.../filter`)

---

### Task 5: Boot 자동설정·starter·legacy·bom, 라이브러리 위생 정리

**Files:**
- Create: `suh-logger-spring-boot-autoconfigure/src/main/java/kr/suhsaechan/suhlogger/boot/autoconfigure/{SuhLoggerAutoConfiguration,SuhLoggerServletAutoConfiguration}.java`
- Create: `suh-logger-spring-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Move+Modify: `src/test/java/kr/suhsaechan/suhlogger/config/AutoConfigurationCompatibilityTest.java` → autoconfigure 테스트 (`boot/autoconfigure` 패키지)
- Test: `.../boot/autoconfigure/SuhLoggerAutoConfigurationTest.java`
- Delete: `src/main/java/kr/suhsaechan/suhlogger/SuhLoggerApplication.java`, `src/main/java/kr/suhsaechan/suhlogger/config/SuhLoggerAutoConfiguration.java`, `src/main/resources/` 전체, `src/test/java/kr/suhsaechan/suhlogger/SuhLoggerApplicationTests.java`, 남은 `src/` 디렉터리

**Interfaces:**
- Consumes: `SuhLoggerConfiguration`, `SuhLoggingFilter`, `ServletRequestContextAccessor`
- Produces: 빈 이름 `suhLoggerProperties`, `suhLoggingFilter`, `suhLoggingFilterRegistration`, `servletRequestContextAccessor`

- [ ] **Step 1: 실패하는 테스트** — `SuhLoggerAutoConfigurationTest.java`

```java
package kr.suhsaechan.suhlogger.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import kr.suhsaechan.suhlogger.aspect.SuhMethodInvocationLoggingAspect;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.filter.SuhLoggingFilter;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class SuhLoggerAutoConfigurationTest {

    private final AutoConfigurations configs = AutoConfigurations.of(
            SuhLoggerAutoConfiguration.class, SuhLoggerServletAutoConfiguration.class);

    @Test
    void servletWebContextRegistersFilterAndAccessor() {
        new WebApplicationContextRunner().withConfiguration(configs).run(ctx -> {
            assertThat(ctx).hasSingleBean(SuhLoggerProperties.class);
            assertThat(ctx).hasSingleBean(SuhMethodInvocationLoggingAspect.class);
            assertThat(ctx).hasSingleBean(SuhLoggingFilter.class);
            assertThat(ctx).hasBean("suhLoggingFilterRegistration");
            assertThat(ctx.getBean(RequestContextAccessor.class).getClass().getSimpleName())
                    .isEqualTo("ServletRequestContextAccessor");
        });
    }

    @Test
    void nonWebContextHasAspectsButNoFilter() {
        new ApplicationContextRunner().withConfiguration(configs).run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(SuhMethodInvocationLoggingAspect.class);
            assertThat(ctx).doesNotHaveBean(SuhLoggingFilter.class);
            assertThat(ctx).doesNotHaveBean(FilterRegistrationBean.class);
        });
    }

    @Test
    void contextStartsWhenServletApiIsMissing() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("jakarta.servlet", "kr.suhsaechan.suhlogger.filter",
                        "kr.suhsaechan.suhlogger.servlet"))
                .withConfiguration(configs)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(SuhMethodInvocationLoggingAspect.class);
                });
    }

    @Test
    void propertiesAreBound() {
        new ApplicationContextRunner().withConfiguration(configs)
                .withPropertyValues("suh-logger.masking.enabled=true", "suh-logger.masking.mask-fields=password,token",
                        "suh-logger.exclude-patterns=/health")
                .run(ctx -> {
                    SuhLoggerProperties p = ctx.getBean(SuhLoggerProperties.class);
                    assertThat(p.getMasking().isEnabled()).isTrue();
                    assertThat(p.getMasking().getMaskFields()).containsExactly("password", "token");
                    assertThat(p.getExcludePatterns()).containsExactly("/health");
                });
    }

    @Test
    void userDefinedPropertiesBeanWins() {
        new ApplicationContextRunner().withConfiguration(configs)
                .withBean("myProps", SuhLoggerProperties.class, () -> {
                    SuhLoggerProperties p = new SuhLoggerProperties();
                    p.setMaxResponseBodySize(7);
                    return p;
                })
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(SuhLoggerProperties.class);
                    assertThat(ctx.getBean(SuhLoggerProperties.class).getMaxResponseBodySize()).isEqualTo(7);
                });
    }

    @Test
    void disabledFlagStillStartsContext() {
        new WebApplicationContextRunner().withConfiguration(configs)
                .withPropertyValues("suh-logger.enabled=false")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }
}
```

`AutoConfigurationCompatibilityTest.java`를 `boot/autoconfigure` 패키지로 옮기고 package 선언만 `kr.suhsaechan.suhlogger.boot.autoconfigure`로 바꾼다 (검증 대상 클래스 이름은 같다).

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :suh-logger-spring-boot-autoconfigure:test`
Expected: 컴파일 실패

- [ ] **Step 3: 자동설정 구현**

`SuhLoggerAutoConfiguration.java`
```java
package kr.suhsaechan.suhlogger.boot.autoconfigure;

import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * suh-logger Boot 자동설정 (Boot 3.x·4.x).
 * 자동설정 순서는 클래스 직접 참조 대신 이름으로 지정한다 — Boot 4에서 패키지가 바뀌어도 로딩이 깨지지 않게.
 */
@AutoConfiguration(
        afterName = {
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",   // Boot 3.x
                "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration"           // Boot 4.x
        },
        beforeName = {
                "org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration", // Boot 3.x
                "org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration"       // Boot 4.x
        })
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@Import(SuhLoggerConfiguration.class)
public class SuhLoggerAutoConfiguration {

    /** core의 POJO를 그대로 바인딩 — core가 Boot 어노테이션에 의존하지 않게 하려고 메서드에 붙인다 */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "suh-logger")
    public SuhLoggerProperties suhLoggerProperties() {
        return new SuhLoggerProperties();
    }
}
```

`AutoConfigurationCompatibilityTest`는 `@AutoConfigureAfter`/`@AutoConfigureBefore` 어노테이션을 찾는다. `@AutoConfiguration(afterName=...)`은 메타 어노테이션이라 `getAnnotation(AutoConfigureAfter.class)`로 안 잡힐 수 있다. 그 경우 테스트를 `AnnotatedElementUtils.findMergedAnnotation(SuhLoggerAutoConfiguration.class, AutoConfigureAfter.class)`로 바꾼다 (검증 의도 — 이름 기반·3.x/4.x 경로 포함 — 는 그대로).

`SuhLoggerServletAutoConfiguration.java`
```java
package kr.suhsaechan.suhlogger.boot.autoconfigure;

import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.filter.SuhLoggingFilter;
import kr.suhsaechan.suhlogger.servlet.ServletRequestContextAccessor;
import kr.suhsaechan.suhlogger.spi.RequestContextAccessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/** Servlet 웹 앱에서만 필터와 요청 정보 어댑터를 등록한다 (non-web·WebFlux에서는 건너뜀) */
@AutoConfiguration(before = SuhLoggerAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = {"jakarta.servlet.Filter", "kr.suhsaechan.suhlogger.filter.SuhLoggingFilter"})
public class SuhLoggerServletAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RequestContextAccessor.class)
    public RequestContextAccessor servletRequestContextAccessor() {
        return new ServletRequestContextAccessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public SuhLoggingFilter suhLoggingFilter(SuhLoggerProperties properties) {
        return new SuhLoggingFilter(properties);
    }

    @Bean
    public FilterRegistrationBean<SuhLoggingFilter> suhLoggingFilterRegistration(SuhLoggingFilter filter) {
        FilterRegistrationBean<SuhLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setName("suhLoggingFilter");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}
```

Servlet 자동설정이 `SuhLoggerAutoConfiguration` 앞에 와야 aspect 생성 시점에 `RequestContextAccessor` 빈 정의가 이미 있다 (`ObjectProvider`라 순서가 달라도 런타임 조회는 되지만, 명시해 둔다). `suhLoggingFilter`는 `SuhLoggerProperties` 빈을 쓰므로 servlet 자동설정만 단독으로 뜨는 경우는 없다 — 두 클래스는 imports 파일에 함께 등록한다.

`AutoConfiguration.imports`
```
kr.suhsaechan.suhlogger.boot.autoconfigure.SuhLoggerServletAutoConfiguration
kr.suhsaechan.suhlogger.boot.autoconfigure.SuhLoggerAutoConfiguration
```

- [ ] **Step 4: 위생 정리 (삭제)**

```bash
rm src/main/java/kr/suhsaechan/suhlogger/SuhLoggerApplication.java
rm src/main/java/kr/suhsaechan/suhlogger/config/SuhLoggerAutoConfiguration.java
rm -r src/main/resources
rm src/test/java/kr/suhsaechan/suhlogger/SuhLoggerApplicationTests.java
mv src/test/java/kr/suhsaechan/suhlogger/config/AutoConfigurationCompatibilityTest.java \
   suh-logger-spring-boot-autoconfigure/src/test/java/kr/suhsaechan/suhlogger/boot/autoconfigure/
find src -type f   # 남은 파일이 없어야 한다
rm -r src
```

삭제 사유(커밋 메시지·마이그레이션 가이드에 적는다): 라이브러리 jar 안의 `@SpringBootApplication`·`application.properties`는 사용자 앱과 충돌하고, `spring.factories`는 Boot 3+에서 읽지 않는다.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :suh-logger-spring-boot-autoconfigure:test`
Expected: 8 tests PASS (신규 6 + 호환 2)

- [ ] **Step 6: 커밋** — `/pro-commit` (경로: `suh-logger-spring-boot-autoconfigure/`, `suh-logger-spring-boot-starter/`, `suh-logger-legacy/build.gradle`, `suh-logger-bom/`, 삭제된 `src/`)

---

### Task 6: 2.x 호환 E2E (모의 소비자 앱)

**Files:**
- Test: `suh-logger-legacy/src/test/java/kr/suhsaechan/suhlogger/e2e/Legacy2xCompatibilityE2ETest.java`
- Test: `suh-logger-legacy/src/test/java/kr/suhsaechan/suhlogger/e2e/ConsumerApp.java`

**Interfaces:**
- Consumes: 기존 좌표 모듈(`suh-logger-legacy`)의 전이 의존만 — 2.x 사용자가 보는 것과 같다.

- [ ] **Step 1: 소비자 앱과 E2E 테스트 작성**

`ConsumerApp.java`
```java
package kr.suhsaechan.suhlogger.e2e;

import java.util.Map;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 2.x README 사용법 그대로 작성한 모의 소비자 앱 */
@SpringBootApplication
public class ConsumerApp {

    @RestController
    public static class LoginController {
        @LogMonitor
        @PostMapping("/api/login")
        public Map<String, String> login(@RequestBody Map<String, String> body) {
            return Map.of("user", body.get("username"), "status", "ok");
        }
    }
}
```

`Legacy2xCompatibilityE2ETest.java`
```java
package kr.suhsaechan.suhlogger.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/** 실제 서버를 띄워 HTTP로 호출 — 2.x 좌표·import·프로퍼티가 3.0 구조에서 그대로 동작하는지 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = ConsumerApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "suh-logger.masking.enabled=true",
                "suh-logger.masking.mask-fields=body",
                "suh-logger.exclude-patterns=/actuator"
        })
class Legacy2xCompatibilityE2ETest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void annotationAndFilterWorkEndToEnd(CapturedOutput output) {
        ResponseEntity<Map> res = rest.postForEntity("/api/login",
                Map.of("username", "suh", "password", "p@ss"), Map.class);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).containsEntry("status", "ok");
        // @LogMonitor 호출·시간 로그
        assertThat(output.getOut()).contains("[LoginController.login] CALL").contains("[TIME]: LoginController.login");
        // 파라미터 이름 body가 mask-fields에 걸려 비밀번호 원문이 남지 않는다
        assertThat(output.getOut()).doesNotContain("p@ss");
        // Servlet 필터의 응답 로깅
        assertThat(output.getOut()).contains("RESPONSE LOGGING").contains("URI: /api/login");
    }
}
```

참고: 2.x 마스킹은 파라미터 **이름** 기준이라 `mask-fields=password`로는 `body` 맵 안의 `password`가 가려지지 않는다 (중첩 마스킹은 #55 범위). 그래서 이 E2E는 2.x 동작 그대로 `body`를 마스킹 키로 쓴다.

- [ ] **Step 2: 실행**

Run: `./gradlew :suh-logger-legacy:test`
Expected: 1 test PASS

실패 시 확인 순서: (1) 로그가 `OutputCaptureExtension`에 안 잡히면 logback 콘솔 appender가 켜져 있는지 (`spring-boot-starter-test`가 logback을 가져온다) (2) 필터 로그가 없으면 `SuhLoggerServletAutoConfiguration` 조건 확인.

- [ ] **Step 3: 전체 빌드**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL, 모든 모듈 테스트 PASS

- [ ] **Step 4: 배포 산출물 확인**

Run: `./gradlew publishToMavenLocal && ls ~/.m2/repository/kr/suhsaechan/`
Expected: `suh-logger`, `suh-logger-core`, `suh-logger-spring`, `suh-logger-servlet`, `suh-logger-spring-boot-autoconfigure`, `suh-logger-spring-boot-starter`, `suh-logger-bom` 디렉터리

Run: `unzip -l ~/.m2/repository/kr/suhsaechan/suh-logger-core/2.0.3/suh-logger-core-2.0.3.jar | grep -E 'application.properties|SuhLoggerApplication|spring.factories'`
Expected: 출력 없음

- [ ] **Step 5: 커밋** — `/pro-commit` (경로: `suh-logger-legacy/src/`)

---

## Self-Review 기록

- Spec §2 모듈 중 webflux·json-jackson2/3·test-kit은 #58·#59 범위라 이 계획에 없다 (의도).
- Spec §3 LogEvent 파이프라인은 #55·#56 계획에서 도입한다 (spec §11 갱신 반영).
- Spec §5 `internal` 패키지·`@Incubating` → Task 2. japicmp·ArchUnit → #59.
- Spec §6 설정 모델 → Task 2(POJO 유지)·Task 5(`@Bean @ConfigurationProperties`).
- 타입 일관성: `RequestContextAccessor.current()`·`RequestSnapshot` getter 이름이 Task 2·3·4에서 같다. `SuhLoggerConfiguration.SuhLoggerInitializer`는 기존 `SuhLoggerAutoConfiguration.SuhLoggerInitializer`를 대체한다 (FQN 변경 — 마이그레이션 가이드 기재 대상).
