package ru.fominmv.simplechat.client.event


import ru.fominmv.simplechat.core.Message


sealed trait Event


sealed trait LifecycleEvent extends Event

case class PreOpenEvent()   extends LifecycleEvent
case class PostOpenEvent()  extends LifecycleEvent
case class PreCloseEvent()  extends LifecycleEvent
case class PostCloseEvent() extends LifecycleEvent


sealed trait ServerEvent extends Event

case class PingedEvent()                                               extends ServerEvent
case class MessageReceivedEvent(message: Message)                      extends ServerEvent
case class MessageRejectedEvent(message: Message)                      extends ServerEvent
case class MessageAcceptedEvent(message: Message)                      extends ServerEvent
case class NameRejectedEvent(newName: String, oldName: Option[String]) extends ServerEvent
case class NameAcceptedEvent(newName: String, oldName: Option[String]) extends ServerEvent
case class DisconnectedByServerEvent()                                 extends ServerEvent
case class FatalServerErrorEvent()                                     extends ServerEvent
case class ConnectionLostEvent()                                       extends ServerEvent


sealed trait ClientEvent extends Event

case class PrePingingEvent()                                          extends ClientEvent
case class PostPingingEvent()                                         extends ClientEvent
case class PreTrySendMessageEvent(message: Message)                   extends ClientEvent
case class PostTrySendMessageEvent(message: Message)                  extends ClientEvent
case class PreTrySetNameEvent(name: String, oldName: Option[String])  extends ClientEvent
case class PostTrySetNameEvent(name: String, oldName: Option[String]) extends ClientEvent
case class DisconnectedEvent()                                        extends ClientEvent