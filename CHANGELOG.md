# Changelog

## [1.0.9] – 2025-08-03
- **코드 구조 개선**: 로거와 직접적인 관련이 없는 범용 유틸리티 로직을 `commonUtil.java`로 분리
  - JTS Geometry, MultipartFile 처리 로직 등을 별도 유틸리티 클래스로 분리
  - 단일 책임 원칙에 따른 클래스 역할 명확화
  - 코드 재사용성 및 유지보수성 향상
- **JTS Geometry 순환 참조 문제 해결**: PostgreSQL JTS Point 객체 로깅 시 발생하는 무한 순환 참조 버그 수정
  - Jackson ObjectMapper에 순환 참조 방지 설정 추가 (`FAIL_ON_SELF_REFERENCES = false`, `WRITE_SELF_REFERENCES_AS_NULL = true`)
  - JTS Geometry 객체(Point, Polygon 등) 전용 안전 처리 로직 구현
  - `envelope` 속성으로 인한 1000번 중첩 오류 방지
  - 좌표 정보(x, y, longitude, latitude), SRID, WKT 형식 등 유용한 정보만 추출하여 로깅

## [1.0.7] – 2025-07-08
- **추가 개선사항**: 기존 1.0.6 버전 기반 추가 수정 및 최적화

## [1.0.6] – 2025-07-08
- **완전한 로깅 의존성 분리**: 상위 프로젝트의 로깅 프레임워크와 완전히 독립된 로깅 시스템 구현
  - SLF4J, Logback, Log4j 등 모든 외부 로깅 프레임워크 의존성 제거
  - Spring Boot의 기본 로깅 모듈(`spring-boot-starter-logging`) 완전 제외
  - Java Util Logging(JUL)만을 사용한 순수 자체 로깅 시스템 구축
  - 상위 프로젝트의 로깅 설정과 완전히 격리되어 로그 충돌 및 간섭 현상 해결

## [1.0.4] – 2025-07-08
- **독립적인 로깅 시스템 구축**: 상위 프로젝트의 로깅 설정과 완전히 분리된 독립적인 로깅 시스템 구현
  - `SuhLoggerAutoConfiguration` 클래스 추가: Spring Boot 자동 설정 지원
  - `SuhLoggerConfig` 클래스 추가: 독립적인 로거 인스턴스 및 커스텀 포맷터 제공
  - 상위 프로젝트의 로거 설정에 영향받지 않는 완전 격리된 로깅 환경 제공
- **멀티파트 파일 로깅 지원**: MultipartFile 객체를 안전하게 로깅할 수 있는 기능 추가
  - 파일명, 콘텐츠 타입, 파일 크기, 빈 파일 여부 등 파일 정보 자동 추출
  - 리플렉션을 활용한 안전한 MultipartFile 정보 추출로 의존성 최소화
  - 파일 업로드 관련 디버깅 및 모니터링 개선

## [1.0.1] – 2025-04-21
-  메소드로깅 커스텀 어노테이션 추가 (HTTP 웹 요청 로깅 포함)

## [0.0.3] – 2025-04-19
- `superLog` 메서드 옵션값 추가 (ClassType 출력 여부)

## [0.0.2] – 2025-04-19
- `superLog` 메서드 추가 (사용자 정의 로그 레벨 지원)
- `SuhTimeUtil` 시간 포맷 메서드 보강

## [0.0.1] – 2025-04-18
- 초기 릴리즈
    - `lineLog`, `timeLog` 등 기본 기능 제공
