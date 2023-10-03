package ru.fominmv.simplechat.server.event


class ConcurentEventListener(val eventListener: EventListener) extends EventListener:
    override def on(event: Event): Unit =
        eventListener synchronized {
            eventListener on event
        }