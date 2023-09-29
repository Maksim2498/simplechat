package ru.fominmv.simplechat.server.event


import ru.fominmv.simplechat.core.Message

import ru.fominmv.simplechat.server.{Client, Server}


case class BroadcastEventListener(val server: Server) extends EventListener:
    override def onPreClose: Unit =
        server broadcastMessage "Closing..."

    override def onConnected(client: Client): Unit =
        server.broadcastMessage(
            s"${client.fullname} connected from ${client.address}",
            Set(client.id),
        )

    override def onSetName(client: Client, oldName: Option[String]): Unit =
        server.broadcastMessage(
            s"${Client.fullname(client.id, oldName)} set his/her name to ${client.name.get}",
            Set(client.id),
        )

    override def onMessage(client: Client, text: String): Unit =
        server.broadcastMessage(
            Message(client.fullname, text),
            Set(client.id),
        )

    override def onDisconnected(client: Client): Unit =
        server.broadcastMessage(
            s"${client.fullname} disconnected",
            Set(client.id),
        )

    override def onConnectionLost(client: Client): Unit =
        server.broadcastMessage(
            s"Lost connection with ${client.fullname}",
            Set(client.id),
        )