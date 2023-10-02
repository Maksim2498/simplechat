package ru.fominmv.simplechat.client.event


import ru.fominmv.simplechat.core.Message


class ConcurentEventListener(val eventListener: EventListener) extends EventListener:
    override def onPreOpen: Unit =
        eventListener synchronized {
            eventListener.onPreOpen
        }

    override def onPostOpen: Unit =
        eventListener synchronized {
            eventListener.onPostOpen
        }

    override def onPreClose: Unit =
        eventListener synchronized {
            eventListener.onPreClose
        }

    override def onPostClose: Unit =
        eventListener synchronized {
            eventListener.onPostClose
        }

    override def onMessage(message: Message): Unit =
        eventListener synchronized {
            eventListener onMessage message
        }

    override def onSetName(newName: String, oldName: Option[String]): Unit =
        eventListener synchronized {
            eventListener.onSetName(newName, oldName)
        }

    override def onNameRejected(name: String): Unit =
        eventListener synchronized {
            eventListener onNameRejected name
        }

    override def onMessageRejected(text: String): Unit =
        eventListener synchronized {
            eventListener onMessageRejected text
        }

    override def onDisconnectedByServer: Unit =
        eventListener synchronized {
            eventListener.onDisconnectedByServer
        }

    override def onDisconnected: Unit =
        eventListener synchronized {
            eventListener.onDisconnected
        }

    override def onConnectionLost: Unit =
        eventListener synchronized {
            eventListener.onConnectionLost
        }

    override def onFatalError: Unit =
        eventListener synchronized {
            eventListener.onFatalError
        }