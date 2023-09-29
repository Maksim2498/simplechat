package ru.fominmv.simplechat.core.util


object UnsignedUtil:
    val UBYTE_MAX:  Int  = 256
    val USHORT_MAX: Int  = 65_535
    val UINT_MAX:   Long = 4_294_967_295L

    def byteToUnsigned(byte: Byte): Int = byte & 0xFF

    def shortToUnsigned(short: Short): Int = short & 0xFF_FF

    def intToUnsigned(int: Int): Long = int & 0xFF_FF_FF_FF