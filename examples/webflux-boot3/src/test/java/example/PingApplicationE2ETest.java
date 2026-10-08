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

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "suh-logger.exclude-patterns=/actuator-like")
class PingApplicationE2ETest {

    @Value("${local.server.port}")
    int port;

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void reactiveResponseIsLogged(CapturedOutput output) throws Exception {
        HttpResponse<String> res = get("/api/ping");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).isEqualTo("{\"pong\":\"ok\"}");
        assertThat(output.getOut()).contains("RESPONSE LOGGING").contains("URI: /api/ping")
                .contains("Response Body: {\"pong\":\"ok\"}");
    }

    @Test
    void excludedPathIsNotLogged(CapturedOutput output) throws Exception {
        assertThat(get("/actuator-like/health").body()).isEqualTo("UP");
        assertThat(output.getOut()).doesNotContain("URI: /actuator-like/health");
    }
}
