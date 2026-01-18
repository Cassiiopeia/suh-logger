package kr.suhsaechan.suhlogger.config;

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

  private static final String LOGGER_NAME = "kr.suhsaechan.suhlogger";
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
    // kr.suhsaechan.suhlogger 네임스페이스의 독립 로거 생성
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
   * 스프링부트 기본 로그 포맷터
   * 스프링부트 기본 형식: 2025-09-19T10:30:15.123+09:00  INFO 12345 --- [           main] c.e.d.DemoApplication                   : message
   */
  public static class SuhLogFormatter extends Formatter {

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .withZone(ZoneId.systemDefault());
    
    // ANSI 색상 코드 정의
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String MAGENTA = "\u001B[35m";  // PID용 보라색


    @Override
    public String format(LogRecord record) {
      StringBuilder sb = new StringBuilder();

      // 타임스탬프 (스프링부트 기본 형식: 2025-09-19T10:30:15.123+09:00)
      sb.append(DATE_FORMATTER.format(Instant.ofEpochMilli(record.getMillis())));

      // 로그 레벨 (색상 적용 + 글자 수에 따른 공백 조정)
      String level = convertLogLevel(record.getLevel());
      String coloredLevel = getColoredLevel(level);
      
      // 레벨 글자 수에 따라 공백 조정: INFO(4글자)=2개, ERROR/DEBUG(5글자)=1개
      int spaceCount = (level.length() == 4) ? 2 : 1;
      sb.append(" ".repeat(spaceCount))
          .append(coloredLevel)
          .append(" ");

      // PID (프로세스 ID), 구분자, 스레드 이름
      String pid = String.valueOf(ProcessHandle.current().pid());
      String threadName = Thread.currentThread().getName();
      // 스레드 이름이 긴 경우 15자리로 제한
      if (threadName.length() > 15) {
        threadName = threadName.substring(0, 15);
      }
      sb.append(String.format("%5s", MAGENTA + pid + RESET))
          .append(" --- ")
          .append(String.format("[%15s]", threadName))
          .append(" ");

      // 클래스명 (축약된 형태, 40자리 고정, 청록색 적용)
      String className = getAbbreviatedClassName(record);
      sb.append(String.format("%-40s", CYAN + className + RESET))
          .append(" : ");

      // 로그 메시지
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
     * 로그 레벨에 따른 색상 적용
     */
    private String getColoredLevel(String level) {
      switch (level) {
        case "ERROR":
          return RED + level + RESET;
        case "WARN":
          return YELLOW + level + RESET;
        case "INFO":
          return GREEN + level + RESET;
        case "DEBUG":
          return BLUE + level + RESET;
        default:
          return CYAN + level + RESET;
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
        if (!className.startsWith("kr.suhsaechan.suhlogger") && 
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
     * 예외 정보를 스프링부트 스타일로 포맷팅
     */
    private void appendException(StringBuilder sb, Throwable thrown) {
      try {
        sb.append("\n")
          .append(thrown.getClass().getName())
          .append(": ")
          .append(thrown.getMessage())
          .append("\n");

        for (StackTraceElement element : thrown.getStackTrace()) {
          sb.append("\tat ")
              .append(element.toString())
              .append("\n");
        }
        
        // Caused by 처리
        Throwable cause = thrown.getCause();
        while (cause != null) {
          sb.append("Caused by: ")
            .append(cause.getClass().getName())
            .append(": ")
            .append(cause.getMessage())
            .append("\n");
            
          for (StackTraceElement element : cause.getStackTrace()) {
            sb.append("\tat ")
                .append(element.toString())
                .append("\n");
          }
          
          cause = cause.getCause();
        }
      } catch (Exception ex) {
        sb.append("예외 출력 중 오류 발생")
          .append("\n");
      }
    }
  }
} 