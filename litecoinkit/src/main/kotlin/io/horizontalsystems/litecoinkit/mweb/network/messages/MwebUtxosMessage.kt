package io.horizontalsystems.litecoinkit.mweb.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.network.messages.IMessageSerializer

/**
 * Raw MWEB output as parsed from the P2P `mwebutxos` message.
 *
 * Wire layout per output (LIP-0003):
 *   leaf_index     : varint
 *   commitment     : 33 bytes  (Pedersen commitment)
 *   sender_pubkey  : 33 bytes  (Ke – ephemeral key)
 *   receiver_pubkey: 33 bytes  (Ko – one-time key)
 *   features       : 1 byte
 *   masked_value   : 8 bytes
 *   masked_nonce   : 4 bytes
 *   proof_size     : varint
 *   range_proof    : proof_size bytes
 */
data class MwebRawOutput(
    val leafIndex: Long,
    val commitment: ByteArray,       // 33 bytes
    val senderPubKey: ByteArray,     // 33 bytes (Ke)
    val receiverPubKey: ByteArray,   // 33 bytes (Ko)
    val features: Byte,
    val maskedValue: ByteArray,      // 8 bytes
    val maskedNonce: ByteArray,      // 4 bytes
    val rangeProof: ByteArray        // variable
)

// ---------------------------------------------------------------------------

class MwebUtxosMessage(
    val blockHash: ByteArray,
    val outputs: List<MwebRawOutput>
) : IMessage {
    override fun toString() = "MwebUtxosMessage(block=${blockHash.toHex()}, count=${outputs.size})"
}

class MwebUtxosMessageParser : IMessageParser {
    override val command = "mwebutxos"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val blockHash = input.readBytes(32)
        val count = input.readVarInt().toInt()
        val outputs = (0 until count).map {
            val leafIndex = input.readVarInt()
            val commitment = input.readBytes(33)
            val senderPubKey = input.readBytes(33)
            val receiverPubKey = input.readBytes(33)
            val features = input.readByte()
            val maskedValue = input.readBytes(8)
            val maskedNonce = input.readBytes(4)
            val proofSize = input.readVarInt().toInt()
            val rangeProof = if (proofSize > 0) input.readBytes(proofSize) else byteArrayOf()
            MwebRawOutput(leafIndex, commitment, senderPubKey, receiverPubKey, features, maskedValue, maskedNonce, rangeProof)
        }
        return MwebUtxosMessage(blockHash, outputs)
    }
}

class MwebUtxosMessageSerializer : IMessageSerializer {
    override val command = "mwebutxos"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is MwebUtxosMessage) return null
        val out = BitcoinOutput()
        out.write(message.blockHash)
        out.writeVarInt(message.outputs.size.toLong())
        for (o in message.outputs) {
            out.writeVarInt(o.leafIndex)
            out.write(o.commitment)
            out.write(o.senderPubKey)
            out.write(o.receiverPubKey)
            out.write(byteArrayOf(o.features))
            out.write(o.maskedValue)
            out.write(o.maskedNonce)
            out.writeVarInt(o.rangeProof.size.toLong())
            if (o.rangeProof.isNotEmpty()) out.write(o.rangeProof)
        }
        return out.toByteArray()
    }
}

private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }