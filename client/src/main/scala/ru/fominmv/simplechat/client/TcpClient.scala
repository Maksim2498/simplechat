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
import ru.fominmv.simplechat.core.util.lifecycle.LifecyclePhase.*
import ru.fominmv.simplechat.core.util.lifecycle.LifecyclePhase
import ru.fominmv.simplechat.core.util.StringExtension.escape
import ru.fominmv.simplechat.core.util.ThreadUtil
import ru.fominmv.simplechat.core.util.UnsignedUtil.USHORT_MAX
import ru.fominmv.simplechat.core.Message

import event.{ConcurentEventListener, CascadeEventListener}


class TcpClient protected (
                 initName:           Option[String],
             val address:            InetAddress,
             val port:               Int,
             val pingInterval:       FiniteDuration,
             val maxPendingCommands: Int,
    override val protocol:           Protocol,
    override val eventListener:      CascadeEventListener,
) extends Client:
    if !((0 to USHORT_MAX) contains port) then
        throw IllegalArgumentException("<port> must be in range [0, 65535]")

    if pingInterval < 0.seconds then
        throw IllegalArgumentException("<pingInterval> must be non-negative")

    if maxPendingCommands < 0 then
        throw IllegalArgumentException("<maxPendingCommands> must be non-negative")


    override def lifecyclePhase: LifecyclePhase =
        _lifecyclePhase

    override def name: Option[String] =
        _name

    override def name_=(name: String): Unit =
        ClosedException.checkOpen(this, "Client is closed")

        try
            sendSetNameCommand(makeNextSetNameCommandCode(name), name)
        catch
            case e: Exception => onException(e)

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
            concurentEventListener.publishDisconnected

    override def open: Unit =
        try
            _lifecyclePhase synchronized {
                if !canOpen then
                    return

                onPreOpen
            }

            onOpening
            onPostOpen
        catch
            case e: Exception =>
                _lifecyclePhase = CLOSED
                throw e
    

    @volatile
    protected var _name                          = initName

    @volatile
    protected var _lifecyclePhase                = NEW

    protected val logger                         = LogManager getLogger getClass
    protected val concurentEventListener         = ConcurentEventListener(eventListener)
    protected val socket                         = Socket()
    protected val sendMessagePendingCommandCodes = HashMap[Short, String]()
    protected val nameSetPendingCommandCodes     = HashMap[Short, String]()
    protected val pendingCommandCodes            = if maxPendingCommands != 0 then
        HashSet[Short]()
    else
        null
    protected val pingingThread                  = if pingInterval != 0.seconds then
        Thread(
            () => pingingThreadBody,
            "Pinger",
        )
    else
        null
    protected val packetReceivingThread          = Thread(
        () => packetReceivingThreadBody,
        "Packet Receiver",
    )


    protected def makeNextSendMessageCommandCode(text: String): Short =
        checkPendingCommandCount

        val code = Random.nextInt.toShort

        sendMessagePendingCommandCodes addOne (code, text)

        code

    protected def makeNextSetNameCommandCode(name: String): Short =
        checkPendingCommandCount

        val code = Random.nextInt.toShort

        nameSetPendingCommandCodes addOne (code, name)

        code

    protected def makeNextCommandCode: Short =
        checkPendingCommandCount

        val code = Random.nextInt.toShort

        if pendingCommandCodes != null then
            pendingCommandCodes addOne code

        code

    protected def checkPendingCommandCount: Unit =
        if totalPendingCommandCount >= maxPendingCommands then
            throw RuntimeException("Pending commands limit is reached")

    protected def totalPendingCommandCount: Int =
        if pendingCommandCodes != null then
            sendMessagePendingCommandCodes.size +
            nameSetPendingCommandCodes.size     +
            pendingCommandCodes.size
        else
            0

    protected def onPreOpen: Unit =
        logger debug "Opening..."
        concurentEventListener.publishPreOpen
        _lifecyclePhase = OPENING

    protected def onOpening: Unit =
        connect
        startThreads
        waitThreadsStarted

    protected def startThreads: Unit =
        startPingingThreadIfNeeded
        startPacketReceivingThread

    protected def onPostOpen: Unit =
        _lifecyclePhase = OPEN
        concurentEventListener.publishPostOpen
        logger debug "Opened"

        if eventListener.eventListeners.isEmpty then
            logger debug "No event listeners were registered"
        else
            logger debug s"Registered event listeners: ${eventListener.eventListeners map(_.name) mkString ", "}"

        if _name != None then
            name = _name.get

    protected def connect: Unit =
        logger debug "Connecting..."
        socket connect InetSocketAddress(address, port)
        logger debug "Connected"

    protected def startPingingThreadIfNeeded: Unit =
        if pingingThread != null then
            logger debug s"Starting ${pingingThread.getName} thread..."
            pingingThread.start

    protected def startPacketReceivingThread: Unit =
        logger debug s"Starting ${packetReceivingThread.getName} thread..."
        packetReceivingThread.start

    protected def waitThreadsStarted: Unit =
        synchronized {
            while
                try
                    wait()
                catch
                    case _: InterruptedException => onInterruptedException

                threadsStarting
            do ()
        }

    protected def threadsStarting: Boolean =
        packetReceivingThreadStarting ||
        piningThreadStarting

    protected def packetReceivingThreadStarting: Boolean =
        packetReceivingThread.getState == Thread.State.NEW

    protected def piningThreadStarting: Boolean =
        pingingThread          != null &&
        pingingThread.getState == Thread.State.NEW

    protected def pingingThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        while running do
            try
                logger debug "Waiting..."
                Thread sleep pingInterval.toMillis
                pingServer
            catch
                case e: Exception => onAnyException(e)

        assert(closed)

        logger debug "Finished"

    protected def packetReceivingThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        while running do
            try
                logger debug "Waiting for packets..."

                val packet = protocol readServerPacket inputStream

                logger debug "Packet received"

                onPacket(packet)
            catch
                case e: Exception => onAnyException(e)

        assert(closed)

        logger debug "Finished"

    protected def inputStream: InputStream =
        socket.getInputStream

    protected def outputStream: OutputStream =
        socket.getOutputStream

    protected def onPacket(packet: ServerPacket): Unit =
        packet match
            case Response(code, status) => onResponse(code, status)
            case command: ServerCommand => onCommand(command)

    protected def onResponse(code: Short, status: Status): Unit =
        logger debug s"Got response: $code - $status"

        if status == FATAL then
            onFatalResponse
            return

        val newNameOption = nameSetPendingCommandCodes remove code

        if newNameOption != None then
            onNameResponse(status, code, newNameOption.get)
            return

        val textOption = sendMessagePendingCommandCodes remove code

        if textOption != None then
            onMessageResponse(status, code, textOption.get)
            return

        onGeneralResponse(status, code)

    protected def onFatalResponse: Unit =
        logger debug "Server responded with fatal error"
        closeGenerally
        concurentEventListener.publishFatalServerError

    protected def onNameResponse(status: Status, code: Short, newName: String): Unit =
        val oldName = _name

        if status == OK then
            logger debug "Name set accepted"
            _name = Some(newName)
            concurentEventListener.publishNameAccepted(newName, oldName)
        else
            logger debug "Name set rejected"
            concurentEventListener publishNameRejected(newName, oldName)

    protected def onMessageResponse(status: Status, code: Short, text: String): Unit =
        val message = makeMessage(text)

        if status == OK then
            logger debug "Message rejected"
            concurentEventListener publishMessageAccepted message
        else
            logger debug "Message accepted"
            concurentEventListener publishMessageRejected message

    protected def onGeneralResponse(status: Status, code: Short): Unit =
        if pendingCommandCodes == null || (pendingCommandCodes remove code) then
            logger debug "Response accepted"
            return

        logger debug "Response with such a code wasn't expected"

        throw ProtocolException()

    protected def onCommand(command: ServerCommand): Unit =
        command match
            case PingServerCommand(code)                 => onPingCommand(code)
            case CloseServerCommand(code)                => onCloseCommand(code)
            case SendMessageServerCommand(code, message) => onSendMessageCommand(code, message)

    protected def onPingCommand(code: Short): Unit =
        logger debug s"Received ping command: $code"
        sendResponse(code, OK)
        logger debug "Pong"
        concurentEventListener.publishPinged

    protected def onCloseCommand(code: Short): Unit =
        logger debug s"Received disconnect command: $code"
        sendResponse(code, OK)
        closeGenerally
        logger debug "Disconnected"
        concurentEventListener.publishDisconnectedByServer

    protected def onSendMessageCommand(code: Short, message: Message): Unit =
        logger debug s"Received send message command: $code - $message"
        sendResponse(code, OK)
        concurentEventListener publishMessageReceived message

    protected def sendResponse(code: Short, status: Status): Unit =
        logger debug s"Sending response: $code - $status"
        sendPacket(Response(code, status))
        logger debug "Response sent"

    protected def sendPingCommand(code: Short): Unit =
        concurentEventListener.publishPrePinging
        logger debug s"Sending ping command: $code..."
        sendCommand(PingClientCommand(code))
        logger debug "Ping command is sent"
        concurentEventListener.publishPostPinging

    protected def sendCloseCommand(code: Short): Unit =
        logger debug s"Sending close command: $code..."
        sendCommand(CloseClientCommand(code))
        logger debug "Close command is sent"

    protected def sendSendMessageCommand(code: Short, text: String): Unit =
        val message = makeMessage(text)

        concurentEventListener publishPreTrySendMessage message
        logger debug s"Sending send message command: $code - \"${text.escape}\"..."
        sendCommand(SendMessageClientCommand(code, text))
        logger debug "Send message command is sent"
        concurentEventListener publishPostTrySendMessage message

    protected def sendSetNameCommand(code: Short, name: String): Unit =
        concurentEventListener.publishPreTrySetName(name, _name)
        logger debug s"Sending set name command: $code - \"${name.escape}\"..."
        sendCommand(SetNameClientCommand(code, name))
        nameSetPendingCommandCodes addOne (code, name)
        logger debug "Set name command is sent"
        concurentEventListener.publishPostTrySetName(name, _name)

    protected def sendCommand(command: ClientCommand): Unit =
        sendPacket(command)

    protected def sendPacket(packet: ClientPacket): Unit =
        outputStream synchronized {
            protocol.writePacket(packet, outputStream)
        }

    protected def onAnyException(exception: Exception): Unit =
        exception match
            case ie:   InterruptedException   => onInterruptedException(ie)
            case iioe: InterruptedIOException => onInterruptedIOException(iioe)
            case ce:   ClosedException        => onClosedException(ce)
            case ioe:  IOException            => onIOException(ioe)
            case e:    Exception              => onException(e)

    protected def onInterruptedException(exception: InterruptedException): Unit =
        logger debug "Aborted: interrupted"
        Thread.interrupted

    protected def onInterruptedIOException(exception: InterruptedIOException): Unit =
        logger debug "Aborted: I/O interrupted"
        Thread.interrupted

    protected def onClosedException(exception: ClosedException): Unit =
        logger debug s"Aborted: ${exception.getMessage}"

    protected def onIOException(exception: IOException): Unit =
        if closed then
            logger debug "Aborted: socket is closed"
        else
            onException(exception)

    protected def onException(exception: Exception): Unit =
        logger error exception
        closeGenerally(true)
        concurentEventListener.publishConnectionLost

    protected def closeGenerally: Boolean =
        closeGenerally()

    protected def closeGenerally(doSendCloseCommand: Boolean = false): Boolean =
        _lifecyclePhase synchronized {
            if !canClose then
                return false

            onPreClose
        }

        onClosing(doSendCloseCommand)
        onPostClose

        true

    protected def onPreClose: Unit =
        concurentEventListener.publishPreClose
        logger debug "Closing..."
        _lifecyclePhase = CLOSING

    protected def onClosing: Unit =
        onClosing()

    protected def onClosing(doSendCloseCommand: Boolean = false): Unit =
        if doSendCloseCommand then
            trySendCloseCommand

        closeSocket
        stopThreads
        clearCollection

    protected def onPostClose: Unit =
        _lifecyclePhase = CLOSED
        logger debug "Closed"
        concurentEventListener.publishPostClose

    protected def trySendCloseCommand: Unit =
        try
            sendCloseCommand(0)
        catch
            case e: Exception => logger error e

    protected def stopThreads: Unit =
        stopPingingThreadIfNeeded
        stopPacketReceivingThread

    protected def clearCollection: Unit =
        nameSetPendingCommandCodes.clear

        if pendingCommandCodes != null then
            pendingCommandCodes.clear

    protected def closeSocket: Unit =
        logger debug "Closing socket..."

        try
            socket.close
        catch
            case e: Exception => logger error e

        logger debug "Socket is closed"

    protected def stopPingingThreadIfNeeded: Unit =
        if pingingThread != null then
            ThreadUtil.stop(pingingThread, Some(logger))

    protected def stopPacketReceivingThread: Unit =
        ThreadUtil.stop(packetReceivingThread, Some(logger))

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