package ru.fominmv.simplechat.core.protocol.text


import java.io.{IOException, InputStream, OutputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.util.{Scanner, NoSuchElementException}

import ru.fominmv.simplechat.core.protocol.{
    Protocol,
    Packet,
    ClientPacket,
    ServerPacket,
}


class TextProtocol(
    val charset: Charset = StandardCharsets.UTF_8
) extends Protocol:
    override def readClientPacket(stream: InputStream): ClientPacket =
        readTextPacket(stream).toClientPacket

    override def readServerPacket(stream: InputStream): ServerPacket =
        readTextPacket(stream).toServerPacket

    override def writePacket(packet: Packet, stream: OutputStream): Unit =
        writeTextPacket(TextPacket(packet), stream)


    private def readTextPacket(stream: InputStream): TextPacket =
        try
            TextPacket parse Scanner(stream).nextLine
        catch
            case _: NoSuchElementException => throw IOException()

    private def writeTextPacket(packet: TextPacket, stream: OutputStream): Unit =
        stream write (packet.toString + "\n" getBytes charset)