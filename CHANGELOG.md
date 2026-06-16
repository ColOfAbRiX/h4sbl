# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-06-15

### Added

- Custom sensitive header support via `LogConfig.sensitiveHeaders` — extend the built-in redaction set
  (Authorization, Proxy-Authorization, Cookie, Set-Cookie) with application-specific headers
- `LogConfig.sanitizeBody` hook — apply a `String => String` transformation to request/response bodies
  before logging, enabling format-aware body redaction (e.g., masking password fields in JSON)

### Fixed

- URI query parameter values are now redacted when `redactHeaders = true`, preventing tokens,
  API keys, and other secrets in query strings from leaking into logs

## [1.0.1] - 2026-05-27

### Added

- Using Scala3 3.3.7 LTS
