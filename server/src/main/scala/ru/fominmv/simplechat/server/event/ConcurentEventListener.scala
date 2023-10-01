package ru.fominmv.simplechat.server.event


import ru.fominmv.simplechat.server.Client


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

    override def onConnected(client: Client): Unit =
        eventListener synchronized {
            eventListener onConnected client
        }

    override def onSetName(client: Client, oldName: Option[String]): Unit =
        eventListener synchronized {
            eventListener.onSetName(client, oldName)
        }

    override def onMessage(client: Client, text: String): Unit =
        eventListener synchronized {
            eventListener.onMessage(client, text)
        }

    override def onDisconnectedByServer(client: Client): Unit =
        eventListener synchronized {
            eventListener onDisconnectedByServer client
        }

    override def onDisconnected(client: Client): Unit =
        eventListener synchronized {
            eventListener onDisconnected client
        }

    override def onConnectionLost(client: Client): Unit =
        eventListener synchronized {
            eventListener onConnectionLost client
        }