package ru.fominmv.simplechat.server


import scala.collection.mutable.{HashMap, Stack, HashSet}
import scala.concurrent.duration.{FiniteDuration, DurationInt}
import scala.util.Random

import java.io.{
    InputStream,
    OutputStream,
    IOException,
    InterruptedIOException,
}
import java.net.{ServerSocket, Socket, SocketException, InetAddress}

import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.protocol.Status.*
import ru.fominmv.simplechat.core.protocol.error.{
    ProtocolException,
    UnsupportedClientCommandException,
}
import ru.fominmv.simplechat.core.protocol.{
    Status,
    Response,
    Protocol,
    
    ClientPacket,
    ClientCommand,
    PingClientCommand,
    CloseClientCommand,
    SendMessageClientCommand,
    SetNameClientCommand,
    
    ServerCommand,
    PingServerCommand,
    CloseServerCommand,
    SendMessageServerCommand
}
import ru.fominmv.simplechat.core.util.StringExtension.{escape}
import ru.fominmv.simplechat.core.util.ThreadUtil
import ru.fominmv.simplechat.core.{Message, NameValidator, DefaultNameValidator}
import ru.fominmv.simplechat.server.event.{
    EventListener,
    ConcurentEventListener,
    CascadeEventListener,
}
import ru.fominmv.simplechat.server.Config.*
import ru.fominmv.simplechat.server.State.*
import ru.fominmv.simplechat.server.{Client => ClientTrait}
import ru.fominmv.simplechat.core.protocol.ServerPacket


class TcpServer(
             val port:               Int                  = DEFAULT_PORT,
             val backlog:            Int                  = DEFAULT_BACKLOG,
             val maxPendingCommands: Int                  = DEFAULT_MAX_PENDING_COMMANDS,
             val pingInterval:       FiniteDuration       = DEFAULT_PING_INTERVAL,
             var nameValidator:      NameValidator        = DefaultNameValidator,
    override val name:               String               = DEFAULT_NAME,
    override val protocol:           Protocol             = DEFAULT_PROTOCOL,
    override val eventListener:      CascadeEventListener = CascadeEventListener(),
) extends Server:
    override def state: State =
        _state

    override def clients: Set[ClientTrait] =
        _clients synchronized {
            _clients.values.toSet
        }

    override def open: Unit =
        _state synchronized {
            ClosedException.checkOpen(this, "Server is closed")
            concurentEventListener.onPreOpen
            _state = OPENING
        }

        logger debug "Opening..."

        startConnectionListeningThread
        startPingingThreadIfNeeded
        waitThreadsStarted

        logger debug "Opened"

        _state = OPEN

        concurentEventListener.onPostOpen

    override def close: Unit =
        _state synchronized {
            if closed then
                return

            concurentEventListener.onPreClose

            _state = CLOSING
        }

        logger debug "Closing..."

        closeClients
        closeSocket
        stopConnectionListeningThread
        stopPingingThread

        logger debug "Closed"

        _state = CLOSED

        concurentEventListener.onPostClose


    @volatile
    private var _state                    = NEW

    @volatile
    private var lastClientId              = 0

    private val concurentEventListener    = ConcurentEventListener(eventListener)
    private val socket                    = ServerSocket(port, backlog)
    private val _clients                  = HashMap[Int, this.Client]()
    private val connectionListeningThread = Thread(
        () => connectionListeningThreadBody,
        "Connection Listener",
    )
    private val pingingThread             = if pingInterval != 0.seconds then
        Thread(
            () => pingingThreadBody,
            "Pinger",
        )
    else
        null


    if maxPendingCommands < 0 then
        throw IllegalArgumentException("maxPendingCommands must be non-negative")

    if pingInterval < 0.seconds then
        throw IllegalArgumentException("pingClientsEvery must be non-negative")


    private def startConnectionListeningThread: Unit =
        logger debug "Starting connection listening thread..."
        connectionListeningThread.start

    private def startPingingThreadIfNeeded: Unit =
        if pingingThread != null then
            logger debug "Starting pinging thread..."
            pingingThread.start

    private def waitThreadsStarted: Unit =
        synchronized {
            while
                try
                    wait()
                catch
                    case _: InterruptedException => onInterruptedException

                connectionListeningThread.getState == Thread.State.NEW ||
                pingingThread                      != null             &&
                pingingThread.getState             == Thread.State.NEW
            do ()
        }

    private def connectionListeningThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        while !closed do
            try
                logger debug "Waiting for connections..."

                val clientSocket = socket.accept

                logger debug s"Received connection from ${clientSocket.getInetAddress}"

                if closed then
                    logger debug "Server is closing"
                    logger debug "Closing connection..."
                    clientSocket.close
                    logger debug "Closed"
                    return

                val clientId     = lastClientId

                lastClientId += 1

                val client = new Client(clientId, clientSocket)

                concurentEventListener onConnected client
            catch
                case e: Exception => onAnyException(e)

    private def pingingThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        while !closed do
            try
                logger debug "Waiting..."
                Thread sleep pingInterval.toMillis
                pingClients
            catch
                case e: Exception => onAnyException(e)

    private def closeClients: Unit =
        logger debug "Closing all clients..."

        _clients synchronized {
            for (_, client) <- _clients do
                client closeWithoutNotifying true
        }

        logger debug "All clients are closed"

    private def closeSocket: Unit =
        logger debug "Closing socket..."

        try
            socket.close
        catch
            case e: Exception => logger error e

        logger debug "Socket is closed"

    private def stopConnectionListeningThread: Unit =
        ThreadUtil.stop(connectionListeningThread, Some(logger))

    private def stopPingingThread: Unit =
        if pingingThread != null then
            ThreadUtil.stop(pingingThread, Some(logger))

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

    private def onClosedException(exception: ClosedException): Unit =
        logger debug s"Aborted: ${exception.getMessage}"

    private def onIOException(exception: IOException): Unit =
        if closed then
            logger debug "Aborted: socket is closed"
        else
            onException(exception)

    private def onException(exception: Exception): Unit =
        logger error exception
        close


    class Client protected[server] (
                val id:     Int,
        private val socket: Socket,
                @volatile
                var name:   Option[String] = None,
    ) extends ClientTrait:
        override def closed: Boolean =
            !open

        override def close: Unit =
            closeWithoutNotifying(true)
            concurentEventListener onDisconnectedByServer this

        override def address: InetAddress =
            socket.getInetAddress

        override def server: TcpServer =
            TcpServer.this

        override def sendMessage(message: Message): Unit =
            ClosedException.checkOpen(this, "Client is closed")

            try
                sendSendMessageCommand(makeNextCommandCode, message)
            catch
                case e: Exception => onException(e)

        override def ping: Unit =
            ClosedException.checkOpen(this, "Client is closed")

            try
                sendPingCommand(makeNextCommandCode)
            catch
                case e: Exception => onException(e)


        @volatile
        private var open                  = true

        private val pendingCommandCodes   = if maxPendingCommands != 0 then HashSet[Short]() else null
        private val logger                = LogManager getLogger getClass.getName + s"#$id"
        private val packetReceivingThread = Thread(
            () => packetReceivingThreadBody,
            "Packet Receiver",
        )


        logger debug "Opening..."

        logger debug "Starting packet receiving thread..."
        packetReceivingThread.start

        synchronized {
            while
                try
                    wait()
                catch
                    case _: InterruptedException => onInterruptedException

                packetReceivingThread.getState == Thread.State.NEW
            do ()
        }

        _clients synchronized {
            _clients addOne (id, this)
        }

        logger debug "Opened"
        

        protected[server] def closeWithoutNotifying: Unit =
            closeWithoutNotifying(false)

        protected[server] def closeWithoutNotifying(doSendCloseCommand: Boolean = false): Unit =
            _clients synchronized {
                open synchronized {
                    if closed then
                        return

                    open = false
                }

                logger debug "Closing..."

                if doSendCloseCommand then
                    try
                        sendCloseCommand(0)
                    catch
                        case e: Exception => logger error e

                closeSocket
                stopPacketReceivingThread
                removeFromClients

                logger debug "Closed"
            }


        private def inputStream: InputStream =
            socket.getInputStream

        private def outputStream: OutputStream =
            socket.getOutputStream

        private def makeNextCommandCode: Short =
            if pendingCommandCodes == null then
                return Random.nextInt.toShort

            if pendingCommandCodes.size >= maxPendingCommands then
                throw RuntimeException("Pending commands limit is reached")

            val code = Random.nextInt.toShort

            pendingCommandCodes addOne code

            code

        private def packetReceivingThreadBody: Unit =
            logger debug "Started"

            synchronized {
                notify()
            }

            while open do
                try
                    logger debug "Waiting for packets..."

                    val packet = protocol readClientPacket inputStream

                    logger debug "Packet received"

                    onPacket(packet)
                catch
                    case e: Exception => onAnyException(e)

        private def onPacket(packet: ClientPacket): Unit =
            packet match
                case command: ClientCommand => onCommand(command)
                case Response(code, status) => onResponse(code, status)

        private def onCommand(command: ClientCommand): Unit =
            command match
                case PingClientCommand(code)              => onPingCommand(code)
                case CloseClientCommand(code)             => onCloseCommand(code)
                case SetNameClientCommand(code, name)     => onSetNameCommand(code, name)
                case SendMessageClientCommand(code, text) => onSendMessageCommand(code, text)

        private def onPingCommand(code: Short): Unit =
            logger debug s"Received ping command: $code"

            sendResponse(code, OK)
            
            logger debug "Pong"

        private def onCloseCommand(code: Short): Unit =
            logger debug s"Received close command: $code"

            sendResponse(code, OK)
            closeWithoutNotifying

            logger debug "Disconnected"

            concurentEventListener onDisconnected this

        private def onSetNameCommand(code: Short, name: String): Unit =
            logger debug s"Received set name command: $code - \"${escape(name)}\"..."

            _clients synchronized {
                val status = if nameGood(name) then
                    val oldName = this.name

                    this.name = Some(name)

                    logger debug "The name is accepted"

                    concurentEventListener.onSetName(this, oldName)

                    OK
                else
                    logger debug "The name is bad or occupied"
                    ERROR

                sendResponse(code, status)
            }

        private def nameGood(name: String): Boolean =
            if !(nameValidator nameGood name) then
                return false
            
            val lowerName = name.toLowerCase

            _clients synchronized {
                return !clients.exists(
                    _.name
                     .map(_.toLowerCase)
                     .getOrElse(null) == lowerName
                )
            }

        private def onSendMessageCommand(code: Short, text: String): Unit =
            logger debug s"Received send message command: $code - \"${escape(text)}\""

            val status = if name == None then
                logger debug "The message is discarded: name isn't set"
                ERROR
            else
                logger debug "The message is accepted"
                concurentEventListener.onMessage(this, text)
                OK

            sendResponse(code, status)

        private def onResponse(code: Short, status: Status): Unit =
            logger debug s"Got response: $code - $status"

            if status == FATAL then
                logger debug "Client responded fith fatal error"
                closeWithoutNotifying
                concurentEventListener onFatalError this
                return

            if pendingCommandCodes == null || (pendingCommandCodes remove code) then
                logger debug "Reponse accepted"
                return

            logger debug "Response with such a code wasn't expected"

            throw ProtocolException()

        private def sendResponse(code: Short, status: Status): Unit =
            logger debug s"Sending response: $code - $status"
            sendPacket(Response(code, status))
            logger debug "Reponse sent"

        private def sendPingCommand(code: Short): Unit =
            logger debug s"Sending ping command: $code..."
            sendCommand(PingServerCommand(code))
            logger debug "Ping command is sent"

        private def sendCloseCommand(code: Short): Unit =
            logger debug s"Sending close command: $code..."
            sendCommand(CloseServerCommand(code))
            logger debug "Close command is sent"

        private def sendSendMessageCommand(code: Short, message: Message): Unit =
            logger debug s"Sending send message command: $code - $message..."
            sendCommand(SendMessageServerCommand(code, message))
            logger debug "Send message command is sent"

        private def sendCommand(command: ServerCommand): Unit =
            sendPacket(command)

        private def sendPacket(command: ServerPacket): Unit =
            outputStream synchronized {
                protocol.writePacket(command, outputStream)
            }

        private def closeSocket: Unit =
            logger debug "Closing socket..."

            try
                socket.close
            catch
                case e: Exception => logger error e

            logger debug "Socket is closed"

        private def stopPacketReceivingThread: Unit =
            ThreadUtil.stop(packetReceivingThread, Some(logger))

        private def removeFromClients: Unit =
            logger debug "Removing from client map..."

            _clients synchronized {
                _clients remove id
            }

            logger debug "Removed from client map"

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
            if open then
                onException(exception)
            else
                logger debug "Aborted: socket is closed"

        private def onException(exception: Exception): Unit =
            logger error exception
            closeWithoutNotifying(true)
            concurentEventListener onConnectionLost this


private val logger = LogManager getLogger classOf[TcpServer]