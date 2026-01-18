# API 레퍼런스

suh-logger가 제공하는 유틸리티 클래스의 전체 API를 설명합니다.

## SuhLogger

로깅 유틸리티 클래스입니다. 정적 메서드와 인스턴스 메서드를 모두 제공합니다.

### 정적 메서드 (권장)

#### 객체 로깅

| 메서드 | 설명 |
|--------|------|
| `superLog(Object obj)` | INFO 레벨로 객체를 JSON 형식으로 출력 |
| `superLog(Object obj, boolean showClassName)` | 클래스명 출력 여부 선택 |
| `superLogDebug(Object obj)` | DEBUG 레벨로 출력 |
| `superLogWarn(Object obj)` | WARN 레벨로 출력 |
| `superLogError(Object obj)` | ERROR 레벨로 출력 |

```java
// 객체를 JSON으로 예쁘게 출력
SuhLogger.superLog(userDto);

// 클래스명 없이 출력
SuhLogger.superLog(userDto, false);
```

#### 단순 메시지 로깅

| 메서드 | 설명 |
|--------|------|
| `info(String message)` | INFO 레벨 로그 |
| `debug(String message)` | DEBUG 레벨 로그 |
| `warn(String message)` | WARN 레벨 로그 |
| `error(String message)` | ERROR 레벨 로그 |
| `error(String message, Throwable t)` | 예외와 함께 ERROR 로그 |

```java
SuhLogger.info("처리 시작");
SuhLogger.error("처리 실패", exception);
```

#### 구분선 출력

| 메서드 | 설명 |
|--------|------|
| `lineLog(String title)` | INFO 레벨 구분선 |
| `lineLogDebug(String title)` | DEBUG 레벨 구분선 |
| `lineLogWarn(String title)` | WARN 레벨 구분선 |
| `lineLogError(String title)` | ERROR 레벨 구분선 |
| `divider()` | 단순 구분선 |
| `logHeader(String title)` | 중앙 정렬 헤더 |

```java
SuhLogger.lineLog("처리 시작");
// 출력: ======================== 처리 시작 ========================

SuhLogger.divider();
// 출력: ============================================================
```

#### 실행 시간 측정

```java
SuhLogger.timeLog(() -> {
    // 측정할 작업
    heavyProcessing();
});
// 출력: ============ [methodName] 실행 시간: 1초 234ms ============
```

#### JSON 로깅

```java
SuhLogger.infoJson("사용자 정보", userObject);
```

#### 로그 레벨 설정

```java
// enum으로 설정
SuhLogger.setLogLevel(SuhLogger.LogLevel.DEBUG);

// JUL Level로 직접 설정
SuhLogger.setLogLevel(Level.FINE);
```

#### 파일 로깅

```java
SuhLogger.addFileLogger("/var/log/myapp/app.log");
```

### 인스턴스 메서드 (SLF4J 스타일)

클래스별 로거 인스턴스를 생성하여 SLF4J 스타일로 사용할 수 있습니다.

```java
public class MyService {
    private static final SuhLogger log = SuhLogger.getLogger(MyService.class);

    public void process() {
        log.infoMsg("처리 시작: userId={}", userId);
        log.debugMsg("상세 정보: {}, {}", param1, param2);
        log.warnMsg("경고: {}", message);
        log.errorMsg("에러 발생", exception);
    }
}
```

| 메서드 | 설명 |
|--------|------|
| `debugMsg(String message, Object... args)` | DEBUG 레벨 ({} 플레이스홀더 지원) |
| `infoMsg(String message, Object... args)` | INFO 레벨 |
| `warnMsg(String message, Object... args)` | WARN 레벨 |
| `errorMsg(String message, Object... args)` | ERROR 레벨 |
| `errorMsg(String message, Throwable t)` | 예외와 함께 ERROR |

#### 레벨 체크

```java
if (log.isDebugEnabled()) {
    log.debugMsg("비용이 큰 연산: {}", expensiveOperation());
}
```

| 메서드 | 설명 |
|--------|------|
| `isDebugEnabled()` | DEBUG 레벨 활성화 여부 |
| `isInfoEnabled()` | INFO 레벨 활성화 여부 |
| `isWarnEnabled()` | WARN 레벨 활성화 여부 |
| `isErrorEnabled()` | ERROR 레벨 활성화 여부 |

---

## SuhTimeUtil

시간 관련 유틸리티 클래스입니다.

### 상수

| 상수 | 값 |
|------|-----|
| `BASIC_DATE_TIME_FORMAT` | `"yyyy-MM-dd HH:mm:ss"` |
| `FILENAME_DATE_TIME_FORMAT` | `"yyyyMMdd_HHmmss"` |
| `DATE_TIME_MILLIS_FORMAT` | `"yyyy-MM-dd HH:mm:ss.SSS"` |
| `KOREAN_DATE_TIME_FORMAT` | `"yyyy년 MM월 dd일 HH시 mm분 ss초"` |

### 현재 시간 포맷팅

| 메서드 | 출력 예시 |
|--------|----------|
| `formatLocalDateTimeNow()` | `"2026-01-18 14:30:45"` |
| `formatLocalDateTimeMillisNow()` | `"2026-01-18 14:30:45.123"` |
| `formatLocalDateTimeNowForFileName()` | `"20260118_143045"` |
| `readableCurrentLocalDateTime()` | `"2026년 01월 18일 14시 30분 45초"` |

```java
String now = SuhTimeUtil.formatLocalDateTimeNow();
// "2026-01-18 14:30:45"

String fileName = "backup_" + SuhTimeUtil.formatLocalDateTimeNowForFileName() + ".zip";
// "backup_20260118_143045.zip"
```

### 시간 변환

| 메서드 | 설명 |
|--------|------|
| `readableLocalDateTime(LocalDateTime)` | LocalDateTime을 한국어 형식으로 변환 |
| `convertMillisToReadableTime(long)` | 밀리초를 읽기 쉬운 형식으로 변환 |
| `convertDurationToReadableTime(Duration)` | Duration을 읽기 쉬운 형식으로 변환 |

```java
SuhTimeUtil.convertMillisToReadableTime(850);
// "850미리초"

SuhTimeUtil.convertMillisToReadableTime(55000);
// "55초"

SuhTimeUtil.convertMillisToReadableTime(125000);
// "2분 5초"
```

---

## CommonUtil

객체 직렬화 및 마스킹 관련 유틸리티 클래스입니다.

### 상수

| 상수 | 값 | 설명 |
|------|-----|------|
| `DEFAULT_MASK_VALUE` | `"****"` | 기본 마스킹 값 |

### 마스킹 관련

| 메서드 | 설명 |
|--------|------|
| `isSensitive(String name, List<String> keywords)` | 민감한 필드/헤더인지 확인 |
| `getMaskValue(MaskingConfig config)` | 마스킹 값 반환 |
| `maskParameters(Map params, List maskFields, String maskValue)` | 파라미터 맵 마스킹 |
| `maskHeaders(Map headers, List maskHeaders, String maskValue)` | 헤더 맵 마스킹 |

```java
// 민감 필드 체크
boolean sensitive = CommonUtil.isSensitive("userPassword", Arrays.asList("password", "secret"));
// true

// 파라미터 마스킹
Map<String, Object> params = new HashMap<>();
params.put("username", "john");
params.put("password", "secret123");

Map<String, Object> masked = CommonUtil.maskParameters(
    params,
    Arrays.asList("password"),
    "****"
);
// {username=john, password=****}
```

### 직렬화 관련

| 메서드 | 설명 |
|--------|------|
| `makeSafeForSerialization(Object obj)` | 객체를 안전한 형태로 변환 |
| `makeSafeForSerialization(Object obj, List excludedClasses)` | 제외 클래스 지정 |
| `createSafeMap(Object obj)` | 객체를 Map으로 변환 |
| `createSafeMap(Object obj, MaskingConfig config)` | 마스킹 적용하여 Map 변환 |

```java
// 순환 참조 객체 안전하게 변환
Object safe = CommonUtil.makeSafeForSerialization(complexObject);

// 특정 클래스 제외
Object safe = CommonUtil.makeSafeForSerialization(
    obj,
    Arrays.asList("org.locationtech.jts.geom.Point")
);
```

### 특수 객체 처리

| 메서드 | 설명 |
|--------|------|
| `extractMultipartFileInfo(Object)` | MultipartFile 정보 추출 |
| `extractJTSGeometryInfo(Object)` | JTS Geometry 정보 추출 |
| `extractFileInfo(File)` | File 정보 추출 |
| `extractVectorInfo(Vector)` | Vector 정보 추출 |

```java
// MultipartFile 안전하게 로깅
Map<String, Object> fileInfo = CommonUtil.extractMultipartFileInfo(multipartFile);
// {_type=MultipartFile, fileName=test.pdf, contentType=application/pdf, size=1024}

// JTS Point 안전하게 로깅
Map<String, Object> pointInfo = CommonUtil.extractJTSGeometryInfo(point);
// {_type=JTS_Geometry, _class=Point, x=127.123, y=37.456, srid=4326}
```

### 타입 체크

| 메서드 | 설명 |
|--------|------|
| `isMultipartFileType(Object)` | MultipartFile 타입인지 확인 |
| `isJTSGeometryType(Object)` | JTS Geometry 타입인지 확인 |
| `isExcludedClass(Object, List)` | 제외 클래스인지 확인 |
