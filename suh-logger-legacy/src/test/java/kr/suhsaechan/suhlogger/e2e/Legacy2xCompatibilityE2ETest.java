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
        // 마스킹 설정을 하나도 주지 않는다 — 3.0 기본값만으로 토큰·비밀번호가 가려져야 한다 (#55)
        properties = "suh-logger.exclude-patterns=/actuator")
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
        assertThat(res.getBody()).containsEntry("accessToken", "AT-SECRET-1"); // 클라이언트 응답은 그대로
        // 요청 본문의 중첩 password, 반환값·응답 본문의 토큰이 어떤 로그에도 남지 않는다
        assertThat(output.getOut()).doesNotContain("p@ss").doesNotContain("AT-SECRET-1").doesNotContain("RT-SECRET-2");
        assertThat(output.getOut()).contains("\"username\": \"suh\"");
        assertThat(output.getOut()).contains("RESPONSE LOGGING").contains("URI: /api/login");
    }
}
