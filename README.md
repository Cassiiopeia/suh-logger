# Suh-Logger API

**Suh-Logger**는 애플리케이션 전반에 걸쳐 일관되고 가독성 높은 로그를 남기기 위한 자바 로깅 유틸리티 라이브러리입니다. JSON 포맷으로 객체를 직관적으로 출력하고, 실행 시간을 자동으로 측정해 보여줌으로써 디버깅과 성능 모니터링 작업을 획기적으로 단순화합니다.

<!-- 수정하지마세요 자동으로 동기화 됩니다 -->
## Version : v1.3.9 (2025-09-20)

[전체 버전 기록 보기](CHANGELOG.md)

## 1. 패키지 구조
```text
me.suhsaechan.suhlogger
├─ util
│  ├─ SuhLogger.java       # 주요 로그 메서드와 실행 시간 측정 기능 제공
│  ├─ SuhTimeUtil.java     # 시간 포맷팅 및 Duration ↔ 문자열 변환 헬퍼 클래스
│  └─ commonUtil.java      # 범용 유틸리티 메서드 (JSON 직렬화, 순환 참조 처리 등)
├─ aspect
│  ├─ SuhExecutionTimeLoggingAspect.java  # 메서드 실행 시간 측정 AOP
│  └─ SuhMethodInvocationLoggingAspect.java # 메서드 호출 정보 로깅 AOP
├─ annotation
│  ├─ LogCall.java         # 메서드 호출 정보 로깅 어노테이션
│  ├─ LogTime.java         # 메서드 실행 시간 로깅 어노테이션
│  └─ LogMonitor.java      # 메서드 호출 정보 및 실행 시간 모두 로깅하는 어노테이션
└─ config
   ├─ SuhLoggerAutoConfiguration.java  # Spring Boot 자동 설정 클래스
   └─ SuhLoggerConfig.java            # 독립적인 로거 설정 및 포맷터 클래스
```

## 2. 도입 효과
- **가독성 향상**: 복잡한 객체를 자동으로 예쁘게 JSON 출력하여, 로그 분석 시 한눈에 파라미터와 상태를 파악할 수 있습니다.
- **성능 가시화**: 비즈니스 로직의 실행 시간을 자동 측정해 로그로 남김으로써, 성능 병목 지점을 빠르게 찾을 수 있습니다.
- **일관성 있는 포맷**: 구분선 메서드를 통해 로그의 시작·종료를 명확히 구분, 개발·운영 중 발생하는 로그 패턴을 통일합니다.
- **설정 부담 경감**: 별도의 설정 없이 라이브러리 의존성만 추가하면 즉시 사용 가능, AOP 설정과 Jackson 모듈 등록을 내부에서 처리합니다.
- **AOP 기반 로깅**: 어노테이션만 추가하면 메서드 실행 시간과 호출 정보를 자동으로 로깅합니다.
- **완전한 로깅 독립성**: SLF4J, Logback 등 모든 외부 로깅 프레임워크와 완전히 분리된 순수 JUL 기반 로깅 시스템으로 상위 프로젝트와의 로그 충돌 및 간섭을 완전히 해결합니다.
- **멀티파트 파일 지원**: MultipartFile 객체의 메타데이터(파일명, 크기, 타입 등)를 안전하게 로깅하여 파일 업로드 관련 디버깅을 지원합니다.
- **JTS Geometry 순환 참조 해결**: PostgreSQL의 JTS Point, Polygon 등 지리 정보 객체의 무한 순환 참조 문제를 해결하여 안전하게 로깅 가능합니다.
- **모듈화된 유틸리티**: 로깅과 직접 관련 없는 범용 기능들을 별도 유틸리티 클래스로 분리하여 코드 재사용성과 유지보수성을 향상시켰습니다.
- **순수 POJO 방식**: Lombok 의존성을 완전히 제거하고 순수 Java 방식으로 구현하여 의존성 부담을 최소화했습니다.

## 3. 의존성 추가 (Gradle)
```groovy
repositories {
  maven {
    url "http://suh-project.synology.me:9999/repository/maven-releases/"
    allowInsecureProtocol = true
  }
}

dependencies {
  implementation 'me.suhsaechan:suh-logger:X.X.X' // 최신 버전으로 설정
}
```

## 4. 주요 클래스 및 메서드

### 4.1 SuhLogger
| 메서드                          | 설명                                                      |
|--------------------------------|----------------------------------------------------------|
| `superLog(Object obj)`         | INFO 레벨로 `obj`를 JSON 형태로 가시성 있게 출력            |
| `superLogDebug(Object obj)`    | DEBUG 레벨로 `obj`를 JSON 형태로 출력                     |
| `superLogWarn(Object obj)`     | WARN 레벨로 `obj`를 JSON 형태로 출력                      |
| `superLogError(Object obj)`    | ERROR 레벨로 `obj`를 JSON 형태로 출력                     |
| `lineLog(String title)`        | INFO 레벨로 구분선만 출력 (제목 없음)                      |
| `lineLog(String title)`        | INFO 레벨로 `title`을 가운데 둔 구분선 출력               |
| `timeLog(ThrowingRunnable task)` | `task` 실행 전후로 시간 측정, 완료 후 소요 시간 로그 출력     |

### 4.2 SuhTimeUtil
| 메서드                                      | 설명                                              |
|--------------------------------------------|--------------------------------------------------|
| `formatLocalDateTimeNow()`                 | `yyyy-MM-dd HH:mm:ss` 형식의 현재 시간 반환         |
| `formatLocalDateTimeMillisNow()`           | `yyyy-MM-dd HH:mm:ss.SSS` 형식의 현재 시간 반환     |
| `formatLocalDateTimeNowForFileName()`      | `yyyyMMdd_HHmmss` 형식의 현재 시간 반환          |
| `readableCurrentLocalDateTime()`           | `yyyy년 MM월 dd일 HH시 mm분 ss초` 형식의 현재 시간 반환 |
| `convertMillisToReadableTime(long millis)`  | ms 단위 시간을 "n분 m초/ millisecond" 문자열로 변환   |
| `convertDurationToReadableTime(Duration)`   | `Duration` → "n분 m초" 형식으로 변환                |

### 4.3 commonUtil
| 메서드                                      | 설명                                              |
|--------------------------------------------|--------------------------------------------------|
| `makeSafeForSerialization(Object obj)`     | 순환 참조를 일으키는 객체를 안전한 형태로 변환           |
| `extractJTSGeometryInfo(Object geometry)`  | JTS Geometry 객체에서 좌표, SRID, WKT 정보 추출      |
| `extractMultipartFileInfo(Object file)`    | MultipartFile 객체에서 파일명, 크기, 타입 정보 추출    |
| `isJTSGeometryType(Object obj)`           | 객체가 JTS Geometry 타입인지 확인                   |
| `isMultipartFileType(Object obj)`         | 객체가 MultipartFile 타입인지 확인                  |
| `createSafeMap(Object obj)`               | 리플렉션을 통해 객체를 안전한 Map으로 변환             |

### 4.4 로깅 어노테이션
| 어노테이션           | 설명                                                        | 헤더 옵션 |
|--------------------|-------------------------------------------------------------|----------|
| `@LogCall`         | 메서드 호출 시 파라미터 정보와 반환값을 자동으로 로깅             | `header = true/false` |
| `@LogTime`         | 메서드 실행 시간을 측정하여 자동으로 로깅                        | 없음 |
| `@LogMonitor`      | `@LogCall`과 `@LogTime` 기능을 모두 제공 (복합 로깅)           | `header = true/false` |

#### 헤더 로깅 우선순위
1. **어노테이션 설정** (최우선): `@LogCall(header = true)` 또는 `@LogMonitor(header = true)`
2. **전역 설정**: `suh-logger.header.enabled = true`
3. **기본값**: `false` (헤더 로깅 안함)

## 5. 사용 예시

### 5.1 기본 로깅 기능
```java
import me.suhsaechan.suhlogger.util.SuhLogger;
import me.suhsaechan.suhlogger.util.commonUtil;

// 1) 객체 로그
MyDto dto = new MyDto("Alice", 30);
SuhLogger.superLog(dto);

// 2) JTS Point 객체 안전 로깅
Point point = geometryFactory.createPoint(new Coordinate(127.123, 37.456));
SuhLogger.superLog(point); // 순환 참조 없이 안전하게 로깅

// 3) 직렬화 문제가 있는 객체 안전 변환
Object safeObject = commonUtil.makeSafeForSerialization(problematicObject);
SuhLogger.superLog(safeObject);

// 4) 구분선
SuhLogger.lineLog("PROCESS START");

// 5) 실행 시간 측정
SuhLogger.timeLog(() -> {
    // 비즈니스 로직 수행
    processData();
});

// 6) 프로세스 종료 구분선
SuhLogger.lineLog("PROCESS END");
```

### 5.2 어노테이션 기반 자동 로깅
```java
import me.suhsaechan.suhlogger.annotation.LogCall;
import me.suhsaechan.suhlogger.annotation.LogTime;
import me.suhsaechan.suhlogger.annotation.LogMonitor;

// 메서드 파라미터와 반환값 로깅 (헤더 제외)
@LogCall
public ResponseDto processUserRequest(UserRequestDto request) {
    // 비즈니스 로직
    return new ResponseDto("Success");
}

// 메서드 파라미터와 반환값 로깅 + HTTP 헤더 정보 포함
@LogCall(header = true)
public ResponseDto loginUser(LoginRequest request) {
    // 로그인 로직 - 헤더 정보가 중요한 경우
    return new ResponseDto("Login Success");
}

// 메서드 실행 시간 측정 및 로깅
@LogTime
public void heavyDataProcessing() {
    // 시간이 오래 걸리는 작업
}

// 복합 로깅 (파라미터, 반환값, 실행 시간 모두 로깅, 헤더 제외)
@LogMonitor
public List<Product> searchProducts(SearchCriteria criteria) {
    // 상품 검색 로직
    return productRepository.findByCriteria(criteria);
}

// 복합 로깅 + HTTP 헤더 정보 포함
@LogMonitor(header = true)
public ResponseDto processPayment(PaymentRequest request) {
    // 결제 처리 - 요청 헤더 정보가 중요한 경우
    return paymentService.process(request);
}
```

#### 어노테이션 출력 예시
```
============ [ProductService.searchProducts] CALL ============
============ CALL PARAMETER ============
{
  "criteria" : {
    "keyword" : "phone",
    "minPrice" : 100.0,
    "maxPrice" : 1000.0
  }
}
============ HTTP REQUEST INFO ============
{
  "method" : "GET",
  "URI" : "/api/v1/products/search",
  "requestId" : "req-12345"
}
============ [ProductService.searchProducts] RESULT ============
[
  {
    "id" : 1,
    "name" : "Smartphone X",
    "price" : 599.99
  },
  {
    "id" : 2,
    "name" : "Phone Case",
    "price" : 19.99
  }
]
============ [TIME]: ProductService.searchProducts : 253 ms ============
```

## 6. 설정 옵션 (application.yml)

### 6.1 전체 설정 예시 (모든 옵션 포함)

```yaml
suh-logger:
  # 전체 로깅 활성화 여부
  enabled: true                    # true(기본값), false
  
  # Response Body JSON pretty print 활성화 여부  
  pretty-print-json: false         # false(기본값), true
  
  # Response Body 로깅 최대 크기 (bytes)
  max-response-body-size: 4096     # 양의 정수 (기본값: 4096)
  
  # 로깅에서 제외할 URL 패턴들
  exclude-patterns:                # 문자열 배열 (기본값: 빈 배열)
    - "/actuator"                  # Spring Boot Actuator 제외
    - "/health"                    # Health Check 제외
    - "/login"                     # 로그인 엔드포인트 제외 (예시)
    - "/logout"                    # 로그아웃 엔드포인트 제외 (예시)
    - "/auth"                      # 인증 관련 엔드포인트 제외 (예시)
  
  # JSON 직렬화에서 제외할 클래스들
  excluded-classes:                # 문자열 배열 (기본값: 빈 배열)
    - "org.springframework.web.multipart.MultipartFile"
    - "java.util.Vector"
    - "java.io.File"
  
  # 마스킹 관련 설정
  masking:
    header: true                   # true(기본값), false - 민감한 헤더 마스킹 여부
  
  # 헤더 로깅 관련 설정  
  header:
    enabled: false                 # false(기본값), true - 전역 헤더 로깅 활성화 여부
```

### 6.2 최소 설정 예시 (기본값 사용)

```yaml
suh-logger:
  enabled: true                    # 로깅 활성화만 설정
```

### 6.3 개발 환경 권장 설정

```yaml
suh-logger:
  enabled: true
  pretty-print-json: true          # 개발 시 JSON을 예쁘게 출력
  max-response-body-size: 8192     # 더 큰 응답도 로깅
  header:
    enabled: true                  # 헤더 정보도 로깅
  exclude-patterns:
    - "/actuator"
    - "/health"
```

### 6.4 운영 환경 권장 설정

```yaml
suh-logger:
  enabled: true
  pretty-print-json: false         # 운영에서는 압축된 JSON 사용
  max-response-body-size: 2048     # 로그 크기 제한
  header:
    enabled: false                 # 헤더 정보는 보안상 제외
  masking:
    header: true                   # 민감한 헤더는 반드시 마스킹
  exclude-patterns:
    - "/actuator"
    - "/health"
    - "/metrics"
```

### 6.5 JSON Pretty Print 설정

Response Body의 JSON 출력 형태를 제어할 수 있습니다.

#### 설정 비교
```yaml
# 압축된 JSON 출력 (기본값)
suh-logger:
  pretty-print-json: false
```
**출력 예시:**
```
Response Body: {"totalCount":2,"items":{"content":[{"itemId":"eb818062-217f-4bec-88b7-13f5f698fc74","itemName":"asdfasdstring","price":1073741824}]}}
```

```yaml
# 예쁘게 포맷된 JSON 출력
suh-logger:
  pretty-print-json: true
```
**출력 예시:**
```
Response Body: {
  "totalCount" : 2,
  "items" : {
    "content" : [ {
      "itemId" : "eb818062-217f-4bec-88b7-13f5f698fc74",
      "itemName" : "asdfasdstring",
      "price" : 1073741824
    } ]
  }
}
```

#### 권장 사용법
- **개발 환경**: `pretty-print-json: true` - 가독성 우선
- **운영 환경**: `pretty-print-json: false` - 로그 크기 절약

### 6.6 제외 클래스 설정

JSON 직렬화가 불가능하거나 원하지 않는 클래스들을 설정을 통해 제외할 수 있습니다.

**설정 가능한 제외 클래스 예시:**
- `MultipartFile`: 파일 업로드 객체 → 메타데이터만 추출
- `Vector`: 레거시 컬렉션 → 크기, 용량, 요소 타입 정보만 추출  
- `File`: 파일 시스템 객체 → 파일 정보만 추출

**제외된 클래스 로깅 예시:**
```json
{
  "_type": "EXCLUDED_CLASS",
  "_class": "org.springframework.web.multipart.MultipartFile",
  "_simpleName": "MultipartFile",
  "_toString": "org.springframework.web.multipart.support.StandardMultipartHttpServletRequest$StandardMultipartFile@1a2b3c4d"
}
```

### 6.7 헤더 마스킹 기능

보안을 위해 민감한 헤더 정보는 자동으로 마스킹 처리됩니다.

**마스킹 대상 헤더:**
- `Authorization` (Bearer 토큰, API 키 등)
- `Cookie` (세션 쿠키)
- `Set-Cookie` (응답 쿠키)
- `X-Auth-Token` (커스텀 인증 토큰)
- `X-API-Key` (API 키)
- 기타 `token`, `auth`가 포함된 헤더명

**마스킹 예시:**
```
// 마스킹 전
"Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
"Cookie": "JSESSIONID=ABC123; user_token=xyz789"

// 마스킹 후  
"Authorization": "****"
"Cookie": "****"
```

**마스킹 비활성화:**
```yaml
suh-logger:
  masking:
    header: false    # 모든 헤더 정보를 그대로 출력
```

### 6.8 자동 제외되는 요청들

suh-logger는 성능 최적화를 위해 다음 요청들을 **자동으로 필터링에서 제외**합니다:

#### 정적 리소스 (자동 제외)
```
/static/*     - Spring Boot 기본 정적 리소스 경로
/css/*        - CSS 파일들
/js/*         - JavaScript 파일들  
/images/*     - 이미지 파일들
*.ico         - 파비콘 파일
*.png, *.jpg  - 이미지 파일 확장자
*.css, *.js   - 스타일시트, 스크립트 파일 확장자
```

이러한 정적 리소스들은 비즈니스 로직과 관련이 없고 요청량이 많아 로깅할 필요가 없으므로 자동으로 제외됩니다.

#### 사용자 정의 제외 패턴
위의 `exclude-patterns` 설정을 통해 추가로 제외할 URL 패턴을 지정할 수 있습니다.

#### 필터링 우선순위
1. **정적 리소스 체크** (최우선) - `shouldNotFilter()` 메서드에서 처리
2. **전체 로깅 활성화 체크** - `enabled: false`인 경우 모든 로깅 중단
3. **사용자 정의 제외 패턴 체크** - `exclude-patterns`에 매칭되는 URL 제외
4. **로깅 실행** - 위 조건들을 통과한 요청만 로깅

```java
// 예시: 다음과 같은 요청들이 자동으로 제외됩니다
GET /favicon.ico          → 자동 제외 (정적 리소스)
GET /css/style.css        → 자동 제외 (정적 리소스)  
GET /js/app.js           → 자동 제외 (정적 리소스)
GET /images/logo.png     → 자동 제외 (정적 리소스)
GET /actuator/health     → 사용자 설정에 따라 제외 가능
POST /api/users          → 로깅 대상 (비즈니스 로직)
```

### 6.9 어노테이션 사용법

suh-logger는 **어노테이션 기반**으로 동작합니다. 어노테이션을 달지 않으면 로깅되지 않습니다:

```java
// Service 클래스에서 사용
@Service
public class UserService {
    
    @LogCall  // 메서드 파라미터와 반환값 로깅
    public User findUser(Long userId) {
        return userRepository.findById(userId);
    }
    
    @LogTime  // 실행 시간만 로깅
    public void heavyProcess() {
        // 시간이 오래 걸리는 작업
    }
    
    @LogMonitor  // 파라미터, 반환값, 실행시간 모두 로깅
    public List<User> searchUsers(String keyword) {
        return userRepository.findByKeyword(keyword);
    }
}

// Controller에서도 사용 가능 (Spring Security와 충돌 없음)
@RestController
public class UserController {
    
    @PostMapping("/login")
    @LogMonitor  // 안전하게 동작
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 로그인 로직
        return ResponseEntity.ok(loginResponse);
    }
}
```

### 6.10 전체 로깅 제어

`enabled: false`로 설정하면 **모든 로깅이 비활성화**됩니다:

```yaml
suh-logger:
  enabled: false  # 어노테이션이 있어도 로깅하지 않음
```

## 7. 어노테이션 사용 시 주의사항
- `@LogCall`, `@LogTime`, `@LogMonitor` 어노테이션은 Spring AOP를 기반으로 동작하므로 Spring 환경에서만 사용 가능합니다.
- 프록시 기반으로 동작하므로 동일 클래스 내 메서드 호출에는 AOP가 적용되지 않습니다.
- 적용 대상이 Spring Bean으로 등록된 클래스의 public 메서드여야 합니다.
- `@LogMonitor`는 두 기능을 모두 제공하므로 `@LogCall`과 `@LogTime`을 동시에 사용하는 경우 대체하여 사용할 수 있습니다.
- **Controller, Service, Repository 등 모든 Spring Bean에서 사용 가능**합니다.

---
*자세한 가이드와 기여 방법은 각종 문서(USAGE.md, TROUBLESHOOTING.md, CONTRIBUTING.md) 를 참고하세요.*
