# 어노테이션 가이드

suh-logger가 제공하는 AOP 기반 로깅 어노테이션을 설명합니다.

## 어노테이션 종류

| 어노테이션 | 설명 |
|-----------|------|
| `@LogCall` | 메서드 파라미터와 반환값 로깅 |
| `@LogTime` | 메서드 실행 시간 측정 |
| `@LogMonitor` | `@LogCall` + `@LogTime` 통합 |

## TriState 옵션

`header`, `mask` 속성은 `TriState` enum을 사용합니다.

```java
import static kr.suhsaechan.suhlogger.annotation.TriState.*;

@LogMonitor(header = ON)   // 헤더 강제 출력
@LogMonitor(header = OFF)  // 헤더 강제 미출력
@LogMonitor(header = DEFAULT)  // 전역 설정 따름 (기본값)
```

| 값 | 설명 |
|----|------|
| `DEFAULT` | 전역 설정(application.yml) 따름 |
| `ON` | 강제 활성화 |
| `OFF` | 강제 비활성화 |

## @LogCall

메서드 호출 시 파라미터와 반환값을 로깅합니다.

### 속성

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `header` | TriState | DEFAULT | 헤더 로깅 제어 |
| `params` | boolean | true | 파라미터 로깅 여부 |
| `result` | boolean | true | 반환값 로깅 여부 |
| `mask` | TriState | DEFAULT | 마스킹 제어 |
| `maskFields` | String[] | {} | 추가 마스킹 필드 |

### 사용 예시

```java
import static kr.suhsaechan.suhlogger.annotation.TriState.*;

// 기본 사용 - 파라미터 + 반환값 로깅
@LogCall
public User findUser(Long userId) { ... }

// 파라미터만 로깅 (반환값 제외)
@LogCall(result = false)
public void updateUser(UserDto dto) { ... }

// 반환값만 로깅 (파라미터 제외)
@LogCall(params = false)
public List<User> getAllUsers() { ... }

// 헤더 정보 포함
@LogCall(header = ON)
public ResponseDto secureApi(RequestDto req) { ... }

// 특정 필드 마스킹
@LogCall(mask = ON, maskFields = {"password", "ssn"})
public User createUser(UserCreateDto dto) { ... }
```

## @LogTime

메서드 실행 시간을 측정합니다.

### 사용 예시

```java
@LogTime
public void heavyProcessing() {
    // 무거운 작업
}
```

### 출력 예시

```
============ [TIME]: MyService.heavyProcessing : 1초 234ms ============
```

## @LogMonitor

`@LogCall`과 `@LogTime`을 합친 기능입니다. 파라미터, 반환값, 실행시간을 모두 로깅합니다.

### 속성

`@LogCall`과 동일한 속성을 지원합니다.

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `header` | TriState | DEFAULT | 헤더 로깅 제어 |
| `params` | boolean | true | 파라미터 로깅 여부 |
| `result` | boolean | true | 반환값 로깅 여부 |
| `mask` | TriState | DEFAULT | 마스킹 제어 |
| `maskFields` | String[] | {} | 추가 마스킹 필드 |

### 사용 예시

```java
import static kr.suhsaechan.suhlogger.annotation.TriState.*;

// 기본 사용 - 전체 로깅
@LogMonitor
public List<Product> searchProducts(String keyword) { ... }

// 헤더 강제 출력 + 마스킹
@LogMonitor(header = ON, mask = ON, maskFields = {"creditCard"})
public PaymentResult processPayment(PaymentRequest req) { ... }

// 최소 로깅 (파라미터, 반환값 제외 - 실행시간만)
@LogMonitor(params = false, result = false)
public void batchJob() { ... }

// 모든 옵션 조합
@LogMonitor(
    header = ON,
    params = true,
    result = false,
    mask = ON,
    maskFields = {"internalId", "token"}
)
public void fullCustom() { ... }
```

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

## 우선순위

어노테이션 설정이 전역 설정(application.yml)보다 우선합니다.

```
어노테이션 ON/OFF > 전역 설정 > 기본값
```

예시:
- `@LogMonitor(header = ON)` → 전역 설정과 관계없이 헤더 출력
- `@LogMonitor(header = OFF)` → 전역 설정과 관계없이 헤더 미출력
- `@LogMonitor` (header 미지정) → 전역 설정 따름

## 주의사항

### AOP 제약사항

- Spring Bean으로 등록된 클래스의 **public 메서드**에서만 동작
- 동일 클래스 내 메서드 호출에는 AOP 미적용 (프록시 기반)

```java
@Service
public class MyService {

    @LogMonitor  // ✅ 외부에서 호출 시 AOP 적용
    public void publicMethod() {
        privateMethod();  // ❌ 내부 호출 - AOP 미적용
    }

    @LogMonitor  // ❌ private 메서드 - AOP 미적용
    private void privateMethod() { ... }
}
```

### 권장 사용 패턴

```java
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/users")
    @LogMonitor(header = ON, mask = ON, maskFields = {"password"})
    public ResponseEntity<User> createUser(@RequestBody UserCreateDto dto) {
        return ResponseEntity.ok(userService.create(dto));
    }

    @GetMapping("/users/{id}")
    @LogCall  // 간단한 조회는 @LogCall
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }
}
```
