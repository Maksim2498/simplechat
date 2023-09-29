package ru.fominmv.simplechat.core.protocol.error


class UnsupportedClientCommandException extends UnsupportedCommandException:
    override def getMessage: String = "Unsupported client command"