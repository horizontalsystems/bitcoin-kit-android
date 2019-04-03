package io.horizontalsystems.bitcoinkit.dash.messages

import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.network.messages.Message
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import java.io.ByteArrayInputStream

class TransactionLockVoteMessage(payload: ByteArray) : Message("txlvote") {

    var txHash: ByteArray
    var outpoint: Outpoint
    var outpointMasternode: Outpoint
    var quorumModifierHash: ByteArray
    var masternodeProTxHash: ByteArray
    var vchMasternodeSignature: ByteArray
    var hash: ByteArray

    init {
        val bitcoinInput = BitcoinInput(ByteArrayInputStream(payload))

        txHash = bitcoinInput.readBytes(32)
        outpoint = Outpoint(bitcoinInput)
        outpointMasternode = Outpoint(bitcoinInput)
        quorumModifierHash = bitcoinInput.readBytes(32)
        masternodeProTxHash = bitcoinInput.readBytes(32)
        vchMasternodeSignature = bitcoinInput.readBytes(96)

        val hashPayload = BitcoinOutput()
                .write(txHash)
                .write(outpoint.txHash)
                .writeUnsignedInt(outpoint.vout)
                .write(outpointMasternode.txHash)
                .writeUnsignedInt(outpointMasternode.vout)
                .write(quorumModifierHash)
                .write(masternodeProTxHash)
                .toByteArray()

        hash = HashUtils.doubleSha256(hashPayload)

        bitcoinInput.close()
    }

    override fun getPayload(): ByteArray {
        TODO()
    }

    override fun toString(): String {
        return "TransactionLockVoteMessage(hash=${hash.reversedArray().toHexString()}, txHash=${txHash.reversedArray().toHexString()})"
    }

}

class Outpoint(input: BitcoinInput) {
    val txHash = input.readBytes(32)
    val vout = input.readUnsignedInt()
}