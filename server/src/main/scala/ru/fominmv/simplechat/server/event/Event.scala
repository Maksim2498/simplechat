package ru.fominmv.simplechat.server.event


import ru.fominmv.simplechat.core.Message
import ru.fominmv.simplechat.server.Client


sealed trait Event


sealed trait LifeCycleEvent extends Event

case class PreOpenEvent()   extends LifeCycleEvent
case class PostOpenEvent()  extends LifeCycleEvent
case class PreCloseEvent()  extends LifeCycleEvent
case class PostCloseEvent() extends LifeCycleEvent


sealed trait ServerEvent extends Event

case class PrePingClientsEvent(except: Set[Int] = Set[Int]())  extends ServerEvent
case class PostPingClientsEvent(except: Set[Int] = Set[Int]()) extends ServerEvent

case class PreBroadcastMessageEvent(
    message: Message,
    except:  Set[Int] = Set[Int](),
)  extends ServerEvent

case class PostBroadcastMessageEvent(
    message: Message,
    except:  Set[Int] = Set[Int](),
) extends ServerEvent


sealed trait ClientEvent extends Event

case class ConnectedEvent(client: Client)                        extends ClientEvent
case class PingEvent(client: Client)                             extends ClientEvent
case class SetNameEvent(client: Client, oldName: Option[String]) extends ClientEvent
case class MessageEvent(client: Client, text: String)            extends ClientEvent
case class DisconnectedByServerEvent(client: Client)             extends ClientEvent
case class DisconnectedEvent(client: Client)                     extends ClientEvent
case class ConnectionLostEvent(client: Client)                   extends ClientEvent
case class FatalErrorEvent(client: Client)                       extends ClientEvent