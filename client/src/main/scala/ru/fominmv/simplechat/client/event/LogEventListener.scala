package ru.fominmv.simplechat.client.event


import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.core.util.StringExtension.escape
import ru.fominmv.simplechat.core.Message


object LogEventListener extends EventListener:
    override def on(event: Event): Unit =
        event match
            case PreOpenEvent() => 
                logger info "Connecting to server..."

            case PostOpenEvent() =>
                logger info "Connected"

            case PreCloseEvent() =>
                logger info "Closing connection..."

            case PostCloseEvent() =>
                logger info "Connection closed"

            case PingedEvent() =>

            case MessageReceivedEvent(message) =>
                logger info s"${message.author}: ${message.text}"

            case MessageRejectedEvent(message) =>
                logger error s"Server rejected a message: $message"

            case MessageAcceptedEvent(message) =>

            case NameRejectedEvent(newName, oldName) =>
                if oldName == None then
                    logger error s"Server rejected a name set to \"${newName.escape}\""
                else
                    logger error s"Server rejected a name change from \"${oldName.get.escape}\" to \"${newName.escape}\""

            case NameAcceptedEvent(newName, oldName) =>

            case DisconnectedByServerEvent() =>
                logger info "Disconnected by server"

            case FatalServerErrorEvent() =>
                logger error s"Server responded with fatal error"

            case DisconnectedEvent() =>
                logger info "Disconnected"

            case ConnectionLostEvent() =>
                logger info s"Connection lost"

            case PreTrySendMessageEvent(message) =>

            case PostTrySendMessageEvent(message) =>

            case PreTrySetNameEvent(name, oldName) =>

            case PostTrySetNameEvent(name, oldName) =>

            case PrePingingEvent() =>

            case PostPingingEvent() =>
        

    private val logger = LogManager getLogger LogEventListener