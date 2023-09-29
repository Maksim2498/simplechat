package ru.fominmv.simplechat.core.protocol.error


class UnsupportedServerCommandException extends UnsupportedCommandException:
    override def getMessage: String = "Unsupported server command"