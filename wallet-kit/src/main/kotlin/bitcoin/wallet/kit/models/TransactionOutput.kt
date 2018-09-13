package bitcoin.wallet.kit.models

import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import io.realm.RealmObject
import java.io.IOException

/**
 * Transaction output
 *
 *  Size        Field                Description
 *  ====        =====                ===========
 *  8 bytes     OutputValue          Value expressed in Satoshis (0.00000001 BTC)
 *  VarInt      OutputScriptLength   Script length
 *  Variable    OutputScript         Script
 */
open class TransactionOutput : RealmObject {

    /**
     * int64, Transaction Value
     */
    var value: Long = 0

    /**
     * uchar[], Usually contains the public key as a Bitcoin script setting up
     * conditions to claim this output.
     */
    var pkScript: ByteArray = byteArrayOf()

    var publicKey: PublicKey? = null

    constructor()

    @Throws(IOException::class)
    constructor(input: BitcoinInput) {
        value = input.readLong()
        val scriptLength = input.readVarInt()
        pkScript = input.readBytes(scriptLength.toInt())
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .writeLong(value)
                .writeVarInt(pkScript.size.toLong())
                .write(pkScript)
                .toByteArray()
    }
}