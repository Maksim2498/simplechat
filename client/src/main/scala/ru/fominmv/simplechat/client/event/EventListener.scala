package ru.fominmv.simplechat.client.event


import ru.fominmv.simplechat.core.Message


trait EventListener:
    def onPreOpen: Unit = ()
    def onPostOpen: Unit = ()
    def onPreClose: Unit = ()
    def onPostClose: Unit = ()
    def onMessage(message: Message): Unit = ()
    def onSetName(newName: String, oldName: Option[String]): Unit = ()
    def onNameRejected(name: String): Unit = ()
    def onMessageRejected(text: String): Unit = ()
    def onDisconnectedByServer: Unit = ()
    def onDisconnected: Unit = ()
    def onConnectionLost: Unit = ()
    def onFatalError: Unit = ()