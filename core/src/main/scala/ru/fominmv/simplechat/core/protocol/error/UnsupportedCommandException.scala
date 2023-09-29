package ru.fominmv.simplechat.core.protocol.error


class UnsupportedCommandException extends ProtocolException:
    override def getMessage: String = "Unsupported command"