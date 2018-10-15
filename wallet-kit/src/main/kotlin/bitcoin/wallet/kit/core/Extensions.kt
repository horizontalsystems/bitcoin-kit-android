package bitcoin.wallet.kit.core

import bitcoin.wallet.kit.hdwallet.PublicKey
import io.horizontalsystems.hdwalletkit.HDPublicKey
import io.horizontalsystems.hdwalletkit.HDWallet

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

fun HDWallet.publicKey(index: Int, external: Boolean) = publicKey(index, hdPublicKey(index, external))

fun HDWallet.receivePublicKey(index: Int) = publicKey(index, hdPublicKey(index, true))

fun HDWallet.changePublicKey(index: Int) = publicKey(index, hdPublicKey(index, false))

private fun publicKey(index: Int, hdPubKey: HDPublicKey): PublicKey = PublicKey(index, hdPubKey.external, hdPubKey.publicKey, hdPubKey.publicKeyHash)
