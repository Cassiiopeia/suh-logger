# Releasing suh-logger (maintainers)

릴리스는 projectops 흐름을 그대로 쓴다: `develop`에서 작업 → `develop → main` 릴리스 PR → 머지되면 `main` push로 배포 워크플로우가 돈다.

## 버전은 커밋이 정한다

`version.yml`의 `semver_auto: true`. 릴리스 구간 커밋 제목으로 올림 폭이 정해진다.

| 커밋 타입 | 예 | 올림 |
|---|---|---|
| `type!` (호환성 깨짐) | `... : feat! : ...` | major |
| `feat` | `... : feat : ...` | minor |
| 그 외 | `fix`, `refactor`, `docs`, `chore`, `test` | patch |

공개 API를 깨는 변경은 반드시 `!`를 붙인다. 붙이지 않으면 `apiCompat`(japicmp)가 CI에서 막는다.

## 배포 대상

| 저장소 | 조건 | 비고 |
|---|---|---|
| SUH Nexus (`nexus.suhsaechan.kr`) | `nexusUsername`·`nexusPassword` | 2.x부터 쓰던 저장소, HTTPS만 |
| Maven Central | `mavenCentralUsername`·`mavenCentralPassword` | 값이 있을 때만 업로드 (없으면 Nexus만) |
| 서명 (Central 필수) | `signingInMemoryKey`·`signingInMemoryKeyPassword` | 값이 있을 때만 서명 |

모든 값은 GitHub Secret `GRADLE_PROPERTIES`(배포 워크플로우가 `gradle.properties`로 풀어 쓴다)에 넣는다. 레포에는 커밋하지 않는다.

## Maven Central 설정 (2026-10-12 완료)

현재 상태: 네임스페이스 `kr.suhsaechan` 검증 완료, 업로드 토큰·서명 키가 `GRADLE_PROPERTIES` Secret에 등록돼 있습니다. 다음 릴리스부터 Nexus와 Central 양쪽에 올라갑니다.

다시 만들어야 할 때 (토큰 폐기, 키 교체 등):

1. https://central.sonatype.com 에 GitHub 로그인 → Publishing → Namespaces. `kr.suhsaechan`은 DNS TXT 레코드로 소유를 확인합니다 (도메인 DNS에 화면의 Verification Key를 `@` TXT로 추가한 뒤 Verify Namespace).
2. https://central.sonatype.com/usertoken 에서 User Token을 발급합니다 (계정 비밀번호가 아니라 토큰). 값은 발급 직후 한 번만 보입니다.
3. 서명용 GPG 키를 만들고 공개키를 키서버에 올립니다. 키서버 업로드가 `gpg --send-keys`로 안 되면 HTTP API(`https://keyserver.ubuntu.com/pks/add`, `https://keys.openpgp.org/vks/v1/upload`)를 씁니다.
   ```bash
   gpg --full-generate-key                                   # RSA 4096, 서명 용도
   gpg --armor --export-secret-keys <KEY_ID>                 # signingInMemoryKey 값
   ```
4. GitHub Secret `GRADLE_PROPERTIES`를 아래 형식으로 **통째로** 교체합니다 (이 Secret은 읽을 수 없고 덮어쓰기만 됩니다).
   ```properties
   nexusUsername=...
   nexusPassword=...
   mavenCentralUsername=<token username>
   mavenCentralPassword=<token password>
   signingInMemoryKey=<armored private key>
   signingInMemoryKeyPassword=<passphrase>
   ```
   ⚠️ 워크플로우가 `echo -e "$GRADLE_PROPERTIES" > gradle.properties`로 풀기 때문에, `signingInMemoryKey`의 줄바꿈은 **`\\n`(백슬래시 두 개 + n)** 으로 이중 이스케이프해야 합니다. 한 번만 이스케이프하면 키가 60줄로 풀려 properties 파싱이 깨집니다.
5. 검증: 같은 방식으로 파일을 만들어 `./gradlew :suh-logger-core:signMavenPublication -Pversion=0.0.0-test`가 `.asc` 파일을 만드는지 확인합니다.

Central 배포는 **되돌릴 수 없습니다** (한 번 올라간 버전은 지울 수 없음). 테스트 배포를 하지 말고, 자격증명은 `GET https://central.sonatype.com/api/v1/publisher/published?...`에 `Authorization: Bearer base64(user:password)`로 호출해 200/401로 확인합니다.

## 공개 API 호환 검사 기준 갱신

`gradle/libs.versions.toml`의 `api-baseline`:

- `"none"`: 검사하지 않음 (3.0.0 배포 전)
- `"3.0.0"`: 3.0.0 대비 바이너리 호환을 `./gradlew check`가 강제

**3.0.0이 배포되면 바로 `"3.0.0"`으로 바꾼다.** major 릴리스 직후에는 새 major 버전으로 다시 올린다.

로컬에서 미리 보기: `./gradlew apiCompat -PapiBaseline=<버전>` (기준 jar는 Maven Central → Nexus 순으로 받는다).
