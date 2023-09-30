package ru.fominmv.simplechat.server


import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.util.Closeable
import ru.fominmv.simplechat.core.Message
import ru.fominmv.simplechat.server.event.EventListener


trait Server extends Closeable:
    def name:          String
    def protocol:      Protocol
    def eventListener: EventListener
    def clients:       List[Client]

    @throws[ClosedException]("When closed")
    def pingClients: Unit =
        pingClients()

    @throws[ClosedException]("When closed")
    def pingClients(except: Set[Int] = Set()): Unit =
        ClosedException.checkOpen(this, "Server is closed")

        for
            client <- clients
            if !(except contains client.id)
        do
            client.ping

    @throws[ClosedException]("When closed")
    def broadcastMessage(text: String): Unit =
        broadcastMessage(text, Set())

    @throws[ClosedException]("When closed")
    def broadcastMessage(text: String, except: Set[Int]): Unit =
        broadcastMessage(makeMessage(text), except)

    @throws[ClosedException]("When closed")
    def broadcastMessage(message: Message, except: Set[Int] = Set()): Unit =
        ClosedException.checkOpen(this, "Server is closed")

        for
            client <- clients
            if !(except contains client.id)
        do
            client.ping

    def makeMessage(text: String): Message =
        Message(name, text)