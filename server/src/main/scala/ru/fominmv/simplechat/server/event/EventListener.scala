package ru.fominmv.simplechat.server.event


import ru.fominmv.simplechat.core.Message
import ru.fominmv.simplechat.server.Client


trait EventListener:
    def on(event: Event): Unit

    // Life-Cycle:

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
        on(PrePingClientsEvent(except))

    def publishPostPingClients: Unit =
        publishPostPingClients()

    def publishPostPingClients(except: Set[Int] = Set[Int]()): Unit =
        on(PostPingClientsEvent(except))

    def publishPreBroadcastMessage(message: Message, except: Set[Int] = Set()): Unit =
        on(PreBroadcastMessageEvent(message, except))

    def publishPostBroadcastMessage(message: Message, except: Set[Int] = Set()): Unit =
        on(PostBroadcastMessageEvent(message, except))

    // Client:

    def publishConnected(client: Client): Unit =
        on(ConnectedEvent(client))

    def publishPing(client: Client): Unit =
        on(PingEvent(client))

    def publishSetName(client: Client, oldName: Option[String]): Unit =
        on(SetNameEvent(client, oldName))

    def publishMessage(client: Client, text: String): Unit =
        on(MessageEvent(client, text))

    def publishDisconnectedByServer(client: Client): Unit =
        on(DisconnectedByServerEvent(client))

    def publishDisconnected(client: Client): Unit =
        on(DisconnectedEvent(client))

    def publishConnectionLost(client: Client): Unit =
        on(ConnectionLostEvent(client))

    def publishFatalError(client: Client): Unit =
        on(FatalErrorEvent(client))