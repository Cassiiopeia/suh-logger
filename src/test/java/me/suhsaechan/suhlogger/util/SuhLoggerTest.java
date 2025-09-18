package me.suhsaechan.suhlogger.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SuhLoggerTest {

  public static void main(String[] args) {
    SuhLoggerTest test = new SuhLoggerTest();
    System.out.println("=== SuhLogger 색상 테스트 시작 ===");
    test.coloredLogTest();
    System.out.println("=== SuhLogger 기본 테스트 시작 ===");
    test.suhLoggerTest();
  }

  @Test
  public void mainTest() {
    suhLoggerTest();
  }

  @Test
  public void coloredLogTest() {
    SuhLogger.lineLog("색상 로그 테스트 시작");
    
    // 다양한 로그 레벨 테스트 (IntelliJ 스타일 색상)
    SuhLogger.info("이것은 INFO 레벨 로그입니다 (초록색)");
    SuhLogger.warn("이것은 WARN 레벨 로그입니다 (노란색)");
    SuhLogger.error("이것은 ERROR 레벨 로그입니다 (빨간색)");
    SuhLogger.debug("이것은 DEBUG 레벨 로그입니다 (파란색)");
    
    // 예외와 함께 로그 테스트
    try {
      throw new RuntimeException("테스트 예외입니다 - 스택 트레이스도 색상 적용됨");
    } catch (Exception e) {
      SuhLogger.error("예외가 발생했습니다", e);
    }
    
    // 구분선 테스트
    SuhLogger.lineLog("색상 적용된 구분선");
    
    SuhLogger.lineLog("색상 로그 테스트 완료");
  }

  void test() {
    System.out.println("testTEST");
  }

  void suhLoggerTest() {

    SuhLogger.lineLog(null);
    SuhLogger.lineLog("test");
    SuhLogger.lineLog(null);

    SuhLogger.timeLog(this::test);
    SuhLogger.lineLog(null);
    // Jackson 의존성 없이 기본 로그만 테스트
    SuhLogger.info("기본 로그 테스트");
  }


}