package io.horizontalsystems.bitcoinkit.models

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.extensions.toReversedHex
import io.horizontalsystems.bitcoinkit.serializers.BlockHeaderSerializer
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.utils.HashUtils

/**
 * Block
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      TxCount         Number of transactions in the block
 *  Variable    Transactions    The transactions in the block
 */

@Entity
class Block() {

    //  Header
    @ColumnInfo(name = "block_version")
    var version: Int = 0
    var previousBlockHashReversedHex: String = ""
    var previousBlockHash: ByteArray = byteArrayOf()
    var merkleRoot: ByteArray = byteArrayOf()
    @ColumnInfo(name = "block_timestamp")
    var timestamp: Long = 0
    var bits: Long = 0
    var nonce: Long = 0

    @PrimaryKey
    var headerHashReversedHex: String = ""
    var headerHash: ByteArray = byteArrayOf()
    var height: Int = 0
    var stale = false

    fun previousBlock(storage: IStorage): Block? {
        return storage.getBlock(hashHex = previousBlockHashReversedHex)
    }

    constructor(header: BlockHeader, previousBlock: Block) : this(header, height = previousBlock.height + 1)
    constructor(header: BlockHeader, height: Int) : this() {
        version = header.version
        previousBlockHash = header.previousBlockHeaderHash
        previousBlockHashReversedHex = header.previousBlockHeaderHash.toReversedHex()
        merkleRoot = header.merkleRoot
        timestamp = header.timestamp
        bits = header.bits
        nonce = header.nonce

        headerHash = HashUtils.doubleSha256(BlockHeaderSerializer.serialize(header))
        headerHashReversedHex = headerHash.toReversedHex()
        this.height = height
    }

//    constructor(headerHash: ByteArray, height: Int) : this() {
//        this.headerHash = headerHash
//        this.headerHashReversedHex = HashUtils.toHexString(headerHash.reversedArray())
//        this.height = height
//    }
}
