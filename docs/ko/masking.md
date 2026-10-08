# 마스킹 가이드

**3.0부터 마스킹이 기본으로 켜져 있습니다.** 2.x에서는 꺼져 있어서 로그인 응답의 액세스 토큰·리프레시 토큰·CSRF 토큰이 로그에 평문으로 남았습니다 (#55).

## 무엇을 가리나

키 이름에 민감 단어가 **포함되면**(대소문자 무시) 가립니다. 그래서 `token` 하나로 `accessToken`, `refreshToken`, `idToken`, `pushToken`이 모두 잡힙니다.

내장 필드 키: `password`, `passwd`, `pwd`, `secret`, `token`, `authorization`, `credential`, `apikey`, `api_key`, `api-key`, `csrf`, `cookie`, `privatekey`, `private_key`

내장 헤더 키: `authorization`, `cookie`, `set-cookie`, `x-api-key`, `x-auth-token`, `x-csrf-token`, `x-xsrf-token`

`pii` 프리셋은 `email`, `phone`, `mobile`, `ssn`, `residentnumber`, `address`를 더합니다. 디버깅에 필요한 경우가 많아 기본값에서는 뺐습니다.

## 어디에 적용되나

| 대상 | 방식 |
|---|---|
| 메서드 파라미터 | 각 인자를 트리(Map·List·값)로 바꾼 뒤, 어느 깊이에 있든 민감 키의 값을 하위 객체째 가림 |
| 반환값 | 파라미터와 같음. `ResponseEntity`의 body 포함 |
| HTTP 응답 본문 | 앱의 `ObjectMapper`/`JsonMapper`로 JSON을 읽어 가림. 없으면 `"key": value` 패턴으로 가림. pretty print·사용자 포매터보다 **먼저** 적용 |
| 요청 헤더 | `header.enabled`를 켰을 때 |

트리 변환은 앱의 Jackson 설정을 먼저 씁니다 (`@JsonIgnore`, 이름 규칙, Kotlin 모듈 반영). 실패하면 리플렉션으로 필드를 읽습니다. JPA 엔티티와 Hibernate 프록시는 지연 로딩 쿼리를 일으킬 수 있어 변환하지 않고 `toString()`을 그대로 둡니다.

## 메서드별 제어

```java
@LogMonitor(mask = TriState.ON, maskFields = {"cardNumber"})   // 강제로 켜고 키 추가
public void pay(PaymentRequest request) { ... }

@LogCall(mask = TriState.OFF)                                  // 이 메서드는 가리지 않음
public void debugDump(Object state) { ... }
```

## 설정

```yaml
suh-logger:
  masking:
    mask-fields: [ cardNumber, ssn ]   # 기본 목록에 추가
    presets: [ pii ]
    use-defaults: false                # 내 목록만 쓰고 싶을 때만
    mask-value: "[hidden]"
```

응답 본문을 남기는 상태에서 `masking.enabled=false`로 끄면 기동 시 경고가 남습니다.

## 주의

- 포함 비교라 `tokenCount` 같은 무해한 키도 가려질 수 있습니다. 더 많이 가리는 쪽이 안전한 실패라서 이렇게 설계했습니다.
- 마스킹은 suh-logger가 남기는 로그에만 적용됩니다. 앱이 직접 남기는 로그는 대상이 아닙니다.
