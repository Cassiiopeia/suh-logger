package me.suhsaechan.suhlogger.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.suhsaechan.suhlogger.config.SuhLoggerConfig;

/**
 * 로그 유틸리티 클래스
 */
public class SuhLogger {

	private static final ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.enable(SerializationFeature.INDENT_OUTPUT);

	private static final int LINE_LENGTH = 60; // "=" 줄에 대한 최대 길이 지정
	private static final String SEPARATOR_CHAR = "=";
	
	// 로거 인스턴스
	private static final Logger logger = SuhLoggerConfig.getLogger();

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
			// 객체를 JSON 문자열로 변환
			String json = objectMapper.writeValueAsString(obj);
			logAtLevel(level, "{0}", json);
		} catch (JsonProcessingException e) {
			logAtLevel(LogLevel.ERROR, "아닛!? JSON 변환 실패 !!: {0}", e.getMessage());
			logAtLevel(level, "대신 toString() 사용~!! : {0}", obj.toString());
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
	 * 로그 파일 핸들러
	 */
	public static void addFileLogger(String logFilePath) {
		SuhLoggerConfig.addFileHandler(logFilePath);
	}
}
