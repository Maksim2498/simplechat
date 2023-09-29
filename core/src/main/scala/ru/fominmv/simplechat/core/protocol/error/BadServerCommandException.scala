package ru.fominmv.simplechat.core.protocol.error


class BadServerCommandException extends BadCommandException:
    override def getMessage: String = "Bad server command packet"