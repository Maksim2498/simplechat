package ru.fominmv.simplechat.core.protocol.error


class BadResponseException extends ProtocolException:
    override def getMessage: String = "Bad response packet"