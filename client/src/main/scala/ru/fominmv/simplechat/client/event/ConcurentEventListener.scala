package ru.fominmv.simplechat.client.event


class ConcurentEventListener(val eventListener: EventListener) extends EventListener:
    override def on(event: Event): Unit =
        eventListener synchronized {
            eventListener on event
        }