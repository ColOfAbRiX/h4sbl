[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/com.colofabrix.scala/h4sbl_3.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.colofabrix.scala%22%20AND%20a:%22h4sbl_3%22)

# Http4s Better Logger (h4sbl)

**h4sbl** is a Scala 3 library that provides enhanced logging middleware for
[http4s](https://http4s.org/) applications. It offers colorful, configurable, and comprehensive
logging for HTTP operations.

The standard http4s logger is functional but limited. h4sbl provides a cleaner, more informative
logging experience with color-coded output, sensitive header redaction, and log level-aware
verbosity.

## Features

- **Colorful Output** - Color-coded HTTP methods, status codes, and headers for easy visual parsing
- **Configurable** - Full control over colors, header redaction, and body logging
- **Log Level Aware** - Automatically adjusts verbosity based on your logger configuration
- **Header Redaction** - Automatically redacts sensitive headers (Authorization, Cookie, etc.) with support for custom headers
- **URI Redaction** - Query parameter values are automatically redacted when header redaction is enabled
- **Body Capture** - Optionally logs request and response bodies at TRACE level with a sanitization hook
- **Minimal Boilerplate** - Simple API that wraps your existing http4s client

## Installation

Add the following dependencies to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "com.colofabrix.scala" %% "h4sbl"         % "1.1.0",
  "org.http4s"           %% "http4s-client" % <version>,  // Required peer dependency
  "org.typelevel"        %% "cats-effect"   % <version>,  // Required peer dependency
)
```

> **Note:** h4sbl uses `Provided` scope for http4s and Cats Effect, giving you full control over the
versions in your project.

## Quick Start

### Basic Usage

Wrap your http4s client with the logging middleware:

```scala
import cats.effect.*
import com.colofabrix.scala.http4s.middleware.betterlogger.*
import org.http4s.ember.client.EmberClientBuilder

object MyApp extends IOApp.Simple:

  def run: IO[Unit] =
    EmberClientBuilder
      .default[IO]
      .build
      .map(ClientLogger(_))  // Wrap with logging
      .use { client =>
        client.expect[String]("https://httpbin.org/get").flatMap(IO.println)
      }
```

### With Header Redaction Control

Control whether sensitive headers are redacted in logs:

```scala
import com.colofabrix.scala.http4s.middleware.betterlogger.*

// Redact sensitive headers (default behavior)
val safeLoggingClient = ClientLogger(httpClient)

// Show all headers (useful for debugging)
val debugClient = ClientLogger.withConfig(LogConfig(redactHeaders = false))(httpClient)
```

When `redactHeaders = true`, the following are automatically redacted:
- **Headers:** Authorization, Proxy-Authorization, Cookie, Set-Cookie (built-in) plus any
  custom headers specified in `sensitiveHeaders`
- **URI query parameters:** All query string values (e.g. `?token=secret` becomes `?token=<REDACTED>`)

### Full Configuration

For complete control over logging behavior:

```scala
import com.colofabrix.scala.http4s.middleware.betterlogger.*
import org.typelevel.ci.CIString

val config =
  LogConfig(
    redactHeaders = true,
    sensitiveHeaders = Set(CIString("X-Api-Key"), CIString("X-Auth-Token")),
    colors = LogColors.default,
    logRequestBody = true,
    logResponseBody = true,
    sanitizeBody = body => body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\": \"<REDACTED>\""),
  )

val loggingClient = ClientLogger.withConfig(config)(httpClient)
```

### Body Sanitization

When logging bodies at TRACE level, you can provide a `sanitizeBody` function to redact
sensitive data before it appears in logs. The function receives the raw body string and returns
the sanitized version:

```scala
import com.colofabrix.scala.http4s.middleware.betterlogger.*

val config = LogConfig(
  sanitizeBody = body =>
    body
      .replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\": \"<REDACTED>\"")
      .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\": \"<REDACTED>\"")
)

val loggingClient = ClientLogger.withConfig(config)(httpClient)
```

> **Note:** Body sanitization is applied at the string level. For complex formats (JSON, XML, protobuf),
> consider sanitizing upstream at the application layer before the body reaches the logger.

### Disable Colors

For log files or environments that don't support ANSI colors:

```scala
import com.colofabrix.scala.http4s.middleware.betterlogger.*

val config = LogConfig(colors = LogColors.noColors)
val loggingClient = ClientLogger.withConfig(config)(httpClient)
```

## API Reference

### ClientLogger

The main entry point for creating a logging middleware.

| Method                              | Description                                            |
|-------------------------------------|--------------------------------------------------------|
| `apply(client)`                     | Wrap client with default configuration                 |
| `withConfig(config)(client)`        | Wrap client with full configuration                    |

### LogConfig

Configuration case class for logging behavior.

| Parameter          | Type              | Default              | Description                                                    |
|--------------------|-------------------|----------------------|----------------------------------------------------------------|
| `redactHeaders`    | `Boolean`         | `true`               | Redact sensitive headers and URI query params in output        |
| `sensitiveHeaders` | `Set[CIString]`   | `Set.empty`          | Additional header names to redact (on top of built-in set)     |
| `colors`           | `LogColors`       | `LogColors.default`  | Color scheme for console output                                |
| `logRequestBody`   | `Boolean`         | `true`               | Log request bodies (only at TRACE level)                       |
| `logResponseBody`  | `Boolean`         | `true`               | Log response bodies (only at TRACE level)                      |
| `sanitizeBody`     | `String => String`| `identity`           | Transform function applied to body strings before logging      |

### LogColors

Color scheme configuration for log output.

| Parameter            | Default          | Description                              |
|----------------------|------------------|------------------------------------------|
| `httpVersion`        | White            | Color for HTTP version (e.g., HTTP/1.1)  |
| `safeMethod`         | Green            | Color for GET, HEAD, OPTIONS             |
| `unsafeMethod`       | Yellow           | Color for POST, PUT, DELETE, etc.        |
| `uri`                | Magenta + Bold   | Color for request URI                    |
| `headers`            | Blue             | Color for headers section                |
| `body`               | White            | Color for body content                   |
| `successStatus`      | Green            | Color for 1xx, 2xx, 3xx responses        |
| `clientErrorStatus`  | Yellow           | Color for 4xx responses                  |
| `serverErrorStatus`  | Red              | Color for 5xx responses                  |

**Presets:**
- `LogColors.default` - Full ANSI color support
- `LogColors.noColors` - Plain text output

## Log Level Behavior

h4sbl adjusts its output based on the configured log level:

| Log Level | Behavior                                           |
|-----------|----------------------------------------------------|
| TRACE     | Full output: method, URI, headers, and bodies      |
| DEBUG     | Method, URI, and headers (no bodies)               |
| INFO+     | Minimal or no logging                              |

## Advanced Usage

### Custom Color Scheme

Create a custom color scheme for your logs:

```scala
import scala.Console.*

val customColors =
  LogColors(
    httpVersion = CYAN,
    safeMethod = BLUE,
    unsafeMethod = RED,
    uri = s"$WHITE$BOLD",
    headers = YELLOW,
    body = WHITE,
    successStatus = GREEN,
    clientErrorStatus = MAGENTA,
    serverErrorStatus = s"$RED$BOLD",
    reset = RESET,
  )

val config = LogConfig(colors = customColors)
val loggingClient = ClientLogger.withConfig(config)(httpClient)
```

### Integration with Logback

Configure your `logback.xml` to control logging verbosity:

```xml
<configuration>
  <!-- Show full details including bodies -->
  <logger name="com.colofabrix.scala.http4s.middleware.betterlogger" level="TRACE"/>

  <!-- Or just headers, no bodies -->
  <logger name="com.colofabrix.scala.http4s.middleware.betterlogger" level="DEBUG"/>
</configuration>
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

h4sbl is released under the MIT license. See [LICENSE](LICENSE) for details.

## Author

[Fabrizio Colonna](mailto:colofabrix@tin.it)

## See Also

- [http4s](https://http4s.org/) - Typeful, functional HTTP for Scala
- [Cats Effect](https://typelevel.org/cats-effect/) - The pure asynchronous runtime for Scala
- [log4cats](https://typelevel.org/log4cats/) - Logging for Cats Effect
