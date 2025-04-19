# Suh-Logger 문제 해결 가이드

## 1. 로그 출력이 안 될 때
- `@Slf4j` 어노테이션이 있는지 확인
- SLF4J 구현체(logback)가 하나만 있는지 확인
    - `build.gradle` 에 `slf4j-simple` 이 추가되어 있으면 제거하세요.

## 2. 의존성 충돌
- Spring Boot 프로젝트라면 `spring-boot-starter-aop` 와 `spring-boot-starter` 만 사용
- `api 'org.slf4j:slf4j-api'` 는 생략 가능 (starter 내부 포함)

## 3. JSON 변환 에러
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` 의존성 확인
- `objectMapper.registerModule(new JavaTimeModule())` 적용 여부 확인

---

### CHANGELOG.md
```markdown
