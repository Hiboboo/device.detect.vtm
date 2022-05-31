package com.jinkeen.vtm.detect.util

import com.jinkeen.base.util.toHexStr

fun ByteArray.toHexString(dec: Int, length: Int): String {
    val temp = ByteArray(length)
    System.arraycopy(this, dec, temp, 0, length)
    return temp.toHexStr()
}