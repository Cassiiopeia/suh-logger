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

suh-logger는 Java Util Logging(JUL)을 사용하며 SLF4J와 독립적입니다.
SLF4J 관련 경고가 발생해도 정상 동작합니다.

### Logback 충돌

suh-logger는 Logback을 사용하지 않습니다. 충돌이 발생하면 다음을 확인하세요:

```groovy
dependencies {
    implementation('kr.suhsaechan:suh-logger:x.x.x') { // 최신 버전으로 변경하세요
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging'
    }
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

운영 환경에서는 INFO 이상 레벨만 출력하도록 설정:

```java
SuhLogger.setLogLevel(SuhLogger.LogLevel.INFO);
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

네, suh-logger는 Java Util Logging(JUL)을 사용하므로 SLF4J 의존성이 필요 없습니다.

### Q: Logback 설정이 적용되나요?

아니요, suh-logger는 독립적인 로깅 시스템을 사용합니다.

### Q: 파일 로깅은 어떻게 하나요?

```java
SuhLogger.addFileLogger("/var/log/myapp/app.log");
```

### Q: 로그 포맷을 변경할 수 있나요?

현재는 기본 포맷만 지원합니다. 향후 버전에서 커스터마이징 지원 예정입니다.
