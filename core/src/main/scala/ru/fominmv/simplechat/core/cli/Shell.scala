package ru.fominmv.simplechat.core.cli


import scala.io.AnsiColor.*

import java.io.EOFException

import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.util.Closeable


trait Shell extends Closeable:
    @throws[ClosedException]("When closed")
    def run: Unit =
        ClosedException.checkOpen(this, "Shell is closed")

        greeting

        while !closed do
            try {
                val input = console read prompt

                try
                    if input.trim startsWith commandPrefix then
                        onCommandInput(input)
                    else
                        onNonCommandInput(input)
                catch
                    case e: Exception => console print s"$RED${e.getMessage}$RESET"
            } catch
                case _: InterruptedException => onInterruptedException
                case _: EOFException         => onEOFException

        farewell

    def console: Console = Console

    def commandPrefix: String = "/"

    def prompt: String = "> "

    def commands: Set[Command]


    protected val HELP_COMMAND = Command(
        name        = "help",
        description = Some("Prints help message"),
        action      = _ => printHelp
    )

    protected val CLOSE_COMMAND = Command(
        name        = "close",
        description = Some("Closes shell"),
        action      = _ => close
    )


    protected def greeting: Unit =
        if commands contains CLOSE_COMMAND then
            console print s"To quit enter /${CLOSE_COMMAND.name} or press Ctrl-C"
        else
            console print "To quit press Ctrl-C"

        if commands contains HELP_COMMAND then
            console print s"Too see full command list enter /${HELP_COMMAND.name}"

    protected def farewell: Unit =
        console print "Bye!"

    protected def onInterruptedException: Unit =
        close

    protected def onEOFException: Unit =
        close

    protected def onNonCommandInput(input: String): Unit = ()

    @throws[IllegalArgumentException]("On bad input")
    protected def onCommandInput(input: String): Unit =
        val commandPrefixPos = input indexOf   commandPrefix
        val commandString    = input substring (commandPrefixPos + commandPrefix.length)
        val args             = commandString split "\\s+"
        val commandName      = args(0)
        val lowerCommandName = commandName.toLowerCase
        val commandOption    = commands find (_.name.toLowerCase == lowerCommandName)

        if commandOption == None then
            throw IllegalArgumentException(s"Command /$commandName not found")

        val command = commandOption.get

        if command.args.length != args.length - 1 then
            throw IllegalArgumentException(s"Command /$commandName takes ${command.args.length} argument(s) not ${args.length - 1}")

        command action args.drop(1).toList
    
    protected def printHelp: Unit =
        printHelp()
    
    protected def printHelp(
        usage: String = "Usage:\n"                            +
                        "    /<command> - executes command\n" +
                        "    <message>  - send message\n"     +
                        "\n"                                  +
                        "Commands:\n"
    ): Unit =
        if !usage.isBlank then
            usage.lines forEach (console print _)

        for command <- commands do
            val usage             = (commandUsageMap get command.name).get
            val descriptionIndent = " " * (commandDescriptionIndent - usage.length + 1)
            val description       = command.description getOrElse ""
            val message           = s"    $usage$descriptionIndent$description"

            console print message
    

    private lazy val commandDescriptionIndent: Int =
        commandUsageMap.map(_._2.length).max

    private lazy val commandUsageMap: Map[String, String] =
        val builder = Map.newBuilder[String, String]

        for command <- commands do
            builder addOne (command.name, formatCommandUsage(command))

        builder.result

    
    private def formatCommandUsage(command: Command): String =
        if command.args.isEmpty then
            s"${command.name}"
        else
            s"${command.name} ${command.args.map("<" + _ + ">").mkString(" ")}"