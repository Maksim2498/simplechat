package ru.fominmv.simplechat.core.protocol.error


import java.io.IOException


class ProtocolException extends IOException:
    override def getMessage: String = "Bad packet"