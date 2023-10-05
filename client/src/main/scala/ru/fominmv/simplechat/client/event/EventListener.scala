package ru.fominmv.simplechat.client.event


import ru.fominmv.simplechat.core.Message


trait EventListener:
    def on(event: Event): Unit

    def name: String =
        getClass.getSimpleName

    // Lifecycle:

    def publishPreOpen: Unit =
        on(PreOpenEvent())

    def publishPostOpen: Unit =
        on(PostOpenEvent())

    def publishPreClose: Unit =
        on(PreCloseEvent())

    def publishPostClose: Unit =
        on(PostCloseEvent())

    // Server

    def publishPinged: Unit =
        on(PingedEvent())

    def publishMessageReceived(message: Message): Unit =
        on(MessageReceivedEvent(message))

    def publishMessageRejected(message: Message): Unit =
        on(MessageRejectedEvent(message))

    def publishMessageAccepted(message: Message): Unit =
        on(MessageAcceptedEvent(message))

    def publishNameRejected(newName: String, oldName: Option[String]): Unit =
        on(NameRejectedEvent(newName, oldName))

    def publishNameAccepted(newName: String, oldName: Option[String]): Unit =
        on(NameAcceptedEvent(newName, oldName))

    def publishDisconnectedByServer: Unit =
        on(DisconnectedByServerEvent())

    def publishFatalServerError: Unit =
        on(FatalServerErrorEvent())

    def publishConnectionLost: Unit =
        on(ConnectionLostEvent())

    // Client:

    def publishPrePinging: Unit =
        on(PrePingingEvent())

    def publishPostPinging: Unit =
        on(PostPingingEvent())

    def publishPreTrySendMessage(message: Message): Unit =
        on(PreTrySendMessageEvent(message))

    def publishPostTrySendMessage(message: Message): Unit =
        on(PostTrySendMessageEvent(message))

    def publishPreTrySetName(newName: String, oldName: Option[String]): Unit =
        on(PreTrySetNameEvent(newName, oldName))

    def publishPostTrySetName(newName: String, oldName: Option[String]): Unit =
        on(PostTrySetNameEvent(newName, oldName))

    def publishDisconnected: Unit =
        on(DisconnectedEvent())