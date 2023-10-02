package ru.fominmv.simplechat.server.event


import ru.fominmv.simplechat.server.Client


class ConcurentEventListener(val eventListener: EventListener) extends EventListener:
    override def on(event: Event): Unit =
        eventListener synchronized {
            eventListener on event
        }