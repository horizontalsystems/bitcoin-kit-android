package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.utils.HashUtils
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Ignore
import io.realm.annotations.PrimaryKey
import java.io.IOException

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

    // Get transaction hash (actually calculate the hash of transaction data).
    @delegate:Ignore
    val txHash: ByteArray by lazy {
        HashUtils.doubleSha256(toByteArray())
    }

    var processed: Boolean = false

    var block: Block? = null

    @PrimaryKey
    var reversedHashHex = ""

    object Status {
        const val NEW = 1
        const val RELAYED = 2
        const val INVALID = 3
    }

    var status: Int = Status.RELAYED

    var isMine = false

    constructor()

    @Throws(IOException::class)
    constructor(input: BitcoinInput) {
        version = input.readInt()

        val inputCount = input.readVarInt() // do not store count
        repeat(inputCount.toInt()) {
            inputs.add(TransactionInput(input))
        }

        val outputCount = input.readVarInt() // do not store count
        repeat(outputCount.toInt()) {
            outputs.add(TransactionOutput(input))
        }

        lockTime = input.readUnsignedInt()
    }

    fun toByteArray(): ByteArray {
        val buffer = BitcoinOutput()
        buffer.writeInt(version).writeVarInt(inputs.size.toLong())
        for (i in inputs.indices) {
            buffer.write(inputs[i]?.toByteArray())
        }
        buffer.writeVarInt(outputs.size.toLong())
        for (i in outputs.indices) {
            buffer.write(outputs[i]?.toByteArray())
        }
        buffer.writeUnsignedInt(lockTime)
        return buffer.toByteArray()
    }

    fun getInputCount(): Long {
        return inputs.size.toLong()
    }

    fun getOutputCount(): Long {
        return outputs.size.toLong()
    }

    // http://bitcoin.stackexchange.com/questions/3374/how-to-redeem-a-basic-tx
    fun validate() {
        val output = BitcoinOutput()
        output.writeInt(version)
        // TODO: implement
    }
}
