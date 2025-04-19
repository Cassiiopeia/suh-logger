package me.suhsaechan.suhlogger.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SuhLoggerTest {

  @Test
  public void mainTest() {
    suhLoggerTest();
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
  }


}