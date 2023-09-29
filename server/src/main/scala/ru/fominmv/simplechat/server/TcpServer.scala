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
import ru.fominmv.simplechat.server.event.EventListener
import ru.fominmv.simplechat.server.Config.*


class TcpServer(
             val port:               Int            = DEFAULT_PORT,
             val backlog:            Int            = DEFAULT_BACKLOG,
             val maxPendingCommands: Int            = DEFAULT_MAX_PENDING_COMMANDS,
             val pingInterval:       FiniteDuration = DEFAULT_PING_INTERVAL,
             var nameValidator:      NameValidator  = DefaultNameValidator,
    override val name:               String         = DEFAULT_NAME,
    override val protocol:           Protocol       = DEFAULT_PROTOCOL,
    override val eventListener:      EventListener  = new EventListener {},
) extends Server:
    override def clients: List[Client] =
        clientMap.values.toList

    override def broadcastMessage(message: Message, except: Set[Int] = Set()): Unit =
        ClosedException.checkOpen(this, "Server is closed")

        for
            (id, client) <- clientMap
            if !(except contains id)
        do
            client sendMessage message

    override def closed: Boolean =
        !open

    override def close: Unit =
        open synchronized {
            if closed then
                return

            eventListener.onPreClose

            open = false
        }

        logger debug "Closing..."

        closeClients
        closeSocket
        stopConnectionAccpetingThread

        logger debug "Closed"

        eventListener.onPostClose


    @volatile
    private var open                      = true

    @volatile
    private var lastClientId              = 0

    private val socket                    = ServerSocket(port, backlog)
    private val clientMap                 = HashMap[Int, TcpServerClient]()
    private val connectionAcceptingThread = Thread(
        () => connectionAcceptingThreadBody,
        "Connection Listener"
    )


    if maxPendingCommands < 0 then
        throw IllegalArgumentException("maxPendingCommands must be non-negative")

    if pingInterval < 0.seconds then
        throw IllegalArgumentException("pingClientsEvery must be non-negative")

    eventListener synchronized {
        eventListener.onPreOpen
    }

    logger debug "Opening..."

    logger debug "Starting connection listening thread..."
    connectionAcceptingThread.start

    synchronized {
        while
            try
                wait()
            catch
                case _: InterruptedException => onInterrupted

            connectionAcceptingThread.getState == Thread.State.NEW
        do ()
    }

    logger debug "Opened"

    eventListener synchronized {
        eventListener.onPostOpen
    }


    private def connectionAcceptingThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        while open do
            try
                logger debug "Waiting for connections..."

                val clientSocket = socket.accept
                val clientId     = lastClientId

                lastClientId += 1

                logger debug s"Received connection from ${clientSocket.getInetAddress}"

                val client = TcpServerClient(clientId, clientSocket)

                eventListener synchronized {
                    eventListener onConnected client
                }
            catch
                case e: SocketException        =>
                    if open then
                        onException(e)

                case ioe: IOException          => logger error ioe
                case _:   InterruptedException => onInterrupted
                case e:   Exception            => onException(e)

    private def onInterrupted: Unit =
        logger debug "Interrupted"
        Thread.interrupted

    private def onException(exception: Exception): Unit =
        logger error exception
        close

    private def closeClients: Unit =
        logger debug "Closing all clients..."

        clientMap synchronized {
            clientMap foreach (_._2.close)
        }

        logger debug "All clients are closed"

    private def closeSocket: Unit =
        logger debug "Closing socket..."

        try
            socket.close
        catch
            case e: Exception => logger error e

        logger debug "Socket is closed"

    private def stopConnectionAccpetingThread: Unit =
        ThreadUtil.stop(connectionAcceptingThread, Some(logger))


    class TcpServerClient(
        val id:     Int,
        val socket: Socket,
        var name:   Option[String] = None,
    ) extends Client:
        override def closed: Boolean = !open

        override def close: Unit =
            open synchronized {
                if closed then
                    return

                open = false
            }

            logger debug "Closing..."

            sendCloseCommand
            closeSocket
            stopPingingThread
            stopPacketReceivingThread
            removeFromClients

            logger debug "Closed"

        override def address: InetAddress = socket.getInetAddress

        def sendMessage(message: Message): Unit =
            ClosedException.checkOpen(this, "Client is closed")

            logger debug s"Sending message: $message..."

            try
                val code    = makeNextCommandCode
                val command = SendMessageServerCommand(code, message)

                sendCommand(command)

                logger debug "The message is sent"
            catch
                case e: Exception => onException(e)

        def ping: Unit =
            ClosedException.checkOpen(this, "Client is closed")

            logger debug s"Pinging..."

            try
                val code    = makeNextCommandCode
                val command = PingServerCommand(code)

                sendCommand(command)

                logger debug "Pinged"
            catch
                case e: Exception => onException(e)


        @volatile
        private var open                  = true

        private val pendingCommandCodes   = if maxPendingCommands != 0 then HashSet[Short]() else null
        private val logger                = LogManager getLogger getClass.getName + s"#$id"
        private val pingingThread         = Thread(
            () => pingingThreadBody,
            "Pinger",
        )
        private val packetReceivingThread = Thread(
            () => packetReceivingThreadBody,
            "Packet Receiver",
        )


        logger debug "Opening..."

        logger debug "Starting packet receiving thread..."
        packetReceivingThread.start

        if isPinginging then
            logger debug "Starting pinging thead..."
            pingingThread.start

        synchronized {
            while
                try
                    wait()
                catch
                    case _: InterruptedException => onInterrupted

                packetReceivingThread.getState == Thread.State.NEW ||
                isPinginging                                       &&
                pingingThread.getState         == Thread.State.NEW
            do ()
        }

        clientMap synchronized {
            clientMap addOne (id, this)
        }

        logger debug "Opened"


        private def isPinginging: Boolean =
            pingInterval.toMillis != 0

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

        private def pingingThreadBody: Unit =
            logger debug "Started"

            synchronized {
                notify()
            }

            while open do
                try
                    logger debug "Waiting..."
                    Thread sleep pingInterval.toMillis
                    ping
                catch
                    case _:   InterruptedIOException => onInterrupted
                    case _:   InterruptedException   => onInterrupted
                    case ioe: IOException            => onIOException(ioe)
                    case e:   Exception              => onException(e)

        private def packetReceivingThreadBody: Unit =
            logger debug "Started"

            synchronized {
                notify()
            }

            while open do
                try
                    logger debug "Waiting for packets..."

                    val packet = protocol readClientPacket inputStream

                    logger debug "Received packet"

                    onPacket(packet)
                catch
                    case _:   InterruptedIOException => onInterrupted
                    case _:   InterruptedException   => onInterrupted
                    case ioe: IOException            => onIOException(ioe)
                    case e:   Exception              => onException(e)

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
            logger debug "Received ping command"

            sendResponse(code, OK)
            
            logger debug "Pong"

        private def onCloseCommand(code: Short): Unit =
            logger debug "Received disconnect command..."

            sendResponse(code, OK)
            close

            logger debug "Disconnected"

            eventListener synchronized {
                eventListener onDisconnected this
            }

        private def onSetNameCommand(code: Short, name: String): Unit =
            logger debug s"Received name set command: \"${escape(name)}\"..."

            val status = if nameGood(name) then
                val oldName = this.name

                this.name = Some(name)

                logger debug "The name is accepted"

                eventListener synchronized {
                    eventListener.onSetName(this, oldName)
                }

                OK
            else
                logger debug "The name is bad or occupied"
                ERROR

            sendResponse(code, status)

        private def nameGood(name: String): Boolean =
            if !nameValidator.nameGood(name) then
                return false
            
            val lowerName = name.toLowerCase

            clientMap synchronized {
                return !clientMap.exists(
                    _._2
                     .name
                     .map(_.toLowerCase)
                     .getOrElse(null) == lowerName
                )
            }

        private def onSendMessageCommand(code: Short, text: String): Unit =
            logger debug s"Received message: \"${escape(text)}\""

            val status = if name == None then
                logger debug "The message is discarded: name isn't set"
                ERROR
            else
                logger debug "The message is accepted"

                eventListener synchronized {
                    eventListener.onMessage(this, text)
                }

                OK

            sendResponse(code, status)

        private def onResponse(code: Short, status: Status): Unit =
            logger debug s"Got response: $code - $status"

            if pendingCommandCodes == null || (pendingCommandCodes remove code) then
                logger debug "Reponse accepted"
            else
                logger error "Response with such a code wasn't expected"
                throw ProtocolException()

        private def sendResponse(code: Short, status: Status): Unit =
            sendResponse(Response(code, status))

        private def sendResponse(response: Response): Unit =
            outputStream synchronized {
                protocol.writePacket(response, outputStream)
            }

        private def sendCloseCommand: Unit =
            sendCommand(CloseServerCommand(0))

        private def sendCommand(command: ServerCommand): Unit =
            logger debug s"Sending command with code ${command.code}..."

            outputStream synchronized {
                protocol.writePacket(command, outputStream)
            }

            logger debug "Command is sent"

        private  def onInterrupted: Unit =
            logger debug "Interrupted"
            Thread.interrupted

        private def onIOException(exception: IOException): Unit =
            if open then
                onException(exception)
            else
                logger debug "Socket is closed"

        private def onException(exception: Exception): Unit =
            logger error exception

            eventListener synchronized {
                eventListener onConnectionLost this
            }

            close

        private def closeSocket: Unit =
            logger debug "Closing socket..."

            try
                socket.close
            catch
                case e: Exception => logger error e

            logger debug "Socket is closed"

        private def stopPacketReceivingThread: Unit =
            ThreadUtil.stop(packetReceivingThread, Some(logger))

        private def stopPingingThread: Unit =
            ThreadUtil.stop(pingingThread, Some(logger))

        private def removeFromClients: Unit =
            logger debug "Removing from client map..."

            clientMap synchronized {
                clientMap remove id
            }

            logger debug "Removed from client map"


private val logger = LogManager getLogger classOf[TcpServer]