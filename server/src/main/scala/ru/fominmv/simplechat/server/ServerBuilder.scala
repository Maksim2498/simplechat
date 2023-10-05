package ru.fominmv.simplechat.server


import scala.concurrent.duration.{FiniteDuration, DurationInt}

import java.net.InetAddress

import ru.fominmv.simplechat.core.protocol.text.TextProtocol
import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.{NameValidator, DefaultNameValidator}

import event.{
    CascadeEventListener,
    BroadcastEventListener,
    BufferingEventListener,
    LogEventListener,
    MulticastEventListener,
}
import Config.*


class ServerBuilder(
    var broadcastMessages:  Boolean        = DEFAULT_BROADCAST_MESSAGES,
    var logMessages:        Boolean        = DEFAULT_LOG_MESSAGES,
    var port:               Int            = DEFAULT_PORT,
    var backlog:            Int            = DEFAULT_BACKLOG,
    var maxPendingCommands: Int            = DEFAULT_MAX_PENDING_COMMANDS,
    var name:               String         = DEFAULT_NAME,
    var bufferingDuration:  FiniteDuration = DEFAULT_BUFFERING_DURATION,
    var pingInterval:       FiniteDuration = DEFAULT_PING_INTERVAL,
    var nameValidator:      NameValidator  = DefaultNameValidator,
    var protocol:           Protocol       = DEFAULT_PROTOCOL,
    var doMulticast:        Boolean        = DEFAULT_DO_MULTICAST,
    var multicastAddress:   InetAddress    = DEFAULT_MULTICAST_ADDRESS,
    var multicastPort:      Int            = DEFAULT_MULTICAST_PORT,
):
    def builderServer: Server =
        val server = TcpServer(
            port               = port,
            backlog            = backlog,
            maxPendingCommands = maxPendingCommands,
            pingInterval       = pingInterval,
            nameValidator      = nameValidator,
            name               = name,
            protocol           = protocol,
        )

        val eventListeners = server.eventListener.eventListeners

        if logMessages then
            eventListeners addOne LogEventListener

        if broadcastMessages then
            val broadcastEventListener = BroadcastEventListener(server)

            val eventListener = if bufferingDuration > 0.seconds then
                val bufferingEvnetListener = BufferingEventListener(bufferingDuration)

                bufferingEvnetListener.eventListeners addOne broadcastEventListener

                bufferingEvnetListener
            else
                broadcastEventListener

            eventListeners addOne eventListener

        if doMulticast then
            eventListeners addOne MulticastEventListener(
                address  = multicastAddress,
                port     = multicastPort,
                protocol = server.protocol,
            )

        server

