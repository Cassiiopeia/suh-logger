# ADR 0001: Framework-free core with per-stack adapters

- Status: accepted (3.0.0)
- Issue: #57

## Context

Until 2.x everything lived in one jar: the static `SuhLogger`, the AOP aspects and a servlet filter. The aspect imported `jakarta.servlet` directly, so non-servlet applications (WebFlux, batch) could fail to load it. The jar also shipped an `@SpringBootApplication` class and an `application.properties`.

## Decision

- `suh-logger-core` depends only on `slf4j-api` and holds annotations, `SuhLoggerProperties`, masking, serialization and the SPI.
- Each framework gets an adapter module: `spring` (AOP), `servlet`, `webflux`, `json-jackson2`, `json-jackson3`. Auto-configuration picks the adapters by classpath conditions. The starter brings them all, so applications add one dependency.
- Adapters turn framework objects into neutral values (`RequestSnapshot`, response bytes) and share one implementation for masking, exclusion and formatting (`HttpExchangeLogger`).
- `SuhLoggerProperties` stays a plain JavaBean in core. Boot binds it with `@Bean @ConfigurationProperties` on the auto-configuration method instead of annotating the class.
- Module boundaries are checked by ArchUnit rules in `suh-logger-architecture-tests`.

## Consequences

- A new environment means one adapter, not changes across the codebase.
- More artifacts. The BOM and the starter hide this from users.
- Package names that users import did not change, so 2.x code compiles. The auto-configuration class moved.
