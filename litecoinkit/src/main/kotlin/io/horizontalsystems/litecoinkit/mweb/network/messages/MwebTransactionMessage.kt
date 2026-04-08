package io.horizontalsystems.litecoinkit.mweb.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.network.messages.IMessageSerializer

/**
 * P2P message carrying a raw MWEB transaction payload.
 *
 * Command: "mwebtx"
 *
 * CAVEAT: The command string "mwebtx" is unconfirmed. Verify against
 * litecoin-project/litecoin src/net_processing.cpp before production deployment.
 * An alternative is that the MWEB data travels inside a standard "tx" message
 * appended as a witness-like extension.
 */
class MwebTransactionMessage(val mwebTxBytes: ByteArray) : IMessage {
    override fun toString() = "MwebTransactionMessage(${mwebTxBytes.size} bytes)"
}

class MwebTransactionMessageSerializer : IMessageSerializer {
    override val command = "mwebtx"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is MwebTransactionMessage) return null
        return message.mwebTxBytes
    }
}

class MwebTransactionMessageParser : IMessageParser {
    override val command = "mwebtx"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        // The MWEB transaction payload is all remaining bytes in this message frame.
        // Read until the stream is exhausted.
        val buffer = mutableListOf<Byte>()
        try {
            while (true) {
                val b = input.read()
                if (b == -1) break
                buffer.add(b.toByte())
            }
        } catch (_: Exception) { /* stream ended */ }
        return MwebTransactionMessage(buffer.toByteArray())
    }
}
