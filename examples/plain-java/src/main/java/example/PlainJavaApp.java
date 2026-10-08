package example;

import java.util.List;
import java.util.Map;
import kr.suhsaechan.suhlogger.config.SuhLoggerProperties;
import kr.suhsaechan.suhlogger.util.SuhLogger;

/** Spring 없이 core만 쓰는 예 — 설정은 SuhLoggerProperties를 직접 만들어 넘긴다 */
public class PlainJavaApp {

    public static void main(String[] args) {
        SuhLoggerProperties properties = new SuhLoggerProperties();
        properties.setExcludedClasses(List.of("java.io.InputStream"));
        SuhLogger.setProperties(properties);

        SuhLogger.superLog(Map.of("job", "nightly-report", "rows", 1200));
        SuhLogger.timeLog(() -> Thread.sleep(5));
    }
}
