package io.horizontalsystems.bitcoinkit.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Ping Message
 *
 *  Size        Field   Description
 *  ====        =====   ===========
 *  8 bytes     Nonce   Random value
 */
class PingMessage : Message {

    var nonce: Long = 0
        internal set

    constructor(nonce: Long) : super("ping") {
        this.nonce = nonce
    }

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("ping") {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            nonce = input.readLong()
        }
    }

    override fun getPayload(): ByteArray {
        return BitcoinOutput().writeLong(nonce).toByteArray()
    }

    override fun toString(): String {
        return "PingMessage(nonce=$nonce)"
    }
}
