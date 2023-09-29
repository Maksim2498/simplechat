package ru.fominmv.simplechat.core.protocol.text


import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks.{breakable, break}

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
    CloseClientCommand,
    PingClientCommand,
    SetNameClientCommand,
    SendMessageClientCommand,

    ServerCommand,
    PingServerCommand,
    CloseServerCommand,
    SendMessageServerCommand,
}
import ru.fominmv.simplechat.core.util.StringExtension.{escape}
import ru.fominmv.simplechat.core.Message


sealed trait TextPacket extends ToPacketConvertible:
    def typeName: String
    def code:     Short

object TextPacket:
    def apply(packet: Packet): TextPacket =
        packet match
            case response: Response => TextResponse(response)
            case command:  Command  => TextCommand(command)

    def parse(string: String): TextPacket =
        val parts = string.trim.split("\\s+", 2)

        parts(0).toLowerCase match
            case TextResponse.TYPE_NAME => TextResponse parse string
            case TextCommand.TYPE_NAME  => TextCommand  parse string
            case _                      => throw ProtocolException()


case class TextResponse(
    code:       Short,
    statusName: String,
) extends TextPacket:
    override def typeName: String = TextResponse.TYPE_NAME

    override def toClientPacket: Response = toResponse

    override def toServerPacket: Response = toResponse

    override def toString: String = s"${typeName} ${code} ${statusName}"


    private def toResponse: Response =
        try
            Response(code, Status valueOf statusName.toUpperCase)
        catch
            case _: IllegalArgumentException => throw BadResponseException()

object TextResponse:
    val TYPE_NAME = "r"

    def apply(response: Response): TextResponse =
        TextResponse(response.code, response.status.toString)

    def parse(string: String): TextResponse =
        val parts = string.trim split "\\s+"

        if parts.length != 3 then
            throw BadResponseException()

        try
            if parts(0).toLowerCase != TYPE_NAME then
                throw BadResponseException()

            val code       = java.lang.Short parseShort parts(1)
            val statusName = parts(2)

            TextResponse(code, statusName)
        catch
            case _: NumberFormatException => throw BadResponseException()



case class TextCommand(
    code: Short,
    name: String,
    args: List[String] = List(),
) extends TextPacket:
    override def typeName: String =
        TextCommand.TYPE_NAME

    override def toClientPacket: ClientCommand =
        name.toLowerCase match
            case TextCommand.PING_CLIENT_COMMAND_NAME =>
                if !args.isEmpty then
                    throw BadClientCommandException()

                PingClientCommand(code)

            case TextCommand.CLOSE_CLIENT_COMMAND_NAME =>
                if !args.isEmpty then
                    throw BadClientCommandException()

                CloseClientCommand(code)

            case TextCommand.SEND_MESSAGE_CLIENT_COMMAND_NAME =>
                if args.length != 1 then
                    throw BadClientCommandException()

                SendMessageClientCommand(code, text = args(0))

            case TextCommand.SET_NAME_CLIENT_COMMAND_NAME =>
                if args.length != 1 then
                    throw BadClientCommandException()

                SetNameClientCommand(code, name = args(0))

            case _ => throw UnsupportedClientCommandException()

    override def toServerPacket: ServerCommand =
        name.toLowerCase match
            case TextCommand.PING_SERVER_COMMAND_NAME =>
                if !args.isEmpty then
                    throw BadServerCommandException()

                PingServerCommand(code)

            case TextCommand.CLOSE_SERVER_COMMAND_NAME =>
                if !args.isEmpty then
                    throw BadServerCommandException()

                CloseServerCommand(code)

            case TextCommand.SEND_MESSAGE_SERVER_COMMAND_NAME => 
                if args.length != 2 then
                    throw BadServerCommandException()

                SendMessageServerCommand(
                    code,
                    Message(
                        author = args(0),
                        text   = args(1)
                    ),
                )
            
            case _ => throw UnsupportedServerCommandException()

    override def toString: String =
        s"$code $name ${args map (a => s"\"${a.escape}\"") mkString " "}"

object TextCommand:
    val TYPE_NAME                        = "c"

    val PING_CLIENT_COMMAND_NAME         = "ping"
    val CLOSE_CLIENT_COMMAND_NAME        = "close"
    val SET_NAME_CLIENT_COMMAND_NAME     = "set_name"
    val SEND_MESSAGE_CLIENT_COMMAND_NAME = "send_msg"

    val PING_SERVER_COMMAND_NAME         = PING_CLIENT_COMMAND_NAME
    val CLOSE_SERVER_COMMAND_NAME        = CLOSE_CLIENT_COMMAND_NAME
    val SEND_MESSAGE_SERVER_COMMAND_NAME = SEND_MESSAGE_CLIENT_COMMAND_NAME


    def apply(command: Command): TextCommand =
        command match
            case clientCommand: ClientCommand => TextCommand(clientCommand)
            case serverCommand: ServerCommand => TextCommand(serverCommand)

    def apply(command: ClientCommand): TextCommand =
        command match
            case PingClientCommand(code) =>
                TextCommand(
                    code,
                    PING_CLIENT_COMMAND_NAME,
                )

            case CloseClientCommand(code) =>
                TextCommand(
                    code,
                    CLOSE_CLIENT_COMMAND_NAME,
                )

            case SetNameClientCommand(code, name) => 
                TextCommand(
                    code,
                    SET_NAME_CLIENT_COMMAND_NAME,
                    List(name),
                )

            case SendMessageClientCommand(code, text) =>
                TextCommand(
                    code,
                    SEND_MESSAGE_CLIENT_COMMAND_NAME,
                    List(text),
                )

    def apply(command: ServerCommand): TextCommand =
        command match
            case PingServerCommand(code) =>
                TextCommand(
                    code,
                    PING_SERVER_COMMAND_NAME,
                )

            case CloseServerCommand(code) =>
                TextCommand(
                    code,
                    CLOSE_SERVER_COMMAND_NAME,
                )

            case SendMessageServerCommand(code, message) =>
                TextCommand(
                    code,
                    SEND_MESSAGE_SERVER_COMMAND_NAME,
                    List(message.author, message.text),
                )

    def parse(string: String): TextCommand =
        try
            val tokens = Token tokenize string

            if tokens.length < 3
            || !tokens(0).isInstanceOf[NameToken]
            || !tokens(1).isInstanceOf[ShortToken]
            || !tokens(2).isInstanceOf[NameToken] then
                throw BadCommandException()

            val typeName = tokens(0).asInstanceOf[NameToken].value

            if typeName.toLowerCase != TYPE_NAME then
                throw BadCommandException()

            val tail = tokens drop 3
            
            if tail exists (!_.isInstanceOf[StringToken]) then
                throw BadCommandException()

            val codeToken = tokens(1).asInstanceOf[ShortToken]
            val nameToken = tokens(2).asInstanceOf[NameToken]
            val argTokens = tail map (_.asInstanceOf[StringToken])

            val code = codeToken.value
            val name = nameToken.value
            val args = argTokens.map(_.value).toList

            TextCommand(code, name, args)
        catch
            case _: IllegalArgumentException => throw BadCommandException()


private sealed trait Token:
    val length: Int

private object Token:
    def tokenize(string: String): List[Token] =
        val tokens = ArrayBuffer[Token]()

        var i = 0

        while i < string.length do
            string charAt i match
                case char if char.isWhitespace => i += 1
                case _ =>
                    val token = parse(string substring i)

                    tokens addOne token

                    i += token.length

        tokens.toList

    def parse(string: String): Token =
        if string.isEmpty then
            throw IllegalArgumentException()

        val parse = string charAt 0 match
            case char if ShortToken startsWith char  => ShortToken.parse
            case char if StringToken startsWith char => StringToken.parse
            case char if NameToken startsWith char   => NameToken.parse
            case _                                   => throw IllegalArgumentException()

        parse(string)

    def goodBoundary(string: String, at: Int): Boolean =
        at >= string.length ||
        (string charAt at).isWhitespace


private case class ShortToken(
    value:  Short,
    length: Int,
) extends Token

private object ShortToken:
    def parse(string: String): ShortToken =
        val digits = string takeWhile (_.isDigit)

        if  digits.isEmpty || !Token.goodBoundary(string, digits.length) then
            throw IllegalArgumentException()
        
        val value = digits.map(_.asDigit)
                          .reduce(_ * 10 + _)

        ShortToken(value.toShort, digits.length)

    def startsWith(char: Char): Boolean =
        char.isDigit


private case class StringToken(
    value:  String,
    length: Int
) extends Token

private object StringToken:
    def parse(string: String): StringToken =
        if string.isEmpty || !startsWith(string charAt 0) then
            throw IllegalArgumentException()

        val buffer = ArrayBuffer[Char]()

        var bad = true
        var i   = 1

        breakable {
            while i < string.length do
                var char = string charAt i

                i += 1

                if char == '"' then
                    bad = false
                    break
                
                if char == '\\' then
                    if i < string.length then
                        char = string charAt i match
                            case 'b'  => '\b'
                            case 'f'  => '\f'
                            case 'r'  => '\r'
                            case 'n'  => '\n'
                            case 't'  => '\t'
                            case '\'' => '\''
                            case '"'  => '"'
                            case '\\' => '\\'
                            case char => char

                    i += 1

                buffer addOne char
        }

        if bad || !Token.goodBoundary(string, i) then
            throw IllegalArgumentException()

        StringToken(buffer.mkString, i)

    def startsWith(char: Char): Boolean =
        char == '"'


private case class NameToken(value: String) extends Token:
    val length = value.length

private object NameToken:
    def parse(string: String): NameToken =
        val name = string takeWhile (charPart(_))

        if name.isEmpty || !Token.goodBoundary(string, name.length) then
            throw IllegalArgumentException()
        
        NameToken(name)

    def startsWith(char: Char): Boolean =
        charPart(char)

    private def charPart(char: Char): Boolean =
        ('A' to 'Z' contains char) ||
        ('a' to 'z' contains char) ||
        char == '_'