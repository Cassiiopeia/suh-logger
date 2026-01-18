package kr.suhsaechan.suhlogger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class SuhLoggerApplication {

  public static void main(String[] args) {
    SpringApplication.run(SuhLoggerApplication.class, args);
  }

}
