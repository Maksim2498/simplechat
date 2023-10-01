package ru.fominmv.simplechat.server.event


import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.server.Client


object LogEventListener extends EventListener:
    override def onPreOpen: Unit =
        logger info "Opening server..."

    override def onPostOpen: Unit =
        logger info "Opened"

    override def onPreClose: Unit =
        logger info "Closing server..."

    override def onPostClose: Unit =
        logger info "Closed"

    override def onConnected(client: Client): Unit =
        logger info s"${client.fullname} connected from ${client.address}"

    override def onSetName(client: Client, oldName: Option[String]): Unit =
        logger info s"${Client.fullname(client.id, oldName)} set his/her name to ${client.name.get}"

    override def onMessage(client: Client, text: String): Unit =
        logger info s"${client.fullname}: $text"

    override def onDisconnectedByServer(client: Client): Unit =
        logger info s"${client.fullname} was disconnected by server"

    override def onDisconnected(client: Client): Unit =
        logger info s"${client.fullname} disconnected"

    override def onConnectionLost(client: Client): Unit =
        logger info s"Lost connection with ${client.fullname}"


    private val logger = LogManager getLogger getClass
