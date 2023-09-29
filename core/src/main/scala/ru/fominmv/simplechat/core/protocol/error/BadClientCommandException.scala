package ru.fominmv.simplechat.core.protocol.error


class BadClientCommandException extends BadCommandException:
    override def getMessage: String = "Bad client command packet"