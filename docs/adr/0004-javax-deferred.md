# ADR 0004: Defer javax (Spring 5 / Boot 2 / Java 8) support

- Status: accepted (3.0.0)

## Context

Many XML-configured production systems still run Spring 5 with `javax.servlet` on Java 8 or 11. Supporting them would require compiling core for Java 8 and maintaining a `javax` servlet adapter, which doubles the CI matrix.

## Decision

3.0 targets Java 17 and Jakarta (Spring 6+/Boot 3+). javax support is planned as a separate adapter module once there is demand. The core/adapter split (ADR 0001) keeps this an additive change.

## Consequences

- Java 8/11 and javax users stay on 2.x for now.
- Core keeps modern Java (records, `List.of`) until a decision on a Java 8 baseline is made.
