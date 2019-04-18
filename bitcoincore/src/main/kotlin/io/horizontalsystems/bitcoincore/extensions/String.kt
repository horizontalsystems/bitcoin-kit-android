package io.horizontalsystems.bitcoincore.extensions

fun String.hexToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

fun String.toReversedByteArray(): ByteArray {
    return hexToByteArray().reversedArray()
}

