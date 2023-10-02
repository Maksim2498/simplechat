package ru.fominmv.simplechat.server.event


import scala.concurrent.duration.{FiniteDuration, DurationInt}
import scala.collection.mutable.ArrayBuffer

import ru.fominmv.simplechat.server.Client


class BufferingEventListener(
    val bufferDuration: FiniteDuration = 10.seconds,
)extends EventListener:
    val eventListeners = ArrayBuffer[EventListener]()


    override def on(event: Event): Unit =
        ???
        

    private val buffer = ArrayBuffer[Event]()


    if bufferDuration < 0.seconds then
        throw IllegalArgumentException("bufferDuration must be non-negative")