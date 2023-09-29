package ru.fominmv.simplechat.core.util


trait Closeable:
    def closed: Boolean

    def close: Unit