package bitcoin.wallet.kit.core

// https://gist.github.com/fabiomsr/845664a9c7e92bafb6fb0ca70d4e44fd
fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

@Throws(NumberFormatException::class)
fun String.hexStringToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}
