package ru.fominmv.simplechat.server


import scala.concurrent.duration.{FiniteDuration, DurationInt}

import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.{LogManager, Level}

import scopt.{OParser, Read}

import ru.fominmv.simplechat.core.protocol.binary.BinaryProtocol
import ru.fominmv.simplechat.core.protocol.text.TextProtocol
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

            configureLogger(config)

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

    implicit val protocolRead: Read[Protocol] =
        Read reads (
            _ match
                case "text"   => TextProtocol()
                case "binary" => BinaryProtocol()
        )

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

            opt[Boolean]("broadcast-messages")
                .text("Enables or disables broadcasting of received messages to all clients")
                .action((v, c) => c.copy(broadcastMessages = v)),

            opt[Boolean]("print-messages")
                .text("Enables or disables printing of received messages")
                .action((v, c) => c.copy(logMessages = v)),

            opt[Int]("port")
                .text("Specifies server port")
                .action((v, c) => c.copy(port = v))
                .validate(
                    if (0 to USHORT_MAX) contains _ then
                        success
                    else
                        failure("Option --port must be in range [0, 65535]")
                ),

            opt[Int]("backlog")
                .text("Sepcifies limit of simultaneous connection attempts to the server")
                .action((v, c) => c.copy(backlog = v))
                .validate(v =>
                    if v >= 0 then
                        success
                    else
                        failure("Option --backlog must be non-negative")
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

            opt[String]("name")
                .text("Specifies server name")
                .action((v, c) => c.copy(name = v)),

            opt[FiniteDuration]("ping-interval")
                .text("Specifies delay between clients pinging (if 0 then pinging is disabled)")
                .action((v, c) => c.copy(pingInterval = v))
                .validate(v =>
                    if v >= 0.seconds then
                        success
                    else
                        failure("Option --max-pending-commands must be non-negative")
                ),

            opt[Protocol]("protocol")
                .text("Specifies server protocol type")
                .action((v, c) => c.copy(protocol = v)),
        )


    private def configureLogger(config: Config): Unit =
        val logConfigBuilder = ConfigurationBuilderFactory.newConfigurationBuilder
        val appenderName     = "Console"
        val console          = logConfigBuilder.newAppender(appenderName, "SimplechatConsoleAppender")
        val layout           = logConfigBuilder newLayout "PatternLayout"
        val rootLogger       = logConfigBuilder newRootLogger (
            if config.debug then
                Level.ALL
            else
                Level.INFO
        )

        layout.addAttribute(
            "pattern",
            if config.debug then
                //        bright
                //        magenta  magenta
                //        |        |
                // dim    | dim    | dim      highlight
                // |      | |      | |        |
                // v      v v      v v        v
                // [<logger>:<thread>] <message>
                "%style{[}{dim}"                     +
                "%style{%logger{1}}{bright magenta}" +
                "%style{:}{dim}"                     +
                "%style{%t}{magenta}"                +
                "%style{]}{dim}"                     +
                " %highlight{%msg}{INFO=white}"
            else
                "%highlight{%msg}{INFO=white}"
        )

        console add layout
        logConfigBuilder add console

        rootLogger add (logConfigBuilder newAppenderRef appenderName)
        logConfigBuilder add rootLogger

        logConfigBuilder setStatusLevel Level.WARN

        val logConfig = logConfigBuilder.build

        Configurator initialize logConfig

        val logContext = (LogManager getContext false).asInstanceOf[LoggerContext]

        logContext setConfiguration logConfig

    private def run(config: Config): Unit =
        val builder = ServerBuilder(
            broadcastMessages  = config.broadcastMessages,
            logMessages        = config.logMessages,
            port               = config.port,
            backlog            = config.backlog,
            maxPendingCommands = config.maxPendingCommands,
            name               = config.name,
            pingInterval       = config.pingInterval,
            protocol           = config.protocol,
        )
        val server  = builder.builderServer
        val shell   = Shell(server)

        shell.run