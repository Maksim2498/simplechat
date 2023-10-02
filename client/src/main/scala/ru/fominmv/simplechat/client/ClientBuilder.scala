package ru.fominmv.simplechat.client

import scala.concurrent.duration.FiniteDuration

import java.net.InetAddress

import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.client.event.{CascadeEventListener, LogEventListener}


class ClientBuilder(
    val address:            InetAddress    = Config.DEFAULT_ADDRESS,
    val port:               Int            = Config.DEFAULT_PORT,
    val name:               Option[String] = Config.DEFAULT_NAME,
    val maxPendingCommands: Int            = Config.DEFAULT_MAX_PENDING_COMMANDS,
    val pingInterval:       FiniteDuration = Config.DEFAULT_PING_INTERVAL,
    val protocol:           Protocol       = Config.DEFAULT_PROTOCOL,
    val logMessages:        Boolean        = Config.DEFAULT_LOG_MESSAGES,
):
    def buildClient: Client =
        val client = TcpClient(
            address            = address,
            port               = port,
            name               = name,
            maxPendingCommands = maxPendingCommands,
            pingInterval       = pingInterval,
            protocol           = protocol,
        )

        val eventListeners = client.eventListener.eventListeners

        if logMessages then
            eventListeners addOne LogEventListener

        client