package ru.fominmv.simplechat.core.util


import scala.collection.mutable.ArrayBuffer
import scala.annotation.switch


object StringExtension:
    extension (string: String)
        def escape: String =
            string.map(
                (_: @switch) match
                    case '\b' => "\\b"
                    case '\f' => "\\f"
                    case '\r' => "\\r"
                    case '\n' => "\\n"
                    case '\t' => "\\t"
                    case '\'' => "\\'"
                    case '"'  => "\\\""
                    case '\\' => "\\\\"
                    case char => char.toString
            ).mkString

        def unescape: String =
            val buffer = ArrayBuffer[Char]()

            var i = 0

            while i < string.length do
                var char = string charAt i

                i += 1

                if char == '\\' then
                    if i < string.length then
                        char = (string charAt i: @switch) match
                            case 'b'  => '\b'
                            case 'f'  => '\f'
                            case 'r'  => '\r'
                            case 'n'  => '\n'
                            case 't'  => '\t'
                            case '\'' => '\''
                            case '"'  => '"'
                            case '\\' => '\\'
                            case char => char

                        i += 1

                buffer addOne char

            buffer.mkString