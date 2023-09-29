package ru.fominmv.simplechat.core.protocol.binary


import java.io.{IOException, InputStream, OutputStream}
import java.nio.charset.{Charset, StandardCharsets}
import java.util.{Scanner, NoSuchElementException}

import ru.fominmv.simplechat.core.protocol.{
    Protocol,
    Packet,
    ClientPacket,
    ServerPacket,
}


class BinaryProtocol(
    val charset: Charset = StandardCharsets.UTF_8
) extends Protocol:
    override def readClientPacket(stream: InputStream): ClientPacket =
        readBinaryPacket(stream).toClientPacket

    override def readServerPacket(stream: InputStream): ServerPacket =
        readBinaryPacket(stream).toServerPacket

    override def writePacket(packet: Packet, stream: OutputStream): Unit =
        writeBinaryPacket(BinaryPacket(packet), stream)


    private def readBinaryPacket(stream: InputStream): BinaryPacket =
        BinaryPacket read stream

    private def writeBinaryPacket(packet: BinaryPacket, stream: OutputStream): Unit =
        packet.write(stream, charset)