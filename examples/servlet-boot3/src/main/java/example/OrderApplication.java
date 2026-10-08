package example;

import java.util.Map;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
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
    }
}
