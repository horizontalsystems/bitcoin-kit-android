package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.utils.Utils

/**
 * Just wraps a ByteArray so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map
 */
class HashBytes(val bytes: ByteArray) : Comparable<HashBytes> {
    private val hashLength = 32

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is HashBytes) return false

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        // Use the first 4 bytes, not the last 4 which are often zeros (in little-endian)
        return Utils.intFromBytes(bytes[0], bytes[1], bytes[2], bytes[3])
    }

    override fun compareTo(other: HashBytes): Int {
        for (i in hashLength - 1 downTo 0) {
            val otherByte = other.bytes[i].toInt() and 0xff
            val thisByte = bytes[i].toInt() and 0xff
            if (thisByte > otherByte)
                return 1
            if (thisByte < otherByte)
                return -1
        }

        return 0
    }

    override fun toString(): String {
        return bytes.toHexString()
    }
}
