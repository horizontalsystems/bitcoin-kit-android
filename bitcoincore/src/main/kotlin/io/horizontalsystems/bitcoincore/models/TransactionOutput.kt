package io.horizontalsystems.bitcoincore.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import io.horizontalsystems.bitcoincore.core.IStorage
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

@Entity(indices = [Index("publicKeyPath", "transactionHash")],
        primaryKeys = ["transactionHash", "index"],
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

class TransactionOutput {

    var value: Long = 0
    var lockingScript: ByteArray = byteArrayOf()
    var index: Int = 0

    var transactionHash = byteArrayOf()
    var publicKeyPath: String? = null
    var scriptType: Int = ScriptType.UNKNOWN
    var keyHash: ByteArray? = null
    var address: String? = null

    fun transaction(storage: IStorage): Transaction? {
        return storage.getTransaction(hash = transactionHash)
    }

    fun publicKey(storage: IStorage): PublicKey? {
        publicKeyPath?.let { return storage.getPublicKey(byPath = it) } ?: return null
    }

    fun used(storage: IStorage): Boolean {
        return storage.hasInputs(ofOutput = this)
    }

    constructor()
    constructor(value: Long, index: Int, script: ByteArray, type: Int = ScriptType.UNKNOWN, address: String? = null, keyHash: ByteArray? = null, publicKey: PublicKey? = null) {
        this.value = value
        this.lockingScript = script
        this.index = index
        this.scriptType = type
        this.address = address
        this.keyHash = keyHash
        this.publicKeyPath = publicKey?.path
    }

}
