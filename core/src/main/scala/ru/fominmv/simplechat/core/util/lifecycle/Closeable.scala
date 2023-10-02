package ru.fominmv.simplechat.core.util.lifecycle


trait Closeable:
    def closed: Boolean
    def close: Unit