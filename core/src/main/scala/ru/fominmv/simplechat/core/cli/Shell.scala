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


    protected def greeting: Unit =
        console print "Press Ctrl-C to quit"

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