package ru.fominmv.simplechat.server.event


import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.server.Client


object LogEventListener extends EventListener:
    override def on(event: Event): Unit =
        event match
            case PreOpenEvent() => 
                logger info "Opening server..."

            case PostOpenEvent() =>
                logger info "Opened"

            case PreCloseEvent() =>
                logger info "Closing server..."

            case PostCloseEvent() =>
                logger info "Closed"

            case PrePingEvent(except) =>

            case PostPingEvent(except) =>

            case PreBroadcastMessageEvent(message, except) =>

            case PostBroadcastMessageEvent(message, except) =>
                logger info s"${message.author}: ${message.text}"

            case ConnectedEvent(client) =>
                logger info s"${client.fullname} connected from ${client.address}"

            case PingedEvent(client) =>

            case SetNameEvent(client, oldName) =>
                logger info s"${Client.fullname(client.id, oldName)} set his/her name to ${client.name.get}"

            case MessageReceivedEvent(client, text) =>
                logger info s"Received message from ${client.fullname}: $text"

            case DisconnectedByServerEvent(client) =>
                logger info s"${client.fullname} was disconnected by server"

            case DisconnectedEvent(client) =>
                logger info s"${client.fullname} disconnected"

            case ConnectionLostEvent(client) =>
                logger info s"Lost connection with ${client.fullname}"

            case FatalErrorEvent(client) =>
                logger error s"${client.fullname} reponded with fatal error"
        

    private val logger = LogManager getLogger getClass
