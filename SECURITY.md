# Security policy

suh-logger handles data that often contains secrets: request bodies, tokens, headers. Reports about leaks are taken seriously.

## Reporting a vulnerability

Please **do not open a public issue**. Report privately through GitHub:

**[Report a vulnerability](https://github.com/Cassiiopeia/suh-logger/security/advisories/new)** (Security → Advisories → Report a vulnerability)

Include the suh-logger version, your Spring Boot version, a minimal reproduction and what was exposed. You will get an answer in the advisory thread. Fixes are released as a patch version and credited in the advisory unless you ask otherwise.

## In scope

- Values that should be masked by default but appear in logs
- Masking that can be bypassed (nested structures, encodings, specific types)
- Anything that lets log output change application behaviour

## Supported versions

| Version | Security fixes |
|---|---|
| 3.x | yes |
| 2.x | no — upgrade to 3.x, which masks secrets by default |

## Hardening tips

- Keep `suh-logger.masking.enabled=true` (the default) in every environment that handles real data.
- Prefer `suh-logger.response-body=error-only` in production.
- Add domain-specific keys with `suh-logger.masking.mask-fields`, and personal data with `suh-logger.masking.presets=pii`.
