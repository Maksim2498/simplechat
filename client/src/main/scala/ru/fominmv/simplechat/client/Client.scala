package ru.fominmv.simplechat.client


import ru.fominmv.simplechat.core.util.Closeable


class Client(
    val username: String,
) extends Closeable:
    override def closed: Boolean = ???

    override def close: Unit = ???