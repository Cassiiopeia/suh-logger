# Suh-Logger API

**Suh-Logger**는 애플리케이션 전반에 걸쳐 일관되고 가독성 높은 로그를 남기기 위한 자바 로깅 유틸리티 라이브러리입니다. JSON 포맷으로 객체를 직관적으로 출력하고, 실행 시간을 자동으로 측정해 보여줌으로써 디버깅과 성능 모니터링 작업을 획기적으로 단순화합니다.

## 1. 패키지 구조
```text
me.suhsaechan.suhlogger.util
├─ SuhLogger.java         # 주요 로그 메서드와 실행 시간 측정 기능 제공
└─ SuhTimeUtil.java       # 시간 포맷팅 및 Duration ↔ 문자열 변환 헬퍼 클래스
```

## 2. 도입 효과
- **가독성 향상**: 복잡한 객체를 자동으로 예쁘게 JSON 출력하여, 로그 분석 시 한눈에 파라미터와 상태를 파악할 수 있습니다.
- **성능 가시화**: 비즈니스 로직의 실행 시간을 자동 측정해 로그로 남김으로써, 성능 병목 지점을 빠르게 찾을 수 있습니다.
- **일관성 있는 포맷**: 구분선 메서드를 통해 로그의 시작·종료를 명확히 구분, 개발·운영 중 발생하는 로그 패턴을 통일합니다.
- **설정 부담 경감**: 별도의 설정 없이 라이브러리 의존성만 추가하면 즉시 사용 가능, AOP 설정과 Jackson 모듈 등록을 내부에서 처리합니다.

## 3. 의존성 추가 (Gradle)
```groovy
repositories {
  maven {
    url "http://suh-project.synology.me:9999/repository/maven-releases/"
    allowInsecureProtocol = true
  }
}

dependencies {
  implementation 'me.suhsaechan:suh-logger:0.0.2'
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
| `convertMillisToReadableTime(long millis)`  | ms 단위 시간을 “n분 m초/ millisecond” 문자열로 변환   |
| `convertDurationToReadableTime(Duration)`   | `Duration` → “n분 m초” 형식으로 변환                |

## 5. 사용 예시
```java
import me.suhsaechan.suhlogger.util.SuhLogger;

// 1) 객체 로그
MyDto dto = new MyDto("Alice", 30);
SuhLogger.superLog(dto);

// 2) 구분선
SuhLogger.lineLog("PROCESS START");

// 3) 실행 시간 측정
SuhLogger.timeLog(() -> {
    // 비즈니스 로직 수행
    processData();
});

// 4) 프로세스 종료 구분선
SuhLogger.lineLog("PROCESS END");
```

---
*자세한 가이드와 기여 방법은 각종 문서(USAGE.md, TROUBLESHOOTING.md, CONTRIBUTING.md) 를 참고하세요.*

