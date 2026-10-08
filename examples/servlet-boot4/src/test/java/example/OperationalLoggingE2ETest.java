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

/** #56 운영 설정: 한 줄 형식, 요청 ID, /actuator 기본 제외 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"suh-logger.format=line", "suh-logger.request-id.enabled=true"})
class OperationalLoggingE2ETest {

    @Value("${local.server.port}")
    int port;

    private HttpResponse<String> post(String path, String requestId) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"item\":\"pen\"}"));
        if (requestId != null) {
            b.header("X-Request-Id", requestId);
        }
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void oneLinePerRequestWithRequestId(CapturedOutput output) throws Exception {
        HttpResponse<String> res = post("/api/orders", "trace-123");
        assertThat(res.headers().firstValue("X-Request-Id")).contains("trace-123");
        assertThat(output.getOut()).containsPattern("POST /api/orders -> 200 \\(\\d+ms\\) rid=trace-123 body=");
        assertThat(output.getOut()).doesNotContain("RESPONSE LOGGING");
    }

    @Test
    void actuatorIsExcludedByDefault(CapturedOutput output) throws Exception {
        post("/actuator/health", null);
        assertThat(output.getOut()).doesNotContain("/actuator/health ->");
    }
}
