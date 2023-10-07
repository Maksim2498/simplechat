package ru.fominmv.simplechat.client

import scala.concurrent.duration.FiniteDuration

import java.io.ByteArrayInputStream
import java.net.{
    DatagramPacket,
    InetAddress,
    InetSocketAddress,
    NetworkInterface,
    MulticastSocket,
}

import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.util.UnsignedUtil.USHORT_MAX
import ru.fominmv.simplechat.core.util.ThreadUtil.{startThread, stopThread}

import event.CascadeEventListener
import Config.*


class PolycastClient(
    val multicastAddress:   InetAddress              = DEFAULT_MULTICAST_ADDRESS,
    val multicastPort:      Int                      = DEFAULT_MULTICAST_PORT,
    val networkInterface:   Option[NetworkInterface] = DEFAULT_NETWORK_INTERFACE,
        name:               Option[String]           = DEFAULT_NAME,
        address:            InetAddress              = DEFAULT_ADDRESS,
        port:               Int                      = DEFAULT_PORT,
        pingInterval:       FiniteDuration           = DEFAULT_PING_INTERVAL,
        maxPendingCommands: Int                      = DEFAULT_MAX_PENDING_COMMANDS,
        protocol:           Protocol                 = DEFAULT_PROTOCOL,
        eventListener:      CascadeEventListener     = CascadeEventListener(),
) extends TcpClient(
    initName           = name,
    address            = address,
    port               = port,
    pingInterval       = pingInterval,
    maxPendingCommands = maxPendingCommands,
    protocol           = protocol,
    eventListener      = eventListener,
):
    if !multicastAddress.isMulticastAddress then
        throw IllegalArgumentException("<multicastAddress> must be a multicast address")

    if !((0 to USHORT_MAX) contains multicastPort) then
        throw IllegalArgumentException("<multicastPort> must be in range [0, 65535]")


    protected val multicastSocket                = MulticastSocket(multicastPort)
    protected val multicastPacketReceivingThread = Thread(
        () => multicastPacketReceivingThreadBody,
        "Multicast Packet Receiver",
    )


    override protected def prepareAllSockets: Unit =
        super.prepareAllSockets
        joinMulticastGroup

    protected def joinMulticastGroup: Unit =
        logger debug s"Joining multicast group $multicastAddress..."

        multicastSocket.joinGroup(
            InetSocketAddress(multicastAddress, multicastPort),
            networkInterface getOrElse null,
        )

        logger debug "Joined multicast group"

    override protected def startThreads: Unit =
        super.startThreads
        startMulticastPacketReceivingThread

    protected def startMulticastPacketReceivingThread: Unit =
        startThread(multicastPacketReceivingThread, Some(logger))

    override protected def threadsStarting: Boolean =
        super.threadsStarting ||
        multicastPacketReceivingThreadStarting

    protected def multicastPacketReceivingThreadStarting: Boolean =
        multicastPacketReceivingThread.getState == Thread.State.NEW

    override protected def closeAllSockets: Unit =
        super.closeAllSockets
        closeMulticastSocket

    protected def closeMulticastSocket: Unit =
        logger debug "Closing multicast socket..."

        try
            multicastSocket.close
        catch
            case e: Exception => logger error e

        logger debug "Multicast socket is closed"

    override protected def stopAllThreads: Unit =
        super.stopAllThreads
        stopMulticastPacketReceivingThread

    protected def stopMulticastPacketReceivingThread: Unit =
        stopThread(multicastPacketReceivingThread, Some(logger))

    protected def multicastPacketReceivingThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        val buffer   = new Array[Byte](65507)
        val datagram = DatagramPacket(buffer, buffer.length)

        while running do
            try
                datagram setLength buffer.length

                logger debug "Waiting for datagram packets..."
                multicastSocket receive datagram
                logger debug s"Received packet from ${datagram.getSocketAddress}"

                val stream = ByteArrayInputStream(datagram.getData, 0, datagram.getLength)
                val packet = protocol readServerPacket stream

                onPacket(packet, false)
            catch
                case e: Exception => onAnyException(e)

        assert(closed)

        logger debug "Finished"