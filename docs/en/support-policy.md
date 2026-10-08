# Support policy

## Versions

suh-logger follows semantic versioning. The release workflow derives the version from commit titles: `type!` → major, `feat` → minor, anything else → patch.

- **Public API:** `kr.suhsaechan.suhlogger.annotation`, `config.SuhLoggerProperties`, `spi`, `util.SuhLogger`, `util.SuhTimeUtil`, `util.CommonUtil`, the configuration classes, and every `suh-logger.*` property key. It breaks only in a major release, and CI checks it with japicmp against the previous release.
- **`@Incubating`:** may change in a minor release. The release notes say what changed.
- **`*.internal.*`:** no guarantees. Do not use it from applications.
- **Deprecations:** marked `@Deprecated(since = ..., forRemoval = true)` in a minor release and removed no earlier than the next major. Renamed properties keep working with a startup warning until then.

## Platforms

| | Supported |
|---|---|
| Java | 17, 21 (bytecode targets 17) |
| Spring Boot | 3.x (latest two minors), 4.x |
| Spring Framework without Boot | 6.x |
| `javax.*` (Boot 2 / Spring 5) | not supported; planned as a separate adapter |

A platform leaves the table only in a major release.

## Releases

Fixes go into the latest minor release of the latest major.
