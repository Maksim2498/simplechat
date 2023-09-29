package ru.fominmv.simplechat.core.util


import java.io.{OutputStream, ByteArrayOutputStream}
import java.nio.charset.{Charset, StandardCharsets}


trait ToBytesConvertible:
    def toBytes: Array[Byte] = toBytes()

    def toBytes(charset: Charset = StandardCharsets.UTF_8): Array[Byte] =
        val stream = new ByteArrayOutputStream()

        write(stream, charset)

        stream.toByteArray

    def write(stream: OutputStream, charset: Charset = StandardCharsets.UTF_8): Unit