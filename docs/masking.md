# 마스킹 가이드

민감한 정보(비밀번호, 토큰, 개인정보 등)를 로그에서 마스킹 처리하는 방법을 설명합니다.

## 기본 개념

마스킹은 **기본적으로 비활성화**되어 있습니다. 필요한 경우에만 활성화하여 사용하세요.

```yaml
suh-logger:
  masking:
    enabled: false  # 기본값: 마스킹 비활성화
```

## 전역 마스킹 설정

### application.yml

```yaml
suh-logger:
  masking:
    enabled: true              # 마스킹 활성화
    mask-value: "****"         # 마스킹 값 (기본값)
    mask-headers:              # 마스킹할 헤더 키워드
      - Authorization
      - Cookie
      - X-Api-Key
      - token
    mask-fields:               # 마스킹할 필드 키워드
      - password
      - secret
      - apiKey
      - credential
      - ssn
      - creditCard
```

### 동작 방식

필드명/헤더명에 키워드가 **포함**되면 마스킹 처리됩니다.

예시: `mask-fields: ["password"]` 설정 시
- `password` → 마스킹 ✅
- `userPassword` → 마스킹 ✅
- `password123` → 마스킹 ✅
- `pwd` → 마스킹 안됨 ❌

## 어노테이션 기반 마스킹

### TriState로 마스킹 제어

```java
import static kr.suhsaechan.suhlogger.annotation.TriState.*;

// 전역 설정 따름 (기본값)
@LogMonitor
public void method1() { ... }

// 마스킹 강제 활성화
@LogMonitor(mask = ON)
public void method2() { ... }

// 마스킹 강제 비활성화
@LogMonitor(mask = OFF)
public void method3() { ... }
```

### 메서드별 추가 마스킹 필드

전역 설정에 추가로 마스킹할 필드를 지정할 수 있습니다.

```java
// 전역 설정 + internalId, tempToken 추가 마스킹
@LogMonitor(mask = ON, maskFields = {"internalId", "tempToken"})
public void sensitiveMethod(SensitiveDto dto) {
    ...
}
```

## 마스킹 대상

### 1. 파라미터 마스킹

메서드 파라미터 객체의 필드가 마스킹됩니다.

```java
public class UserCreateDto {
    private String username;
    private String password;     // 마스킹 대상
    private String email;
    private String apiKey;       // 마스킹 대상
}

@LogMonitor(mask = ON, maskFields = {"password", "apiKey"})
public User createUser(UserCreateDto dto) { ... }
```

**출력:**
```json
{
  "username" : "john",
  "password" : "****",
  "email" : "john@example.com",
  "apiKey" : "****"
}
```

### 2. 헤더 마스킹

HTTP 요청 헤더가 마스킹됩니다.

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
```

**출력:**
```json
{
  "headers" : {
    "Content-Type" : "application/json",
    "Authorization" : "****",
    "Cookie" : "****",
    "User-Agent" : "Mozilla/5.0..."
  }
}
```

## 커스텀 마스킹 값

기본 마스킹 값(`****`)을 변경할 수 있습니다.

```yaml
suh-logger:
  masking:
    enabled: true
    mask-value: "[REDACTED]"
```

**출력:**
```json
{
  "password" : "[REDACTED]",
  "apiKey" : "[REDACTED]"
}
```

## 우선순위

```
어노테이션 mask = ON/OFF > 전역 masking.enabled
```

| 전역 설정 | 어노테이션 | 결과 |
|----------|-----------|------|
| enabled: false | mask = DEFAULT | 마스킹 안함 |
| enabled: false | mask = ON | 마스킹 함 |
| enabled: true | mask = DEFAULT | 마스킹 함 |
| enabled: true | mask = OFF | 마스킹 안함 |

## 사용 예시

### 민감 정보가 포함된 API

```java
@RestController
public class AuthController {

    @PostMapping("/login")
    @LogMonitor(mask = ON, maskFields = {"password"})
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // password 필드가 마스킹되어 로깅됨
        return authService.login(request);
    }

    @PostMapping("/register")
    @LogMonitor(mask = ON, maskFields = {"password", "ssn", "creditCard"})
    public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
        // 여러 민감 필드 마스킹
        return userService.register(request);
    }
}
```

### 내부 API (마스킹 불필요)

```java
@RestController
public class InternalController {

    @GetMapping("/internal/status")
    @LogMonitor(mask = OFF)  // 내부 API는 마스킹 불필요
    public ResponseEntity<Status> getStatus() {
        return statusService.getStatus();
    }
}
```

## 환경별 권장 설정

### 개발 환경

```yaml
suh-logger:
  masking:
    enabled: false  # 디버깅을 위해 원본 데이터 확인
```

### 운영 환경

```yaml
suh-logger:
  masking:
    enabled: true
    mask-headers:
      - Authorization
      - Cookie
      - X-Api-Key
    mask-fields:
      - password
      - secret
      - token
      - apiKey
      - ssn
      - creditCard
```

## 주의사항

- 마스킹은 **로그 출력**에만 적용됩니다. 실제 데이터는 변경되지 않습니다.
- 키워드는 **대소문자 구분 없이** 매칭됩니다. (`Password`, `PASSWORD`, `password` 모두 매칭)
- 너무 짧은 키워드(예: `id`, `key`)는 의도치 않은 마스킹을 유발할 수 있습니다.
