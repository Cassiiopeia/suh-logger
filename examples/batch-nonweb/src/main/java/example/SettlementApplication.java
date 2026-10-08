package example;

import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class SettlementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementApplication.class, args);
    }

    @Service
    public static class SettlementService {

        @LogMonitor
        public long settle(String merchantId, long amount) {
            return amount - amount / 100;
        }
    }
}
