package example;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class PingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PingApplication.class, args);
    }

    @RestController
    public static class PingController {

        @GetMapping("/api/ping")
        public Mono<Map<String, String>> ping() {
            return Mono.just(Map.of("pong", "ok"));
        }

        @GetMapping("/actuator-like/health")
        public Mono<String> health() {
            return Mono.just("UP");
        }
    }
}
