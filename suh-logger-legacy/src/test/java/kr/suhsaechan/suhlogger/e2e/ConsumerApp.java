package kr.suhsaechan.suhlogger.e2e;

import java.util.Map;
import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 2.x README 사용법 그대로 작성한 모의 소비자 앱 */
@SpringBootApplication
public class ConsumerApp {

    @RestController
    public static class LoginController {

        @LogMonitor
        @PostMapping("/api/login")
        public Map<String, String> login(@RequestBody Map<String, String> body) {
            // passQL #396 사례: 로그인 응답에 토큰이 실린다
            return Map.of("user", body.get("username"), "status", "ok",
                    "accessToken", "AT-SECRET-1", "refreshToken", "RT-SECRET-2");
        }
    }
}
