# Migrating from 2.x to 3.0

Code written for 2.x keeps compiling. These are unchanged:

- the dependency `kr.suhsaechan:suh-logger` (it now pulls in `suh-logger-spring-boot-starter`)
- `kr.suhsaechan.suhlogger.annotation.*`, `kr.suhsaechan.suhlogger.util.SuhLogger`, `SuhTimeUtil`, `CommonUtil`
- `kr.suhsaechan.suhlogger.config.SuhLoggerProperties` and every `suh-logger.*` key

## Behaviour changes to review

| Change | 2.x | 3.0 | Keep the old behaviour |
|---|---|---|---|
| Masking | off | **on**, with built-in sensitive keys | `suh-logger.masking.enabled=false` (startup warning) |
| `mask-fields` | the only keys | **added** to the defaults | `suh-logger.masking.use-defaults=false` |
| Error responses | not logged | 4xx/5xx logged; 5xx at WARN | `suh-logger.response-body=none` hides bodies, not the status line |
| Default exclusions | none | `/actuator/**` | set `exclude-patterns` to your own list (it replaces the default) |
| `exclude-patterns` matching | substring | Ant patterns. Values without wildcards still use substring matching and log a deprecation warning | rewrite `auth/login` as `/api/auth/login/**` |
| DTO output | `toString()` | field tree (JSON-like); entities without their associations | — |
| Request log | no timing | `Duration: N ms` line in block format | — |

## Moved or removed classes

| 2.x | 3.0 |
|---|---|
| `kr.suhsaechan.suhlogger.config.SuhLoggerAutoConfiguration` | `kr.suhsaechan.suhlogger.boot.autoconfigure.SuhLoggerAutoConfiguration` (+ `SuhLoggerServletAutoConfiguration`, `SuhLoggerReactiveAutoConfiguration`, `SuhLoggerJsonAutoConfiguration`). Update `@SpringBootApplication(exclude = ...)` if you used it |
| `SuhLoggerAutoConfiguration.SuhLoggerInitializer` | `kr.suhsaechan.suhlogger.spring.SuhLoggerConfiguration.SuhLoggerInitializer` |
| `kr.suhsaechan.suhlogger.SuhLoggerApplication` (inside the jar) | removed — a library should not ship an application class |
| `application.properties` inside the jar | removed — it could shadow your own settings |
| `META-INF/spring.factories` | removed — Boot 3+ reads `AutoConfiguration.imports` only |
| aspects with field injection (`@Autowired`) | constructor injection; registered by `SuhLoggerConfiguration`, not component scanning |
| transitive `jackson-databind` 2.18.3 (`api`) | not exported — your application's Jackson version is used (Jackson 2 on Boot 3, Jackson 3 on Boot 4) |

## Gradle example

```groovy
dependencies {
    implementation 'kr.suhsaechan:suh-logger-spring-boot-starter:x.x.x' // 최신 버전으로 변경하세요
}
```

## Checklist

- [ ] Search logs or tests that expected unmasked values and update them.
- [ ] If you relied on `/actuator` being logged, set `exclude-patterns` explicitly.
- [ ] Rewrite substring exclusions as Ant patterns to silence the deprecation warning.
- [ ] If you excluded the auto-configuration by class, use the new class name.
- [ ] Kotlin: make sure the `kotlin-spring` plugin is applied (watch for the startup warning).
