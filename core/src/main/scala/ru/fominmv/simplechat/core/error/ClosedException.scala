package ru.fominmv.simplechat.core.error


import ru.fominmv.simplechat.core.util.lifecycle.Closeable


class ClosedException(val message: String = "Closed") extends RuntimeException(message)

object ClosedException:
    def checkOpen(closeable: Closeable, message: String = "Closed"): Unit =
        if closeable.closed then
            throw ClosedException(message)