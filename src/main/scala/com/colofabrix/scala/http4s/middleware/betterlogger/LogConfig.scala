package com.colofabrix.scala.http4s.middleware.betterlogger

import org.typelevel.ci.CIString
import scala.Console.*

/**
 * Configuration for the HTTP client logger middleware.
 *
 * This case class provides all customization options for the logging behavior,
 * including header redaction, body logging, and output colors.
 *
 * @param redactHeaders whether to redact sensitive headers like Authorization, Cookie, etc.
 *                      When true, sensitive header values are replaced with "<REDACTED>"
 * @param sensitiveHeaders additional header names to redact on top of the built-in set
 *                         (Authorization, Proxy-Authorization, Cookie, Set-Cookie).
 *                         Only effective when `redactHeaders` is `true`.
 * @param colors the color scheme to use for console output
 * @param logRequestBody whether to include request bodies in log output (only at TRACE level)
 * @param logResponseBody whether to include response bodies in log output (only at TRACE level)
 * @param sanitizeBody a function applied to the body string before logging. Use this to redact
 *                     sensitive fields (e.g., passwords, tokens) from request/response bodies.
 *                     Defaults to identity (no transformation). Only applied at TRACE level.
 *
 * @example
 *   {{{
 *   // Default configuration
 *   val config = LogConfig.default
 *
 *   // Custom configuration
 *   val config = LogConfig(
 *     redactHeaders = false,
 *     colors = LogColors.noColors,
 *     logRequestBody = true,
 *     logResponseBody = false
 *   )
 *
 *   // Redact standard headers plus custom ones
 *   val config = LogConfig(
 *     redactHeaders = true,
 *     sensitiveHeaders = Set(CIString("X-Api-Key"), CIString("X-Auth-Token")),
 *   )
 *   }}}
 */
final case class LogConfig(
  redactHeaders: Boolean = true,
  sensitiveHeaders: Set[CIString] = Set.empty,
  colors: LogColors = LogColors.default,
  logRequestBody: Boolean = true,
  logResponseBody: Boolean = true,
  sanitizeBody: String => String = identity,
)

/**
 * Companion object providing default configuration instances.
 */
object LogConfig {

  /**
   * Default logging configuration.
   *
   * Uses default settings: redacts sensitive headers, logs request/response bodies,
   * and uses the default color scheme.
   */
  val default: LogConfig =
    LogConfig()

}

/**
 * Color scheme for log output.
 *
 * Defines ANSI color codes for different parts of the HTTP log message.
 * Use [[LogColors.noColors]] for plain text output without colors.
 *
 * @param httpVersion color for HTTP version (e.g., "HTTP/1.1")
 * @param safeMethod color for safe HTTP methods (GET, HEAD, OPTIONS)
 * @param unsafeMethod color for unsafe HTTP methods (POST, PUT, DELETE, etc.)
 * @param uri color for the request URI
 * @param headers color for the headers section
 * @param body color for the body content
 * @param successStatus color for 1xx, 2xx, and 3xx status codes
 * @param clientErrorStatus color for 4xx status codes
 * @param serverErrorStatus color for 5xx status codes
 * @param reset ANSI reset code to clear formatting
 */
final case class LogColors(
  httpVersion: String = WHITE,
  safeMethod: String = GREEN,
  unsafeMethod: String = YELLOW,
  uri: String = s"$MAGENTA$BOLD",
  headers: String = BLUE,
  body: String = WHITE,
  successStatus: String = GREEN,
  clientErrorStatus: String = YELLOW,
  serverErrorStatus: String = RED,
  reset: String = RESET,
)

/**
 * Companion object providing preset color schemes.
 */
object LogColors {

  /**
   * Default color scheme with ANSI colors for terminal output.
   */
  val default: LogColors =
    LogColors()

  /**
   * Color scheme with no colors (plain text output).
   *
   * Useful for log files or environments that don't support ANSI colors.
   */
  val noColors: LogColors =
    LogColors("", "", "", "", "", "", "", "", "", "")

}
