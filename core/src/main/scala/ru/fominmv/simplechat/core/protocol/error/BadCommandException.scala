package ru.fominmv.simplechat.core.protocol.error


class BadCommandException extends ProtocolException:
    override def getMessage: String = "Bad command packet"