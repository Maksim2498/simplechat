package ru.fominmv.simplechat.client


import ru.fominmv.simplechat.core.cli.error.ConsoleInterruptedException
import ru.fominmv.simplechat.core.cli.{
    Command,
    Shell => ShellTrait,
}

import event.{
    Event,
    PostCloseEvent,
    EventListener,
    NameAcceptedEvent,
}


class Shell(val client: Client) extends ShellTrait:
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
    

    override protected def onInput(input: String): Unit =
        if input.isBlank then
            return

        client sendMessageToServer input

    override protected def readInput(buffer: String = ""): String =
        var currentBuffer = buffer

        while true do
            try
                return super.readInput(currentBuffer)
            catch
                case cie: ConsoleInterruptedException =>
                    if updatingName then
                        currentBuffer = cie.partialInput
                        updatingName  = false
                    else
                        throw cie

        throw IllegalStateException("Break through infinite loop")

    private var open         = true
    private var updatingName = false

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


    console.showInput = false

    client.eventListener.eventListeners addOne new EventListener:
        override def on(event: Event): Unit =
            event match
                case PostCloseEvent() =>
                    close

                case NameAcceptedEvent(_, _) =>
                    updatingName = true
                    console.interrupt

                case _ =>