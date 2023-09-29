package ru.fominmv.simplechat.core.util


import org.apache.logging.log4j.Logger


object ThreadUtil:
    def stop(thread: Thread, logger: Option[Logger] = None): Boolean =
        def log(message: String) =
            logger foreach (_ debug message)

        log(s"Closing ${thread.getName} thread...")

        if Thread.currentThread == thread then
            log("Canceled: thread cannot close itself")
            return false

        if thread.isAlive then
            thread.interrupt
            thread.join

        log(s"${thread.getName} thread is closed")

        true