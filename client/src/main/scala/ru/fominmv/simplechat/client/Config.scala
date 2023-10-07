package ru.fominmv.simplechat.client

import scala.concurrent.duration.{FiniteDuration, DurationInt}

import java.net.{InetAddress, NetworkInterface}

import ru.fominmv.simplechat.core.protocol.text.TextProtocol
import ru.fominmv.simplechat.core.protocol.Protocol

import Config.*


case class Config(
    val run:                Boolean                  = DEFAULT_RUN,
    val debug:              Boolean                  = DEFAULT_DEBUG,
    val port:               Int                      = DEFAULT_PORT,
    val address:            InetAddress              = DEFAULT_ADDRESS,
    val maxPendingCommands: Int                      = DEFAULT_MAX_PENDING_COMMANDS,
    val logMessages:        Boolean                  = DEFAULT_LOG_MESSAGES,
    val name:               Option[String]           = DEFAULT_NAME,
    val protocol:           Protocol                 = DEFAULT_PROTOCOL,
    val pingInterval:       FiniteDuration           = DEFAULT_PING_INTERVAL,
    val networkInterface:   Option[NetworkInterface] = DEFAULT_NETWORK_INTERFACE,
    val doMulticast:        Boolean                  = DEFAULT_DO_MULTICAST,
    val multicastAddress:   InetAddress              = DEFAULT_MULTICAST_ADDRESS,
    val multicastPort:      Int                      = DEFAULT_MULTICAST_PORT,
)

object Config:
    val DEFAULT_RUN:                  Boolean                  = true
    val DEFAULT_DEBUG:                Boolean                  = false
    val DEFAULT_ADDRESS:              InetAddress              = InetAddress.getLocalHost
    val DEFAULT_PORT:                 Int                      = 24982
    val DEFAULT_MAX_PENDING_COMMANDS: Int                      = 50
    val DEFAULT_LOG_MESSAGES:         Boolean                  = true
    val DEFAULT_NAME:                 Option[String]           = None
    val DEFAULT_PROTOCOL:             Protocol                 = TextProtocol()
    val DEFAULT_PING_INTERVAL:        FiniteDuration           = 10.seconds
    val DEFAULT_NETWORK_INTERFACE:    Option[NetworkInterface] = None
    val DEFAULT_DO_MULTICAST:         Boolean                  = false
    val DEFAULT_MULTICAST_ADDRESS:    InetAddress              = InetAddress getByName "233.0.0.1"
    val DEFAULT_MULTICAST_PORT:       Int                      = 24982
