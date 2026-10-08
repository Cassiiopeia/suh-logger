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

## Maven Central 최초 설정 (한 번만)

1. https://central.sonatype.com 에 가입하고 **Namespace `kr.suhsaechan`** 을 등록한다. 도메인 소유 확인은 DNS TXT 레코드로 한다.
2. **User Token**을 발급한다 (계정 비밀번호가 아니라 토큰).
3. GPG 키를 만들고 공개키를 키서버에 올린다.
   ```bash
   gpg --full-generate-key                      # RSA 4096
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   gpg --armor --export-secret-keys <KEY_ID>    # signingInMemoryKey 값
   ```
4. `GRADLE_PROPERTIES` Secret에 추가한다.
   ```properties
   mavenCentralUsername=<token username>
   mavenCentralPassword=<token password>
   signingInMemoryKey=<armored private key, 줄바꿈은 \n 으로>
   signingInMemoryKeyPassword=<passphrase>
   ```
5. 다음 릴리스부터 Central에도 올라간다 (`publishToMavenCentral(automaticRelease = true)`).

## 공개 API 호환 검사 기준 갱신

`gradle/libs.versions.toml`의 `api-baseline`:

- `"none"`: 검사하지 않음 (3.0.0 배포 전)
- `"3.0.0"`: 3.0.0 대비 바이너리 호환을 `./gradlew check`가 강제

**3.0.0이 배포되면 바로 `"3.0.0"`으로 바꾼다.** major 릴리스 직후에는 새 major 버전으로 다시 올린다.

로컬에서 미리 보기: `./gradlew apiCompat -PapiBaseline=<버전>` (기준 jar는 Maven Central → Nexus 순으로 받는다).
