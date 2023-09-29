package ru.fominmv.simplechat.core.protocol.binary


import java.io.{
    EOFException,
    InputStream,
    OutputStream,
    DataOutputStream,
    DataInputStream,
    PushbackInputStream,
}
import java.nio.charset.{Charset, StandardCharsets}
import java.util.NoSuchElementException

import ru.fominmv.simplechat.core.protocol.error.{
    ProtocolException,
    BadResponseException,
    BadCommandException,
    BadClientCommandException,
    UnsupportedClientCommandException,
    BadServerCommandException,
    UnsupportedServerCommandException,
}
import ru.fominmv.simplechat.core.protocol.{
    Status,
    Packet,
    Command,
    Response,
    ToPacketConvertible,

    ClientCommand,
    PingClientCommand,
    CloseClientCommand,
    SetNameClientCommand,
    SendMessageClientCommand,

    ServerCommand,
    PingServerCommand,
    CloseServerCommand,
    SendMessageServerCommand,
}
import ru.fominmv.simplechat.core.util.StringExtension.{escape}
import ru.fominmv.simplechat.core.util.ToBytesConvertible
import ru.fominmv.simplechat.core.util.UnsignedUtil.*
import ru.fominmv.simplechat.core.Message


sealed trait BinaryPacket extends ToPacketConvertible, ToBytesConvertible:
    def typeId: Byte
    def code:   Short

object BinaryPacket:
    def apply(packet: Packet): BinaryPacket =
        packet match
            case response: Response => BinaryResponse(response)
            case command:  Command  => BinaryCommand(command)

    def read(stream: InputStream, charset: Charset = StandardCharsets.UTF_8): BinaryPacket =
        val pushbaskStream = PushbackInputStream(stream)
        val byte           = pushbaskStream.read
        val read           = byte match
            case BinaryResponse.TYPE_ID => BinaryResponse.read
            case BinaryCommand.TYPE_ID  => BinaryCommand.read
            case -1                     => throw EOFException()
            case _                      => throw ProtocolException()

        pushbaskStream unread byte

        read(pushbaskStream, charset)


case class BinaryResponse(
    code:     Short,
    statusId: Byte,
) extends BinaryPacket:
    override def typeId: Byte = BinaryResponse.TYPE_ID

    override def toClientPacket: Response = toResponse

    override def toServerPacket: Response = toResponse

    override def write(stream: OutputStream, charset: Charset = StandardCharsets.UTF_8): Unit =
        val dataStream = DataOutputStream(stream)

        dataStream writeByte  typeId
        dataStream writeShort code
        dataStream writeByte  statusId

    override def toString: String = s"${typeId} ${code} ${statusId}"


    private def toResponse: Response =
        try
            Response(code, Status fromOrdinal statusId)
        catch
            case _: NoSuchElementException => throw BadResponseException()


object BinaryResponse:
    val TYPE_ID: Byte = 0

    def apply(response: Response): BinaryResponse =
        BinaryResponse(response.code, response.status.ordinal.toByte)

    def read(stream: InputStream, charset: Charset = StandardCharsets.UTF_8): BinaryResponse =
        val dataStream = DataInputStream(stream)
        val typeId     = dataStream.readByte

        if typeId != TYPE_ID then
            throw BadResponseException()

        val code     = dataStream.readShort
        val statusId = dataStream.readByte

        BinaryResponse(code, statusId)


case class BinaryCommand(
    code:      Short,
    commandId: Byte,
    args:      List[String] = List()
) extends BinaryPacket:
    if args.length > UBYTE_MAX || args.exists(_.length > USHORT_MAX) then
        throw BadCommandException()

    override def typeId: Byte = BinaryCommand.TYPE_ID

    override def toClientPacket: ClientCommand =
        commandId match
            case BinaryCommand.CLOSE_CLIENT_COMMAND_ID =>
                if !args.isEmpty then
                    throw BadClientCommandException()

                CloseClientCommand(code)

            case BinaryCommand.SEND_MESSAGE_CLIENT_COMMAND_ID =>
                if args.length != 1 then
                    throw BadClientCommandException()

                SendMessageClientCommand(code, text = args(0))

            case BinaryCommand.SET_NAME_CLIENT_COMMAND_ID =>
                if args.length != 1 then
                    throw BadClientCommandException()

                SetNameClientCommand(code, name = args(0))
        
            case _ => throw UnsupportedClientCommandException()

    override def toServerPacket: ServerCommand =
        commandId match
            case BinaryCommand.PING_SERVER_COMMAND_ID =>
                if !args.isEmpty then
                    throw BadServerCommandException()

                PingServerCommand(code)

            case BinaryCommand.CLOSE_SERVER_COMMAND_ID =>
                if !args.isEmpty then
                    throw BadServerCommandException()

                CloseServerCommand(code)

            case BinaryCommand.SEND_MESSAGE_SERVER_COMMAND_ID =>
                if args.length != 2 then
                    throw BadServerCommandException()

                SendMessageServerCommand(
                    code,
                    Message(
                        author = args(0),
                        text   = args(1),
                    ),
                )

            case _ => throw UnsupportedServerCommandException()

    override def write(stream: OutputStream, charset: Charset = StandardCharsets.UTF_8): Unit =
        val dataStream = DataOutputStream(stream)

        dataStream writeByte  typeId
        dataStream writeShort code
        dataStream writeByte  commandId
        dataStream writeByte  args.length.toByte

        for arg <- args do
            dataStream writeShort arg.length.toShort
            dataStream write      arg.getBytes(charset)

    override def toString: String =
        s"$typeId $code $commandId ${args map (a => s"\"${a.escape}\"") mkString " "}"

object BinaryCommand:
    val TYPE_ID:                        Byte = 1

    val PING_CLIENT_COMMAND_ID:         Byte = 0
    val CLOSE_CLIENT_COMMAND_ID:        Byte = 1
    val SEND_MESSAGE_CLIENT_COMMAND_ID: Byte = 2
    val SET_NAME_CLIENT_COMMAND_ID:     Byte = 3

    val PING_SERVER_COMMAND_ID:         Byte = 0
    val CLOSE_SERVER_COMMAND_ID:        Byte = 1
    val SEND_MESSAGE_SERVER_COMMAND_ID: Byte = 2

    def apply(command: Command): BinaryCommand =
        command match
            case serverCommand: ServerCommand => BinaryCommand(serverCommand)
            case clientCommand: ClientCommand => BinaryCommand(clientCommand)

    def apply(command: ClientCommand): BinaryCommand =
        command match
            case PingClientCommand(code) =>
                BinaryCommand(
                    code,
                    PING_CLIENT_COMMAND_ID,
                )

            case CloseClientCommand(code) =>
                BinaryCommand(
                    code,
                    CLOSE_CLIENT_COMMAND_ID,
                )

            case SetNameClientCommand(code, name) =>
                BinaryCommand(
                    code,
                    SET_NAME_CLIENT_COMMAND_ID,
                    List(name),
                )

            case SendMessageClientCommand(code, text) =>
                BinaryCommand(
                    code,
                    SEND_MESSAGE_CLIENT_COMMAND_ID,
                    List(text),
                )

    def apply(command: ServerCommand): BinaryCommand =
        command match
            case PingServerCommand(code) =>
                BinaryCommand(
                    code,
                    PING_SERVER_COMMAND_ID,
                )

            case CloseServerCommand(code) =>
                BinaryCommand(
                    code,
                    CLOSE_SERVER_COMMAND_ID,
                )

            case SendMessageServerCommand(code, message) =>
                BinaryCommand(
                    code,
                    SEND_MESSAGE_SERVER_COMMAND_ID,
                    List(message.author, message.text),
                )

    def read(stream: InputStream, charset: Charset = StandardCharsets.UTF_8): BinaryCommand =
        val dataStream = DataInputStream(stream)
        val typeId     = dataStream.readByte

        if typeId != TYPE_ID then
            throw BadCommandException()

        val code      = dataStream.readShort
        val commandId = dataStream.readByte
        val argCount  = byteToUnsigned(dataStream.readByte)

        var args = for i <- 0 to argCount yield
            val length = shortToUnsigned(dataStream.readShort)
            val bytes  = new Array[Byte](length)

            dataStream readFully bytes

            String(bytes, charset)

        BinaryCommand(code, commandId, args.toList)