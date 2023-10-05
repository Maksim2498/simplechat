package ru.fominmv.simplechat.server.event


import java.io.ByteArrayOutputStream
import java.net.{
    DatagramPacket,
    InetAddress,
    InetSocketAddress,
    MulticastSocket,
    NetworkInterface,
}

import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.util.lifecycle.LifecyclePhase.*
import ru.fominmv.simplechat.core.util.lifecycle.{LifecycleDriven, LifecyclePhase}
import ru.fominmv.simplechat.core.util.UnsignedUtil.USHORT_MAX
import ru.fominmv.simplechat.core.protocol.{
    Protocol,
    SendMessageServerCommand,
}
import ru.fominmv.simplechat.core.Message
import ru.fominmv.simplechat.server.Config.*

import error.EventAbortedException


class MulticastEventListener(
    val address:   InetAddress = DEFAULT_MULTICAST_ADDRESS,
    val port:      Int         = DEFAULT_MULTICAST_PORT,
    val protocol:  Protocol    = DEFAULT_PROTOCOL,
    val autoOpen:  Boolean     = true,
    val autoClose: Boolean     = true,
) extends EventListener, LifecycleDriven:
    if !address.isMulticastAddress then
        throw IllegalArgumentException("<address> must be a multicast address")

    if !((0 to USHORT_MAX) contains port) then
        throw IllegalArgumentException("<port> must be in range [0, 65535]")


    override def close: Unit =
        if !canClose then
            return

        _lifecyclePhase = CLOSING
        logger debug "Closing..."

        closeSocket

        _lifecyclePhase = CLOSED
        logger debug "Closed"

    override def open: Unit =
        try
            if !canOpen then
                return

            _lifecyclePhase = OPENING
            logger debug "Opening..."

            openSocket

            _lifecyclePhase = OPEN
            logger debug "Opened"
        catch
            case e: Exception =>
                _lifecyclePhase = CLOSED
                throw e

    override def lifecyclePhase: LifecyclePhase =
        _lifecyclePhase

    override def on(event: Event): Unit =
        event match
            case PreOpenEvent() =>
                if autoOpen then
                    this.open // Without "this." compilator complained

            case PostCloseEvent() =>
                if autoClose then
                    close

            case PreBroadcastMessageEvent(message, _) =>
                ClosedException.checkOpen(this, "Event listener is closed")

                try
                    logger debug s"Multicasting message: $message..."
                    socket send createDatagramPacket(message)
                    logger debug "Multicasted"
                catch
                    case e: Exception => logger error e

                throw EventAbortedException()

            case _ =>

    private var lastId          = 0
    private var _lifecyclePhase = NEW

    private val socket          = MulticastSocket()


    private def openSocket: Unit =
        assert(!socket.isClosed)

        logger debug "Opening socket..."

        // socket.joinGroup(
        //     InetSocketAddress(address, port),
        //     findNetworkInterface,
        // )

        logger debug "Socket is opened"

    private def findNetworkInterface: NetworkInterface =
        logger debug "Searching for appropriate network interface..."

        val interfaces = NetworkInterface.getNetworkInterfaces

        while interfaces.hasMoreElements do
            val interface = interfaces.nextElement

            if  interface.isUp &&
                interface.supportsMulticast then
                logger debug s"Found network interface: ${interface.getDisplayName}"
                return interface

        throw RuntimeException("Failed to obtain network interface")

    private def closeSocket: Unit =
        assert(!socket.isClosed)

        logger debug "Closing socket..."

        try
            socket.close
        catch
            case e: Exception => logger error e

        logger debug "Socket is closed"

    private def createDatagramPacket(message: Message): DatagramPacket =
        val stream = ByteArrayOutputStream()
        val packet = SendMessageServerCommand(lastId.toShort, message)

        lastId += 1

        protocol.writePacket(packet, stream)

        DatagramPacket(
            stream.toByteArray,
            stream.size,
            address,
            port,
        )
    
private val logger = LogManager getLogger classOf[MulticastEventListener]