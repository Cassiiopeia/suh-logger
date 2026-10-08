# Masking

Masking is on by default since 3.0. In 2.x it was off, and login responses, refresh tokens and CSRF tokens ended up in application logs in plain text (#55).

## What is matched

A key is sensitive when its name **contains** one of the configured words, ignoring case. `token` therefore covers `accessToken`, `refreshToken`, `idToken` and `pushToken`.

Built-in field keys: `password`, `passwd`, `pwd`, `secret`, `token`, `authorization`, `credential`, `apikey`, `api_key`, `api-key`, `csrf`, `cookie`, `privatekey`, `private_key`.

Built-in header keys: `authorization`, `cookie`, `set-cookie`, `x-api-key`, `x-auth-token`, `x-csrf-token`, `x-xsrf-token`.

The `pii` preset adds `email`, `phone`, `mobile`, `ssn`, `residentnumber` and `address`. These are not in the defaults because they are often needed while debugging.

## Where it applies

| Source | How |
|---|---|
| Method parameters | Each argument becomes a tree (map/list/value). A sensitive key at any depth is replaced, along with its whole subtree |
| Return values | Same as parameters, including the body of a `ResponseEntity` |
| HTTP response bodies | Parsed as JSON with your application's `ObjectMapper`/`JsonMapper` when present. Otherwise `"key": value` pairs are masked by pattern. Masking runs before pretty printing and before a custom `HttpLogFormatter` |
| Request headers | When `header.enabled` is on |

Objects are turned into trees with your application's Jackson mapper first (so `@JsonIgnore`, naming strategies and the Kotlin module apply). If that fails, suh-logger reads fields by reflection. JPA entities and Hibernate proxies are not converted: walking them could trigger lazy loading, so they keep their `toString()`.

## Per method

```java
@LogMonitor(mask = TriState.ON, maskFields = {"cardNumber"})   // force on and add keys
public void pay(PaymentRequest request) { ... }

@LogCall(mask = TriState.OFF)                                  // never mask this method
public void debugDump(Object state) { ... }
```

## Configuration

```yaml
suh-logger:
  masking:
    mask-fields: [ cardNumber, ssn ]   # added to the defaults
    presets: [ pii ]
    use-defaults: false                # only if you want your list alone
    mask-value: "[hidden]"
```

Turning masking off entirely (`masking.enabled=false`) while response bodies are logged prints a warning at startup.
