package ru.fominmv.simplechat.server


import scala.concurrent.duration.*

import ru.fominmv.simplechat.core.protocol.text.TextProtocol
import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.{NameValidator, DefaultNameValidator}
import ru.fominmv.simplechat.server.event.EventListener


case class Config(
    run:                Boolean        = Config.DEFAULT_RUN,
    debug:              Boolean        = Config.DEFAULT_DEBUG,
    port:               Int            = Config.DEFAULT_PORT,
    backlog:            Int            = Config.DEFAULT_BACKLOG,
    maxPendingCommands: Int            = Config.DEFAULT_MAX_PENDING_COMMANDS,
    logMessages:        Boolean        = Config.DEFAULT_LOG_MESSAGES,
    broadcastMessages:  Boolean        = Config.DEFAULT_BROADCAST_MESSAGES,
    bufferingDuration:  FiniteDuration = Config.DEFAULT_BUFFERING_DURATION,
    name:               String         = Config.DEFAULT_NAME,
    protocol:           Protocol       = Config.DEFAULT_PROTOCOL,
    pingInterval:       FiniteDuration = Config.DEFAULT_PING_INTERVAL,
)

object Config:
    val DEFAULT_RUN:                  Boolean        = true
    val DEFAULT_DEBUG:                Boolean        = false
    val DEFAULT_PORT:                 Int            = 24982
    val DEFAULT_BACKLOG:              Int            = 50
    val DEFAULT_MAX_PENDING_COMMANDS: Int            = 50
    val DEFAULT_LOG_MESSAGES:         Boolean        = true
    val DEFAULT_BROADCAST_MESSAGES:   Boolean        = true
    val DEFAULT_BUFFERING_DURATION:   FiniteDuration = 0.seconds
    val DEFAULT_NAME:                 String         = "<Server>"
    val DEFAULT_PROTOCOL:             Protocol       = TextProtocol()
    val DEFAULT_PING_INTERVAL:        FiniteDuration = 10.seconds