package me.suhsaechan.suhlogger.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.suhsaechan.suhlogger.config.SuhLoggerConfig;

/**
 * SuhLogger 유틸리티 클래스
 * Java Util Logging을 사용한 로깅 유틸리티
 */
public class SuhLogger {

	private static final int LINE_LENGTH = 60; // "=" 줄에 대한 최대 길이 지정
	private static final String SEPARATOR_CHAR = "=";
	
	// 로거 인스턴스
	private static final Logger logger = SuhLoggerConfig.getLogger();
	
	private static final ObjectMapper objectMapper = createObjectMapper();
	
	/**
	 * ObjectMapper 생성 및 설정
	 */
	private static ObjectMapper createObjectMapper() {
		// 기본 ObjectMapper 설정
		return new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.enable(SerializationFeature.INDENT_OUTPUT)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
			logger.log(Level.INFO, message + "\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object));
		} catch (JsonProcessingException e) {
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
			// 먼저 MultipartFile 관련 객체인지 확인하고 안전하게 처리
			Object safeObject = makeSafeForSerialization(obj);
			String json = objectMapper.writeValueAsString(safeObject);
			logAtLevel(level, "{0}", json);
		} catch (JsonProcessingException e) {
			logAtLevel(LogLevel.ERROR, "아닛!? JSON 변환 실패 !!: {0}", e.getMessage());
			
			// 직렬화에 실패한 경우 대체 처리 시도
			try {
				logAtLevel(level, "안전 변환 시도 중...");
				// 객체를 완전히 분해하여 직렬화 가능한 형태로 변환
				Map<String, Object> safeMap = createSafeMap(obj);
				String safeJson = objectMapper.writeValueAsString(safeMap);
				logAtLevel(level, "안전 변환 결과: {0}", safeJson);
			} catch (Exception ex) {
				// 모든 처리가 실패한 경우 toString() 사용
				logAtLevel(level, "대신 toString() 사용~!! : {0}", obj.toString());
			}
		}

		lineLogImpl(null, level);
	}
	
	/**
	 * 객체를 JSON 직렬화 가능한 안전한 형태로 변환
	 * 특히 MultipartFile과 같은 직렬화 불가능 객체를 처리
	 */
	private static Object makeSafeForSerialization(Object obj) {
		if (obj == null) {
			return null;
		}
		
		// InputStream은 항상 안전한 맵으로 대체
		if (obj instanceof InputStream) {
			Map<String, Object> result = new HashMap<>();
			result.put("_type", "InputStream");
			result.put("_class", obj.getClass().getName());
			return result;
		}
		
		// 클래스 이름에 MultipartFile이 포함된 객체는 안전하게 처리
		String className = obj.getClass().getName();
		if (className.contains("MultipartFile")) {
			return extractMultipartFileInfo(obj);
		}
		
		// Map의 경우 각 값을 안전하게 처리
		if (obj instanceof Map) {
			Map<Object, Object> original = (Map<Object, Object>) obj;
			Map<Object, Object> safe = new HashMap<>();
			
			for (Map.Entry<Object, Object> entry : original.entrySet()) {
				safe.put(entry.getKey(), makeSafeForSerialization(entry.getValue()));
			}
			
			return safe;
		}
		
		// 컬렉션의 경우 각 항목을 안전하게 처리
		if (obj instanceof Collection) {
			Collection<?> original = (Collection<?>) obj;
			Object[] safe = new Object[original.size()];
			
			int i = 0;
			for (Object item : original) {
				safe[i++] = makeSafeForSerialization(item);
			}
			
			return safe;
		}
		
		// 배열의 경우 각 항목을 안전하게 처리
		if (obj.getClass().isArray()) {
			try {
				Object[] array = (Object[]) obj;
				Object[] safe = new Object[array.length];
				
				for (int i = 0; i < array.length; i++) {
					safe[i] = makeSafeForSerialization(array[i]);
				}
				
				return safe;
			} catch (ClassCastException e) {
				// 원시 타입 배열인 경우 그대로 반환
				return obj;
			}
		}
		
		// 일반 객체의 경우, toString() 결과에서 "MultipartFile"이 포함되어 있으면 안전하게 처리
		String toString = obj.toString();
		if (toString.contains("MultipartFile")) {
			return createSafeMap(obj);
		}
		
		return obj;
	}
	
	/**
	 * MultipartFile 객체에서 중요 정보를 추출
	 */
	private static Map<String, Object> extractMultipartFileInfo(Object multipartFile) {
		Map<String, Object> info = new HashMap<>();
		info.put("_type", "MultipartFile");
		
		try {
			// 리플렉션을 사용하여 MultipartFile 메서드 호출
			try {
				Object fileName = multipartFile.getClass().getMethod("getOriginalFilename").invoke(multipartFile);
				info.put("fileName", fileName);
			} catch (Exception e) {
				info.put("fileName", "unknown");
			}
			
			try {
				Object contentType = multipartFile.getClass().getMethod("getContentType").invoke(multipartFile);
				info.put("contentType", contentType);
			} catch (Exception e) {
				info.put("contentType", "unknown");
			}
			
			try {
				Object size = multipartFile.getClass().getMethod("getSize").invoke(multipartFile);
				info.put("size", size);
			} catch (Exception e) {
				info.put("size", -1);
			}
			
			try {
				Object isEmpty = multipartFile.getClass().getMethod("isEmpty").invoke(multipartFile);
				info.put("isEmpty", isEmpty);
			} catch (Exception e) {
				info.put("isEmpty", "unknown");
			}
			
		} catch (Exception e) {
			info.put("error", "정보 추출 실패: " + e.getMessage());
		}
		
		return info;
	}
	
	/**
	 * 객체를 안전하게 Map으로 변환
	 */
	private static Map<String, Object> createSafeMap(Object obj) {
		if (obj == null) {
			return new HashMap<>();
		}
		
		Map<String, Object> result = new HashMap<>();
		result.put("_class", obj.getClass().getName());
		result.put("_toString", obj.toString());
		
		// 리플렉션을 사용하여 필드 값 추출 시도
		try {
			for (Field field : obj.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				String fieldName = field.getName();
				
				try {
					Object value = field.get(obj);
					
					// 특수한 필드 타입 처리
					if (value == null) {
						result.put(fieldName, null);
					} else if (value instanceof InputStream) {
						result.put(fieldName, "[InputStream]");
					} else if (isMultipartFileType(value)) {
						result.put(fieldName, extractMultipartFileInfo(value));
					} else if (value instanceof Collection) {
						result.put(fieldName, "[Collection: " + ((Collection<?>) value).size() + " items]");
					} else if (value.getClass().isArray()) {
						try {
							Object[] array = (Object[]) value;
							result.put(fieldName, "[Array: " + array.length + " items]");
						} catch (ClassCastException e) {
							result.put(fieldName, "[Primitive Array]");
						}
					} else if (value instanceof Map) {
						result.put(fieldName, "[Map: " + ((Map<?, ?>) value).size() + " entries]");
					} else {
						// 기본 타입이나 String은 직접 포함
						result.put(fieldName, value);
					}
				} catch (Exception e) {
					result.put(fieldName, "[접근 불가: " + e.getMessage() + "]");
				}
			}
		} catch (Exception e) {
			result.put("_error", "필드 추출 실패: " + e.getMessage());
		}
		
		return result;
	}
	
	/**
	 * 객체가 MultipartFile 타입인지 확인
	 */
	private static boolean isMultipartFileType(Object obj) {
		if (obj == null) {
			return false;
		}
		
		// 클래스 이름으로 확인
		String className = obj.getClass().getName();
		if (className.contains("MultipartFile")) {
			return true;
		}
		
		// 인터페이스로 확인
		for (Class<?> iface : obj.getClass().getInterfaces()) {
			if (iface.getName().contains("MultipartFile")) {
				return true;
			}
		}
		
		// toString()으로 확인
		String toString = obj.toString();
		return toString.contains("MultipartFile");
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
}
