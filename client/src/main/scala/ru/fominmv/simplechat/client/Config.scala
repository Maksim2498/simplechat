package ru.fominmv.simplechat.client

import scala.concurrent.duration.{FiniteDuration, DurationInt}

import java.net.InetAddress

import ru.fominmv.simplechat.core.protocol.text.TextProtocol
import ru.fominmv.simplechat.core.protocol.Protocol


case class Config(
    val run:                Boolean        = Config.DEFAULT_RUN,
    val debug:              Boolean        = Config.DEFAULT_DEBUG,
    val port:               Int            = Config.DEFAULT_PORT,
    val address:            InetAddress    = Config.DEFAULT_ADDRESS,
    val maxPendingCommands: Int            = Config.DEFAULT_MAX_PENDING_COMMANDS,
    val logMessages:        Boolean        = Config.DEFAULT_LOG_MESSAGES,
    val name:               Option[String] = Config.DEFAULT_NAME,
    val protocol:           Protocol       = Config.DEFAULT_PROTOCOL,
    val pingInterval:       FiniteDuration = Config.DEFAULT_PING_INTERVAL,
)

object Config:
    val DEFAULT_RUN:                  Boolean        = true
    val DEFAULT_DEBUG:                Boolean        = false
    val DEFAULT_ADDRESS:              InetAddress    = InetAddress.getLocalHost
    val DEFAULT_PORT:                 Int            = 24982
    val DEFAULT_MAX_PENDING_COMMANDS: Int            = 50
    val DEFAULT_LOG_MESSAGES:         Boolean        = true
    val DEFAULT_NAME:                 Option[String] = None
    val DEFAULT_PROTOCOL:             Protocol       = TextProtocol()
    val DEFAULT_PING_INTERVAL:        FiniteDuration = 10.seconds
