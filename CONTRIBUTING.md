# Contributing to suh-logger

Thanks for helping. This page is everything you need to go from an idea to a merged pull request. (한국어로 이슈·PR을 써도 됩니다.)

## Before you start

- **Bug or idea?** Open an issue with the template. For larger changes, agree on the approach in the issue first. It saves you a rewrite.
- **Security problems** go to [SECURITY.md](SECURITY.md), not to a public issue.

## Set up

Requirements: JDK 17 (21 also works). Gradle comes with the wrapper.

```bash
git clone https://github.com/Cassiiopeia/suh-logger.git
cd suh-logger
./gradlew build          # all modules, unit tests, architecture rules, examples E2E
```

No credentials are needed to build or test. `gradle.properties` is only needed for publishing, and it is git-ignored.

## Where things go

| You want to… | Change |
|---|---|
| change masking, serialization, HTTP log format | `suh-logger-core` (`internal/mask`, `internal/serialize`, `internal/http`) |
| change annotation behaviour | `suh-logger-spring` (`aspect/`) |
| change servlet / WebFlux request handling | `suh-logger-servlet` / `suh-logger-webflux` |
| add or change a property | `SuhLoggerProperties` in core + `docs/en/configuration.md` + `docs/ko/configuration.md` |
| register something automatically in Boot | `suh-logger-spring-boot-autoconfigure` |
| support a new environment | a new adapter module + `examples/<name>`. See [docs/en/extending.md](docs/en/extending.md#adding-a-new-environment) |

Rules that CI enforces:

- `suh-logger-core` must not import Spring, Jackson, Servlet or Reactor. `suh-logger-architecture-tests` checks this and other module boundaries.
- Public API must stay binary compatible within a major version. `apiCompat` (japicmp) checks it once a baseline release exists. Anything under `*.internal.*` or marked `@Incubating` is exempt.
- Every supported environment has an `examples/*` app with an end-to-end test. Keep them green.

## Make the change

1. Branch from `develop`. The release flow is `develop → main`.
2. Write a failing test first, then the fix. Use `LogCapture` from `suh-logger-test-kit` to assert on log output.
3. Comment the *why*, not the *what*. Existing comments are in Korean; English is fine for new ones.
4. Update the docs in the same pull request. `README.md` is the canonical English version; mirror user-facing changes in `README.ko.md`. Write dependency versions as `x.x.x` with a "latest version" comment.

## Commit messages

```
<issue title without tags> : <type> : <what changed> <issue URL>
```

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`. **Add `!` (`feat!`, `fix!`) when existing users must change something.** The release workflow reads these markers: `!` → major, `feat` → minor, everything else → patch.

## Pull requests

- Target `develop`.
- Link the issue and say how you tested.
- CI runs the compatibility matrix (Java 17 and 21, all examples). A red matrix means an environment broke.
- A maintainer reviews and merges it.

## Releasing

Maintainers only: [docs/maintainers/releasing.md](docs/maintainers/releasing.md).
