package ru.fominmv.simplechat.client.event


import scala.collection.mutable.ArrayBuffer

import ru.fominmv.simplechat.core.Message


class CascadeEventListener extends EventListener:
    val eventListeners = ArrayBuffer[EventListener]()
  
    override def onPreOpen: Unit =
        eventListeners foreach (_.onPreOpen)

    override def onPostOpen: Unit =
        eventListeners foreach (_.onPostOpen)

    override def onPreClose: Unit =
        eventListeners foreach (_.onPreClose)

    override def onPostClose: Unit =
        eventListeners foreach (_.onPostClose)

    override def onMessage(message: Message): Unit =
        eventListeners foreach (_ onMessage message)

    override def onSetName(newName: String, oldName: Option[String]): Unit =
        eventListeners foreach (_.onSetName(newName, oldName))

    override def onNameRejected(name: String): Unit =
        eventListeners foreach (_ onNameRejected name)

    override def onMessageRejected(text: String): Unit =
        eventListeners foreach (_ onMessageRejected text)

    override def onDisconnectedByServer: Unit =
        eventListeners foreach (_.onDisconnectedByServer)

    override def onDisconnected: Unit =
        eventListeners foreach (_.onDisconnected)

    override def onConnectionLost: Unit =
        eventListeners foreach (_.onConnectionLost)

    override def onFatalError: Unit =
        eventListeners foreach (_.onFatalError)

object CascadeEventListener:
    def apply(eventListeners: EventListener*): CascadeEventListener =
        val eventListener = new CascadeEventListener()

        eventListener.eventListeners addAll eventListeners

        eventListener