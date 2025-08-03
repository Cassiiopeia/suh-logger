# Suh-Logger 문제 해결 가이드

## 1. 로그 출력이 안 될 때
- SuhLogger 라이브러리는 순수 JUL(Java Util Logging) 기반으로 동작합니다
- 외부 로깅 프레임워크(SLF4J, Logback)와 완전히 독립적입니다
- 별도의 로깅 설정 없이 바로 사용 가능합니다

## 2. 의존성 충돌
- Spring Boot 프로젝트라면 `spring-boot-starter-aop` 와 `spring-boot-starter` 만 사용
- SLF4J 관련 의존성은 필요 없습니다 (완전 독립적인 로깅 시스템)

## 3. JSON 변환 에러
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` 의존성 확인
- `objectMapper.registerModule(new JavaTimeModule())` 적용 여부 확인

---

### CHANGELOG.md
```markdown
