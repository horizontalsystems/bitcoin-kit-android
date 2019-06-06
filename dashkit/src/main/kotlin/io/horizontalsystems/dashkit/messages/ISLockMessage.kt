package io.horizontalsystems.dashkit.messages

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.utils.HashUtils
import java.io.ByteArrayInputStream

class ISLockMessage(
        val inputs: List<Outpoint>,
        val txHash: ByteArray,
        val sign: ByteArray,
        val hash: ByteArray,
        val requestId: ByteArray
) : IMessage {

    override fun toString(): String {
        return "ISLockMessage(hash=${hash.toReversedHex()}, txHash=${txHash.toReversedHex()})"
    }
}

class ISLockMessageParser : IMessageParser {
    override val command: String = "islock"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { bitcoinInput ->
            val inputsSize = bitcoinInput.readVarInt()
            val inputs = List(inputsSize.toInt()) {
                Outpoint(bitcoinInput)
            }
            val txHash = bitcoinInput.readBytes(32)
            val sign = bitcoinInput.readBytes(96)

            val hash = HashUtils.doubleSha256(payload)

            val requestPayload = BitcoinOutput()
            requestPayload.writeString("islock")
            requestPayload.writeVarInt(inputsSize)
            inputs.forEach {
                requestPayload.write(it.txHash)
                requestPayload.writeUnsignedInt(it.vout)
            }

            val requestId = HashUtils.doubleSha256(requestPayload.toByteArray())

            return ISLockMessage(inputs, txHash, sign, hash, requestId)
        }
    }
}


