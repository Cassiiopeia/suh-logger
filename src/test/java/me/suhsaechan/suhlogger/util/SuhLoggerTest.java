package me.suhsaechan.suhlogger.util;

import me.suhsaechan.suhlogger.config.SuhLoggerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class SuhLoggerTest {

  public static void main(String[] args) {
    SuhLoggerTest test = new SuhLoggerTest();
    System.out.println("=== SuhLogger 색상 테스트 시작 ===");
    test.coloredLogTest();
    System.out.println("=== SuhLogger 기본 테스트 시작 ===");
    test.suhLoggerTest();
    System.out.println("=== MultipartFile 제외 테스트 시작 ===");
    test.multipartFileExclusionTest();
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

  @Test
  public void multipartFileExclusionTest() {
    // Mock 설정으로 excluded-classes 시뮬레이션
    SuhLoggerProperties properties = new SuhLoggerProperties();
    List<String> excludedClasses = Arrays.asList("org.springframework.web.multipart.MultipartFile");
    properties.setExcludedClasses(excludedClasses);
    
    // SuhLogger에 properties 설정
    SuhLogger.setProperties(properties);
    
    // MockMultipartFile 생성
    MockMultipartFile mockFile = new MockMultipartFile(
        "testFile", 
        "test.txt", 
        "text/plain", 
        "테스트 내용입니다".getBytes()
    );
    
    SuhLogger.lineLog("MultipartFile 제외 테스트 시작");
    
    // 1. excluded-classes가 설정된 상태에서 로그 출력
    SuhLogger.info("=== MultipartFile이 excluded-classes에 포함된 경우 ===");
    SuhLogger.superLog(mockFile);
    
    // 2. excluded-classes를 제거하고 로그 출력 (비교용)
    properties.setExcludedClasses(Arrays.asList()); // 빈 리스트로 설정
    SuhLogger.setProperties(properties);
    
    SuhLogger.info("=== MultipartFile이 excluded-classes에 포함되지 않은 경우 ===");
    SuhLogger.superLog(mockFile);
    
    SuhLogger.lineLog("MultipartFile 제외 테스트 완료");
  }

}