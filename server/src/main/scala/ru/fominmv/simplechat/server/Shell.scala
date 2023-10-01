package ru.fominmv.simplechat.server


import ru.fominmv.simplechat.core.cli.{
    Command,
    Shell => ShellTrait,
}


class Shell(val server: Server) extends ShellTrait:
    override def closed: Boolean =
        !open

    override def close: Unit =
        server.close
        open = false

    override def commands: Set[Command] =
        COMMANDS


    override protected def greeting: Unit =
        console print s"To quit press enter /${STOP_COMMAND.name} Ctrl-C"
        console print s"To see full command list enter /${HELP_COMMAND.name}"

    override protected def onNonCommandInput(input: String): Unit =
        if input.isBlank then
            return

        server broadcastMessage input
        console print s"${server.name}: ${input}"


    private var open = true

    private val STOP_COMMAND = Command(
        name        = "stop",
        description = Some("Stops shell and server"),
        action      = _ => close
    )

    private val HELP_COMMAND = Command(
        name        = "help",
        description = Some("Prints help message"),
        action      = _ =>
            console print "Usage:"
            console print "    /<command> - executes specified command"
            console print "    <message>  - broadcasts entered message"
            console print ""
            console print "Commands:"
    
            for command <- commands do
                val usage             = (commandUsageMap get command.name).get
                val descriptionIndent = " " * (commandDescriptionIndent - usage.length + 1)
                val description       = command.description getOrElse ""
                val message           = s"    $usage$descriptionIndent$description"

                console print message
    )

    private val KICK_COMMAND = Command(
        name        = "kick",
        description = Some("Kicks specified user"),
        args        = List("name or #id"),
        action      = args =>
            val targetClientNameOrId = args(0)
            val clients              = server.clients
            val clientOption         = if targetClientNameOrId startsWith "#" then
                try
                    val targetClientId = Integer parseInt (targetClientNameOrId substring 1)

                    clients find (_.id == targetClientId)
                catch
                    case _: NumberFormatException => throw IllegalArgumentException("Bad client id")
            else
                val lowerTargetClientName = targetClientNameOrId.toLowerCase

                clients find (
                    _.name
                    .map(_.toLowerCase)
                    .getOrElse(null) == lowerTargetClientName
                )

            if clientOption == None then
                throw IllegalArgumentException(s"User ${targetClientNameOrId} not found")

            clientOption.get.close
    )

    private val KICK_ALL_COMMAND = Command(
        name        = "kick-all",
        description = Some("Kicks all the users"),
        action      = _ =>
            server.clients foreach (_.close)
    )

    private val LIST_COMMAND = Command(
        name        = "list",
        description = Some("Prints user list"),
        action      = _ =>
            if server.clients.isEmpty then
                console print "There is no clients"
            else
                console print "Clients:"

                server.clients foreach (client =>
                    console print s"    ${client.fullname}"
                )
    )

    private val COMMANDS = Set(
        STOP_COMMAND,
        HELP_COMMAND,
        KICK_COMMAND,
        KICK_ALL_COMMAND,
        LIST_COMMAND,
    )

    private lazy val commandDescriptionIndent: Int =
        commandUsageMap.map(_._2.length).max

    private lazy val commandUsageMap: Map[String, String] =
        val builder = Map.newBuilder[String, String]

        for command <- COMMANDS do
            builder addOne (command.name, formatCommandUsage(command))

        builder.result

    
    private def formatCommandUsage(command: Command): String =
        if command.args.isEmpty then
            s"${command.name}"
        else
            s"${command.name} ${command.args.map("<" + _ + ">").mkString(" ")}"