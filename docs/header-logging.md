# 헤더 로깅 가이드

HTTP 요청 헤더 정보를 로깅하는 방법을 설명합니다.

## 기본 개념

헤더 로깅은 **기본적으로 비활성화**되어 있습니다. 필요한 경우에만 활성화하여 사용하세요.

```yaml
suh-logger:
  header:
    enabled: false  # 기본값: 헤더 출력 안함
```

## 전역 헤더 설정

### application.yml

```yaml
suh-logger:
  header:
    enabled: true               # 헤더 출력 활성화
    include-all: false          # 모든 헤더 출력 여부
    include-headers:            # 출력할 헤더 목록 (include-all: false일 때)
      - Content-Type
      - X-Request-ID
      - Accept-Language
```

### 동작 방식

| enabled | include-all | include-headers | 결과 |
|---------|-------------|-----------------|------|
| false | - | - | 헤더 출력 안함 (기본값) |
| true | true | - | 모든 헤더 출력 |
| true | false | [목록] | 목록에 있는 헤더만 출력 |
| true | false | [] | 헤더 출력 안함 |

## 어노테이션 기반 헤더 제어

### TriState로 헤더 출력 제어

```java
import static kr.suhsaechan.suhlogger.annotation.TriState.*;

// 전역 설정 따름 (기본값)
@LogMonitor
public void method1() { ... }

// 헤더 강제 출력
@LogMonitor(header = ON)
public void method2() { ... }

// 헤더 강제 미출력
@LogMonitor(header = OFF)
public void method3() { ... }
```

### @LogCall에서도 동일하게 사용

```java
@LogCall(header = ON)
public ResponseDto secureApi(RequestDto request) { ... }
```

## 우선순위

```
어노테이션 header = ON/OFF > 전역 header.enabled
```

| 전역 설정 | 어노테이션 | 결과 |
|----------|-----------|------|
| enabled: false | header = DEFAULT | 헤더 출력 안함 |
| enabled: false | header = ON | 헤더 출력 |
| enabled: true | header = DEFAULT | 헤더 출력 |
| enabled: true | header = OFF | 헤더 출력 안함 |

## 출력 예시

### 헤더 출력 활성화

```java
@LogCall(header = ON)
public ResponseDto processRequest(RequestDto request) { ... }
```

**출력:**
```
======================== [UserService.processRequest] CALL ===================
========================== CALL PARAMETER ====================================
{
  "request" : {
    "userId" : 123,
    "action" : "update"
  }
}
========================== HTTP REQUEST INFO =================================
{
  "method" : "POST",
  "URI" : "/api/users/update",
  "headers" : {
    "content-type" : "application/json",
    "user-agent" : "Mozilla/5.0...",
    "accept-language" : "ko-KR,ko;q=0.9"
  }
}
======================== [UserService.processRequest] RESULT =================
{
  "status" : "success"
}
```

### 헤더 출력 비활성화 (기본값)

```java
@LogCall
public ResponseDto processRequest(RequestDto request) { ... }
```

**출력:**
```
======================== [UserService.processRequest] CALL ===================
========================== CALL PARAMETER ====================================
{
  "request" : {
    "userId" : 123,
    "action" : "update"
  }
}
======================== [UserService.processRequest] RESULT =================
{
  "status" : "success"
}
```

## 헤더 마스킹과 함께 사용

헤더 출력과 마스킹은 **독립적으로 동작**합니다.

```yaml
suh-logger:
  header:
    enabled: true
    include-all: true
  masking:
    enabled: true
    mask-headers:
      - Authorization
      - Cookie
      - X-Api-Key
```

**출력:**
```json
{
  "headers" : {
    "content-type" : "application/json",
    "authorization" : "****",
    "cookie" : "****",
    "x-api-key" : "****",
    "user-agent" : "Mozilla/5.0..."
  }
}
```

### 조합 정리

| header.enabled | masking.enabled | 결과 |
|----------------|-----------------|------|
| false | - | 헤더 출력 안함 |
| true | false | 헤더 원본 출력 |
| true | true | 헤더 출력 + 민감 헤더 마스킹 |

## 사용 예시

### 인증 API에서만 헤더 로깅

```java
@RestController
public class AuthController {

    @PostMapping("/login")
    @LogMonitor(header = ON, mask = ON, maskFields = {"password"})
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // 인증 API - 헤더 정보가 중요
        return authService.login(request);
    }
}
```

### 일반 API는 헤더 제외

```java
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    @LogCall  // 헤더 미출력 (기본값)
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

### 디버깅 시 모든 정보 출력

```java
@LogMonitor(header = ON, mask = OFF)  // 헤더 출력 + 마스킹 비활성화
public void debugMethod() { ... }
```

## 환경별 권장 설정

### 개발 환경

```yaml
suh-logger:
  header:
    enabled: true
    include-all: true  # 모든 헤더 확인
  masking:
    enabled: false     # 원본 데이터 확인
```

### 운영 환경

```yaml
suh-logger:
  header:
    enabled: false     # 보안상 헤더 제외
  masking:
    enabled: true
    mask-headers:
      - Authorization
      - Cookie
```

### 특정 헤더만 필요한 경우

```yaml
suh-logger:
  header:
    enabled: true
    include-all: false
    include-headers:
      - Content-Type
      - X-Request-ID
      - X-Correlation-ID
```

## 주의사항

- 헤더에는 민감 정보(Authorization, Cookie 등)가 포함될 수 있으므로 운영 환경에서는 주의하세요.
- `include-all: true`는 모든 헤더를 출력하므로 로그 크기가 증가할 수 있습니다.
- 필요한 헤더만 `include-headers`로 지정하는 것을 권장합니다.
