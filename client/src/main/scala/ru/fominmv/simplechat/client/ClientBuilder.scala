package ru.fominmv.simplechat.client

import scala.concurrent.duration.FiniteDuration

import java.net.{InetAddress, NetworkInterface}

import ru.fominmv.simplechat.core.protocol.Protocol

import event.{CascadeEventListener, LogEventListener}
import Config.*


class ClientBuilder(
    val address:            InetAddress              = DEFAULT_ADDRESS,
    val port:               Int                      = DEFAULT_PORT,
    val name:               Option[String]           = DEFAULT_NAME,
    val maxPendingCommands: Int                      = DEFAULT_MAX_PENDING_COMMANDS,
    val pingInterval:       FiniteDuration           = DEFAULT_PING_INTERVAL,
    val protocol:           Protocol                 = DEFAULT_PROTOCOL,
    val logMessages:        Boolean                  = DEFAULT_LOG_MESSAGES,
    val networkInterface:   Option[NetworkInterface] = DEFAULT_NETWORK_INTERFACE,
    var doMulticast:        Boolean                  = DEFAULT_DO_MULTICAST,
    var multicastAddress:   InetAddress              = DEFAULT_MULTICAST_ADDRESS,
    var multicastPort:      Int                      = DEFAULT_MULTICAST_PORT,
):
    def buildClient: Client =
        val client = if doMulticast then
            PolycastClient(
                networkInterface   = networkInterface,
                multicastAddress   = multicastAddress,
                multicastPort      = multicastPort,
                address            = address,
                port               = port,
                name               = name,
                maxPendingCommands = maxPendingCommands,
                pingInterval       = pingInterval,
                protocol           = protocol,
            )
        else
            TcpClient(
                address            = address,
                port               = port,
                name               = name,
                maxPendingCommands = maxPendingCommands,
                pingInterval       = pingInterval,
                protocol           = protocol,
            )

        val eventListeners = client.eventListener.eventListeners

        if logMessages then
            eventListeners addOne LogEventListener

        client