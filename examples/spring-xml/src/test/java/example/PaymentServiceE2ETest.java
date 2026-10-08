package example;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.Test;

class PaymentServiceE2ETest {

    @Test
    void xmlAppLogsAndMasksCard() {
        try (LogCapture capture = LogCapture.start()) {
            PaymentService.main(new String[0]);
            String text = capture.text();
            assertTrue(text.contains("[PaymentService.pay] CALL"), text);
            assertTrue(text.contains("o-1"), text);
            assertFalse(text.contains("4111-1111-1111-1111"), text);
        }
    }
}
