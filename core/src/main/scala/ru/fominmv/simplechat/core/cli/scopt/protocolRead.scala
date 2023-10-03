package ru.fominmv.simplechat.core.cli.scopt


import scopt.Read

import ru.fominmv.simplechat.core.protocol.binary.BinaryProtocol
import ru.fominmv.simplechat.core.protocol.text.TextProtocol
import ru.fominmv.simplechat.core.protocol.Protocol


implicit val protocolRead: Read[Protocol] =
    Read reads (string =>
        val lowerString = string.toLowerCase

        if "text" startsWith lowerString then
            TextProtocol()
        else if "binary" startsWith lowerString then
            BinaryProtocol()
        else
            throw IllegalArgumentException("Bad protocol string")
    )