package ru.fominmv.simplechat.core.cli.scopt


import scopt.Read

import ru.fominmv.simplechat.core.protocol.Protocol
import ru.fominmv.simplechat.core.protocol.text.TextProtocol
import ru.fominmv.simplechat.core.protocol.binary.BinaryProtocol


implicit val protocolRead: Read[Protocol] =
    Read reads (
        _ match
            case "text"   => TextProtocol()
            case "binary" => BinaryProtocol()
    )