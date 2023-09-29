package ru.fominmv.simplechat.core.protocol


import ru.fominmv.simplechat.core.Message


sealed trait Packet:
    def code: Short



case class Response(
    code:   Short,
    status: Status
) extends Packet



sealed trait Command extends Packet


sealed trait ClientCommand extends Command

case class PingClientCommand(code: Short) extends ClientCommand

case class CloseClientCommand(code: Short) extends ClientCommand

case class SetNameClientCommand(
    code: Short,
    name: String,
) extends ClientCommand

case class SendMessageClientCommand(
    code: Short,
    text: String,
) extends ClientCommand


sealed trait ServerCommand extends Command

case class PingServerCommand(code: Short) extends ServerCommand

case class CloseServerCommand(code: Short) extends ServerCommand

case class SendMessageServerCommand(
    code:    Short,
    message: Message,
) extends ServerCommand



type ClientPacket = Response | ClientCommand
type ServerPacket = Response | ServerCommand