# 문제 해결 가이드

suh-logger 사용 중 발생할 수 있는 문제와 해결 방법을 설명합니다.

## 로그 출력이 안 될 때

### 1. 로깅이 비활성화되어 있는지 확인

```yaml
suh-logger:
  enabled: true  # false이면 모든 로깅 비활성화
```

### 2. AOP 제약사항 확인

- Spring Bean으로 등록된 클래스의 **public 메서드**에서만 어노테이션이 동작합니다.
- private 메서드나 동일 클래스 내 호출에서는 AOP가 적용되지 않습니다.

```java
@Service
public class MyService {

    @LogMonitor  // ✅ 외부 호출 시 동작
    public void publicMethod() {
        internalMethod();  // ❌ 내부 호출 - AOP 미적용
    }

    @LogMonitor  // ❌ private - AOP 미적용
    private void internalMethod() { ... }
}
```

### 3. 컴포넌트 스캔 확인

suh-logger의 컴포넌트가 스캔되고 있는지 확인하세요.

```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.example", "kr.suhsaechan.suhlogger"})
public class Application { ... }
```

## 의존성 충돌

### SLF4J 관련 경고

suh-logger는 SLF4J에 위임하므로, 상위 프로젝트의 SLF4J 구현체(스프링부트 기본 Logback 등)를 그대로 사용합니다.
`SLF4J: No providers were found` 경고가 뜬다면 상위 프로젝트에 SLF4J 구현체(예: `spring-boot-starter`)가 없는 것이므로 이를 추가하세요.

### Logback 충돌

suh-logger는 상위 프로젝트의 Logback을 그대로 따르며, 로깅 의존성을 강제 주입하거나 exclude하지 않습니다.
따라서 별도의 `exclude` 설정 없이 의존성만 추가하면 됩니다:

```groovy
dependencies {
    implementation 'kr.suhsaechan:suh-logger:x.x.x' // 최신 버전으로 변경하세요
}
```

## JSON 직렬화 에러

### 순환 참조 에러

JTS Geometry, JPA Entity 등 순환 참조가 있는 객체는 `CommonUtil.makeSafeForSerialization()`을 사용하세요.

```java
Object safe = CommonUtil.makeSafeForSerialization(complexObject);
SuhLogger.superLog(safe);
```

### 특정 클래스 직렬화 제외

```yaml
suh-logger:
  excluded-classes:
    - "org.locationtech.jts.geom.Point"
    - "com.example.InternalObject"
```

### MultipartFile 로깅

MultipartFile은 자동으로 메타데이터만 추출됩니다:

```json
{
  "_type": "MultipartFile",
  "fileName": "test.pdf",
  "contentType": "application/pdf",
  "size": 1024
}
```

## 헤더가 출력되지 않을 때

### 1. 전역 설정 확인

```yaml
suh-logger:
  header:
    enabled: true  # false이면 헤더 미출력
```

### 2. 어노테이션 설정 확인

```java
@LogMonitor(header = ON)  // 강제 출력
```

### 3. 웹 환경 확인

헤더 로깅은 웹 환경(HttpServletRequest 존재)에서만 동작합니다.
배치 작업이나 테스트 환경에서는 헤더가 출력되지 않습니다.

## 마스킹이 동작하지 않을 때

### 1. 마스킹 활성화 확인

```yaml
suh-logger:
  masking:
    enabled: true  # false이면 마스킹 비활성화
```

### 2. 키워드 확인

키워드가 필드명에 **포함**되어야 합니다 (대소문자 무시):

```yaml
suh-logger:
  masking:
    mask-fields:
      - password  # "password", "userPassword", "PASSWORD" 모두 매칭
```

### 3. 어노테이션 우선순위

```java
@LogMonitor(mask = OFF)  // 전역 설정과 관계없이 마스킹 비활성화
```

## 성능 문제

### 로그 레벨 조정

운영 환경에서는 상위 프로젝트의 로깅 설정으로 suh-logger 출력 레벨을 조정하세요:

```yaml
# application.yml
logging:
  level:
    kr.suhsaechan.suhlogger: INFO
```

### 불필요한 로깅 제외

```yaml
suh-logger:
  exclude-patterns:
    - "/actuator"
    - "/health"
```

### 응답 본문 크기 제한

```yaml
suh-logger:
  max-response-body-size: 1024  # 1KB로 제한
```

## Spring Boot 버전 호환성

suh-logger는 Spring Boot 3.x와 4.x 모두 지원합니다.

| suh-logger 버전 | Spring Boot |
|-----------------|-------------|
| 1.5.0 이상 | 3.x, 4.x |
| 1.4.x 이하 | 3.x 전용 |

## 자주 묻는 질문

### Q: SLF4J 없이도 동작하나요?

suh-logger는 SLF4J에 위임하므로 상위 프로젝트에 SLF4J 구현체가 필요합니다. 스프링부트 스타터(`spring-boot-starter`)를 쓰면 Logback이 기본 포함되므로 별도 설정이 필요 없습니다.

### Q: Logback 설정이 적용되나요?

네. suh-logger 출력도 상위 프로젝트의 Logback 패턴(`logging.pattern.*`)·레벨(`logging.level.*`)·appender를 그대로 따릅니다. 상위 프로젝트의 `@Slf4j` 로그와 동일한 파이프라인을 공유합니다.

### Q: 파일 로깅은 어떻게 하나요?

상위 프로젝트의 logback appender로 설정합니다. suh-logger 로그도 같은 appender를 타므로 함께 파일에 기록됩니다.

```xml
<!-- logback-spring.xml -->
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>/var/log/myapp/app.log</file>
    <encoder><pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{40} - %msg%n</pattern></encoder>
</appender>
```

### Q: 로그 포맷을 변경할 수 있나요?

네. suh-logger는 SLF4J에 위임하므로 상위 프로젝트의 `logging.pattern.console` / `logging.pattern.file` 설정(또는 `logback-spring.xml`)을 그대로 따릅니다.

## 3.0에서 자주 묻는 것

### 값이 `****`로 나와요

3.0부터 마스킹이 기본으로 켜져 있습니다. 키 이름에 `token`, `password`, `secret` 등이 들어가면 가려집니다. 특정 메서드만 원본이 필요하면 `@LogCall(mask = TriState.OFF)`를 쓰고, 전역 설정은 [마스킹 가이드](masking.md)를 보세요.

### `/actuator/health` 요청이 로그에 안 보여요

`/actuator/**`가 기본 제외입니다. 보려면 `suh-logger.exclude-patterns`를 직접 지정하세요 (지정하면 기본 목록을 대체합니다).

### `exclude pattern '...' uses legacy contains matching` 경고

와일드카드가 없는 제외 패턴은 2.x 방식(포함 비교)으로 동작하며 다른 경로까지 제외할 수 있습니다. `/api/auth/login/**`처럼 Ant 패턴으로 바꾸세요.

### Kotlin에서 `@LogMonitor`를 붙였는데 로그가 안 나와요

Kotlin 클래스·메서드는 기본 `final`이라 Spring이 프록시를 만들 수 없습니다. 기동 로그에 `[suh-logger] ... is final` 경고가 있는지 보고, `org.jetbrains.kotlin.plugin.spring` 플러그인을 적용하세요.

### `masking is disabled while response bodies are logged` 경고

`masking.enabled=false`인데 응답 본문을 남기고 있습니다. 토큰·개인정보가 평문으로 남을 수 있습니다. 의도한 것이 아니면 마스킹을 켜거나 `response-body: none`으로 바꾸세요.

### 4xx·5xx 응답이 로그에 나와요

3.0부터 에러 응답도 기록합니다 (5xx는 WARN). 본문만 숨기려면 `response-body: none`을 쓰세요.
