package ru.fominmv.simplechat.core.log

import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.{Level, LogManager}


def configureLogger(debug: Boolean = false): Unit =
    val logConfigBuilder = ConfigurationBuilderFactory.newConfigurationBuilder
    val appenderName     = "Console"
    val console          = logConfigBuilder.newAppender(appenderName, "SimplechatConsoleAppender")
    val layout           = logConfigBuilder newLayout "PatternLayout"
    val rootLogger       = logConfigBuilder newRootLogger (
        if debug then
            Level.ALL
        else
            Level.INFO
    )

    layout.addAttribute(
        "pattern",
        if debug then
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