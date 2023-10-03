package ru.fominmv.simplechat.server.event


import ru.fominmv.simplechat.core.Message
import ru.fominmv.simplechat.server.Client


trait EventListener:
    def on(event: Event): Unit

    // Lifecycle:

    def publishPreOpen: Unit =
        on(PreOpenEvent())

    def publishPostOpen: Unit =
        on(PostOpenEvent())

    def publishPreClose: Unit =
        on(PreCloseEvent())

    def publishPostClose: Unit =
        on(PostCloseEvent())

    // Server:

    def publishPrePingClients: Unit =
        publishPrePingClients()

    def publishPrePingClients(except: Set[Int] = Set[Int]()): Unit =
        on(PrePingEvent(except))

    def publishPostPing: Unit =
        publishPostPing()

    def publishPostPing(except: Set[Int] = Set[Int]()): Unit =
        on(PostPingEvent(except))

    def publishPreBroadcastMessage(message: Message, except: Set[Int] = Set()): Unit =
        on(PreBroadcastMessageEvent(message, except))

    def publishPostBroadcastMessage(message: Message, except: Set[Int] = Set()): Unit =
        on(PostBroadcastMessageEvent(message, except))

    // Client:

    def publishConnected(client: Client): Unit =
        on(ConnectedEvent(client))

    def publishPinged(client: Client): Unit =
        on(PingedEvent(client))

    def publishSetName(client: Client, oldName: Option[String]): Unit =
        on(SetNameEvent(client, oldName))

    def publishMessageReceived(client: Client, text: String): Unit =
        on(MessageReceivedEvent(client, text))

    def publishDisconnectedByServer(client: Client): Unit =
        on(DisconnectedByServerEvent(client))

    def publishDisconnected(client: Client): Unit =
        on(DisconnectedEvent(client))

    def publishConnectionLost(client: Client): Unit =
        on(ConnectionLostEvent(client))

    def publishFatalError(client: Client): Unit =
        on(FatalErrorEvent(client))