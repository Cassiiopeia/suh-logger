package example;

import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.Test;

class PlainJavaAppE2ETest {

    @Test
    void coreWorksWithoutSpring() {
        try (LogCapture capture = LogCapture.start()) {
            PlainJavaApp.main(new String[0]);
            String text = capture.text();
            assertTrue(text.contains("\"job\": \"nightly-report\""), text);
            assertTrue(text.contains("실행 시간"), text);
        }
    }

    @Test
    void springIsReallyAbsent() {
        try {
            Class.forName("org.springframework.context.ApplicationContext");
            throw new AssertionError("Spring must not be on the plain-java classpath");
        } catch (ClassNotFoundException expected) {
            // 정상
        }
    }
}
