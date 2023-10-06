package ru.fominmv.simplechat.server.event


import scala.concurrent.duration.{FiniteDuration, DurationInt}
import scala.collection.mutable.ArrayBuffer

import org.apache.logging.log4j.LogManager

import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.util.lifecycle.LifecyclePhase.*
import ru.fominmv.simplechat.core.util.lifecycle.{LifecycleDriven, LifecyclePhase}
import ru.fominmv.simplechat.core.util.ThreadUtil.{startThread, stopThread}
import ru.fominmv.simplechat.server.Client

import error.EventAbortedException


class BufferingEventListener(
    val bufferingDuration: FiniteDuration = 10.seconds,
    val autoOpen:          Boolean        = true,
    val autoClose:         Boolean        = true,
) extends EventListener, LifecycleDriven:
    if bufferingDuration <= 0.seconds then
        throw IllegalArgumentException("<bufferDuration> must be positive")


    // External uses should be synchronized when open
    val eventListeners = ArrayBuffer[EventListener]()


    override def close: Unit =
        if !canClose then
            return

        logger debug "Closing..."
        _lifecyclePhase = CLOSING

        stopPublishingThread

        eventListeners.clear
        buffer.clear

        _lifecyclePhase = CLOSED
        logger debug "Closed"

    override def open: Unit =
        if !canOpen then
            return

        logger debug "Opening..."
        _lifecyclePhase = OPENING

        startPublishingThread
        waitPublishingThreadStarted

        _lifecyclePhase = OPEN
        logger debug "Opened"

    override def lifecyclePhase: LifecyclePhase =
        _lifecyclePhase

    override def on(event: Event): Unit =
        event match
            case PreOpenEvent() =>
                if autoOpen then
                    return this.open // Without "this." compilator complained

            case PostCloseEvent() =>
                if autoClose then
                    return close

            case _ =>

        ClosedException.checkOpen(this, "Event listener is closed")

        logger debug "Buffering event..."

        buffer synchronized {
            buffer addOne event
        }

        logger debug "Buffered"


    @volatile
    private var _lifecyclePhase = NEW

    private val buffer           = ArrayBuffer[Event]()
    private val publishingThread = Thread(
        () => publishingThreadBody,
        "Publisher",
    )


    private def startPublishingThread: Unit =
        startThread(publishingThread, Some(logger))

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

        while running do
            try
                logger debug "Waiting..."
                Thread sleep bufferingDuration.toMillis
                publish
            catch
                case e: Exception => onAnyException(e)

        assert(closed)

        logger debug "Finished"

    private def publish: Unit =
        logger debug "Publishing events..."

        eventListeners synchronized {
            var newBuffer: ArrayBuffer[Event] = null

            buffer synchronized {
                newBuffer = buffer.clone
                buffer.clear
            }

            newBuffer foreach (event =>
                eventListeners foreach (_ on event)
            )
        }

        logger debug "Published"

    private def stopPublishingThread: Unit =
        stopThread(publishingThread, Some(logger))

    private def onAnyException(exception: Exception): Unit =
        exception match
            case _: EventAbortedException => onEventAbortedException
            case _: InterruptedException  => onInterruptedException
            case e: Exception             => onException(e)

    private def onEventAbortedException: Unit =
        logger debug "Event aborted"

    private def onInterruptedException: Unit =
        logger debug "Aborted: interrupted"
        Thread.interrupted

    private def onException(exception: Exception): Unit =
        logger error exception


private val logger = LogManager getLogger classOf[BufferingEventListener]