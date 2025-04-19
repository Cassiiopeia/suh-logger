package me.suhsaechan.suhlogger.aspect;

import me.suhsaechan.suhlogger.service.TestTargetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SuhLoggingAspectTest {

  @Autowired
  private TestTargetService testTargetService;

  @Test
  public void mainTest() {
//    testTargetService.executeMethodLogging("메소드 로깅 테스트");
//    testTargetService.executeTimeLogging();
    testTargetService.executeMonitoring("모니터링 테스트 데이터");
  }
}