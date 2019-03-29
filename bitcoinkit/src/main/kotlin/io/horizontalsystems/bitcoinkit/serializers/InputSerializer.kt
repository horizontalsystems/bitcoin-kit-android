package io.horizontalsystems.bitcoinkit.serializers

import io.horizontalsystems.bitcoinkit.extensions.toReversedByteArray
import io.horizontalsystems.bitcoinkit.extensions.toReversedHex
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.models.TransactionInput
import io.horizontalsystems.bitcoinkit.storage.InputToSign

object InputSerializer {

    fun serialize(input: TransactionInput): ByteArray {
        return BitcoinOutput()
                .write(input.previousOutputTxReversedHex.toReversedByteArray())
                .writeUnsignedInt(input.previousOutputIndex)
                .writeVarInt(input.sigScript.size.toLong())
                .write(input.sigScript)
                .writeUnsignedInt(input.sequence)
                .toByteArray()
    }

    fun deserialize(input: BitcoinInput): TransactionInput {
        val previousOutputHash = input.readBytes(32)
        val previousOutputTxReversedHex = previousOutputHash.toReversedHex()
        val previousOutputIndex = input.readUnsignedInt()
        val sigScriptLength = input.readVarInt()
        val sigScript = input.readBytes(sigScriptLength.toInt())
        val sequence = input.readUnsignedInt()

        return TransactionInput(previousOutputTxReversedHex, previousOutputIndex, sigScript, sequence)
    }

    fun serializeOutpoint(input: InputToSign): ByteArray {
        return BitcoinOutput()
                .write(input.previousOutput.transactionHashReversedHex.toReversedByteArray())
                .writeInt(input.previousOutput.index)
                .toByteArray()
    }

    fun serializeForSignature(inputToSign: InputToSign, forCurrentInputSignature: Boolean): ByteArray {
        val previousOutput = inputToSign.previousOutput
        val output = BitcoinOutput()
                .write(previousOutput.transactionHashReversedHex.toReversedByteArray())
                .writeUnsignedInt(previousOutput.index.toLong())

        if (forCurrentInputSignature) {
            output.writeVarInt(previousOutput.lockingScript.size.toLong())
                    .write(previousOutput.lockingScript)
        } else {
            output.writeVarInt(0L)
        }

        return output.writeUnsignedInt(inputToSign.input.sequence).toByteArray()
    }

    fun serializeWitness(witness: List<ByteArray>): ByteArray {
        val output = BitcoinOutput()
                .writeVarInt(witness.size.toLong())

        witness.forEach { data ->
            output.writeVarInt(data.size.toLong())
            output.write(data)
        }

        return output.toByteArray()
    }

    fun deserializeWitness(input: BitcoinInput): MutableList<ByteArray> {
        val stackSize = input.readVarInt()
        val witnessData = mutableListOf<ByteArray>()

        for (i in 0 until stackSize) {
            val dataSize = input.readVarInt()
            val data = input.readBytes(dataSize.toInt())
            witnessData.add(data)
        }

        return witnessData
    }
}
