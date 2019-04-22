package io.horizontalsystems.bitcoincore.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Index
import io.horizontalsystems.bitcoincore.core.IStorage
import java.util.*

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

@Entity(indices = [Index("blockHash")],
        primaryKeys = ["hash"],
        foreignKeys = [ForeignKey(
                entity = Block::class,
                parentColumns = ["headerHash"],
                childColumns = ["blockHash"],
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE,
                deferred = true)
        ])

class Transaction {

    var hash: ByteArray = byteArrayOf()
    var blockHash: ByteArray? = null

    var version: Int = 0
    var lockTime: Long = 0
    var timestamp: Long = 0
    var order: Int = 0 // topological order
    var isMine = false
    var isOutgoing = false
    var segwit = false
    var status: Int = Status.RELAYED

    fun block(storage: IStorage): Block? {
        blockHash?.let {
            return storage.getBlock(hashHash = it)
        }

        return null
    }

    constructor()

    @Ignore
    constructor(version: Int = 0, lockTime: Long = 0) {
        this.version = version
        this.lockTime = lockTime
        this.timestamp = Date().time / 1000
    }

    object Status {
        const val NEW = 1
        const val RELAYED = 2
        const val INVALID = 3
    }
}
