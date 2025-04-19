# 기여 가이드라인

Suh Nickname Generator 프로젝트에 기여하는 방법에 대한 가이드라인입니다.

## 개발 환경 설정

1. 저장소를 복제합니다:
   ```bash
   git clone https://github.com/suhsaechan/suh-nickname-generator.git
   cd suh-nickname-generator
   ```

2. Gradle을 사용하여 프로젝트를 빌드합니다:
   ```bash
   ./gradlew build
   ```

## 코드 스타일

- Java 코드는 구글 Java 스타일 가이드를 따릅니다.
- 들여쓰기는 2칸 공백을 사용합니다.
- 모든 public 메소드에는 JavaDoc 주석을 추가합니다.

## 테스트

모든 새로운 기능이나 버그 수정에는 테스트가 포함되어야 합니다:

```bash
./gradlew test
```

## 배포 프로세스

1. 버전을 업데이트합니다 (`build.gradle`의 `version` 속성)
2. 다음 명령어로 Nexus 저장소에 배포합니다:
   ```bash
   ./gradlew publish
   ```

## 이슈 및 풀 리퀘스트

- 이슈는 버그 리포트나 새로운 기능 요청에 사용됩니다.
- 풀 리퀘스트를 제출하기 전에 모든 테스트가 통과하는지 확인해주세요.
