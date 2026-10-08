package example;

import kr.suhsaechan.suhlogger.annotation.LogMonitor;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class PaymentService {

    @LogMonitor
    public String pay(String orderId, String cardNumber) {
        return "PAID:" + orderId;
    }

    public static void main(String[] args) {
        try (ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml")) {
            ctx.getBean(PaymentService.class).pay("o-1", "4111-1111-1111-1111");
        }
    }
}
