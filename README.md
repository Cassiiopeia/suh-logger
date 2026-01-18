<div align="center">

# SUH Logger

**Spring Boot 로깅, 어노테이션 하나면 끝**

<!-- 수정하지마세요 자동으로 동기화 됩니다 -->
## 최신 버전 : v1.5.0

[![Nexus](https://img.shields.io/badge/Nexus-버전_목록-4E9BCD?style=flat-square&logo=sonatype&logoColor=white)](https://nexus.suhsaechan.kr/#browse/browse:maven-releases:kr%2Fsuhsaechan%2Fsuh-logger)
[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x%20/%204.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

[빠른 시작](#-빠른-시작) • [어노테이션](#-어노테이션) • [설정](#%EF%B8%8F-설정) • [상세 문서](#-상세-문서)

</div>

---

## 왜 SUH-LOGGER인가?

| 기존 방식 | SUH-LOGGER |
|----------|------------|
| System.out.println 디버깅 | 구조화된 JSON 로깅 |
| 수동 실행 시간 측정 | `@LogTime` 자동 측정 |
| 복잡한 로깅 설정 | Zero Configuration |
| 순환 참조 에러 | 안전한 직렬화 |
| SLF4J/Logback 충돌 | 독립적 JUL 기반 |

```java
@LogMonitor
public ResponseDto processRequest(RequestDto request) {
    return service.process(request);
}
// → 파라미터, 반환값, 실행시간 자동 로깅!
```

---

## 빠른 시작

### 1. 의존성 추가

**Gradle**
```groovy
repositories {
    mavenCentral()
    maven { url "https://nexus.suhsaechan.kr/repository/maven-releases/" }
}

dependencies {
    implementation 'kr.suhsaechan:suh-logger:x.x.x' // 최신 버전으로 변경하세요
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
    <artifactId>suh-logger</artifactId>
    <version>x.x.x</version> <!-- 최신 버전으로 변경하세요 -->
</dependency>
```

### 2. 바로 사용

```java
@Service
public class ProductService {

    @LogMonitor  // 파라미터 + 반환값 + 실행시간 자동 로깅
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByKeyword(keyword);
    }
}
```

**끝!** Spring Boot Auto Configuration으로 별도 설정 없이 바로 사용 가능합니다.

---

## 어노테이션

| 어노테이션 | 설명 |
|-----------|------|
| `@LogCall` | 메서드 파라미터와 반환값 로깅 |
| `@LogTime` | 메서드 실행 시간 측정 |
| `@LogMonitor` | `@LogCall` + `@LogTime` 통합 |

### 어노테이션 옵션

```java
import static kr.suhsaechan.suhlogger.annotation.TriState.*;

// 기본 - 전역 설정 따름
@LogMonitor
public void basicMethod() {}

// 헤더 강제 출력
@LogMonitor(header = ON)
public void withHeader() {}

// 헤더 끔, 결과 로깅 안함
@LogMonitor(header = OFF, result = false)
public void minimal() {}

// 마스킹 강제 + 추가 필드
@LogMonitor(mask = ON, maskFields = {"ssn", "creditCard"})
public void sensitiveData() {}
```

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `header` | TriState | DEFAULT | 헤더 로깅 (ON/OFF/DEFAULT) |
| `params` | boolean | true | 파라미터 로깅 여부 |
| `result` | boolean | true | 반환값 로깅 여부 |
| `mask` | TriState | DEFAULT | 마스킹 (ON/OFF/DEFAULT) |
| `maskFields` | String[] | {} | 추가 마스킹 필드 |

### 출력 예시

```
======================== [ProductService.searchProducts] CALL ================
========================== CALL PARAMETER ====================================
{
  "keyword" : "phone"
}
======================== [ProductService.searchProducts] RESULT ===============
[
  { "id" : 1, "name" : "Smartphone X", "price" : 599.99 }
]
================ [TIME]: ProductService.searchProducts : 253 ms ===============
```

---

## 직접 로깅 유틸리티

```java
import kr.suhsaechan.suhlogger.util.SuhLogger;

// 객체를 JSON으로 예쁘게 출력
SuhLogger.superLog(myObject);

// 구분선 출력
SuhLogger.lineLog("PROCESS START");

// 실행 시간 측정
SuhLogger.timeLog(() -> {
    expensiveOperation();
});
```

### SuhTimeUtil

```java
import kr.suhsaechan.suhlogger.util.SuhTimeUtil;

SuhTimeUtil.formatLocalDateTimeNow();           // "2026-01-18 14:30:45"
SuhTimeUtil.formatLocalDateTimeMillisNow();     // "2026-01-18 14:30:45.123"
SuhTimeUtil.formatLocalDateTimeNowForFileName(); // "20260118_143045"
SuhTimeUtil.convertMillisToReadableTime(125000); // "2분 5초"
```

---

## 설정

`application.yml`에서 세부 설정이 가능합니다:

```yaml
suh-logger:
  enabled: true                    # 전체 로깅 활성화
  pretty-print-json: false         # JSON 예쁜 출력

  header:
    enabled: false                 # 헤더 출력 (기본: false)
    include-all: false             # 모든 헤더 출력
    include-headers:               # 특정 헤더만 출력
      - Content-Type
      - X-Request-ID

  masking:
    enabled: false                 # 마스킹 (기본: false)
    mask-value: "****"             # 마스킹 값
    mask-headers:                  # 마스킹할 헤더 키워드
      - Authorization
      - Cookie
    mask-fields:                   # 마스킹할 필드 키워드
      - password
      - secret
      - apiKey

  exclude-patterns:                # 로깅 제외 URL
    - "/actuator"
    - "/health"
```

### 개발 환경 권장

```yaml
suh-logger:
  enabled: true
  pretty-print-json: true
  header:
    enabled: true
    include-all: true
```

### 운영 환경 권장

```yaml
suh-logger:
  enabled: true
  pretty-print-json: false
  header:
    enabled: false
  masking:
    enabled: true
    mask-headers: [Authorization, Cookie]
    mask-fields: [password, secret, token]
```

---

## 상세 문서

| 문서 | 설명 |
|------|------|
| [빠른 시작](docs/quick-start.md) | 5분 설정 가이드 |
| [어노테이션 가이드](docs/annotations.md) | @LogCall, @LogMonitor, @LogTime 상세 |
| [설정 가이드](docs/configuration.md) | 전체 설정 옵션 |
| [마스킹 가이드](docs/masking.md) | 민감 정보 마스킹 |
| [헤더 로깅 가이드](docs/header-logging.md) | HTTP 헤더 로깅 |
| [API 레퍼런스](docs/api-reference.md) | SuhLogger, SuhTimeUtil, CommonUtil |
| [문제 해결](docs/troubleshooting.md) | FAQ 및 트러블슈팅 |
| [변경 이력](CHANGELOG.md) | 버전별 변경사항 |

---

## 주의사항

**AOP 기반 동작 제약:**

- Spring Bean으로 등록된 클래스의 **public 메서드**에서만 동작
- 동일 클래스 내 메서드 호출에는 AOP 미적용 (프록시 기반)

```java
@Service
public class MyService {

    @LogMonitor  // ✅ AOP 적용
    public void publicMethod() {
        privateMethod();  // ❌ 내부 호출은 AOP 미적용
    }

    @LogMonitor  // ❌ private 메서드는 AOP 미적용
    private void privateMethod() { ... }
}
```

---

## 요구사항

- **Java 17+**
- **Spring Boot 3.x / 4.x**

---

## 라이선스

MIT License - 자유롭게 사용하세요!

---

<div align="center">

**이 프로젝트가 도움이 되었다면 Star를 눌러주세요!**

Made by [SUH-LAB](https://github.com/Cassiiopeia)

</div>
