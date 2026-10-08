# Configuration reference

Every key lives under `suh-logger.` and is optional. Spring Boot binds them automatically, and your IDE shows completion and descriptions from `spring-configuration-metadata.json`. Without Boot, set the same fields on a `SuhLoggerProperties` bean (XML/JavaConfig) or pass one to `SuhLogger.setProperties(...)` (plain Java).

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch for annotations and HTTP logging |
| `format` | `block` | `block`: framed multi-line output (2.x style). `line`: one event per request — `METHOD URI -> STATUS (Nms) [SLOW] rid=... body=...` |
| `response-body` | `all` | `none`, `error-only` (4xx/5xx only) or `all` |
| `max-response-body-size` | `4096` | Bodies longer than this are replaced with `[Too large to log - N bytes, max: M]`. WebFlux copies only this many bytes |
| `pretty-print-json` | `false` | Indent JSON bodies (folded back to one line in `line` format) |
| `slow-threshold-ms` | `0` | When > 0, requests slower than this are logged at WARN and marked `[SLOW]`. 5xx responses are always WARN |
| `filter-order` | `2147483647` | Order of the servlet filter / WebFlux `WebFilter`. The default runs after Spring Security, so the final status is logged |
| `exclude-patterns` | `[/actuator/**]` | Paths not logged. Ant syntax: `*` one segment, `**` any segments, `?` one character, `{name}` one segment. Setting the list **replaces** the default. A value without wildcards uses legacy *contains* matching and logs a deprecation warning once |
| `excluded-classes` | `[]` | Class names (or fragments) whose instances are logged as a short `EXCLUDED_CLASS` marker |
| `request-id.enabled` | `false` | Puts a request id in MDC and the response header. An incoming header value is reused |
| `request-id.header` | `X-Request-Id` | Header name for reading and writing the id |
| `request-id.mdc-key` | `requestId` | MDC key — use `%X{requestId}` in your log pattern |
| `masking.enabled` | `true` | Mask sensitive values (see [masking](masking.md)) |
| `masking.use-defaults` | `true` | Include the built-in sensitive keys |
| `masking.mask-fields` | `[]` | Extra field keys, **added** to the defaults |
| `masking.mask-headers` | `[]` | Extra header keys, added to the defaults |
| `masking.presets` | `[]` | `pii`: email, phone, mobile, ssn, resident number, address |
| `masking.mask-value` | `****` | Replacement text |
| `header.enabled` | `false` | Log request headers in `@LogMonitor`/`@LogCall` output (servlet only) |
| `header.include-all` | `false` | All headers instead of `include-headers` |
| `header.include-headers` | `[]` | Header names to log |

## Profiles

```yaml
# Local development: see everything
suh-logger:
  pretty-print-json: true
  header:
    enabled: true
    include-all: true
```

```yaml
# Production
suh-logger:
  format: line
  response-body: error-only
  slow-threshold-ms: 500
  request-id:
    enabled: true
```

```yaml
# Tests: silence
suh-logger:
  enabled: false
```

## Log pattern with request id

```yaml
logging:
  pattern:
    level: "%5p [%X{requestId:-}]"
```

On WebFlux the id is written to the response header and the log line but not to MDC, because MDC is thread-local and does not follow a reactive pipeline.
