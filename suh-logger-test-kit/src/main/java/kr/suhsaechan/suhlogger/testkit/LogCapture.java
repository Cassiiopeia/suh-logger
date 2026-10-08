package kr.suhsaechan.suhlogger.testkit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;

/**
 * SuhLogger 출력 검증용 캡처 (logback 바인딩 필요).
 * 콘솔 문자열 비교 대신 logback 이벤트를 모아 SLF4J 위임이 실제로 일어났는지 본다.
 *
 * <pre>
 * try (LogCapture capture = LogCapture.start()) {
 *     service.login(request);
 *     assertFalse(capture.text().contains("p@ss"));
 * }
 * </pre>
 */
public final class LogCapture implements AutoCloseable {

    private final Logger root;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private LogCapture() {
        root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appender.start();
        root.addAppender(appender);
    }

    public static LogCapture start() {
        return new LogCapture();
    }

    public List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }

    public List<ILoggingEvent> events() {
        return List.copyOf(appender.list);
    }

    public String text() {
        return String.join("\n", messages());
    }

    @Override
    public void close() {
        root.detachAppender(appender);
        appender.stop();
    }
}
