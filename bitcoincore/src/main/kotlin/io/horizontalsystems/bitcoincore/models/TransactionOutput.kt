package io.horizontalsystems.bitcoincore.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.TypeConverter
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

/**
 * Transaction output
 *
 *  Size        Field                Description
 *  ====        =====                ===========
 *  8 bytes     OutputValue          Value expressed in Satoshis (0.00000001 BTC)
 *  VarInt      OutputScriptLength   Script length
 *  Variable    OutputScript         Script
 */

@Entity(primaryKeys = ["transactionHash", "index"],
        foreignKeys = [
            ForeignKey(
                    entity = PublicKey::class,
                    parentColumns = ["path"],
                    childColumns = ["publicKeyPath"],
                    onUpdate = ForeignKey.SET_NULL,
                    onDelete = ForeignKey.SET_NULL,
                    deferred = true),
            ForeignKey(
                    entity = Transaction::class,
                    parentColumns = ["hash"],
                    childColumns = ["transactionHash"],
                    onDelete = ForeignKey.CASCADE,
                    onUpdate = ForeignKey.CASCADE,
                    deferred = true)
        ])

class TransactionOutput() {

    var value: Long = 0
    var lockingScript: ByteArray = byteArrayOf()
    var redeemScript: ByteArray? = null
    var index: Int = 0

    var transactionHash = byteArrayOf()
    var publicKeyPath: String? = null
    var changeOutput: Boolean = false
    var scriptType: ScriptType = ScriptType.UNKNOWN
    var keyHash: ByteArray? = null
    var address: String? = null
    var failedToSpend = false

    var pluginId: Byte? = null
    var pluginData: String? = null
    @Ignore
    var signatureScriptFunction: ((List<ByteArray>) -> ByteArray)? = null

    constructor(value: Long, index: Int, script: ByteArray, type: ScriptType = ScriptType.UNKNOWN, address: String? = null, keyHash: ByteArray? = null, publicKey: PublicKey? = null): this() {
        this.value = value
        this.lockingScript = script
        this.index = index
        this.scriptType = type
        this.address = address
        this.keyHash = keyHash
        publicKey?.let { setPublicKey(it) }
    }

    fun setPublicKey(publicKey: PublicKey) {
        this.publicKeyPath = publicKey.path
        this.changeOutput = !publicKey.external
    }
}

class ScriptTypeConverter {
    @TypeConverter
    fun fromInt(value: Int?): ScriptType? {
        return value?.let { ScriptType.fromValue(it) }
    }


    @TypeConverter
    fun scriptTypeToInt(scriptType: ScriptType?): Int? {
        return scriptType?.value
    }
}
