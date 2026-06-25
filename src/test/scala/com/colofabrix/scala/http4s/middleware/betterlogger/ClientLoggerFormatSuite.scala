package com.colofabrix.scala.http4s.middleware.betterlogger

import munit.FunSuite
import org.http4s.*
import org.http4s.syntax.all.uri
import org.typelevel.ci.CIString

class ClientLoggerFormatSuite extends FunSuite {

  private val noColors = LogColors.noColors
  private val debug    = LogLevel.Debug
  private val trace    = LogLevel.Trace

  // --- formatUri ---

  test("formatUri does not redact when redactSecrets is false") {
    val config = LogConfig(redactSecrets = false, colors = noColors)
    val uri    = uri"https://example.com/api?token=secret123&name=john"
    val result = ClientLogger.formatUri(uri, config)
    assert(result.contains("token=secret123"))
    assert(result.contains("name=john"))
  }

  test("formatUri does not redact when there are no query parameters") {
    val config = LogConfig(redactSecrets = true, colors = noColors)
    val uri    = uri"https://example.com/api/path"
    val result = ClientLogger.formatUri(uri, config)
    assertEquals(result, "https://example.com/api/path")
  }

  test("formatUri redacts built-in sensitive query parameter values") {
    val config = LogConfig(redactSecrets = true, colors = noColors)
    val uri    = uri"https://example.com/api?token=secret123&name=john"
    val result = ClientLogger.formatUri(uri, config)
    assert(result.contains("token=<REDACTED>"))
    assert(result.contains("name=john"))
    assert(!result.contains("secret123"))
  }

  test("formatUri redacts all built-in sensitive params") {
    val config = LogConfig(redactSecrets = true, colors = noColors)
    for param <- LogConfig.SensitiveQueryParams do
      val uri    = Uri.unsafeFromString(s"https://example.com/api?$param=value")
      val result = ClientLogger.formatUri(uri, config)
      assert(result.contains(s"$param=<REDACTED>"), clue = s"Failed to redact param: $param")
  }

  test("formatUri uses custom sensitiveQueryParams when provided (exclusive)") {
    val config =
      LogConfig(
        redactSecrets = true,
        colors = noColors,
        sensitiveQueryParams = Set("my_secret"),
      )
    val uri    = uri"https://example.com/api?my_secret=hidden&token=visible"
    val result = ClientLogger.formatUri(uri, config)
    assert(result.contains("my_secret=<REDACTED>"))
    // token is NOT in the custom set, so it should NOT be redacted (exclusive replacement)
    assert(result.contains("token=visible"))
  }

  test("formatUri preserves fragments") {
    val config = LogConfig(redactSecrets = true, colors = noColors)
    val uri    = Uri.unsafeFromString("https://example.com/api?token=secret#section")
    val result = ClientLogger.formatUri(uri, config)
    assert(result.contains("token=<REDACTED>"))
    assert(result.contains("#section"))
  }

  test("formatUri handles query params without values") {
    val config = LogConfig(redactSecrets = true, colors = noColors)
    val uri    = Uri.unsafeFromString("https://example.com/api?flag&token=secret")
    val result = ClientLogger.formatUri(uri, config)
    assert(result.contains("flag"))
    assert(result.contains("token=<REDACTED>"))
  }

  test("formatUri is case-insensitive for query param matching") {
    val config = LogConfig(redactSecrets = true, colors = noColors)
    val uri    = Uri.unsafeFromString("https://example.com/api?TOKEN=secret&Api_Key=key123")
    val result = ClientLogger.formatUri(uri, config)
    assert(result.contains("TOKEN=<REDACTED>"))
    assert(result.contains("Api_Key=<REDACTED>"))
  }

  // --- formatHeaders ---

  test("formatHeaders returns None below Debug level") {
    val config  = LogConfig(colors = noColors)
    val headers = Headers(Header.Raw(CIString("Authorization"), "Bearer secret"))
    val result  = ClientLogger.formatHeaders(headers, config, LogLevel.Info)
    assertEquals(result, None)
  }

  test("formatHeaders redacts Authorization when redactSecrets is true") {
    val config  = LogConfig(redactSecrets = true, colors = noColors)
    val headers = Headers(Header.Raw(CIString("Authorization"), "Bearer secret"))
    val result  = ClientLogger.formatHeaders(headers, config, debug).get
    assert(result.contains("<REDACTED>"))
    assert(!result.contains("Bearer secret"))
  }

  test("formatHeaders does not redact when redactSecrets is false") {
    val config  = LogConfig(redactSecrets = false, colors = noColors)
    val headers = Headers(Header.Raw(CIString("Authorization"), "Bearer secret"))
    val result  = ClientLogger.formatHeaders(headers, config, debug).get
    assert(result.contains("Bearer secret"))
  }

  test("formatHeaders does not redact non-sensitive headers") {
    val config  = LogConfig(redactSecrets = true, colors = noColors)
    val headers = Headers(Header.Raw(CIString("Content-Type"), "application/json"))
    val result  = ClientLogger.formatHeaders(headers, config, debug).get
    assert(result.contains("application/json"))
    assert(!result.contains("<REDACTED>"))
  }

  test("formatHeaders uses custom sensitiveHeaders when provided (exclusive)") {
    val config =
      LogConfig(
        redactSecrets = true,
        colors = noColors,
        sensitiveHeaders = Set(CIString("X-Custom-Auth")),
      )
    val headers =
      Headers(
        Header.Raw(CIString("X-Custom-Auth"), "my-secret"),
        Header.Raw(CIString("Authorization"), "Bearer token"),
      )
    val result = ClientLogger.formatHeaders(headers, config, debug).get
    assert(result.contains("<REDACTED>"))
    // Authorization is NOT in the custom set (exclusive replacement)
    assert(result.contains("Bearer token"))
  }

  test("formatHeaders redacts all built-in sensitive headers") {
    val config = LogConfig(redactSecrets = true, colors = noColors)
    for name <- Headers.SensitiveHeaders do
      val headers = Headers(Header.Raw(name, "secret-value"))
      val result  = ClientLogger.formatHeaders(headers, config, debug).get
      assert(result.contains("<REDACTED>"), clue = s"Failed to redact header: ${name}")
  }

  // --- formatBody ---

  test("formatBody returns None when shouldLog is false") {
    val result = ClientLogger.formatBody("body content", shouldLog = false, trace)
    assertEquals(result, None)
  }

  test("formatBody returns None below Trace level") {
    val result = ClientLogger.formatBody("body content", shouldLog = true, debug)
    assertEquals(result, None)
  }

  test("formatBody returns None for empty body") {
    val result = ClientLogger.formatBody("", shouldLog = true, trace)
    assertEquals(result, None)
  }

  test("formatBody returns formatted body at Trace level when shouldLog is true") {
    val result = ClientLogger.formatBody("hello", shouldLog = true, trace)
    assertEquals(result, Some("""body="hello""""))
  }

  // --- formatRequest ---

  test("formatRequest includes method, URI, and version at Debug level") {
    val config  = LogConfig(redactSecrets = false, colors = noColors)
    val request = Request[fs2.Pure](Method.GET, uri"https://example.com/api")
    val result  = ClientLogger.formatRequest(request, "", config, debug)
    assert(result.contains("HTTP/1.1"))
    assert(result.contains("GET"))
    assert(result.contains("https://example.com/api"))
  }

  test("formatRequest includes redacted URI when redactSecrets is true") {
    val config  = LogConfig(redactSecrets = true, colors = noColors)
    val request = Request[fs2.Pure](Method.GET, Uri.unsafeFromString("https://example.com/api?token=secret"))
    val result  = ClientLogger.formatRequest(request, "", config, debug)
    assert(result.contains("token=<REDACTED>"))
    assert(!result.contains("secret"))
  }

  test("formatRequest includes headers at Debug level") {
    val config  = LogConfig(redactSecrets = false, colors = noColors)
    val request = Request[fs2.Pure](Method.GET, uri"https://example.com/api")
      .putHeaders(Header.Raw(CIString("X-Request-Id"), "123"))
    val result = ClientLogger.formatRequest(request, "", config, debug)
    assert(result.contains("123"))
  }

  test("formatRequest includes body at Trace level") {
    val config  = LogConfig(redactSecrets = false, colors = noColors)
    val request = Request[fs2.Pure](Method.POST, uri"https://example.com/api")
    val result  = ClientLogger.formatRequest(request, """{"data":"value"}""", config, trace)
    assert(result.contains("data"))
    assert(result.contains("value"))
  }

  test("formatRequest does not include body below Trace level") {
    val config  = LogConfig(redactSecrets = false, colors = noColors)
    val request = Request[fs2.Pure](Method.POST, uri"https://example.com/api")
    val result  = ClientLogger.formatRequest(request, """{"data":"value"}""", config, debug)
    assert(!result.contains("data"))
  }

  // --- formatResponse ---

  test("formatResponse includes status and version at Debug level") {
    val config   = LogConfig(redactSecrets = false, colors = noColors)
    val response = Response[fs2.Pure](Status.Ok)
    val result   = ClientLogger.formatResponse(response, "", config, debug)
    assert(result.contains("HTTP/1.1"))
    assert(result.contains("200"))
  }

  test("formatResponse includes headers at Debug level") {
    val config   = LogConfig(redactSecrets = false, colors = noColors)
    val response = Response[fs2.Pure](Status.Ok)
      .putHeaders(Header.Raw(CIString("X-Response-Id"), "abc"))
    val result = ClientLogger.formatResponse(response, "", config, debug)
    assert(result.contains("abc"))
  }

  test("formatResponse includes body at Trace level") {
    val config   = LogConfig(redactSecrets = false, colors = noColors)
    val response = Response[fs2.Pure](Status.Ok)
    val result   = ClientLogger.formatResponse(response, "response-body", config, trace)
    assert(result.contains("response-body"))
  }

  test("formatResponse redacts sensitive headers when redactSecrets is true") {
    val config   = LogConfig(redactSecrets = true, colors = noColors)
    val response = Response[fs2.Pure](Status.Ok)
      .putHeaders(Header.Raw(CIString("Set-Cookie"), "session=abc123"))
    val result = ClientLogger.formatResponse(response, "", config, debug)
    assert(result.contains("<REDACTED>"))
    assert(!result.contains("abc123"))
  }
}
