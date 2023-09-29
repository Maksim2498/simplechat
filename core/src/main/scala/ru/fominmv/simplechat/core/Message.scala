package ru.fominmv.simplechat.core


import ru.fominmv.simplechat.core.util.StringExtension.{escape}


final case class Message(
    val author: String,
    val text:   String,
):
    override def toString: String =
        s"\"${author.escape}\": \"${text.escape}\""