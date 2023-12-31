package ru.fominmv.simplechat.client


import scala.concurrent.duration.{FiniteDuration, DurationInt}

import java.net.{InetAddress, NetworkInterface}

import scopt.OParser

import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.core.cli.scopt.protocolRead
import ru.fominmv.simplechat.core.log.configureLogger
import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.util.JarUtil.jarName
import ru.fominmv.simplechat.core.util.UnsignedUtil.USHORT_MAX


object Main:
    def main(args: Array[String]): Unit =
        try {
            val configOption = OParser.parse(cliParser, args, Config())

            if configOption == None then
                return

            val config = configOption.get

            if !config.run then
                return

            configureLogger(config.debug)

            try
                run(config)
            catch
                case e: Exception =>
                    if config.debug then
                        e.printStackTrace
                    else
                        logger error e
        } catch
            case e: Exception => System.err println e.getMessage


    private val logger = LogManager getLogger getClass

    private val cliParser =
        val builder = OParser.builder[Config]

        import builder.*

        OParser.sequence(
            head(BuildInfo.name, BuildInfo.version),
            programName(s"java -jar $jarName"),

            help('h', "help")
                .text("Prints help message and quits")
                .action((v, c) => c.copy(run = false)),

            version('v', "version")
                .text("Prints version and quits")
                .action((v, c) => c.copy(run = false)),

            opt[Unit]('d', "debug")
                .text("Enables debug mode")
                .action((v, c) => c.copy(debug = true)),

            opt[Boolean]("print-messages")
                .text("Enables or disables printing of received messages")
                .action((v, c) => c.copy(logMessages = v)),

            opt[InetAddress]('a', "address")
                .text("Specifies server address")
                .action((v, c) => c.copy(address = v)),

            opt[Int]('p', "port")
                .text("Specifies server port")
                .action((v, c) => c.copy(port = v))
                .validate(
                    if (0 to USHORT_MAX) contains _ then
                        success
                    else
                        failure("Option --port must be in range [0, 65535]")
                ),

            opt[Int]("max-pending-commands")
                .text("Specifies how many commands may be sent to client without response before connection closure (if 0 then all responses will be ignored)")
                .action((v, c) => c.copy(maxPendingCommands = v))
                .validate(v =>
                    if v >= 0 then
                        success
                    else
                        failure("Option --max-pending-commands must be non-negative")
                ),

            opt[String]('n', "name")
                .text("Specifies client name")
                .action((v, c) => c.copy(name = Some(v))),

            opt[FiniteDuration]("ping-interval")
                .text("Specifies delay between server pinging (if 0 then pinging is disabled)")
                .action((v, c) => c.copy(pingInterval = v))
                .validate(v =>
                    if v >= 0.seconds then
                        success
                    else
                        failure("Option --ping-interval must be non-negative")
                ),

            opt[Protocol]('P', "protocol")
                .text("Specifies client protocol type")
                .action((v, c) => c.copy(protocol = v)),

            opt[String]("network-interface")
                .text("Specifies network interface name used for multicasting (if not set then the default one will be used if possible)")
                .action((v, c) => c.copy(networkInterface = Some(NetworkInterface getByName v)))
                .validate(v =>
                    val networkInterface = NetworkInterface getByName v

                    if networkInterface == null then
                        failure(s"Network interface \"$v\" not found")
                    else if !networkInterface.supportsMulticast then
                        failure(s"Network interface \"$v\" doesn't support multicasting")
                    else
                        success
                ),

            opt[Unit]('m', "multicast")
                .text("Enables message multicasting (if enabled then client will additionally receive packets from server using UDP multicasting in addition to TCP unicast)")
                .action((v, c) => c.copy(doMulticast = true)),

            opt[InetAddress]("multicast-address")
                .text("Specifies multicast address")
                .action((v, c) => c.copy(multicastAddress = v))
                .validate(v =>
                    if v.isMulticastAddress then
                        success
                    else
                        failure("Option --multicast-address must be a multicast address")
                ),

            opt[Int]("multicast-port")
                .text("Specifies multicast port")
                .action((v, c) => c.copy(multicastPort = v ))
                .validate(
                    if (0 to USHORT_MAX) contains _ then
                        success
                    else
                        failure("Option --multicast-port must be in range [0, 65535]")
                ),
        )

    private def run(config: Config): Unit =
        val builder = ClientBuilder(
            address            = config.address,
            port               = config.port,
            name               = config.name,
            maxPendingCommands = config.maxPendingCommands,
            pingInterval       = config.pingInterval,
            protocol           = config.protocol,
            logMessages        = config.logMessages,
            networkInterface   = config.networkInterface,
            doMulticast        = config.doMulticast,
            multicastAddress   = config.multicastAddress,
            multicastPort      = config.multicastPort,
        )

        val client = builder.buildClient

        client.open

        val shell = Shell(client)

        shell.run