package io.horizontalsystems.bitcoincore.extensions

fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

fun ByteArray.toReversedHex(): String {
    return reversedArray().toHexString()
}
