# ADR 0003: Mask sensitive values by default

- Status: accepted (3.0.0)
- Issue: #55

## Context

In 2.x response bodies were logged by default and masking was off. A consumer found access tokens, refresh tokens and CSRF tokens in plain text during QA. DTOs were printed with `toString()`, so records and Lombok `@Data` classes leaked secrets even when parameter-name masking was on.

## Decision

- `masking.enabled` defaults to `true`, with a built-in list of sensitive key fragments. User keys are added to the list, and `use-defaults=false` removes it.
- Values are turned into trees (map/list/value) before logging, so keys are matched at any depth and a matching key hides its whole subtree.
- JPA entities and Hibernate proxies are not converted, to avoid lazy-loading queries.
- Personal data (email, phone) is opt-in through the `pii` preset, because it is often needed while debugging.
- Turning masking off while bodies are logged prints a startup warning.

## Consequences

- Upgrading changes log output: secrets disappear and DTOs appear as field trees. This is a major-version change.
- Substring matching may mask harmless keys such as `tokenCount`. Masking too much is the safer failure mode.
