package io.horizontalsystems.tools.pack

/**
 * Block
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      TxCount         Number of transactions in the block
 *  Variable    Transactions    The transactions in the block
 */


class Block() {

    //  Header
    var version: Int = 0
    var previousBlockHash: ByteArray = byteArrayOf()
    var merkleRoot: ByteArray = byteArrayOf()

    var timestamp: Long = 0
    var bits: Long = 0
    var nonce: Long = 0
    var hasTransactions = false

    var headerHash: ByteArray = byteArrayOf()
    var height: Int = 0
    var stale = false
    var partial = false

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

class BlockHeader(
    val version: Int,
    val previousBlockHeaderHash: ByteArray,
    val merkleRoot: ByteArray,
    val timestamp: Long,
    val bits: Long,
    val nonce: Long,
    val hash: ByteArray)