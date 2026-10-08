package example;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** 실제 서버를 띄워 HTTP로 호출하는 E2E — Boot 버전마다 위치가 바뀐 테스트 유틸 대신 JDK HttpClient 사용 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "suh-logger.pretty-print-json=true")
class OrderApplicationE2ETest {

    @Value("${local.server.port}")
    int port;

    @Test
    void annotationAndResponseLoggingWork(CapturedOutput output) throws Exception {
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/orders"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"item\":\"book\",\"qty\":2}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("CREATED");
        assertThat(output.getOut())
                .contains("[OrderController.create] CALL")
                .contains("[TIME]: OrderController.create")
                .contains("RESPONSE LOGGING")
                .contains("URI: /api/orders")
                // pretty print — Boot 3는 Jackson 2, Boot 4는 Jackson 3 codec이 처리
                .contains("\"item\" : \"book\"");
    }
}
