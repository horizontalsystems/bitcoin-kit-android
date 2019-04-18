package io.horizontalsystems.bitcoinkit.dash.messages

import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.network.messages.IMessageParser
import io.horizontalsystems.bitcoinkit.network.messages.IMessage
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import java.io.ByteArrayInputStream

class TransactionLockVoteMessage(
        var txHash: ByteArray,
        var outpoint: Outpoint,
        var outpointMasternode: Outpoint,
        var quorumModifierHash: ByteArray,
        var masternodeProTxHash: ByteArray,
        var vchMasternodeSignature: ByteArray,
        var hash: ByteArray
) : IMessage {
    override val command: String = "txlvote"

    override fun toString(): String {
        return "TransactionLockVoteMessage(hash=${hash.reversedArray().toHexString()}, txHash=${txHash.reversedArray().toHexString()})"
    }
}

class Outpoint(input: BitcoinInput) {
    val txHash = input.readBytes(32)
    val vout = input.readUnsignedInt()
}

class TransactionLockVoteMessageParser : IMessageParser {
    override val command: String = "txlvote"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { bitcoinInput ->
            val txHash = bitcoinInput.readBytes(32)
            val outpoint = Outpoint(bitcoinInput)
            val outpointMasternode = Outpoint(bitcoinInput)
            val quorumModifierHash = bitcoinInput.readBytes(32)
            val masternodeProTxHash = bitcoinInput.readBytes(32)
            val vchMasternodeSignature = bitcoinInput.readBytes(96)

            val hashPayload = BitcoinOutput()
                    .write(txHash)
                    .write(outpoint.txHash)
                    .writeUnsignedInt(outpoint.vout)
                    .write(outpointMasternode.txHash)
                    .writeUnsignedInt(outpointMasternode.vout)
                    .write(quorumModifierHash)
                    .write(masternodeProTxHash)
                    .toByteArray()

            val hash = HashUtils.doubleSha256(hashPayload)

            return TransactionLockVoteMessage(txHash, outpoint, outpointMasternode, quorumModifierHash, masternodeProTxHash, vchMasternodeSignature, hash)
        }
    }
}
