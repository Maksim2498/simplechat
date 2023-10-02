package ru.fominmv.simplechat.server


import scala.concurrent.duration.FiniteDuration

import ru.fominmv.simplechat.core.protocol.text.TextProtocol
import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.{NameValidator, DefaultNameValidator}
import ru.fominmv.simplechat.server.event.{
    CascadeEventListener,
    BroadcastEventListener,
    LogEventListener,
}
import ru.fominmv.simplechat.server.Config.*


class ServerBuilder(
    var broadcastMessages:  Boolean        = true,
    var logMessages:        Boolean        = true,
    var port:               Int            = DEFAULT_PORT,
    var backlog:            Int            = DEFAULT_BACKLOG,
    var maxPendingCommands: Int            = DEFAULT_MAX_PENDING_COMMANDS,
    var name:               String         = DEFAULT_NAME,
    var pingInterval:       FiniteDuration = DEFAULT_PING_INTERVAL,
    var nameValidator:      NameValidator  = DefaultNameValidator,
    var protocol:           Protocol       = DEFAULT_PROTOCOL,
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

        if broadcastMessages then
            eventListeners addOne BroadcastEventListener(server)

        if logMessages then
            eventListeners addOne LogEventListener

        server

