package kr.suhsaechan.suhlogger.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import kr.suhsaechan.suhlogger.testsupport.LogCapture;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/** Boot 없는 Spring XML 설정에서 동작하는지 */
class XmlConfigurationTest {

    public static class AccountService {

        @LogMonitor
        public String login(String user, String password) {
            return "ok:" + user;
        }
    }

    @Test
    void xmlContextAppliesAspectAndMasking() {
        try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("suh-logger-context.xml");
             LogCapture capture = LogCapture.start()) {
            ctx.getBean(AccountService.class).login("suh", "p@ss");
            String text = capture.text();
            assertTrue(text.contains("[AccountService.login] CALL"), text);
            assertFalse(text.contains("p@ss"), text);
        }
    }
}
