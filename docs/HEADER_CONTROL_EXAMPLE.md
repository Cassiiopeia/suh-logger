# 헤더 출력 제어 기능 사용 가이드

suh-logger v1.2.10에서 새롭게 추가된 헤더 출력 제어 기능을 사용하는 방법을 설명합니다.

## 1. 기본 동작 변경사항

### 이전 버전 (v1.2.9 이하)
- 모든 `@LogCall`, `@LogMonitor` 어노테이션에서 HTTP 헤더 정보가 자동으로 출력됨
- 헤더 출력을 끌 수 있는 방법이 없었음

### 현재 버전 (v1.2.10+)
- **기본적으로 헤더 출력이 비활성화됨** (`suh-logger.header.enabled: false`)
- 필요한 경우에만 properties 설정이나 어노테이션으로 헤더 출력 활성화

## 2. 설정 방법

### 2.1 전역 설정 (application.yml)

```yaml
suh-logger:
  # 전체 로깅 활성화 여부
  enabled: true
  
  # 헤더 출력 제어 (새로 추가된 설정)
  header:
    enabled: false  # 기본값: false (헤더 출력 안함)
  
  # 기존 마스킹 설정 (독립적으로 동작)
  masking:
    header: true    # 기본값: true (헤더 출력 시 민감 정보 마스킹)
```

### 2.2 헤더 출력 활성화

```yaml
suh-logger:
  header:
    enabled: true   # 모든 @LogCall, @LogMonitor에서 헤더 출력
```

## 3. 어노테이션 기반 개별 제어

### 3.1 기본 사용법

```java
@Service
public class UserService {
    
    // 헤더 정보 없이 로깅 (기본값)
    @LogCall
    public User findUser(Long userId) {
        return userRepository.findById(userId);
    }
    
    // 헤더 정보 포함하여 로깅
    @LogCall(header = true)
    public User createUser(UserRequest request) {
        return userRepository.save(new User(request));
    }
    
    // LogMonitor도 동일하게 동작
    @LogMonitor(header = true)
    public List<User> searchUsers(String keyword) {
        return userRepository.findByKeyword(keyword);
    }
}
```

### 3.2 우선순위

1. **어노테이션 설정이 최우선**
   - `@LogCall(header = true)` → 헤더 출력
   - `@LogCall(header = false)` → 헤더 출력 안함

2. **전역 설정은 어노테이션에 명시되지 않은 경우에만 적용**
   - `@LogCall` (header 속성 없음) → 전역 설정(`suh-logger.header.enabled`)에 따라 결정

## 4. 출력 예시

### 4.1 헤더 출력 비활성화 (기본값)

```java
@LogCall
public ResponseDto processRequest(RequestDto request) {
    // 비즈니스 로직
    return new ResponseDto("success");
}
```

**출력 결과:**
```
============ [UserService.processRequest] CALL ============
====================== CALL PARAMETER ======================
{
  "request" : {
    "userId" : 123,
    "action" : "update"
  }
}
============ [UserService.processRequest] RESULT ============
{
  "status" : "success",
  "message" : "처리 완료"
}
============ [TIME]: UserService.processRequest : 45 ms ============
```

### 4.2 헤더 출력 활성화

```java
@LogCall(header = true)
public ResponseDto processRequest(RequestDto request) {
    // 비즈니스 로직
    return new ResponseDto("success");
}
```

**출력 결과:**
```
============ [UserService.processRequest] CALL ============
====================== CALL PARAMETER ======================
{
  "request" : {
    "userId" : 123,
    "action" : "update"
  }
}
==================== HTTP REQUEST INFO =====================
{
  "headers" : {
    "content-type" : "application/json",
    "user-agent" : "Mozilla/5.0...",
    "authorization" : "****",  // 자동 마스킹 처리
    "cookie" : "****"          // 자동 마스킹 처리
  },
  "method" : "POST",
  "URI" : "/api/users/update"
}
============ [UserService.processRequest] RESULT ============
{
  "status" : "success",
  "message" : "처리 완료"
}
============ [TIME]: UserService.processRequest : 45 ms ============
```

## 5. 마스킹과의 관계

헤더 출력 제어와 마스킹은 **독립적으로 동작**합니다:

```yaml
suh-logger:
  header:
    enabled: true   # 헤더 출력 활성화
  masking:
    header: false   # 헤더 마스킹 비활성화 (민감 정보도 그대로 출력)
```

- `header.enabled: true` + `masking.header: true` → 헤더 출력하되 민감 정보 마스킹
- `header.enabled: true` + `masking.header: false` → 헤더를 원본 그대로 출력
- `header.enabled: false` → 마스킹 설정과 관계없이 헤더 출력 안함

## 6. 권장 사용 패턴

### 6.1 운영 환경
```yaml
suh-logger:
  header:
    enabled: false  # 성능과 로그 가독성을 위해 비활성화
  masking:
    header: true    # 보안을 위해 마스킹 활성화 유지
```

### 6.2 개발/디버깅 환경
```yaml
suh-logger:
  header:
    enabled: true   # 디버깅을 위해 헤더 정보 확인
  masking:
    header: false   # 개발 시 원본 헤더 확인 가능
```

### 6.3 특정 API만 헤더 로깅
```java
@RestController
public class AuthController {
    
    // 인증 관련 API에서만 헤더 정보 로깅
    @PostMapping("/login")
    @LogCall(header = true)
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 로그인 로직
    }
    
    // 일반 API는 헤더 정보 없이 로깅
    @GetMapping("/profile")
    @LogCall
    public ResponseEntity<?> getProfile() {
        // 프로필 조회 로직
    }
}
```

## 7. 마이그레이션 가이드

### v1.2.9에서 v1.2.10으로 업그레이드 시

1. **기본 동작 변경**: 헤더가 더 이상 자동으로 출력되지 않습니다.

2. **기존 동작 유지하려면**:
   ```yaml
   suh-logger:
     header:
       enabled: true  # 기존처럼 모든 곳에서 헤더 출력
   ```

3. **선택적 헤더 출력으로 전환**:
   - 전역 설정은 `enabled: false` 유지
   - 필요한 메서드에만 `@LogCall(header = true)` 추가

이렇게 설정하면 로그가 더 깔끔해지고 성능도 향상됩니다!