package ru.fominmv.simplechat.server


import java.net.InetAddress

import ru.fominmv.simplechat.core.error.ClosedException
import ru.fominmv.simplechat.core.util.lifecycle.Closeable
import ru.fominmv.simplechat.core.Message


trait Client extends Closeable:
    def id:      Int
    def address: InetAddress
    def name:    Option[String]
    def server:  Server

    @throws[ClosedException]("When closed")
    def ping: Unit

    @throws[ClosedException]("When closed")
    def sendMessage(message: Message): Unit

    def shortname: String = Client.shortname(id, name)
    def fullname:  String = Client.fullname(id, name)

    def makeSnapshot: Client =
        val _this       = this
        val currentName = name

        new Client:
            val id      = _this.id
            val address = _this.address
            val name    = currentName
            val server  = _this.server

            def ping = _this.ping
            def sendMessage(message: Message) = _this sendMessage message

            def close = _this.close
            def closed = _this.closed

object Client:
    def fullname(id: Int, name: Option[String]): String =
        s"${name getOrElse ""}#$id"

    def shortname(id: Int, name: Option[String]): String =
        name getOrElse s"#$id"