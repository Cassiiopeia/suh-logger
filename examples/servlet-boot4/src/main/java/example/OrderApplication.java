package example;

import java.util.Map;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @RestController
    public static class OrderController {

        @LogMonitor
        @PostMapping("/api/orders")
        public Map<String, Object> create(@RequestBody Map<String, Object> order) {
            return Map.of("item", order.get("item"), "status", "CREATED");
        }

        /** 처리되지 않은 예외 — 로그가 200이 아니라 500으로 남아야 한다 */
        @GetMapping("/api/boom")
        public String boom() {
            throw new IllegalStateException("boom");
        }

        /** 비동기 응답 — 본문이 클라이언트까지 가야 한다 */
        @GetMapping("/api/async")
        public DeferredResult<Map<String, String>> async() {
            DeferredResult<Map<String, String>> result = new DeferredResult<>();
            new Thread(() -> result.setResult(Map.of("async", "done"))).start();
            return result;
        }

        /** SSE — 이벤트가 끝까지 모였다가 나가면 안 된다 */
        @GetMapping(value = "/api/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter events() {
            SseEmitter emitter = new SseEmitter();
            new Thread(() -> {
                try {
                    emitter.send("tick-1");
                    emitter.send("tick-2");
                    emitter.complete();
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }).start();
            return emitter;
        }

        /** 헤더 객체 파라미터 — Boot 4의 HttpHeaders는 Map이 아니라 toString()에 원문이 있었다 */
        @LogMonitor
        @GetMapping("/api/me")
        public String me(@RequestHeader HttpHeaders headers) {
            return "me";
        }
    }
}
