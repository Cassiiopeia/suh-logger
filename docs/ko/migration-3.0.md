# 2.x → 3.0 마이그레이션

2.x로 작성한 코드는 그대로 컴파일됩니다. 다음은 바뀌지 않았습니다.

- 의존성 `kr.suhsaechan:suh-logger` (이제 `suh-logger-spring-boot-starter`를 가져옴)
- `kr.suhsaechan.suhlogger.annotation.*`, `kr.suhsaechan.suhlogger.util.SuhLogger`·`SuhTimeUtil`·`CommonUtil`
- `kr.suhsaechan.suhlogger.config.SuhLoggerProperties`와 모든 `suh-logger.*` 키

## 확인해야 할 동작 변경

| 변경 | 2.x | 3.0 | 예전처럼 하려면 |
|---|---|---|---|
| 마스킹 | 꺼짐 | **켜짐**, 내장 민감 키 사용 | `suh-logger.masking.enabled=false` (기동 시 경고) |
| `mask-fields` | 이 목록만 사용 | 기본 목록에 **추가** | `suh-logger.masking.use-defaults=false` |
| 에러 응답 | 기록 안 함 | 4xx·5xx 기록(처리되지 않은 예외 포함), 5xx는 WARN | `suh-logger.response-body=none`이면 본문은 숨김(상태 줄은 남음) |
| 기본 제외 경로 | 없음 | `/actuator/**` | `exclude-patterns`를 직접 지정 (기본 목록을 대체) |
| `exclude-patterns` 비교 | 포함(contains) | Ant 패턴. 와일드카드 없는 값은 포함 비교를 유지하되 한 번 경고 | `auth/login` → `/api/auth/login/**` |
| DTO 출력 | `toString()` | 필드 트리(JSON 형태), 엔티티는 연관을 열지 않은 필드 트리 | — |
| 요청 로그 | 시간 없음 | block 형식에 `Duration: N ms` 줄 | — |
| 제외 클래스 표시 | `_toString`에 원문 포함 | 타입 정보만 (#39) | — |

## 옮겨지거나 삭제된 클래스

| 2.x | 3.0 |
|---|---|
| `kr.suhsaechan.suhlogger.config.SuhLoggerAutoConfiguration` | `kr.suhsaechan.suhlogger.boot.autoconfigure.SuhLoggerAutoConfiguration` (+ Servlet·Reactive·Json 자동설정). `@SpringBootApplication(exclude = ...)`로 썼다면 새 이름으로 |
| `SuhLoggerAutoConfiguration.SuhLoggerInitializer` | `kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration.SuhLoggerInitializer` |
| jar 안의 `kr.suhsaechan.suhlogger.SuhLoggerApplication` | 삭제 — 라이브러리가 앱 클래스를 가지면 안 됨 |
| jar 안의 `application.properties` | 삭제 — 사용자 설정을 가릴 수 있었음 |
| `META-INF/spring.factories` | 삭제 — Boot 3+는 `AutoConfiguration.imports`만 읽음 |
| aspect의 필드 주입(`@Autowired`) | 생성자 주입. 컴포넌트 스캔이 아니라 `SuhLoggerConfiguration`이 등록 |
| 전이 의존 `jackson-databind` 2.18.3 (`api`) | 내보내지 않음 — 앱의 Jackson 버전 사용 (Boot 3는 Jackson 2, Boot 4는 Jackson 3) |

## 체크리스트

- [ ] 마스킹되지 않은 값을 기대하던 로그 검사·테스트를 고친다.
- [ ] `/actuator` 요청 로그가 필요했다면 `exclude-patterns`를 직접 지정한다.
- [ ] 포함 비교 제외 패턴을 Ant 패턴으로 바꿔 경고를 없앤다.
- [ ] 자동설정을 클래스로 exclude 했다면 새 클래스 이름으로 바꾼다.
- [ ] Kotlin: `kotlin-spring` 플러그인이 적용됐는지 확인한다 (기동 시 경고를 확인).
