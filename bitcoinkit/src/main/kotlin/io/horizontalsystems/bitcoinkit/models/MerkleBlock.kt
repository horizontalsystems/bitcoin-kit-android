package io.horizontalsystems.bitcoinkit.models

import io.horizontalsystems.bitcoinkit.serializers.BlockHeaderSerializer
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.storage.FullTransaction
import io.horizontalsystems.bitcoinkit.utils.HashUtils

/**
 * MerkleBlock
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      HashCount       Number of hashes
 *  Variable    Hashes          Hashes in depth-first order
 *  VarInt      FlagsCount      Number of bytes of flag bits
 *  Variable    Flags           Flag bits packed 8 per byte, least significant bit first
 */
class MerkleBlock() {

    lateinit var header: BlockHeader
    var hashes: List<ByteArray> = listOf()

    var height: Int? = null
    var associatedTransactionHexes = listOf<String>()
    var associatedTransactions = mutableListOf<FullTransaction>()
    val blockHash: ByteArray by lazy {
        HashUtils.doubleSha256(BlockHeaderSerializer.serialize(header))
    }

    val reversedHeaderHashHex: String by lazy {
        HashUtils.toHexString(blockHash.reversedArray())
    }

    val complete: Boolean
        get() = associatedTransactionHexes.size == associatedTransactions.size

    constructor(header: BlockHeader, transactionHashes: List<ByteArray>, transactions: List<FullTransaction>) : this() {
        this.header = header
        this.hashes = transactionHashes
        this.associatedTransactions = transactions.toMutableList()
    }

}
