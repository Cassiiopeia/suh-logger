package example;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;

/** Servlet API가 classpath에 전혀 없는 앱 — 필터 없이 aspect만 동작해야 한다 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
class SettlementApplicationE2ETest {

    @Autowired
    SettlementApplication.SettlementService service;

    @Autowired
    ApplicationContext context;

    @Test
    void servletApiIsReallyAbsent() {
        assertThat(isPresent("jakarta.servlet.Filter")).isFalse();
        assertThat(context.getBeanNamesForType(kr.suhsaechan.suhlogger.spi.RequestContextAccessor.class)).isEmpty();
    }

    @Test
    void aspectWorksWithoutWebStack(CapturedOutput output) {
        assertThat(service.settle("m-1", 10_000)).isEqualTo(9_900);
        assertThat(output.getOut()).contains("[SettlementService.settle] CALL").contains("\"merchantId\": \"m-1\"")
                .contains("[TIME]: SettlementService.settle");
    }

    private static boolean isPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
