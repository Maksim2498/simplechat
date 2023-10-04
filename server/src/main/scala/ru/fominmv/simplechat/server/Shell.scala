package ru.fominmv.simplechat.server


import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.cli.{
    Command,
    Shell => ShellTrait,
}

import event.{
    Event,
    EventListener,
    PostCloseEvent,
}


class Shell(val server: Server) extends ShellTrait:
    override def closed: Boolean =
        !open

    override def close: Unit =
        server.close
        open = false
        console.interrupt

    override def commands: Set[Command] =
        COMMANDS


    override protected def onInput(input: String): Unit =
        if input.isBlank then
            return

        if server.closed then
            throw ClosedException("Server is closing")

        server broadcastMessage input


    private var open = true

    private val KICK_COMMAND = Command(
        name        = "kick",
        description = Some("Kicks specified user"),
        args        = List("name or #id"),
        action      = args =>
            val targetClientNameOrId = args(0)
            val clients              = server.clients
            val clientOption         = if targetClientNameOrId startsWith "#" then
                try
                    val targetClientId = Integer parseInt (targetClientNameOrId substring 1)

                    clients find (_.id == targetClientId)
                catch
                    case _: NumberFormatException => throw IllegalArgumentException("Bad client id")
            else
                val lowerTargetClientName = targetClientNameOrId.toLowerCase

                clients find (
                    _.name
                    .map(_.toLowerCase)
                    .getOrElse(null) == lowerTargetClientName
                )

            if clientOption == None then
                throw IllegalArgumentException(s"User ${targetClientNameOrId} not found")

            clientOption.get.close
    )

    private val KICK_ALL_COMMAND = Command(
        name        = "kick-all",
        description = Some("Kicks all the users"),
        action      = _ =>
            server.clients foreach (_.close)
    )

    private val LIST_COMMAND = Command(
        name        = "list",
        description = Some("Prints user list"),
        action      = _ =>
            if server.clients.isEmpty then
                console print "There is no clients"
            else
                console print "Clients:"

                server.clients foreach (client =>
                    console print s"    ${client.fullname}"
                )
    )

    private val COMMANDS = Set(
        CLOSE_COMMAND,
        HELP_COMMAND,
        KICK_COMMAND,
        KICK_ALL_COMMAND,
        LIST_COMMAND,
    )


    console.showInput = false

    server.eventListener.eventListeners addOne new EventListener:
        override def on(event: Event): Unit =
            event match
                case PostCloseEvent() => close
                case _ =>