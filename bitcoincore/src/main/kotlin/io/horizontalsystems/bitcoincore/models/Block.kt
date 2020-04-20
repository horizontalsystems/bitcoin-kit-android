package io.horizontalsystems.bitcoincore.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.storage.BlockHeader

/**
 * Block
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      TxCount         Number of transactions in the block
 *  Variable    Transactions    The transactions in the block
 */

@Entity(indices = [Index("height")])

class Block() {

    //  Header
    @ColumnInfo(name = "block_version")
    var version: Int = 0
    var previousBlockHash: ByteArray = byteArrayOf()
    var merkleRoot: ByteArray = byteArrayOf()
    @ColumnInfo(name = "block_timestamp")
    var timestamp: Long = 0
    var bits: Long = 0
    var nonce: Long = 0
    var hasTransactions = false

    @PrimaryKey
    var headerHash: ByteArray = byteArrayOf()
    var height: Int = 0
    var stale = false

    fun previousBlock(storage: IStorage): Block? {
        return storage.getBlock(hashHash = previousBlockHash)
    }

    constructor(header: BlockHeader, previousBlock: Block) : this(header, height = previousBlock.height + 1)
    constructor(header: BlockHeader, height: Int) : this() {
        version = header.version
        previousBlockHash = header.previousBlockHeaderHash
        merkleRoot = header.merkleRoot
        timestamp = header.timestamp
        bits = header.bits
        nonce = header.nonce

        headerHash = header.hash
        this.height = height
    }
}
