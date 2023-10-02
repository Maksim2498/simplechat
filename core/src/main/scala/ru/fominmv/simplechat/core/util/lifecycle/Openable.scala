package ru.fominmv.simplechat.core.util.lifecycle


trait Openable extends Closeable:
    def open: Unit