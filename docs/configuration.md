# 설정 가이드

`application.yml`에서 suh-logger의 모든 설정을 관리할 수 있습니다.

## 전체 설정 옵션

```yaml
suh-logger:
  # 전체 로깅 활성화 (기본값: true)
  enabled: true

  # JSON 예쁜 출력 (기본값: false)
  pretty-print-json: false

  # 응답 본문 최대 크기 (기본값: 4096 bytes)
  max-response-body-size: 4096

  # 로깅 제외 URL 패턴
  exclude-patterns:
    - "/actuator"
    - "/health"
    - "/swagger"

  # JSON 직렬화 제외 클래스
  excluded-classes:
    - "org.springframework.web.multipart.MultipartFile"
    - "org.locationtech.jts.geom.Point"

  # 헤더 설정
  header:
    enabled: false              # 헤더 출력 활성화 (기본값: false)
    include-all: false          # 모든 헤더 출력 (기본값: false)
    include-headers:            # 출력할 헤더 목록 (include-all: false일 때)
      - Content-Type
      - X-Request-ID

  # 마스킹 설정
  masking:
    enabled: false              # 마스킹 활성화 (기본값: false)
    mask-value: "****"          # 마스킹 값 (기본값: ****)
    mask-headers:               # 마스킹할 헤더 키워드
      - Authorization
      - Cookie
      - token
    mask-fields:                # 마스킹할 필드 키워드
      - password
      - secret
      - apiKey
```

## 설정 상세

### enabled

전체 로깅 기능을 제어합니다.

```yaml
suh-logger:
  enabled: false  # 모든 로깅 비활성화
```

`false`로 설정하면 `@LogCall`, `@LogMonitor`, `@LogTime` 어노테이션이 동작하지 않습니다.

### pretty-print-json

JSON 출력 형식을 제어합니다.

```yaml
suh-logger:
  pretty-print-json: true  # 들여쓰기된 JSON 출력
```

**pretty-print-json: false (기본값)**
```
{"id":123,"name":"홍길동"}
```

**pretty-print-json: true**
```json
{
  "id" : 123,
  "name" : "홍길동"
}
```

### max-response-body-size

응답 본문 로깅 시 최대 크기를 제한합니다.

```yaml
suh-logger:
  max-response-body-size: 2048  # 2KB로 제한
```

### exclude-patterns

특정 URL 패턴을 로깅에서 제외합니다.

```yaml
suh-logger:
  exclude-patterns:
    - "/actuator"
    - "/health"
    - "/swagger"
    - "/api/v1/internal"
```

### excluded-classes

JSON 직렬화에서 제외할 클래스를 지정합니다. 순환 참조나 직렬화 문제가 있는 객체에 유용합니다.

```yaml
suh-logger:
  excluded-classes:
    - "org.springframework.web.multipart.MultipartFile"
    - "org.locationtech.jts.geom.Point"
    - "com.example.InternalObject"
```

### header 설정

HTTP 헤더 로깅을 제어합니다. 자세한 내용은 [헤더 로깅 가이드](header-logging.md)를 참조하세요.

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `enabled` | boolean | false | 헤더 출력 활성화 |
| `include-all` | boolean | false | 모든 헤더 출력 |
| `include-headers` | List | [] | 출력할 헤더 목록 |

### masking 설정

민감 정보 마스킹을 제어합니다. 자세한 내용은 [마스킹 가이드](masking.md)를 참조하세요.

| 속성 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `enabled` | boolean | false | 마스킹 활성화 |
| `mask-value` | String | "****" | 마스킹 값 |
| `mask-headers` | List | [] | 마스킹할 헤더 키워드 |
| `mask-fields` | List | [] | 마스킹할 필드 키워드 |

## 환경별 권장 설정

### 개발 환경

```yaml
suh-logger:
  enabled: true
  pretty-print-json: true
  header:
    enabled: true
    include-all: true
  masking:
    enabled: false  # 개발 시 원본 데이터 확인
```

### 운영 환경

```yaml
suh-logger:
  enabled: true
  pretty-print-json: false  # 로그 크기 절약
  max-response-body-size: 2048
  exclude-patterns:
    - "/actuator"
    - "/health"
  header:
    enabled: false  # 보안상 헤더 제외
  masking:
    enabled: true
    mask-headers:
      - Authorization
      - Cookie
    mask-fields:
      - password
      - secret
      - token
      - apiKey
```

### 테스트 환경

```yaml
suh-logger:
  enabled: true
  pretty-print-json: true
  header:
    enabled: true
    include-headers:
      - Content-Type
      - X-Request-ID
  masking:
    enabled: true
    mask-fields:
      - password
```

## 자동 제외되는 요청

성능 최적화를 위해 다음 요청은 자동으로 로깅에서 제외됩니다:

- 정적 리소스: `/static/*`, `/css/*`, `/js/*`, `/images/*`
- 파일 확장자: `*.ico`, `*.png`, `*.jpg`, `*.css`, `*.js`
