package ru.fominmv.simplechat.client


import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.util.lifecycle.{LifecycleDriven, LifecyclePhase}
import ru.fominmv.simplechat.core.Message
import ru.fominmv.simplechat.client.event.CascadeEventListener


trait Client extends LifecycleDriven:
    def protocol:      Protocol
    def eventListener: CascadeEventListener

    def name: Option[String]

    @throws[ClosedException]("When closed")
    def name_=(name: String): Unit

    @throws[ClosedException]("When closed")
    def sendMessageToServer(text: String): Unit

    @throws[ClosedException]("When closed")
    def pingServer: Unit

    @throws[RuntimeException]("When name not set")
    def makeMessage(text: String): Message =
        if name == None then
            throw RuntimeException("Name not set")

        Message(name.get, text)