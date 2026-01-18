# 빠른 시작 가이드

5분 안에 suh-logger를 설정하고 사용해보세요.

## 1. 저장소 및 의존성 추가

### Gradle

```groovy
repositories {
    mavenCentral()
    maven {
        url "https://nexus.suhsaechan.kr/repository/maven-releases/"
    }
}

dependencies {
    implementation 'kr.suhsaechan:suh-logger:x.x.x' // 최신 버전으로 변경하세요
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>suh-nexus</id>
        <url>https://nexus.suhsaechan.kr/repository/maven-releases/</url>
    </repository>
</repositories>

<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>suh-logger</artifactId>
    <version>x.x.x</version> <!-- 최신 버전으로 변경하세요 -->
</dependency>
```

## 2. 바로 사용하기

### 어노테이션 기반 로깅

```java
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.annotation.LogCall;
import kr.suhsaechan.suhlogger.annotation.LogTime;

@Service
public class UserService {

    // 파라미터 + 반환값 + 실행시간 자동 로깅
    @LogMonitor
    public User findUser(Long userId) {
        return userRepository.findById(userId);
    }

    // 파라미터 + 반환값만 로깅
    @LogCall
    public List<User> searchUsers(String keyword) {
        return userRepository.findByKeyword(keyword);
    }

    // 실행시간만 측정
    @LogTime
    public void heavyProcessing() {
        // 무거운 작업
    }
}
```

### 직접 로깅

```java
import kr.suhsaechan.suhlogger.util.SuhLogger;

public class MyService {

    public void process() {
        // 구분선 출력
        SuhLogger.lineLog("처리 시작");

        // 객체를 JSON 형식으로 출력
        SuhLogger.superLog(myObject);

        // 실행 시간 측정
        SuhLogger.timeLog(() -> {
            expensiveOperation();
        });

        SuhLogger.lineLog("처리 완료");
    }
}
```

## 3. 출력 예시

```
======================== [UserService.findUser] CALL =========================
========================== CALL PARAMETER ====================================
{
  "userId" : 123
}
======================== [UserService.findUser] RESULT =======================
{
  "id" : 123,
  "name" : "홍길동",
  "email" : "hong@example.com"
}
===================== [TIME]: UserService.findUser : 45 ms ===================
```

## 4. 다음 단계

- [어노테이션 상세](annotations.md) - `@LogCall`, `@LogMonitor`, `@LogTime` 옵션
- [설정 옵션](configuration.md) - application.yml 전체 설정
- [마스킹 기능](masking.md) - 민감 정보 마스킹
- [헤더 로깅](header-logging.md) - HTTP 헤더 로깅 제어
- [API 레퍼런스](api-reference.md) - SuhLogger, SuhTimeUtil 전체 API
