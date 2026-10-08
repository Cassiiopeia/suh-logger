package kr.suhsaechan.suhlogger.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.Test;

class ProxyEligibilityCheckerTest {

    public static class OpenService {

        @LogMonitor
        public final String finalMethod() {
            return "x";
        }

        @LogMonitor
        public String fine() {
            return "y";
        }
    }

    @LogMonitor
    public static final class FinalService {
        public String a() {
            return "a";
        }
    }

    @Test
    void finalMethodIsWarned() {
        try (LogCapture capture = LogCapture.start()) {
            new ProxyEligibilityChecker().check(OpenService.class);
            String text = capture.text();
            assertTrue(text.contains("OpenService.finalMethod is final"), text);
            assertFalse(text.contains("OpenService.fine"), text);
        }
    }

    @Test
    void finalClassIsWarned() {
        try (LogCapture capture = LogCapture.start()) {
            new ProxyEligibilityChecker().check(FinalService.class);
            assertTrue(capture.text().contains("FinalService is final"), capture.text());
        }
    }
}
