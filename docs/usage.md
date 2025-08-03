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
     implementation 'me.suhsaechan:suh-logger:1.0.9'
   }

2. 순수 Java 클래스에서 바로 호출

```java
import me.suhsaechan.suhlogger.util.SuhLogger;

public class SampleService {

   public void run() {
// 구분선 로그
      SuhLogger.lineLog("Service Start");

      // 비즈니스 로직 수행
      SuhLogger.timeLog(this::doWork);

      // 종료 구분선
      SuhLogger.lineLog("Service End");
   }

   private void doWork() throws Exception {
// ...
   }
}
```

3. SuhTimeUtil 단독 사용

```java
import me.suhsaechan.suhlogger.util.SuhTimeUtil;

String now = SuhTimeUtil.formatLocalDateTimeNow();
log.info("현재 시간: {}", now);
```

