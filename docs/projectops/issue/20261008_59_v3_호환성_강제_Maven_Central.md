📝 현재 문제점
---

- 공개 API가 바뀌어도 이를 잡아낼 장치가 없어 오픈소스로 운영할 때 기여 PR이 하위호환을 깨도 리뷰어가 매번 손으로 확인해야 한다.
- 지원하는 Spring Boot·Java 조합을 실제로 검증하는 CI가 없다.
- 배포가 개인 Nexus(`allowInsecureProtocol=true`)에만 되어 외부 사용자는 낯선 저장소를 `repositories`에 추가해야 한다. 오픈소스 라이브러리로는 가장 큰 진입 장벽이다.
- GitHub Release가 0개다 (태그만 존재).

🛠️ 해결 방안 / 제안 기능
---

- 설계 문서: `docs/superpowers/specs/2026-10-08-suh-logger-v3-architecture-design.md`
- japicmp로 직전 릴리스와 바이너리 호환 비교 (`internal`, `@Incubating` 제외). 깨지면 빌드 실패.
- ArchUnit으로 모듈 의존 방향 강제 (core의 Spring import 금지, `internal` 교차 참조 금지).
- CI 매트릭스: Boot 3.x·4.x × Java 17/21 × 예제 앱 E2E.
- Maven Central 배포(GPG 서명) + Nexus 병행. Nexus는 HTTPS 강제.
- `suh-logger-test-kit`: 확장 구현용 적합성 테스트.
- Dependabot 의존성 업데이트.

⚙️ 작업 내용
---

- [ ] japicmp, ArchUnit 설정
- [ ] CI 매트릭스 워크플로우
- [ ] Maven Central 배포 설정 (계정, 네임스페이스 인증, GPG 키는 메인테이너가 준비)
- [ ] Nexus HTTPS 강제
- [ ] test-kit 모듈
- [ ] Dependabot 설정

🙋‍♂️ 담당자
---

- 백엔드: Cassiiopeia
