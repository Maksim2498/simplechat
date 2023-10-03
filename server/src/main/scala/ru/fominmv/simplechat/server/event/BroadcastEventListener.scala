package ru.fominmv.simplechat.server.event


import ru.fominmv.simplechat.core.Message

import ru.fominmv.simplechat.server.{Client, Server}


case class BroadcastEventListener(val server: Server) extends EventListener:
    override def on(event: Event): Unit =
        event match
            case PreOpenEvent() => 

            case PostOpenEvent() =>

            case PreCloseEvent() =>
                server broadcastMessage "Closing..."

            case PostCloseEvent() =>

            case PrePingClientsEvent(except) =>

            case PostPingClientsEvent(except) =>

            case PreBroadcastMessageEvent(message, except) =>

            case PostBroadcastMessageEvent(message, except) =>

            case ConnectedEvent(client) =>
                server.broadcastMessage(
                    s"${client.fullname} connected from ${client.address}",
                    Set(client.id),
                )

            case PingEvent(client) =>

            case SetNameEvent(client, oldName) =>
                server.broadcastMessage(
                    s"${Client.fullname(client.id, oldName)} set his/her name to ${client.name.get}",
                )

            case MessageEvent(client, text) =>
                server.broadcastMessage(
                    Message(client.fullname, text),
                )

            case DisconnectedByServerEvent(client) =>
                server.broadcastMessage(
                    s"${client.fullname} was disconnected by server",
                    Set(client.id),
                )

            case DisconnectedEvent(client) =>
                server.broadcastMessage(
                    s"${client.fullname} disconnected",
                    Set(client.id),
                )

            case ConnectionLostEvent(client) =>
                server.broadcastMessage(
                    s"Lost connection with ${client.fullname}",
                    Set(client.id),
                )

            case FatalErrorEvent(client) =>
                server.broadcastMessage(
                    s"${client.fullname} reponded with fatal error",
                    Set(client.id),
                )