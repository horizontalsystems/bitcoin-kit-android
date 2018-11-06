package io.horizontalsystems.bitcoinkit.models

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects

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

    // Output value
    var value: Long = 0

    // Output script used for authenticating that the redeemer is allowed to spend this output.
    var lockingScript: ByteArray = byteArrayOf()

    // Output transaction index
    var index: Int = 0

    var publicKey: PublicKey? = null
    var scriptType: Int = 0
    var keyHash: ByteArray? = null
    var address: String? = null

    @LinkingObjects("previousOutput")
    val inputs: RealmResults<TransactionInput>? = null

    @LinkingObjects("outputs")
    val transactions: RealmResults<Transaction>? = null
    val transaction: Transaction?
        get() = transactions?.first()

    constructor()
    constructor(input: BitcoinInput, vout: Long) {
        value = input.readLong()
        val scriptLength = input.readVarInt() // do not store
        lockingScript = input.readBytes(scriptLength.toInt())
        index = vout.toInt()
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .writeLong(value)
                .writeVarInt(lockingScript.size.toLong())
                .write(lockingScript)
                .toByteArray()
    }
}
