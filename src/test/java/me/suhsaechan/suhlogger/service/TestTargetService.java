package me.suhsaechan.suhlogger.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.suhsaechan.suhlogger.annotation.LogCall;
import me.suhsaechan.suhlogger.annotation.LogMonitor;
import me.suhsaechan.suhlogger.annotation.LogTime;
import me.suhsaechan.suhlogger.util.SuhLogger;
import org.springframework.stereotype.Service;

@Service
public class TestTargetService {

  @LogCall
  public String executeMethodLogging(String input) {
    SuhLogger.lineLog("서비스 - 메소드 실행 정보 로깅 테스트");
    return "Service result: " + input;
  }

  @LogTime
  public void executeTimeLogging() {
    SuhLogger.lineLog("서비스 - 실행 시간 로깅 테스트");
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @LogMonitor
  public Object executeMonitoring(String data) {
//    SuhLogger.lineLog("서비스 - 통합 모니터링 테스트");
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    Map<String, Object> resultMap = new HashMap<>();
    resultMap.put("id", 1);
    resultMap.put("name", data);
    resultMap.put("timestamp", System.currentTimeMillis());

    List<Map<String, Object>> items = new ArrayList<>();
    Map<String, Object> item1 = new HashMap<>();
    item1.put("itemId", 100);
    item1.put("status", "active");

    Map<String, Object> item2 = new HashMap<>();
    item2.put("itemId", 200);
    item2.put("status", "pending");

    items.add(item1);
    items.add(item2);
    resultMap.put("items", items);

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("version", 1.0);
    metadata.put("source", "test");
    resultMap.put("metadata", metadata);

    return resultMap;
  }
}