package ru.fominmv.simplechat.server


import scala.concurrent.duration.*

import java.net.InetAddress

import ru.fominmv.simplechat.core.protocol.text.TextProtocol
import ru.fominmv.simplechat.core.protocol.Protocol

import Config.*


case class Config(
    doRun:              Boolean        = DEFAULT_DO_RUN,
    doDebug:            Boolean        = DEFAULT_DO_DEBUG,
    port:               Int            = DEFAULT_PORT,
    backlog:            Int            = DEFAULT_BACKLOG,
    maxPendingCommands: Int            = DEFAULT_MAX_PENDING_COMMANDS,
    logMessages:        Boolean        = DEFAULT_LOG_MESSAGES,
    broadcastMessages:  Boolean        = DEFAULT_BROADCAST_MESSAGES,
    bufferingDuration:  FiniteDuration = DEFAULT_BUFFERING_DURATION,
    name:               String         = DEFAULT_NAME,
    protocol:           Protocol       = DEFAULT_PROTOCOL,
    pingInterval:       FiniteDuration = DEFAULT_PING_INTERVAL,
    doMulticast:        Boolean        = DEFAULT_DO_MULTICAST,
    multicastAddress:   InetAddress    = DEFAULT_MULTICAST_ADDRESS,
    multicastPort:      Int            = DEFAULT_MULTICAST_PORT,
)

object Config:
    val DEFAULT_DO_RUN:               Boolean        = true
    val DEFAULT_DO_DEBUG:             Boolean        = false
    val DEFAULT_PORT:                 Int            = 24982
    val DEFAULT_BACKLOG:              Int            = 50
    val DEFAULT_MAX_PENDING_COMMANDS: Int            = 50
    val DEFAULT_LOG_MESSAGES:         Boolean        = true
    val DEFAULT_BROADCAST_MESSAGES:   Boolean        = true
    val DEFAULT_BUFFERING_DURATION:   FiniteDuration = 0.seconds
    val DEFAULT_NAME:                 String         = "<Server>"
    val DEFAULT_PROTOCOL:             Protocol       = TextProtocol()
    val DEFAULT_PING_INTERVAL:        FiniteDuration = 10.seconds
    val DEFAULT_DO_MULTICAST:         Boolean        = false
    val DEFAULT_MULTICAST_ADDRESS:    InetAddress    = InetAddress getByName "233.0.0.1"
    val DEFAULT_MULTICAST_PORT:       Int            = 24982