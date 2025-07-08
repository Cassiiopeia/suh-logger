package me.suhsaechan.suhlogger.config;

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
    }
    
    /**
     * 로거 인스턴스 생성 및 초기화
     */
    private static Logger createLogger() {
        Logger loggerInstance = Logger.getLogger(LOGGER_NAME);
        
        // 기본 핸들러 제거하여 상위 로거 설정의 영향 제거
        for (Handler handler : loggerInstance.getHandlers()) {
            loggerInstance.removeHandler(handler);
        }
        
        // 부모 핸들러 사용 중지
        loggerInstance.setUseParentHandlers(false);
        
        // 콘솔 핸들러 추가
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SuhLogFormatter());
        loggerInstance.addHandler(consoleHandler);
        
        // 로그 레벨 설정
        loggerInstance.setLevel(Level.INFO);
        
        return loggerInstance;
    }
    
    /**
     * 파일 핸들러 추가 (선택적으로 사용)
     */
    public static void addFileHandler(String logFilePath) {
        try {
            FileHandler fileHandler = new FileHandler(logFilePath, true);
            fileHandler.setFormatter(new SuhLogFormatter());
            logger.addHandler(fileHandler);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "파일 로그 핸들러 추가 실패", e);
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