package me.suhsaechan.suhlogger.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.suhsaechan.suhlogger.config.SuhLoggerConfig;
import me.suhsaechan.suhlogger.config.SuhLoggerProperties;

/**
 * SuhLogger 유틸리티 클래스
 * Java Util Logging을 사용한 로깅 유틸리티
 * 
 * 새로운 기능:
 * - SLF4J 호환 API ({} 플레이스홀더 지원) - 인스턴스 메서드로 제공
 * - 클래스별 로거 인스턴스 지원: SuhLogger.getLogger(MyClass.class)
 * - 성능 최적화 (레벨 체크 선행)
 * - 기존 정적 메서드 API는 호환성을 위해 유지
 */
public class SuhLogger {

    private static final int LINE_LENGTH = 60; // "=" 줄에 대한 최대 길이 지정
    private static final String SEPARATOR_CHAR = "=";
    
    // 로거 인스턴스
    private static final Logger logger = SuhLoggerConfig.getLogger();
    
    
    // 설정 프로퍼티 (Spring Context에서 주입받을 수 있도록)
    private static SuhLoggerProperties properties;
    
    // 클래스별 로거 인스턴스 캐시
    private static final ConcurrentHashMap<String, SuhLogger> loggerCache = new ConcurrentHashMap<>();
    
    // 인스턴스 필드
    private final Logger julLogger;
    private final String loggerName;

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성
     */
    private SuhLogger(String name) {
        this.loggerName = name;
        this.julLogger = SuhLoggerConfig.getLogger();
    }

    /**
     * 클래스 기반 로거 생성 (권장 방식)
     */
    public static SuhLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * 이름 기반 로거 생성
     */
    public static SuhLogger getLogger(String name) {
        return loggerCache.computeIfAbsent(name, SuhLogger::new);
    }

    // ========== SLF4J 스타일 인스턴스 메서드들 ==========

    /**
     * DEBUG 레벨 로그 출력 (SLF4J 스타일)
     */
    public void debugMsg(String message) {
        if (julLogger.isLoggable(Level.FINE)) {
            julLogger.log(Level.FINE, message);
        }
    }

    /**
     * DEBUG 레벨 로그 출력 (단일 파라미터, SLF4J 스타일)
     */
    public void debugMsg(String message, Object arg) {
        if (julLogger.isLoggable(Level.FINE)) {
            julLogger.log(Level.FINE, formatMessage(message, arg));
        }
    }

    /**
     * DEBUG 레벨 로그 출력 (다중 파라미터, SLF4J 스타일)
     */
    public void debugMsg(String message, Object... args) {
        if (julLogger.isLoggable(Level.FINE)) {
            julLogger.log(Level.FINE, formatMessage(message, args));
        }
    }

    /**
     * INFO 레벨 로그 출력 (SLF4J 스타일)
     */
    public void infoMsg(String message) {
        if (julLogger.isLoggable(Level.INFO)) {
            julLogger.log(Level.INFO, message);
        }
    }

    /**
     * INFO 레벨 로그 출력 (단일 파라미터, SLF4J 스타일)
     */
    public void infoMsg(String message, Object arg) {
        if (julLogger.isLoggable(Level.INFO)) {
            julLogger.log(Level.INFO, formatMessage(message, arg));
        }
    }

    /**
     * INFO 레벨 로그 출력 (다중 파라미터, SLF4J 스타일)
     */
    public void infoMsg(String message, Object... args) {
        if (julLogger.isLoggable(Level.INFO)) {
            julLogger.log(Level.INFO, formatMessage(message, args));
        }
    }

    /**
     * WARN 레벨 로그 출력 (SLF4J 스타일)
     */
    public void warnMsg(String message) {
        if (julLogger.isLoggable(Level.WARNING)) {
            julLogger.log(Level.WARNING, message);
        }
    }

    /**
     * WARN 레벨 로그 출력 (단일 파라미터, SLF4J 스타일)
     */
    public void warnMsg(String message, Object arg) {
        if (julLogger.isLoggable(Level.WARNING)) {
            julLogger.log(Level.WARNING, formatMessage(message, arg));
        }
    }

    /**
     * WARN 레벨 로그 출력 (다중 파라미터, SLF4J 스타일)
     */
    public void warnMsg(String message, Object... args) {
        if (julLogger.isLoggable(Level.WARNING)) {
            julLogger.log(Level.WARNING, formatMessage(message, args));
        }
    }

    /**
     * ERROR 레벨 로그 출력 (SLF4J 스타일)
     */
    public void errorMsg(String message) {
        if (julLogger.isLoggable(Level.SEVERE)) {
            julLogger.log(Level.SEVERE, message);
        }
    }

    /**
     * ERROR 레벨 로그 출력 (단일 파라미터, SLF4J 스타일)
     */
    public void errorMsg(String message, Object arg) {
        if (julLogger.isLoggable(Level.SEVERE)) {
            julLogger.log(Level.SEVERE, formatMessage(message, arg));
        }
    }

    /**
     * ERROR 레벨 로그 출력 (다중 파라미터, SLF4J 스타일)
     */
    public void errorMsg(String message, Object... args) {
        if (julLogger.isLoggable(Level.SEVERE)) {
            julLogger.log(Level.SEVERE, formatMessage(message, args));
        }
    }

    /**
     * ERROR 레벨 로그 출력 (예외 포함, SLF4J 스타일)
     */
    public void errorMsg(String message, Throwable throwable) {
        if (julLogger.isLoggable(Level.SEVERE)) {
            julLogger.log(Level.SEVERE, message, throwable);
        }
    }

    // ========== 레벨 체크 메서드 ==========

    /**
     * DEBUG 레벨 활성화 여부 확인
     */
    public boolean isDebugEnabled() {
        return julLogger.isLoggable(Level.FINE);
    }

    /**
     * INFO 레벨 활성화 여부 확인
     */
    public boolean isInfoEnabled() {
        return julLogger.isLoggable(Level.INFO);
    }

    /**
     * WARN 레벨 활성화 여부 확인
     */
    public boolean isWarnEnabled() {
        return julLogger.isLoggable(Level.WARNING);
    }

    /**
     * ERROR 레벨 활성화 여부 확인
     */
    public boolean isErrorEnabled() {
        return julLogger.isLoggable(Level.SEVERE);
    }

    // ========== 기존 호환성을 위한 정적 메서드들 ==========

    /**
     * 간단한 JSON 형태의 문자열 생성 (Jackson 대체)
     */
    private static String toSimpleJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj instanceof String) {
            return "\"" + obj.toString().replace("\"", "\\\"") + "\"";
        }
        
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        // 객체를 safe serialization 통해 Map으로 변환 후 JSON-like 문자열 생성
        Object safeObject = (properties != null) 
            ? commonUtil.makeSafeForSerialization(obj, properties.getExcludedClasses())
            : commonUtil.makeSafeForSerialization(obj);
            
        return objectToJsonString(safeObject, 0);
    }
    
    /**
     * 객체를 JSON-like 문자열로 변환 (재귀적)
     */
    private static String objectToJsonString(Object obj, int depth) {
        if (obj == null) return "null";
        if (depth > 10) return "\"[MAX_DEPTH_REACHED]\""; // 무한 재귀 방지
        
        if (obj instanceof String) {
            return "\"" + obj.toString().replace("\"", "\\\"") + "\"";
        }
        
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{\n");
            String indent = "  ".repeat(depth + 1);
            boolean first = true;
            
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",\n");
                sb.append(indent)
                  .append("\"").append(entry.getKey()).append("\": ")
                  .append(objectToJsonString(entry.getValue(), depth + 1));
                first = false;
            }
            
            sb.append("\n").append("  ".repeat(depth)).append("}");
            return sb.toString();
        }
        
        if (obj instanceof Collection) {
            Collection<?> collection = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[\n");
            String indent = "  ".repeat(depth + 1);
            boolean first = true;
            
            for (Object item : collection) {
                if (!first) sb.append(",\n");
                sb.append(indent).append(objectToJsonString(item, depth + 1));
                first = false;
            }
            
            sb.append("\n").append("  ".repeat(depth)).append("]");
            return sb.toString();
        }
        
        if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            StringBuilder sb = new StringBuilder("[\n");
            String indent = "  ".repeat(depth + 1);
            
            for (int i = 0; i < array.length; i++) {
                if (i > 0) sb.append(",\n");
                sb.append(indent).append(objectToJsonString(array[i], depth + 1));
            }
            
            sb.append("\n").append("  ".repeat(depth)).append("]");
            return sb.toString();
        }
        
        // 기본적으로 toString() 사용
        return "\"" + obj.toString().replace("\"", "\\\"") + "\"";
    }
    
    /**
     * 설정 프로퍼티 설정 (Spring에서 주입받을 때 사용)
     */
    public static void setProperties(SuhLoggerProperties properties) {
        SuhLogger.properties = properties;
    }
    
    /**
     * 로그 레벨을 정의
     */
    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * INFO 레벨 로그 출력
     */
    public static void info(String message) {
        logger.log(Level.INFO, message);
    }

    /**
     * WARN 레벨 로그 출력
     */
    public static void warn(String message) {
        logger.log(Level.WARNING, message);
    }

    /**
     * ERROR 레벨 로그 출력
     */
    public static void error(String message) {
        logger.log(Level.SEVERE, message);
    }

    /**
     * DEBUG 레벨 로그 출력
     */
    public static void debug(String message) {
        logger.log(Level.FINE, message);
    }

    /**
     * 예외와 함께 ERROR 레벨 로그 출력
     */
    public static void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    /**
     * 객체를 JSON 형식으로 로그 출력
     */
    public static void infoJson(String message, Object object) {
        try {
            String jsonString = toSimpleJson(object);
            logger.log(Level.INFO, message + "\n" + jsonString);
        } catch (Exception e) {
            error("JSON 변환 실패", e);
        }
    }
    
    /**
     * 로거 레벨 설정 메서드
     */
    public static void setLogLevel(LogLevel level) {
        switch (level) {
            case DEBUG:
                SuhLoggerConfig.setLogLevel(Level.FINE);
                break;
            case INFO:
                SuhLoggerConfig.setLogLevel(Level.INFO);
                break;
            case WARN:
                SuhLoggerConfig.setLogLevel(Level.WARNING);
                break;
            case ERROR:
                SuhLoggerConfig.setLogLevel(Level.SEVERE);
                break;
        }
    }

    /**
     * JUL 레벨을 직접 설정하는 메서드
     */
    public static void setLogLevel(Level level) {
        SuhLoggerConfig.setLogLevel(level);
    }
    
    /**
     * 로그 파일 핸들러 추가
     */
    public static void addFileLogger(String logFilePath) {
        SuhLoggerConfig.addFileHandler(logFilePath);
    }
    
    /**
     * 구분선 출력 (상단)
     */
    public static void topDivider(String title) {
        logger.log(Level.INFO, "============= " + title + " =============");
    }
    
    /**
     * 구분선 출력 (하단)
     */
    public static void bottomDivider(String title) {
        logger.log(Level.INFO, "============ " + title + " ============");
    }
    
    /**
     * 구분선 출력 (기본)
     */
    public static void divider() {
        logger.log(Level.INFO, "============================================================");
    }
    
    /**
     * 중앙 정렬된 제목으로 구분선 출력
     */
    public static void logHeader(String title) {
        String separatorLine = SEPARATOR_CHAR.repeat(LINE_LENGTH);
        logger.log(Level.INFO, separatorLine);
        
        int titleLength = title.length();
        int paddingSize = (LINE_LENGTH - titleLength) / 2;
        
        if (paddingSize > 0) {
            String padding = " ".repeat(paddingSize);
            logger.log(Level.INFO, padding + title);
        } else {
            logger.log(Level.INFO, title);
        }
        
        logger.log(Level.INFO, separatorLine);
    }
    
    /**
     * 입력스트림의 내용을 읽어 로그로 출력
     */
    public static void logStream(InputStream stream) {
        try {
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while ((bytesRead = stream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, bytesRead));
            }
            
            logger.log(Level.INFO, sb.toString());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "스트림 로깅 실패", e);
        }
    }

    /**
     * 객체를 INFO 레벨로 로그 출력 (클래스명 포함)
     */
    public static void superLog(Object obj) {
        superLogImpl(obj, LogLevel.INFO, true);
    }

    /**
     * 객체를 INFO 레벨로 로그 출력 (클래스명 출력 여부 선택)
     */
    public static void superLog(Object obj, boolean showClassName) {
        superLogImpl(obj, LogLevel.INFO, showClassName);
    }

    /**
     * 객체를 DEBUG 레벨로 로그 출력 (클래스명 포함)
     */
    public static void superLogDebug(Object obj) {
        superLogImpl(obj, LogLevel.DEBUG, true);
    }

    /**
     * 객체를 DEBUG 레벨로 로그 출력 (클래스명 출력 여부 선택)
     */
    public static void superLogDebug(Object obj, boolean showClassName) {
        superLogImpl(obj, LogLevel.DEBUG, showClassName);
    }

    /**
     * 객체를 WARN 레벨로 로그 출력 (클래스명 포함)
     */
    public static void superLogWarn(Object obj) {
        superLogImpl(obj, LogLevel.WARN, true);
    }

    /**
     * 객체를 WARN 레벨로 로그 출력 (클래스명 출력 여부 선택)
     */
    public static void superLogWarn(Object obj, boolean showClassName) {
        superLogImpl(obj, LogLevel.WARN, showClassName);
    }

    /**
     * 객체를 ERROR 레벨로 로그 출력 (클래스명 포함)
     */
    public static void superLogError(Object obj) {
        superLogImpl(obj, LogLevel.ERROR, true);
    }

    /**
     * 객체를 ERROR 레벨로 로그 출력 (클래스명 출력 여부 선택)
     */
    public static void superLogError(Object obj, boolean showClassName) {
        superLogImpl(obj, LogLevel.ERROR, showClassName);
    }
    
    /**
     * 다양한 자료형을 지정된 로그 레벨로 JSON 형식으로 가시성 있게 로그 출력
     * @param obj   로그로 출력할 객체
     * @param level 로그 레벨
     * @param showClassName 클래스명 출력 여부
     */
    private static void superLogImpl(Object obj, LogLevel level, boolean showClassName) {
        if (obj == null) {
            lineLogImpl("NULL OBJECT", level);
            logAtLevel(level, "Object is null");
            lineLogImpl(null, level);
            return;
        }

        if (showClassName) {
            String className = obj.getClass().getSimpleName();
            lineLogImpl(className, level);
        } else {
            // 클래스명을 표시하지 않는 경우에도 구분선을 출력하여 가독성 유지
            lineLogImpl(null, level);
        }

        try {
            // 설정된 제외 클래스 목록을 가져와서 안전하게 처리
            Object safeObject = (properties != null) 
                ? commonUtil.makeSafeForSerialization(obj, properties.getExcludedClasses())
                : commonUtil.makeSafeForSerialization(obj);
            String json = toSimpleJson(safeObject);
            logAtLevel(level, "{0}", json);
        } catch (Exception e) {
            logAtLevel(LogLevel.ERROR, "JSON serialization failed: {0}", e.getMessage());
            
            // 직렬화에 실패한 경우 대체 처리 시도
            try {
                logAtLevel(level, "Attempting safe conversion...");
                // 객체를 완전히 분해하여 직렬화 가능한 형태로 변환
                Map<String, Object> safeMap = commonUtil.createSafeMap(obj);
                String safeJson = toSimpleJson(safeMap);
                logAtLevel(level, "Safe conversion result: {0}", safeJson);
            } catch (Exception ex) {
                // 모든 처리가 실패한 경우 toString() 사용
                logAtLevel(level, "Fallback to toString(): {0}", obj.toString());
            }
        }

        lineLogImpl(null, level);
    }
    
    /**
     * ======== 라인 출력 로그 메소드 ==========
     */
    public static void lineLog(String title) {
        lineLogImpl(title, LogLevel.INFO);
    }
    public static void lineLogDebug(String title) {
        lineLogImpl(title, LogLevel.DEBUG);
    }
    public static void lineLogWarn(String title) {
        lineLogImpl(title, LogLevel.WARN);
    }
    public static void lineLogError(String title) {
        lineLogImpl(title, LogLevel.ERROR);
    }

    public static void logServerInitDuration(LocalDateTime serverStartTime){
        LocalDateTime overallEndTime = LocalDateTime.now();
        Duration overallDuration = Duration.between(serverStartTime, overallEndTime);
        lineLog(null);
        lineLog("서버 데이터 초기화 및 업데이트 완료");
        logAtLevel(LogLevel.INFO, "총 소요 시간: {0}", SuhTimeUtil.convertDurationToReadableTime(overallDuration));
        lineLog(null);
    }

    /**
     * 반복 문자열을 생성하는 메서드
     */
    private static String repeat(String str, int count) {
        if (str == null || count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * 다양한 로그 레벨로 제목이 중앙에 포함된 구분선을 로그에 출력합니다.
     */
    private static void lineLogImpl(String title, LogLevel level) {
        String separator;
        if (title == null || title.isEmpty()) {
            separator = repeat(SEPARATOR_CHAR, LINE_LENGTH);
        } else {
            int textLength = title.length() + 2; // 양쪽 공백 포함
            if (textLength >= LINE_LENGTH) {
                // 제목이 너무 길 경우 전체 구분선을 제목으로 대체
                separator = title;
            } else {
                int sideLength = (LINE_LENGTH - textLength) / 2;
                String side = repeat(SEPARATOR_CHAR, sideLength);
                separator = side + " " + title + " " + side;

                // 홀수 길이 조정을 위해 추가 '='
                if (separator.length() < LINE_LENGTH) {
                    separator += SEPARATOR_CHAR;
                }
            }
        }
        logAtLevel(level, "{0}", separator);
    }

    /**
     * 지정된 로그 레벨에 따라 로그를 출력합니다.
     */
    private static void logAtLevel(LogLevel level, String message, Object... args) {
        switch (level) {
            case DEBUG:
                logger.log(Level.FINE, message, args);
                break;
            case INFO:
                logger.log(Level.INFO, message, args);
                break;
            case WARN:
                logger.log(Level.WARNING, message, args);
                break;
            case ERROR:
                logger.log(Level.SEVERE, message, args);
                break;
            default:
                logger.log(Level.INFO, message, args);
        }
    }

    /**
     * 실행 가능한 인터페이스로 예외를 처리 구성
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * 메소드 실행 시간 측정
     */
    public static void timeLog(ThrowingRunnable task) {
        String methodName = new Throwable().getStackTrace()[1].getMethodName(); // 호출한 메소드 이름을 가져오기 위해 인덱스를 1로 변경
        long startTime = System.currentTimeMillis();
        try {
            task.run();
        } catch (Exception e) {
            logAtLevel(LogLevel.ERROR, "[{0}] 실행 중 예외 발생: {1}", new Object[]{methodName, e.getMessage()});
        } finally {
            long endTime = System.currentTimeMillis();
            long durationMillis = endTime - startTime;
            String formattedTime = SuhTimeUtil.convertMillisToReadableTime(durationMillis);
            String log = "[" + methodName + "] 실행 시간: " + formattedTime;
            lineLog(log);
        }
    }

    // ========== SLF4J 스타일 메시지 포맷팅 ==========

    /**
     * SLF4J 스타일 메시지 포맷팅 ({} 플레이스홀더 치환)
     */
    private String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }

        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int messageLength = message.length();
        
        for (int i = 0; i < messageLength - 1; i++) {
            if (message.charAt(i) == '{' && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex++]);
                    i++; // '}' 건너뛰기
                } else {
                    sb.append("{}"); // 인자가 부족한 경우 그대로 유지
                    i++; // '}' 건너뛰기
                }
            } else {
                sb.append(message.charAt(i));
            }
        }
        
        // 마지막 문자 처리
        if (messageLength > 0 && 
            (messageLength == 1 || message.charAt(messageLength - 2) != '{' || message.charAt(messageLength - 1) != '}')) {
            sb.append(message.charAt(messageLength - 1));
        }
        
        return sb.toString();
    }
}