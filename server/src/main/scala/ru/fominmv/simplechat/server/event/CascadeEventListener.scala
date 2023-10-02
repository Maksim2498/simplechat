package ru.fominmv.simplechat.server.event


import scala.collection.mutable.ArrayBuffer

import ru.fominmv.simplechat.server.Client


class CascadeEventListener extends EventListener:
    val eventListeners = ArrayBuffer[EventListener]()


    override def on(event: Event): Unit =
        eventListeners foreach (_ on event)

object CascadeEventListener:
    def apply(eventListeners: EventListener*): CascadeEventListener =
        val eventListener = new CascadeEventListener()

        eventListener.eventListeners addAll eventListeners

        eventListener