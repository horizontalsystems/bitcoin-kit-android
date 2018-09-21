package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
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
    // The hash of the referenced transaction
    var previousOutputTxHash: ByteArray = byteArrayOf()

    // The index of the specific output in the transaction
    var previousOutputIndex: Long = 0

    // Input script
    var sigScript: ByteArray = byteArrayOf()

    // Input sequence number
    var sequence: Long = 0

    var keyHash: ByteArray? = null
    var address: String? = ""

    constructor()

    @Throws(IOException::class)
    constructor(input: BitcoinInput) {
        previousOutputTxHash = input.readBytes(32)
        previousOutputIndex = input.readUnsignedInt()
        val sigScriptLength = input.readVarInt()
        sigScript = input.readBytes(sigScriptLength.toInt())
        sequence = input.readUnsignedInt()
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .write(previousOutputTxHash)
                .writeUnsignedInt(previousOutputIndex)
                .writeVarInt(sigScript.size.toLong())
                .write(sigScript)
                .writeUnsignedInt(sequence)
                .toByteArray()
    }

    fun validate() {
        // throw new ValidateException("Verify signature failed.");
    }
}
