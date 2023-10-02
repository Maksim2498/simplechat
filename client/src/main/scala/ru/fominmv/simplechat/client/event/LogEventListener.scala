package ru.fominmv.simplechat.client.event


import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.core.Message


object LogEventListener extends EventListener:
    override def onPreOpen: Unit =
        logger info "Connecting to server..."

    override def onPostOpen: Unit =
        logger info "Connected"

    override def onPreClose: Unit =
        logger info "Closing connection..."

    override def onPostClose: Unit =
        logger info "Connection closed"

    override def onMessage(message: Message): Unit =
        logger info s"${message.author}: ${message.text}"

    override def onSetName(newName: String, oldName: Option[String]): Unit =
        if oldName == None then
            logger info s"Name set to \"$newName\""
        else
            logger info s"Name changed from \"${oldName.get}\" to \"$newName\""

    override def onNameRejected(name: String): Unit =
        logger error s"Server rejected a name set to \"$name\""

    override def onMessageRejected(text: String): Unit =
        logger error s"Server rejected a message \"$text\""

    override def onDisconnectedByServer: Unit =
        logger info "Disconnected by server"

    override def onDisconnected: Unit =
        logger info "Disconnected"

    override def onConnectionLost: Unit =
        logger info s"Connection lost"

    override def onFatalError: Unit =
        logger error s"Server responded with fatal error"


    private val logger = LogManager getLogger LogEventListener