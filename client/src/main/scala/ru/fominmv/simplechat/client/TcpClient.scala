package ru.fominmv.simplechat.client


import scala.concurrent.duration.{FiniteDuration, DurationInt}
import scala.collection.mutable.{HashSet, HashMap}
import scala.util.Random

import java.io.{
    IOException,
    InterruptedIOException,
    InputStream,
    OutputStream,
}
import java.net.{InetAddress, Socket, InetSocketAddress}

import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.protocol.error.ProtocolException
import ru.fominmv.simplechat.core.protocol.Status.*
import ru.fominmv.simplechat.core.protocol.{
    Protocol,
    Status,
    Response,

    ClientPacket,
    ClientCommand,
    PingClientCommand,
    CloseClientCommand,
    SendMessageClientCommand,
    SetNameClientCommand,

    ServerPacket,
    ServerCommand,
    PingServerCommand,
    CloseServerCommand,
    SendMessageServerCommand,
}
import ru.fominmv.simplechat.core.protocol.ServerPacket
import ru.fominmv.simplechat.core.util.StringExtension.escape
import ru.fominmv.simplechat.core.util.ThreadUtil
import ru.fominmv.simplechat.core.util.UnsignedUtil.*
import ru.fominmv.simplechat.core.Message
import ru.fominmv.simplechat.client.event.{ConcurentEventListener, CascadeEventListener}
import ru.fominmv.simplechat.client.State.*


class TcpClient private (
                 initName:           Option[String],
             val address:            InetAddress,
             val port:               Int,
             val pingInterval:       FiniteDuration,
             val maxPendingCommands: Int,
    override val protocol:           Protocol,
    override val eventListener:      CascadeEventListener,
) extends Client:
    override def state: State =
        _state

    override def name: Option[String] =
        _name

    override def name_=(name: String): Unit =
        ClosedException.checkOpen(this, "Client is closed")

        try
            sendSetNameCommand(makeNextSetNameCommandCode(name), name)
        catch
            case e: Exception => onException(e)

    override def closed: Boolean =
        state == State.CLOSING ||
        state == State.CLOSED

    override def sendMessageToServer(text: String): Unit =
        ClosedException.checkOpen(this, "Client is closed")

        try
            sendSendMessageCommand(makeNextSendMessageCommandCode(text), text)
        catch
            case e: Exception => onException(e)

    override def pingServer: Unit =
        ClosedException.checkOpen(this, "Client is closed")
        
        try
            sendPingCommand(makeNextCommandCode)
        catch
            case e: Exception => onException(e)

    override def close: Unit =
        if closeGenerally(true) then
            concurentEventListener.onDisconnected

    override def open: Unit =
        _state synchronized {
            ClosedException.checkOpen(this, "Client is closed")
            concurentEventListener.onPreOpen
            _state = OPENING
        }

        connect
        startPingingThreadIfNeeded
        startPacketReceivingThread
        waitThreadsStarted

        _state = OPEN

        concurentEventListener.onPostOpen


    @volatile
    private var _name                          = initName

    @volatile
    private var _state                         = NEW

    private val concurentEventListener         = ConcurentEventListener(eventListener)
    private val socket                         = Socket()
    private val sendMessagePendingCommandCodes = HashMap[Short, String]()
    private val nameSetPendingCommandCodes     = HashMap[Short, String]()
    private val pendingCommandCodes            = if maxPendingCommands != 0 then
        HashSet[Short]()
    else
        null
    private val pingingThread                  = if pingInterval != 0.seconds then
        Thread(
            () => pingingThreadBody,
            "Pinger",
        )
    else
        null
    private val packageReceivingThread         = Thread(
        () => packageReceivingThreadBody,
        "Package Receiver",
    )


    if !((0 to USHORT_MAX) contains port) then
        throw IllegalArgumentException("port must be in range [0, 65535]")

    if pingInterval < 0.seconds then
        throw IllegalArgumentException("pingInterval must be non-negative")

    if maxPendingCommands < 0 then
        throw IllegalArgumentException("maxPendingCommands must be non-negative")

    private def makeNextSendMessageCommandCode(text: String): Short =
        checkPendingCommandCount

        val code = Random.nextInt.toShort

        sendMessagePendingCommandCodes addOne (code, text)

        code

    private def makeNextSetNameCommandCode(name: String): Short =
        checkPendingCommandCount

        val code = Random.nextInt.toShort

        nameSetPendingCommandCodes addOne (code, name)

        code

    private def makeNextCommandCode: Short =
        checkPendingCommandCount

        val code = Random.nextInt.toShort

        if pendingCommandCodes != null then
            pendingCommandCodes addOne code

        code

    private def checkPendingCommandCount: Unit =
        if totalPendingCommandCount >= maxPendingCommands then
            throw RuntimeException("Pending commands limit is reached")

    private def totalPendingCommandCount: Int =
        if pendingCommandCodes != null then
            sendMessagePendingCommandCodes.size +
            nameSetPendingCommandCodes.size     +
            pendingCommandCodes.size
        else
            0

    private def connect: Unit =
        logger debug "Connecting..."

        socket connect InetSocketAddress(address, port)

        logger debug "Connected"

    private def startPingingThreadIfNeeded: Unit =
        if pingingThread != null then
            logger debug "Starting pinging thread..."
            pingingThread.start

    private def startPacketReceivingThread: Unit =
        logger debug "Starting packet receiving thread..."
        packageReceivingThread.start

    private def waitThreadsStarted: Unit =
        synchronized {
            while
                try
                    wait()
                catch
                    case _: InterruptedException => onInterruptedException

                packageReceivingThread.getState == Thread.State.NEW ||
                pingingThread                   != null             &&
                pingingThread.getState          == Thread.State.NEW
            do ()
        }

    private def pingingThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        while !closed do
            try
                logger debug "Waiting..."
                Thread sleep pingInterval.toMillis
                pingServer
            catch
                case e: Exception => onAnyException(e)

    private def packageReceivingThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        while !closed do
            try
                logger debug "Waiting for packets..."

                val packet = protocol readServerPacket inputStream

                logger debug "Packet received"

                onPacket(packet)
            catch
                case e: Exception => onAnyException(e)

    private def inputStream: InputStream =
        socket.getInputStream

    private def outputStream: OutputStream =
        socket.getOutputStream

    private def onPacket(packet: ServerPacket): Unit =
        packet match
            case Response(code, status) => onResponse(code, status)
            case command: ServerCommand => onCommand(command)

    private def onResponse(code: Short, status: Status): Unit =
        logger debug s"Got response: $code - $status"

        if status == FATAL then
            logger debug "Server responded with fatal error"
            closeGenerally
            concurentEventListener.onFatalError
            return

        val newName = nameSetPendingCommandCodes remove code

        if newName != None then
            if status == OK then
                logger debug "Name set accepted"

                val oldName = _name

                _name = newName

                concurentEventListener.onSetName(newName.get, oldName)
            else
                logger debug "Name set rejected"
                concurentEventListener onNameRejected newName.get

            return

        val text = sendMessagePendingCommandCodes remove code

        if text != None then
            if status == OK then
                logger debug "Message rejected"
            else
                logger debug "Message accepted"
                concurentEventListener onMessageRejected text.get

            return

        if pendingCommandCodes == null || (pendingCommandCodes remove code) then
            logger debug "Response accepted"
            return

        logger debug "Response with such a code wasn't expected"

        throw ProtocolException()

    private def onCommand(command: ServerCommand): Unit =
        command match
            case PingServerCommand(code)                 => onPingCommand(code)
            case CloseServerCommand(code)                => onCloseCommand(code)
            case SendMessageServerCommand(code, message) => onSendMessageCommand(code, message)

    private def onPingCommand(code: Short): Unit =
        logger debug s"Received ping command: $code"

        sendResponse(code, OK)
        
        logger debug "Pong"

    private def onCloseCommand(code: Short): Unit =
        logger debug s"Received disconnect command: $code"

        sendResponse(code, OK)
        closeGenerally

        logger debug "Disconnected"

        concurentEventListener.onDisconnectedByServer

    private def onSendMessageCommand(code: Short, message: Message): Unit =
        logger debug s"Received send message command: $code - $message"
        sendResponse(code, OK)
        concurentEventListener onMessage message

    private def sendResponse(code: Short, status: Status): Unit =
        logger debug s"Sending response: $code - $status"
        sendPacket(Response(code, status))
        logger debug "Response sent"

    private def sendPingCommand(code: Short): Unit =
        logger debug s"Sending ping command: $code..."
        sendCommand(PingClientCommand(code))
        logger debug "Ping command is sent"

    private def sendCloseCommand(code: Short): Unit =
        logger debug s"Sending close command: $code..."
        sendCommand(CloseClientCommand(code))
        logger debug "Close command is sent"

    private def sendSendMessageCommand(code: Short, text: String): Unit =
        logger debug s"Sending send message command: $code - \"${text.escape}\"..."
        sendCommand(SendMessageClientCommand(code, text))
        logger debug "Send message command is sent"

    private def sendSetNameCommand(code: Short, name: String): Unit =
        logger debug s"Sending set name command: $code - \"${name.escape}\"..."
        sendCommand(SetNameClientCommand(code, name))
        nameSetPendingCommandCodes addOne (code, name)
        logger debug "Set name command is sent"

    private def sendCommand(command: ClientCommand): Unit =
        sendPacket(command)

    private def sendPacket(packet: ClientPacket): Unit =
        outputStream synchronized {
            protocol.writePacket(packet, outputStream)
        }

    private def onAnyException(exception: Exception): Unit =
        exception match
            case ie:   InterruptedException   => onInterruptedException(ie)
            case iioe: InterruptedIOException => onInterruptedIOException(iioe)
            case ce:   ClosedException        => onClosedException(ce)
            case ioe:  IOException            => onIOException(ioe)
            case e:    Exception              => onException(e)

    private def onInterruptedException(exception: InterruptedException): Unit =
        logger debug "Aborted: interrupted"
        Thread.interrupted

    private def onInterruptedIOException(exception: InterruptedIOException): Unit =
        logger debug "Aborted: I/O interrupted"
        Thread.interrupted

    private def onClosedException(exception: ClosedException): Unit =
        logger debug s"Aborted: ${exception.getMessage}"

    private def onIOException(exception: IOException): Unit =
        if closed then
            logger debug "Aborted: socket is closed"
        else
            onException(exception)

    private def onException(exception: Exception): Unit =
        logger error exception
        closeGenerally(true)
        concurentEventListener.onConnectionLost

    private def closeGenerally: Boolean =
        closeGenerally()

    private def closeGenerally(doSendCloseCommand: Boolean = false): Boolean =
        _state synchronized {
            if closed then
                return false

            concurentEventListener.onPreClose

            _state = CLOSING
        }

        logger debug "Closing..."

        if doSendCloseCommand then
            try
                sendCloseCommand(0)
            catch
                case e: Exception => logger error e

        closeSocket
        stopPingingThreadIfNeeded
        stopPacketReceivingThread

        nameSetPendingCommandCodes.clear

        if pendingCommandCodes != null then
            pendingCommandCodes.clear

        logger debug "Closed"

        _state = CLOSED

        concurentEventListener.onPostClose

        true

    private def closeSocket: Unit =
        logger debug "Closing socket..."

        try
            socket.close
        catch
            case e: Exception => logger error e

        logger debug "Socket is closed"

    private def stopPingingThreadIfNeeded: Unit =
        ThreadUtil.stop(pingingThread, Some(logger))

    private def stopPacketReceivingThread: Unit =
        ThreadUtil.stop(packageReceivingThread, Some(logger))

object TcpClient:
    def apply(
        name:               Option[String]       = Config.DEFAULT_NAME,
        address:            InetAddress          = Config.DEFAULT_ADDRESS,
        port:               Int                  = Config.DEFAULT_PORT,
        pingInterval:       FiniteDuration       = Config.DEFAULT_PING_INTERVAL,
        maxPendingCommands: Int                  = Config.DEFAULT_MAX_PENDING_COMMANDS,
        protocol:           Protocol             = Config.DEFAULT_PROTOCOL,
        eventListener:      CascadeEventListener = CascadeEventListener(),
    ): TcpClient =
        new TcpClient(
            initName           = name,
            address            = address,
            port               = port,
            pingInterval       = pingInterval,
            maxPendingCommands = maxPendingCommands,
            protocol           = protocol,
            eventListener      = eventListener,
        )


private val logger = LogManager getLogger classOf[TcpClient]