package ru.fominmv.simplechat.core.cli


import scala.annotation.targetName

import java.io.{EOFException, IOError}

import org.jline.terminal.Terminal.Signal
import org.jline.terminal.TerminalBuilder
import org.jline.reader.{
    LineReader,
    LineReaderBuilder,
    EndOfFileException,
    UserInterruptException,
}

import ru.fominmv.simplechat.core.cli.error.ConsoleInterruptedException
import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.util.lifecycle.Closeable
import ru.fominmv.simplechat.core.util.RuntimeUtil.{
    addShutdownHook,
    removeShutdownHook,
}


trait Console extends Closeable:
    @throws[ConsoleInterruptedException]("When input process is interrupted (if user pressed Ctrl-C for example)")
    @throws[EOFException]("When EOF was reached (or user pressed Ctrl-D for example)")
    @throws[IOError]("When some other I/O error occurred")
    @throws[ClosedException]("When closed")
    def read: String =
        read()

    @throws[ConsoleInterruptedException]("When input process is interrupted (if user pressed Ctrl-C for example)")
    @throws[EOFException]("When EOF was reached (or user pressed Ctrl-D for example)")
    @throws[IOError]("When some other I/O error occurred")
    @throws[ClosedException]("When closed")
    def read(prompt: String = "", buffer: String = ""): String

    @throws[ClosedException]("When closed")
    def print(text: String = ""): Unit

    def interrupt: Unit

    def showInput: Boolean
    def showInput_=(value: Boolean): Unit

object Console extends Console:
    // For Java interoperability
    def instance: Console = this

    override def read(prompt: String = "", buffer: String = ""): String =
        ClosedException.checkOpen(this, "Console is closed")

        try
            lineReader.readLine(prompt, null, buffer)
        catch
            case uie: UserInterruptException => throw ConsoleInterruptedException(uie.getPartialLine)
            case _: EndOfFileException       => throw EOFException()

    override def print(text: String = ""): Unit =
        ClosedException.checkOpen(this, "Console is closed")
        lineReader printAbove text

    override def interrupt: Unit =
        terminal raise Signal.INT

    override def showInput: Boolean =
        !(lineReader isSet LineReader.Option.ERASE_LINE_ON_FINISH)

    override def showInput_=(value: Boolean): Unit =
        lineReader.option(LineReader.Option.ERASE_LINE_ON_FINISH, !value)

    override def closed: Boolean =
        !open

    override def close: Unit =
        terminal.close
        open = false

    def enableShutdownHook: Boolean = enableShutdownHookValue

    def enableShutdownHook_=(enableShutdownHookValue: Boolean): Unit =
        if enableShutdownHookValue != this.enableShutdownHookValue then
            if enableShutdownHookValue then
                addShutdownHook(shutdownHook)
            else
                removeShutdownHook(shutdownHook)

        this.enableShutdownHookValue = enableShutdownHookValue


    private var open                    = true
    private var enableShutdownHookValue = true
    private val terminal                = TerminalBuilder
        .builder
        .build
    private val lineReader              = LineReaderBuilder
        .builder
        .terminal(terminal)
        .build


    if enableShutdownHookValue then
        addShutdownHook(shutdownHook)


    private def shutdownHook(): Unit =
        close