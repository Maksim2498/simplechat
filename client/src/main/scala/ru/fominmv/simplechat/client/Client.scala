package ru.fominmv.simplechat.client


import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.util.Closeable
import ru.fominmv.simplechat.client.event.EventListener


trait Client extends Closeable:
    def name:          Option[String]
    def protocol:      Protocol
    def eventListener: EventListener

    @throws[ClosedException]("When closed")
    def sendMessage(text: String): Unit

    @throws[ClosedException]("When closed")
    def pingServer: Unit