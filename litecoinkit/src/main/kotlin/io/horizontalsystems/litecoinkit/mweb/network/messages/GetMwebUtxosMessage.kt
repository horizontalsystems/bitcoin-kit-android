package io.horizontalsystems.litecoinkit.mweb.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.network.messages.IMessageSerializer

/**
 * `getmwebutxos` request – asks a peer for MWEB UTXOs starting at [startIndex].
 *
 * Wire format:
 *   block_hash  : 32 bytes  (anchor block)
 *   start_index : varint    (first global leaf index to fetch)
 *   num_utxos   : varint    (how many to return; 0 = all from startIndex)
 */
class GetMwebUtxosMessage(
    val blockHash: ByteArray,
    val startIndex: Long,
    val numUtxos: Long = 0L
) : IMessage

class GetMwebUtxosMessageParser : IMessageParser {
    override val command = "getmwebutxos"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val blockHash = input.readBytes(32)
        val startIndex = input.readVarInt()
        val numUtxos = input.readVarInt()
        return GetMwebUtxosMessage(blockHash, startIndex, numUtxos)
    }
}

class GetMwebUtxosMessageSerializer : IMessageSerializer {
    override val command = "getmwebutxos"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is GetMwebUtxosMessage) return null
        return BitcoinOutput()
            .write(message.blockHash)
            .writeVarInt(message.startIndex)
            .writeVarInt(message.numUtxos)
            .toByteArray()
    }
}