package kr.suhsaechan.suhlogger.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/** 실제 서버를 띄워 HTTP로 호출 — 2.x 좌표·import·프로퍼티가 3.0 구조에서 그대로 동작하는지 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = ConsumerApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "suh-logger.masking.enabled=true",
                // 2.x 마스킹은 파라미터 이름 기준 — body 맵 안의 중첩 필드 마스킹은 #55 범위
                "suh-logger.masking.mask-fields=body",
                "suh-logger.exclude-patterns=/actuator"
        })
class Legacy2xCompatibilityE2ETest {

    @Autowired
    TestRestTemplate rest;

    @Test
    @SuppressWarnings("rawtypes")
    void annotationAndFilterWorkEndToEnd(CapturedOutput output) {
        ResponseEntity<Map> res = rest.postForEntity("/api/login",
                Map.of("username", "suh", "password", "p@ss"), Map.class);

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).containsEntry("status", "ok");
        assertThat(output.getOut()).contains("[LoginController.login] CALL").contains("[TIME]: LoginController.login");
        assertThat(output.getOut()).doesNotContain("p@ss");
        assertThat(output.getOut()).contains("RESPONSE LOGGING").contains("URI: /api/login");
    }
}
