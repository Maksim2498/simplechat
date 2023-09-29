package ru.fominmv.simplechat.core.protocol


import java.io.{InputStream, OutputStream, IOException}

import ru.fominmv.simplechat.core.protocol.error.ProtocolException


trait Protocol:
    @throws[IOException]("If failed to read from stream")
    @throws[ProtocolException]("If bad packet encountered")
    def readClientPacket(stream: InputStream): ClientPacket

    @throws[IOException]("If failed to read from stream")
    @throws[ProtocolException]("If bad packet encountered")
    def readServerPacket(stream: InputStream): ServerPacket

    @throws[IOException]("If failed to write to stream")
    def writePacket(packet: Packet, stream: OutputStream): Unit