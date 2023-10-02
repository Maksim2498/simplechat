package ru.fominmv.simplechat.client


import ru.fominmv.simplechat.core.cli.{
    Command,
    Shell => ShellTrait,
}
import ru.fominmv.simplechat.client.event.EventListener


class Shell(val client: Client) extends ShellTrait {
    override def closed: Boolean =
        !open

    override def close: Unit =
        client.close
        open = false
        console.interrupt

    override def commands: Set[Command] =
        COMMANDS

    override def prompt: String =
        s"${client.name getOrElse ""}> "
    

    override protected def onNonCommandInput(input: String): Unit =
        if input.isBlank then
            return

        client sendMessageToServer input
        console print s"${client.name.get}: $input"
    

    private var open = true

    private val SET_NAME_COMMAND = Command(
        name        = "set-name",
        description = Some("Sets name"),
        args        = List("name"),
        action      = args =>
            client.name = args(0)
    )

    private val COMMANDS = Set(
        HELP_COMMAND,
        CLOSE_COMMAND,
        SET_NAME_COMMAND,
    )


    client.eventListener.eventListeners addOne new EventListener:
        override def onPostClose: Unit =
            close
}
