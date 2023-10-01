package ru.fominmv.simplechat.server.event


import scala.collection.mutable.ArrayBuffer

import ru.fominmv.simplechat.server.Client


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

    override def onConnected(client: Client): Unit =
        eventListeners foreach (_ onConnected client)

    override def onSetName(client: Client, oldName: Option[String]): Unit =
        eventListeners foreach (_.onSetName(client, oldName))

    override def onMessage(client: Client, text: String): Unit =
        eventListeners foreach (_.onMessage(client, text))

    override def onDisconnectedByServer(client: Client): Unit =
        eventListeners foreach (_ onDisconnectedByServer client)

    override def onDisconnected(client: Client): Unit =
        eventListeners foreach (_ onDisconnected client)

    override def onConnectionLost(client: Client): Unit =
        eventListeners foreach (_ onConnectionLost client)

object CascadeEventListener:
    def apply(eventListeners: EventListener*): CascadeEventListener =
        val eventListener = new CascadeEventListener()

        eventListener.eventListeners addAll eventListeners

        eventListener