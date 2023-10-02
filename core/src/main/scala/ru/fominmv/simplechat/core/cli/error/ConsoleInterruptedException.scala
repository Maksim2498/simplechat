package ru.fominmv.simplechat.core.cli.error


case class ConsoleInterruptedException(
    val partialInput: String = "",
) extends InterruptedException