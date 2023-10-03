package ru.fominmv.simplechat.server.event


import scala.concurrent.duration.{FiniteDuration, DurationInt}
import scala.collection.mutable.ArrayBuffer

import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.util.lifecycle.LifecyclePhase.*
import ru.fominmv.simplechat.core.util.lifecycle.{LifecycleDriven, LifecyclePhase}
import ru.fominmv.simplechat.core.util.ThreadUtil
import ru.fominmv.simplechat.server.Client


class BufferingEventListener(
    val bufferingDuration: FiniteDuration = 10.seconds,
    val autoOpen:          Boolean        = true,
    val autoClose:         Boolean        = true,
) extends EventListener, LifecycleDriven:
    // External uses should be synchronized when open
    val eventListeners = ArrayBuffer[EventListener]()

    override def close: Unit =
        if !canClose then
            return

        _lifecyclePhase = CLOSING

        logger debug "Closing..."

        stopPublishingThread
        buffer.clear

        logger debug "Closed"

        _lifecyclePhase = CLOSED

    override def open: Unit =
        if !canOpen then
            return

        _lifecyclePhase = OPENING

        logger debug "Opening..."

        startPublishingThread
        waitPublishingThreadStarted

        logger debug "Open"

        _lifecyclePhase = OPEN

    override def lifecyclePhase: LifecyclePhase =
        _lifecyclePhase

    override def on(event: Event): Unit =
        ClosedException.checkOpen(this, "Event listener is closed")

        if autoOpen then
            event match
                case PreOpenEvent() => open
                case _ =>

        buffer synchronized {
            buffer addOne event
        }

        if autoClose then
            event match
                case PostCloseEvent() => close
                case _ =>
        

    @volatile
    private var _lifecyclePhase = NEW

    private val buffer           = ArrayBuffer[Event]()
    private val publishingThread = Thread(
        () => publishingThreadBody,
        "Publisher",
    )


    if bufferingDuration <= 0.seconds then
        throw IllegalArgumentException("bufferDuration must be positive")


    private def startPublishingThread: Unit =
        logger debug "Starting publishing thread..."
        publishingThread.start

    private def waitPublishingThreadStarted: Unit =
        synchronized {
            while
                try
                    wait()
                catch
                    case _: InterruptedException =>

                publishingThread.getState == Thread.State.NEW
            do ()
        }

    private def publishingThreadBody: Unit =
        logger debug "Started"

        synchronized {
            notify()
        }

        while !closed do
            try
                logger debug "Waiting..."
                Thread sleep bufferingDuration.toMillis
                publish
            catch
                case e: Exception => onAnyException(e)

    private def publish: Unit =
        logger debug "Publishing events..."

        eventListeners synchronized {
            buffer synchronized {
                buffer foreach (event =>
                    eventListeners foreach (_ on event)
                )

                buffer.clear
            }
        }

        logger debug "Published"

    private def stopPublishingThread: Unit =
        ThreadUtil.stop(publishingThread, Some(logger))

    private def onAnyException(exception: Exception): Unit =
        exception match
            case _: InterruptedException => onInterruptedException
            case e: Exception            => onException(e)

    private def onInterruptedException: Unit =
        logger debug "Aborted: interrupted"
        Thread.interrupted

    private def onException(exception: Exception): Unit =
        logger error exception


private val logger = LogManager getLogger classOf[BufferingEventListener]