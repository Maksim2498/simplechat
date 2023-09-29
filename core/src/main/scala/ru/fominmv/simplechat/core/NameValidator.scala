package ru.fominmv.simplechat.core


trait NameValidator:
    def nameGood(name: String): Boolean = true