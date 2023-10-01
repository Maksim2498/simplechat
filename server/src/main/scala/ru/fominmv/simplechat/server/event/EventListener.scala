package ru.fominmv.simplechat.server.event


import ru.fominmv.simplechat.server.Client


trait EventListener:
    def onPreOpen: Unit = ()
    def onPostOpen: Unit = ()
    def onPreClose: Unit = ()
    def onPostClose: Unit = ()
    def onConnected(client: Client): Unit = ()
    def onSetName(client: Client, oldName: Option[String]): Unit = ()
    def onMessage(client: Client, text: String): Unit = ()
    def onDisconnectedByServer(client: Client): Unit = ()
    def onDisconnected(client: Client): Unit = ()
    def onConnectionLost(client: Client): Unit = ()