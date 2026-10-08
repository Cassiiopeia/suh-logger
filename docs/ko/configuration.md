# 설정 가이드

모든 키는 `suh-logger.` 아래에 있고 전부 선택입니다. Spring Boot가 자동으로 바인딩하며, IDE는 `spring-configuration-metadata.json`으로 자동완성과 설명을 보여 줍니다. Boot가 없으면 같은 필드를 `SuhLoggerProperties` 빈(XML·JavaConfig)에 설정하거나 `SuhLogger.setProperties(...)`(순수 Java)로 넘깁니다.

## 전체 옵션

| 키 | 기본값 | 설명 |
|---|---|---|
| `enabled` | `true` | 어노테이션·HTTP 로깅 전체 스위치 |
| `format` | `block` | `block`: 구분선으로 둘러싼 여러 줄(2.x 형식). `line`: 요청당 한 줄 — `METHOD URI -> STATUS (Nms) [SLOW] rid=... body=...` |
| `response-body` | `all` | `none`, `error-only`(4xx·5xx만), `all` |
| `max-response-body-size` | `4096` | 이보다 긴 본문은 `[Too large to log - N bytes, max: M]`로 대체. WebFlux는 이 크기까지만 복사 |
| `pretty-print-json` | `false` | JSON 본문 들여쓰기 (`line` 형식에서는 한 줄로 접힘) |
| `slow-threshold-ms` | `0` | 0보다 크면 이보다 느린 요청을 WARN + `[SLOW]`로. 5xx는 항상 WARN |
| `filter-order` | `2147483647` | Servlet 필터·WebFlux `WebFilter` 순서. 기본값은 Spring Security 이후라 최종 상태가 기록됨 |
| `exclude-patterns` | `[/actuator/**]` | 로깅하지 않을 경로. Ant 문법: `*` 한 세그먼트, `**` 여러 세그먼트, `?` 한 글자. **설정하면 기본 목록을 대체**. 와일드카드가 없는 값은 2.x처럼 포함(contains) 비교하고 한 번 경고 |
| `excluded-classes` | `[]` | 이 클래스(또는 이름 일부)의 객체는 `EXCLUDED_CLASS` 표시만 남김 |
| `request-id.enabled` | `false` | 요청 ID를 MDC와 응답 헤더에 넣음. 들어온 헤더 값이 있으면 그대로 사용 |
| `request-id.header` | `X-Request-Id` | 읽고 쓸 헤더 이름 |
| `request-id.mdc-key` | `requestId` | MDC 키 — 로그 패턴에 `%X{requestId}` |
| `masking.enabled` | `true` | 민감 값 마스킹 ([마스킹 가이드](masking.md)) |
| `masking.use-defaults` | `true` | 내장 민감 키 목록 사용 |
| `masking.mask-fields` | `[]` | 추가 필드 키 — 기본 목록에 **더해짐** |
| `masking.mask-headers` | `[]` | 추가 헤더 키 — 기본 목록에 더해짐 |
| `masking.presets` | `[]` | `pii`: email, phone, mobile, ssn, residentnumber, address |
| `masking.mask-value` | `****` | 대체 문자열 |
| `header.enabled` | `false` | `@LogMonitor`/`@LogCall` 출력에 요청 헤더 포함 (Servlet 전용) — [헤더 로깅 가이드](header-logging.md) |
| `header.include-all` | `false` | `include-headers` 대신 전체 헤더 |
| `header.include-headers` | `[]` | 출력할 헤더 이름 |

## 환경별 권장 설정

```yaml
# 로컬 개발: 전부 보기
suh-logger:
  pretty-print-json: true
  header:
    enabled: true
    include-all: true
```

```yaml
# 운영
suh-logger:
  format: line
  response-body: error-only
  slow-threshold-ms: 500
  request-id:
    enabled: true
```

```yaml
# 테스트: 끄기
suh-logger:
  enabled: false
```

## 요청 ID를 로그 패턴에 넣기

```yaml
logging:
  pattern:
    level: "%5p [%X{requestId:-}]"
```

WebFlux에서는 MDC가 스레드 로컬이라 리액티브 체인을 따라가지 않으므로, 요청 ID는 응답 헤더와 로그 줄에만 들어갑니다.

## 정적 리소스

Servlet 필터는 `/static/`, `/css/`, `/js/`, `/images/` 경로와 `.ico`·`.png`·`.jpg`·`.css`·`.js` 파일을 기록하지 않습니다.
