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

/** 3.0 리뷰에서 나온 실제 장애 시나리오를 서버를 띄워 확인한다 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"suh-logger.format=line", "suh-logger.request-id.enabled=true",
                "suh-logger.exclude-patterns=/api/{id}/skip/**"})
class RobustnessE2ETest {

    @Value("${local.server.port}")
    int port;

    private HttpResponse<String> get(String path, String... headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        for (int i = 0; i < headers.length; i += 2) {
            b.header(headers[i], headers[i + 1]);
        }
        return HttpClient.newHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void uriVariableExcludePatternDoesNotBreakRequests() throws Exception {
        assertThat(get("/api/orders").statusCode()).isNotEqualTo(500);
    }

    @Test
    void unhandledExceptionIsLoggedAs500(CapturedOutput output) throws Exception {
        assertThat(get("/api/boom").statusCode()).isEqualTo(500);
        assertThat(output.getOut()).containsPattern("WARN.*GET /api/boom -> 500");
    }

    @Test
    void deferredResultBodyReachesClientAndIsLogged(CapturedOutput output) throws Exception {
        HttpResponse<String> res = get("/api/async");
        assertThat(res.body()).contains("done");
        assertThat(output.getOut()).containsPattern("GET /api/async -> 200 \\(\\d+ms\\).*body=.*done");
    }

    @Test
    void serverSentEventsStillStream() throws Exception {
        HttpResponse<String> res = get("/api/events", "Accept", "text/event-stream");
        assertThat(res.body()).contains("tick-1").contains("tick-2");
    }

    @Test
    void authorizationHeaderParameterIsMasked(CapturedOutput output) throws Exception {
        get("/api/me", "Authorization", "Bearer SECRET-JWT-123");
        assertThat(output.getOut()).contains("[OrderController.me] CALL").doesNotContain("SECRET-JWT-123");
    }

    @Test
    void forgedRequestIdIsReplaced() throws Exception {
        HttpResponse<String> res = get("/api/me", "X-Request-Id", "bad id <script>" + "x".repeat(200));
        String rid = res.headers().firstValue("X-Request-Id").orElse("");
        assertThat(rid).doesNotContain("bad").matches("[0-9a-f-]{36}");
    }
}
