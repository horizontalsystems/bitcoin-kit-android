package io.horizontalsystems.bitcoincore.serializers

import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.storage.InputToSign

object InputSerializer {

    fun serialize(input: TransactionInput): ByteArray {
        return BitcoinOutput()
                .write(input.previousOutputTxHash)
                .writeUnsignedInt(input.previousOutputIndex)
                .writeVarInt(input.sigScript.size.toLong())
                .write(input.sigScript)
                .writeUnsignedInt(input.sequence)
                .toByteArray()
    }

    fun deserialize(input: BitcoinInput): TransactionInput {
        val previousOutputHash = input.readBytes(32)
        val previousOutputIndex = input.readUnsignedInt()
        val sigScriptLength = input.readVarInt()
        val sigScript = input.readBytes(sigScriptLength.toInt())
        val sequence = input.readUnsignedInt()

        return TransactionInput(previousOutputHash, previousOutputIndex, sigScript, sequence)
    }

    fun serializeOutpoint(input: InputToSign): ByteArray {
        return BitcoinOutput()
                .write(input.previousOutput.transactionHash)
                .writeInt(input.previousOutput.index)
                .toByteArray()
    }

    fun serializeForSignature(inputToSign: InputToSign, forCurrentInputSignature: Boolean): ByteArray {
        val previousOutput = inputToSign.previousOutput
        val output = BitcoinOutput()
                .write(previousOutput.transactionHash)
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
