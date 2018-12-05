package io.horizontalsystems.bitcoinkit.models

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Transaction
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  4 bytes     Version         Transaction version
 *  VarInt      InputsCount     Number of inputs
 *  Variable    Inputs          Inputs
 *  VarInt      OutputsCount    Number of outputs
 *  Variable    Outputs         Outputs
 *  4 bytes     LockTime        Transaction lock time
 */
open class Transaction : RealmObject {
    // Transaction version
    var version: Int = 0

    // List of transaction inputs
    var inputs = RealmList<TransactionInput>()

    // List of transaction outputs
    var outputs = RealmList<TransactionOutput>()

    // Transaction lock time
    var lockTime: Long = 0

    @PrimaryKey
    var hashHexReversed = ""
    var hash: ByteArray = byteArrayOf()
    var status: Int = Status.RELAYED
    var block: Block? = null
    var isMine = false
    var isOutgoing = false
    var segwit = false

    constructor()

    constructor(version: Int, lockTime: Long) {
        this.version = version
        this.lockTime = lockTime
    }

    constructor(input: BitcoinInput) {
        version = input.readInt()

        val marker = 0xff and input.readUnsignedByte()
        val inputCount = if (marker == 0) {  // segwit marker: 0x00
            input.read()  // skip segwit flag: 0x01
            segwit = true
            input.readVarInt()
        } else {
            input.readVarInt(marker)
        }

        //  inputs
        for (i in 0 until inputCount) {
            inputs.add(TransactionInput(input))
        }

        //  outputs
        val outputCount = input.readVarInt()
        for (i in 0 until outputCount) {
            outputs.add(TransactionOutput(input, i))
        }

        //  extract witness data
        if (segwit) {
            inputs.forEach { it.storeWitness(input) }
        }

        lockTime = input.readUnsignedInt()

        setHashes()
    }

    fun toByteArray(): ByteArray {
        val buffer = BitcoinOutput()
        buffer.writeInt(version)

        if (segwit) {
            buffer.writeByte(0) // marker 0x00
            buffer.writeByte(1) // flag 0x01
        }

        // inputs
        buffer.writeVarInt(inputs.size.toLong())
        inputs.forEach { buffer.write(it.toByteArray()) }

        // outputs
        buffer.writeVarInt(outputs.size.toLong())
        outputs.forEach { buffer.write(it.toByteArray()) }

        //  serialize witness data
        if (segwit) {
            inputs.forEach { buffer.write(it.toByteArrayWitness()) }
        }

        buffer.writeUnsignedInt(lockTime)
        return buffer.toByteArray()
    }

    fun toSignatureByteArray(inputIndex: Int, isWitness: Boolean = false): ByteArray {
        val buffer = BitcoinOutput().writeInt(version)
        if (isWitness) {
            val outpoints = BitcoinOutput()
            val sequences = BitcoinOutput()

            for (input in inputs) {
                outpoints.write(input.toOutpointByteArray())
                sequences.writeInt32(input.sequence)
            }

            buffer.write(HashUtils.doubleSha256(outpoints.toByteArray())) // hash prevouts
            buffer.write(HashUtils.doubleSha256(sequences.toByteArray())) // hash sequence

            val inputToSign = inputs[inputIndex] ?: throw Exception("invalid input index")
            val previousOutput = checkNotNull(inputToSign.previousOutput) { throw Exception("no previous output") }

            buffer.write(inputToSign.toOutpointByteArray())
            buffer.write(OpCodes.push(OpCodes.p2pkhStart + OpCodes.push(previousOutput.keyHash!!) + OpCodes.p2pkhEnd))
            buffer.writeLong(previousOutput.value)
            buffer.writeInt32(inputToSign.sequence)

            val hashOutputs = BitcoinOutput()
            for (output in outputs) {
                hashOutputs.write(output.toByteArray())
            }

            buffer.write(HashUtils.doubleSha256(hashOutputs.toByteArray()))
        } else {
            // inputs
            buffer.writeVarInt(inputs.size.toLong())
            inputs.forEachIndexed { index, input ->
                buffer.write(input.toSignatureByteArray(index == inputIndex))
            }

            // outputs
            buffer.writeVarInt(outputs.size.toLong())
            outputs.forEach { buffer.write(it.toByteArray()) }
        }

        buffer.writeUnsignedInt(lockTime)
        return buffer.toByteArray()
    }

    fun setHashes() {
        hash = HashUtils.doubleSha256(toByteArray())
        hashHexReversed = HashUtils.toHexStringAsLE(hash)
    }

    object Status {
        const val NEW = 1
        const val RELAYED = 2
        const val INVALID = 3
    }
}
