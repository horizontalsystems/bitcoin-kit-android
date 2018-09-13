package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.utils.BytesUtils
import io.realm.RealmObject
import java.io.IOException

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

    var previousOutput: OutPoint? = null

    var sigScript: ByteArray = byteArrayOf()

    /**
     * uint32, Transaction version as defined by the sender. Intended for
     * "replacement" of transactions when information is updated before
     * inclusion into a block.
     */
    var sequence: Long = 0

    val isCoinbase: Boolean
        get() = (previousOutput?.hash != null && BytesUtils.isZeros(previousOutput?.hash))

    constructor()

    @Throws(IOException::class)
    constructor(input: BitcoinInput) {
        this.previousOutput = OutPoint(input)
        val sigScriptLength = input.readVarInt()
        sigScript = input.readBytes(sigScriptLength.toInt())
        sequence = input.readUnsignedInt()
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .write(previousOutput?.toByteArray())
                .writeVarInt(sigScript.size.toLong())
                .write(sigScript)
                .writeUnsignedInt(sequence)
                .toByteArray()
    }

    fun validate() {
        // throw new ValidateException("Verify signature failed.");
    }
}