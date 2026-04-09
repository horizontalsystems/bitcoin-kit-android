package io.horizontalsystems.litecoinkit.mweb.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.network.messages.IMessageSerializer

/**
 * P2P message carrying a raw MWEB transaction.
 *
 * MWEB-only transactions are broadcast as standard Litecoin "tx" messages where the
 * canonical transaction body is empty (0 inputs, 0 outputs) and the MWEB extension
 * block data is carried in the segwit witness area.
 *
 * Wire layout of the wrapped tx:
 *   nVersion(4 LE) | marker(0x00) | flag(0x01) |
 *   varint(0) inputs | varint(0) outputs |
 *   varint(1) witness items | varint(mwebSize) | mwebTxBytes |
 *   nLocktime(4 LE)
 *
 * NOTE: This wrapping format must be verified against src/net_processing.cpp if test
 * transactions are rejected. An alternative is that Litecoin peers expect a separate
 * "mwebtx" command or MSG_MWEB_TX inv/getdata exchange.
 */
class MwebTransactionMessage(val mwebTxBytes: ByteArray) : IMessage {
    override fun toString() = "MwebTransactionMessage(${mwebTxBytes.size} bytes)"
}

class MwebTransactionMessageSerializer : IMessageSerializer {
    override val command = "tx"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is MwebTransactionMessage) return null
        return wrapMwebTx(message.mwebTxBytes)
    }

    private fun wrapMwebTx(mwebTxBytes: ByteArray): ByteArray = BitcoinOutput()
        .writeInt(2)                                    // nVersion = 2
        .writeByte(0x00)                                // segwit marker
        .writeByte(0x01)                                // segwit flag
        .writeVarInt(0L)                                // 0 inputs
        .writeVarInt(0L)                                // 0 outputs
        .writeVarInt(1L)                                // 1 witness item (MWEB extension)
        .writeVarInt(mwebTxBytes.size.toLong())
        .write(mwebTxBytes)
        .writeInt(0)                                    // nLocktime = 0
        .toByteArray()
}

class MwebTransactionMessageParser : IMessageParser {
    override val command = "tx"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
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
