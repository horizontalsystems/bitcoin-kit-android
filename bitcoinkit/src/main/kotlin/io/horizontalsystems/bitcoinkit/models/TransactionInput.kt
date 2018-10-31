package io.horizontalsystems.bitcoinkit.models

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects

/**
 * Transaction input
 *
 *  Size        Field                Description
 *  ===         =====                ===========
 *  32 bytes    OutputHash           Double SHA-256 hash of the transaction containing the output to be used by this input
 *  4 bytes     OutputIndex          Index of the output within the transaction
 *  VarInt      InputScriptLength    Script length
 *  Variable    InputScript          Script
 *  4 bytes     InputSeqNumber       Input sequence number (irrelevant unless transaction LockTime is non-zero)
 */
open class TransactionInput : RealmObject {
    // The hash of the referenced transaction
    var previousOutputHash: ByteArray = byteArrayOf()

    // The index of the specific output in the transaction
    var previousOutputIndex: Long = 0

    // Input script
    var sigScript: ByteArray = byteArrayOf()

    // Input sequence number
    var sequence: Long = 0

    // Internal fields
    var previousOutput: TransactionOutput? = null
    var previousOutputHexReversed = ""
    var keyHash: ByteArray? = null
    var address: String? = ""
    var witness = RealmList<ByteArray>()

    @LinkingObjects("inputs")
    val transactions: RealmResults<Transaction>? = null
    val transaction: Transaction?
        get() = transactions?.first()

    constructor()
    constructor(input: BitcoinInput) {
        previousOutputHash = input.readBytes(32)
        previousOutputHexReversed = HashUtils.toHexStringAsLE(previousOutputHash)
        previousOutputIndex = input.readUnsignedInt()
        val sigScriptLength = input.readVarInt()
        sigScript = input.readBytes(sigScriptLength.toInt())
        sequence = input.readUnsignedInt()
    }

    fun storeWitness(input: BitcoinInput) {
        val stackSize = input.readVarInt()

        for (i in 0 until stackSize) {
            val dataSize = input.readVarInt()
            val data = input.readBytes(dataSize.toInt())
            witness.add(data)
        }
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .write(previousOutputHash)
                .writeUnsignedInt(previousOutputIndex)
                .writeVarInt(sigScript.size.toLong())
                .write(sigScript)
                .writeUnsignedInt(sequence)
                .toByteArray()
    }

    fun toByteArrayWitness(): ByteArray {
        val output = BitcoinOutput()
                .writeVarInt(witness.size.toLong())

        witness.forEach { data ->
            output.writeVarInt(data.size.toLong())
            output.write(data)
        }

        return output.toByteArray()
    }

    fun toSignatureByteArray(forCurrentInputSignature: Boolean): ByteArray {
        val output = BitcoinOutput()
                .write(previousOutputHash)
                .writeUnsignedInt(previousOutputIndex)

        if (forCurrentInputSignature) {
            val prevOutput = checkNotNull(previousOutput) {
                throw IllegalStateException("No previous output")
            }

            output.writeVarInt(prevOutput.lockingScript.size.toLong())
                  .write(prevOutput.lockingScript)
        } else {
            output.writeVarInt(0L)
        }

        return output.writeUnsignedInt(sequence).toByteArray()
    }

}
