package kr.suhsaechan.suhlogger.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot 3.x 및 4.x 호환성 검증 테스트
 */
class AutoConfigurationCompatibilityTest {

  @Test
  void autoConfigureAfterAnnotationUsesStringNames() {
    AutoConfigureAfter annotation = SuhLoggerAutoConfiguration.class
        .getAnnotation(AutoConfigureAfter.class);

    assertNotNull(annotation, "@AutoConfigureAfter 어노테이션이 존재해야 함");
    assertTrue(annotation.name().length > 0, "name 속성을 사용해야 함");
    assertEquals(0, annotation.value().length, "value 속성(클래스 직접 참조)은 사용하지 않아야 함");

    // Spring Boot 3.x와 4.x 경로가 모두 포함되어 있는지 확인
    String[] names = annotation.name();
    boolean has3xSecurityPath = false;
    boolean has4xSecurityPath = false;

    for (String name : names) {
      if (name.contains("org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")) {
        has3xSecurityPath = true;
      }
      if (name.contains("org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration")) {
        has4xSecurityPath = true;
      }
    }

    assertTrue(has3xSecurityPath, "Spring Boot 3.x Security 경로가 포함되어야 함");
    assertTrue(has4xSecurityPath, "Spring Boot 4.x Security 경로가 포함되어야 함");
  }

  @Test
  void autoConfigureBeforeAnnotationUsesStringNames() {
    AutoConfigureBefore annotation = SuhLoggerAutoConfiguration.class
        .getAnnotation(AutoConfigureBefore.class);

    assertNotNull(annotation, "@AutoConfigureBefore 어노테이션이 존재해야 함");
    assertTrue(annotation.name().length > 0, "name 속성을 사용해야 함");
    assertEquals(0, annotation.value().length, "value 속성(클래스 직접 참조)은 사용하지 않아야 함");

    // Spring Boot 3.x와 4.x 경로가 모두 포함되어 있는지 확인
    String[] names = annotation.name();
    boolean has3xErrorPath = false;
    boolean has4xErrorPath = false;

    for (String name : names) {
      if (name.contains("org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration")) {
        has3xErrorPath = true;
      }
      if (name.contains("org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration")) {
        has4xErrorPath = true;
      }
    }

    assertTrue(has3xErrorPath, "Spring Boot 3.x Error 경로가 포함되어야 함");
    assertTrue(has4xErrorPath, "Spring Boot 4.x Error 경로가 포함되어야 함");
  }
}
