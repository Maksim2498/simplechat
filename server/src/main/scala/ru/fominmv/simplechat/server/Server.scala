package ru.fominmv.simplechat.server


import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.util.lifecycle.{LifecycleDriven, LifecyclePhase}
import ru.fominmv.simplechat.core.Message
import ru.fominmv.simplechat.server.event.CascadeEventListener


trait Server extends LifecycleDriven:
    def name:          String
    def protocol:      Protocol
    def clients:       Set[Client]
    def eventListener: CascadeEventListener

    @throws[ClosedException]("When closed")
    def pingClients: Unit =
        pingClients()

    @throws[ClosedException]("When closed")
    def pingClients(except: Set[Int] = Set()): Unit;

    @throws[ClosedException]("When closed")
    def broadcastMessage(text: String): Unit =
        broadcastMessage(text, Set())

    @throws[ClosedException]("When closed")
    def broadcastMessage(text: String, except: Set[Int]): Unit =
        broadcastMessage(makeMessage(text), except)

    @throws[ClosedException]("When closed")
    def broadcastMessage(message: Message, except: Set[Int] = Set()): Unit;

    def makeMessage(text: String): Message =
        Message(name, text)