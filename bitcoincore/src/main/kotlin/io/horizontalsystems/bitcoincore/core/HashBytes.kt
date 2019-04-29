package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.utils.Utils

/**
 * Just wraps a ByteArray so that equals and hashcode work correctly, allowing it to be
 * used as keys in a map
 */
class HashBytes(val bytes: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is HashBytes) return false

        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        // Use the first 4 bytes, not the last 4 which are often zeros (in little-endian)
        return Utils.intFromBytes(bytes[0], bytes[1], bytes[2], bytes[3])
    }

    override fun toString(): String {
        return bytes.toHexString()
    }
}
