package me.suhsaechan.suhlogger.config;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    
    private static final String LOGGER_NAME = "SuhLogger";
    private static final Logger logger = createLogger();
    
    // 표준 출력 스트림을 직접 사용
    private static final PrintStream standardOut = System.out;
    private static final PrintStream standardErr = System.err;
    
    // JUL 로거를 SLF4J로 라우팅하지 않도록 설정
    static {
        // 다른 JUL 로거 설정을 초기화에서 제거
        System.setProperty("java.util.logging.config.file", "no-such-file");
    }
    
    /**
     * 로거 인스턴스 조회
     */
    public static Logger getLogger() {
        return logger;
    }
    
    /**
     * 로거 레벨 설정
     */
    public static void setLogLevel(Level level) {
        logger.setLevel(level);
        
        for (Handler handler : logger.getHandlers()) {
            handler.setLevel(level);
        }
    }
    
    /**
     * 로거 인스턴스 생성 및 초기화
     */
    private static Logger createLogger() {
        // 완전히 새로운 독립 로거 생성
        Logger loggerInstance = Logger.getLogger(LOGGER_NAME);
        
        // 기존 핸들러와 상위 핸들러 모두 제거
        for (Handler handler : loggerInstance.getHandlers()) {
            loggerInstance.removeHandler(handler);
        }
        
        // SLF4J와의 연결 끊기
        loggerInstance.setUseParentHandlers(false);
        
        // 커스텀 콘솔 핸들러 추가 (SLF4J를 거치지 않고 직접 콘솔에 출력)
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
     * SLF4J를 우회하여 직접 콘솔에 출력하는 핸들러
     */
    private static class DirectConsoleHandler extends ConsoleHandler {
        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            
            String message = getFormatter().format(record);
            
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                standardErr.print(message);
            } else {
                standardOut.print(message);
            }
        }
    }
    
    /**
     * SuhLogger의 커스텀 로그 포맷터
     */
    public static class SuhLogFormatter extends Formatter {
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            
            // 날짜와 시간 추가
            sb.append(dateFormat.format(new Date(record.getMillis())))
                .append(" | ");
            
            // 로그 레벨 추가
            sb.append(record.getLevel().getName())
                .append(" | ");
            
            // 로그 메시지 추가
            sb.append(formatMessage(record))
                .append("\n");
            
            // 예외가 있는 경우 스택 트레이스 추가
            if (record.getThrown() != null) {
                try {
                    Throwable thrown = record.getThrown();
                    sb.append(thrown.getClass().getName())
                        .append(": ")
                        .append(thrown.getMessage())
                        .append("\n");
                    
                    for (StackTraceElement element : thrown.getStackTrace()) {
                        sb.append("\tat ")
                            .append(element.toString())
                            .append("\n");
                    }
                } catch (Exception ex) {
                    sb.append("예외 출력 중 오류 발생\n");
                }
            }
            
            return sb.toString();
        }
    }
} 