package ru.fominmv.simplechat.core.protocol


trait ToPacketConvertible:
    def toClientPacket: ClientPacket
    def toServerPacket: ServerPacket