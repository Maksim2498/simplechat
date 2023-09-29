package ru.fominmv.simplechat.core


object DefaultNameValidator extends NameValidator:
    override def nameGood(name: String): Boolean =
        name matches "[A-Za-z_-][A-Za-z0-9_-]{0,63}"