package me.suhsaechan.suhlogger.config;

import java.io.PrintStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * SuhLogger의 설정을 담당하는 클래스
 * 로거 인스턴스 생성 및 포맷터 설정 담당
 */
public class SuhLoggerConfig {

  private static final String LOGGER_NAME = "me.suhsaechan.suhlogger";
  private static final Logger logger = createLogger();

  // 표준 출력 스트림을 직접 사용
  private static final PrintStream standardOut = System.out;
  private static final PrintStream standardErr = System.err;

  /**
   * 로거 인스턴스 조회
   */
  public static Logger getLogger() {
    return logger;
  }

  /**
   * 로그 레벨 설정
   */
  public static void setLogLevel(Level level) {
    logger.setLevel(level);

    for (Handler handler : logger.getHandlers()) {
      handler.setLevel(level);
    }
  }

  /**
   * 로거 인스턴스 생성 및 초기화
   * 전역 설정에 영향을 주지 않는 독립적인 로거 생성
   */
  private static Logger createLogger() {
    // me.suhsaechan.suhlogger 네임스페이스의 독립 로거 생성
    // 전역 로거 설정 변경 안함
    Logger loggerInstance = Logger.getLogger(LOGGER_NAME);

    // 기존 핸들러와 상위 핸들러 모두 제거 : 완전히 독립적 동작
    for (Handler handler : loggerInstance.getHandlers()) {
      loggerInstance.removeHandler(handler);
    }

    // 부모 로거의 핸들러를 사용하지 않음 (다른 로깅 시스템과 독립)
    loggerInstance.setUseParentHandlers(false);

    // 커스텀 콘솔 핸들러 추가 : 다른 로깅 프레임워크 X : 직접 콘솔 출력
    DirectConsoleHandler consoleHandler = new DirectConsoleHandler();
    consoleHandler.setFormatter(new SuhLogFormatter());
    consoleHandler.setLevel(Level.INFO);
    loggerInstance.addHandler(consoleHandler);

    // 로그 레벨 설정
    loggerInstance.setLevel(Level.INFO);

    return loggerInstance;
  }

  /**
   * 파일 핸들러 추가
   */
  public static void addFileHandler(String logFilePath) {
    try {
      FileHandler fileHandler = new FileHandler(logFilePath, true);
      fileHandler.setFormatter(new SuhLogFormatter());
      logger.addHandler(fileHandler);
    } catch (Exception e) {
      standardErr.println("파일 로그 핸들러 추가 실패: " + e.getMessage());
    }
  }

  /**
   * 직접 콘솔에 출력 핸들러
   */
  private static class DirectConsoleHandler extends ConsoleHandler {

    @Override
    public void publish(LogRecord record) {
      if (!isLoggable(record)) {
        return;
      }

      String message = getFormatter().format(record);

      // 모든 로그를 standardOut으로 출력하여 색상 일관성 유지
      standardOut.print(message);
    }

    @Override
    public void flush() {
      standardOut.flush();
      standardErr.flush();
    }

    @Override
    public void close() throws SecurityException {
      // 표준 출력은 닫지 않음
    }
  }

  /**
   * SuhLogger의 커스텀 로그 포맷터 
   * 스프링부트 스타일 포맷: 2025-09-19 14:23:45.123  INFO 12345 --- [           main] package.ClassName                        : message
   */
  public static class SuhLogFormatter extends Formatter {

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    // ANSI 색상 코드 정의 (IntelliJ IDEA 표준 8색)
    private static final String RESET = "\033[0m";
    private static final String GREEN = "\033[32m";            // INFO 레벨 (표준 초록)
    private static final String YELLOW = "\033[33m";           // WARN 레벨 (표준 노랑)
    private static final String RED = "\033[31m";              // ERROR 레벨 (표준 빨강)
    private static final String BLUE = "\033[34m";             // DEBUG 레벨 (표준 파랑)
    private static final String CYAN = "\033[36m";             // 클래스명 (표준 시안)
    private static final String WHITE = "\033[37m";            // 메시지 (표준 흰색)

    @Override
    public String format(LogRecord record) {
      StringBuilder sb = new StringBuilder();

      // 타임스탬프 (스프링부트 형식: 2025-09-19 14:23:45.123) - 기본색
      sb.append(DATE_FORMATTER.format(Instant.ofEpochMilli(record.getMillis())))
          .append("  ");

      // 로그 레벨 (5자리 고정, 좌측 정렬) - 레벨별 색상
      String level = convertLogLevel(record.getLevel());
      String levelColor = getLevelColor(record.getLevel());
      sb.append(levelColor)
          .append(String.format("%-5s", level))
          .append(RESET)
          .append(" ");

      // PID (프로세스 ID), 구분자, 스레드 이름 - 기본색
      String pid = String.valueOf(ProcessHandle.current().pid());
      String threadName = Thread.currentThread().getName();
      // 스레드 이름이 긴 경우 앞부분만 사용
      if (threadName.length() > 8) {
        threadName = threadName.substring(0, 8);
      }
      sb.append(String.format("%5s", pid))
          .append(" --- ")
          .append(String.format("[%-8s]", threadName))
          .append(" ");

      // 클래스명 (축약된 형태, 20자리 고정) - 시안색
      String className = getAbbreviatedClassName(record);
      sb.append(CYAN)
          .append(String.format("%-20s", className))
          .append(RESET)
          .append(" : ");

      // 로그 메시지 - 기본 색상 (메시지는 색상 없이)
      sb.append(formatMessage(record))
          .append("\n");

      // 예외가 있는 경우 스택 트레이스 추가
      if (record.getThrown() != null) {
        appendException(sb, record.getThrown());
      }

      return sb.toString();
    }

    /**
     * JUL 로그 레벨을 IntelliJ 스타일로 변환
     */
    private String convertLogLevel(Level level) {
      if (level == Level.SEVERE) {
        return "ERROR";
      } else if (level == Level.WARNING) {
        return "WARN";
      } else if (level == Level.INFO) {
        return "INFO";
      } else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
        return "DEBUG";
      } else {
        return level.getName();
      }
    }

    /**
     * 로그 레벨에 따른 색상 반환
     */
    private String getLevelColor(Level level) {
      if (level == Level.SEVERE) {
        return RED;     // ERROR - 빨간색
      } else if (level == Level.WARNING) {
        return YELLOW;  // WARN - 노란색
      } else if (level == Level.INFO) {
        return GREEN;   // INFO - 초록색
      } else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
        return BLUE;    // DEBUG - 파란색
      } else {
        return WHITE;   // 기타 - 흰색
      }
    }

    /**
     * 스프링부트 스타일의 축약된 클래스명 생성
     * 예: org.springframework.boot.SpringApplication -> o.s.b.SpringApplication
     */
    private String getAbbreviatedClassName(LogRecord record) {
      String fullClassName = null;
      
      // 스택 트레이스에서 실제 호출 위치 찾기
      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      
      for (StackTraceElement element : stackTrace) {
        String className = element.getClassName();
        if (!className.startsWith("me.suhsaechan.suhlogger") && 
            !className.startsWith("java.util.logging") &&
            !className.equals("java.lang.Thread")) {
          fullClassName = className;
          break;
        }
      }
      
      // 스택 트레이스에서 찾지 못한 경우 record의 소스 클래스명 사용
      if (fullClassName == null && record.getSourceClassName() != null) {
        fullClassName = record.getSourceClassName();
      }
      
      if (fullClassName == null) {
        return "Unknown";
      }
      
      return abbreviateClassName(fullClassName);
    }

    /**
     * 클래스명을 스프링부트 스타일로 축약
     * 예: org.springframework.boot.SpringApplication -> o.s.b.SpringApplication
     */
    private String abbreviateClassName(String fullClassName) {
      String[] parts = fullClassName.split("\\.");
      if (parts.length <= 1) {
        return fullClassName;
      }
      
      StringBuilder abbreviated = new StringBuilder();
      
      // 마지막 부분(클래스명)을 제외한 모든 패키지명을 첫 글자로 축약
      for (int i = 0; i < parts.length - 1; i++) {
        if (parts[i].length() > 0) {
          abbreviated.append(parts[i].charAt(0)).append(".");
        }
      }
      
      // 마지막 클래스명은 전체 이름 사용
      abbreviated.append(parts[parts.length - 1]);
      
      return abbreviated.toString();
    }

    /**
     * 예외 정보를 스프링부트 스타일로 포맷팅 (색상 적용)
     */
    private void appendException(StringBuilder sb, Throwable thrown) {
      try {
        sb.append("\n")
          .append(RED)  // 예외 클래스명과 메시지는 빨간색
          .append(thrown.getClass().getName())
          .append(": ")
          .append(thrown.getMessage())
          .append(RESET)
          .append("\n");

        for (StackTraceElement element : thrown.getStackTrace()) {
          sb.append(WHITE)  // 스택 트레이스는 일반색상 (흰색)
              .append("\tat ")
              .append(element.toString())
              .append(RESET)
              .append("\n");
        }
        
        // Caused by 처리
        Throwable cause = thrown.getCause();
        while (cause != null) {
          sb.append(RED)  // Caused by도 빨간색
            .append("Caused by: ")
            .append(cause.getClass().getName())
            .append(": ")
            .append(cause.getMessage())
            .append(RESET)
            .append("\n");
            
          for (StackTraceElement element : cause.getStackTrace()) {
            sb.append(WHITE)  // 스택 트레이스는 일반색상 (흰색)
                .append("\tat ")
                .append(element.toString())
                .append(RESET)
                .append("\n");
          }
          
          cause = cause.getCause();
        }
      } catch (Exception ex) {
        sb.append(RED)
          .append("예외 출력 중 오류 발생")
          .append(RESET)
          .append("\n");
      }
    }
  }
} 