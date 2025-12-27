package com.colofabrix.scala.http4s.middleware.betterlogger

import cats.kernel.Order
import cats.syntax.contravariant.*

/**
 * Log levels for the HTTP4s logger middleware.
 */
private[betterlogger] enum LogLevel(val level: Int) extends Ordered[LogLevel] {

  case Trace extends LogLevel(5)
  case Debug extends LogLevel(4)
  case Info  extends LogLevel(3)
  case Warn  extends LogLevel(2)
  case Error extends LogLevel(1)
  case Off   extends LogLevel(0)

  /**
   * Compares this log level to another.
   */
  def compare(that: LogLevel): Int =
    this.level - that.level

}

private[betterlogger] object LogLevel:
  given Order[LogLevel] =
    Order[Int].contramap[LogLevel](_.level)
