package ru.fominmv.simplechat.core.cli


case class Command(
    val name:        String,
    val args:        List[String]                 = List(),
    val description: Option[String]               = None,
    val action:      (args: List[String]) => Unit = (args) => (),
)