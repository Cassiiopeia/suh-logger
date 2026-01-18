## 절대 금지사항
- **절대로 git commit, git push, git add 등의 git 명령어 사용 금지**
- 사용자가 명시적으로 요청하지 않은 이상 git 관련 작업 금지
- 코드 변경 후 자동으로 커밋하거나 푸시하지 말 것

## 문서 작성 규칙

### 의존성 버전 표기
README.md 및 문서에서 의존성 버전을 표기할 때:
- 버전은 `x.x.x`로 표기 (하드코딩 금지)
- 주석으로 최신 버전 확인 안내 추가

**Gradle 예시:**
```groovy
dependencies {
    implementation 'kr.suhsaechan:suh-logger:x.x.x' // 최신 버전으로 변경하세요
}
```

**Maven 예시:**
```xml
<dependency>
    <groupId>kr.suhsaechan</groupId>
    <artifactId>suh-logger</artifactId>
    <version>x.x.x</version> <!-- 최신 버전으로 변경하세요 -->
</dependency>
```