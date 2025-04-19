# Suh-Logger API

## 1. 패키지 구조

```text
me.suhsaechan.suhlogger.util
├─ SuhLogger.java
└─ SuhTimeUtil.java
```


## 2. 주요 클래스 및 메서드

### SuhLogger
| 메서드                    | 설명                                     |
|-------------------------|----------------------------------------|
| `superLog(Object obj)`      | INFO 레벨로 `obj`를 JSON 형태로 출력         |
| `superLogDebug(Object obj)` | DEBUG 레벨로 `obj`를 JSON 형태로 출력        |
| `superLogWarn(Object obj)`  | WARN 레벨로 `obj`를 JSON 형태로 출력         |
| `superLogError(Object obj)` | ERROR 레벨로 `obj`를 JSON 형태로 출력        |
| `lineLog(String title)`     | INFO 레벨로 구분선 출력 (제목 없음)          |
| `lineLog(String title)`     | INFO 레벨로 `title`을 가운데 둔 구분선 출력 |
| `timeLog(ThrowingRunnable task)` | `task`의 실행 시간을 측정해 로그로 출력        |

### SuhTimeUtil
| 메서드                                | 설명                                               |
|------------------------------------|--------------------------------------------------|
| `formatLocalDateTimeNow()`         | `yyyy-MM-dd HH:mm:ss` 형식의 현재 시간 반환            |
| `formatLocalDateTimeMillisNow()`   | `yyyy-MM-dd HH:mm:ss.SSS` 형식의 현재 시간 반환        |
| `formatLocalDateTimeNowForFileName()` | `yyyyMMdd_HHmmss` 형식의 현재 시간 반환              |
| `readableCurrentLocalDateTime()`   | `yyyy년 MM월 dd일 HH시 mm분 ss초` 형식의 현재 시간 반환  |
| `convertMillisToReadableTime(long)`  | millis → “n분 m초” 혹은 “n초/밀리초” 형식으로 변환    |
| `convertDurationToReadableTime(Duration)` | `Duration` → “n분 m초” 형식으로 변환                 |

## 3. 사용 예시

```java
import me.suhsaechan.suhlogger.util.SuhLogger;

// 객체를 JSON 으로 예쁘게
MyDto dto = new MyDto(...);
SuhLogger.superLog(dto);

// 단순 구분선
SuhLogger.lineLog("START");

// 실행 시간 측정
SuhLogger.timeLog(() -> {
    // doSomething();
});

# Suh-Logger 사용법

1. 의존성 추가 (Gradle)
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

