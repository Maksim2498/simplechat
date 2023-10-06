package ru.fominmv.simplechat.core.util


import org.apache.logging.log4j.Logger


object ThreadUtil:
    def startThread(thread: Thread, logger: Option[Logger] = None): Unit =
        implicit val implicitLogger = logger

        log(s"Starting ${thread.getName} thread...")
        thread.start

    def stopThread(thread: Thread, logger: Option[Logger] = None): Boolean =
        implicit val implicitLogger = logger

        log(s"Closing ${thread.getName} thread...")

        if Thread.currentThread == thread then
            log("Canceled: thread cannot close itself")
            return false

        if thread.isAlive then
            thread.interrupt
            thread.join

        log(s"${thread.getName} thread is closed")

        true


    private def log(message: String)(implicit logger: Option[Logger]): Unit =
        logger foreach (_ debug message)