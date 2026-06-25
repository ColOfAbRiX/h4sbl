# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-06-29

### Changed

- Renamed `LogConfig.redactHeaders` to `LogConfig.redactSecrets` — a single master
  switch that controls all redaction (headers, query parameters, and body sanitization)
- `sensitiveHeaders` now replaces the built-in set when provided (exclusive), instead of being
  additive. When empty, the http4s built-in set is used (Authorization, Cookie, Set-Cookie)
- Body sanitization (`sanitizeBody`) is now gated on `redactSecrets = true`

### Added

- `LogConfig.sensitiveQueryParams` — specify custom query parameter names to redact, replacing
  the built-in set when provided
- `LogConfig.SensitiveQueryParams` — built-in set of commonly sensitive query parameter names
  (token, api_key, password, secret, etc.) used when `sensitiveQueryParams` is empty
- URI query parameter redaction — sensitive parameter values are replaced with `<REDACTED>` when
  `redactSecrets = true`, preventing tokens, API keys, and other secrets from leaking into logs
- `LogConfig.sanitizeBody` hook — apply a `String => String` transformation to request/response
  bodies before logging, enabling format-aware body redaction

## [1.1.0] - 2026-06-20

### Added

- Custom sensitive header support via `LogConfig.sensitiveHeaders`
- `LogConfig.sanitizeBody` hook for body redaction

### Fixed

- URI query parameter values are now redacted when `redactHeaders = true`

## [1.0.1] - 2026-05-27

### Added

- Using Scala3 3.3.7 LTS

[1.2.0]: https://github.com/ColOfAbRiX/h4sbl/releases/tag/1.2.0
[1.1.0]: https://github.com/ColOfAbRiX/h4sbl/releases/tag/1.1.0
[1.0.1]: https://github.com/ColOfAbRiX/h4sbl/releases/tag/1.0.1
