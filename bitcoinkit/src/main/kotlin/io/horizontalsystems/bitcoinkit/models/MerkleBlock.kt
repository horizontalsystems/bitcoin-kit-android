package io.horizontalsystems.bitcoinkit.models

import io.horizontalsystems.bitcoinkit.extensions.toReversedHex
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.storage.FullTransaction

class MerkleBlock(val header: BlockHeader, val associatedTransactionHexes: List<String>) {

    var height: Int? = null
    var associatedTransactions = mutableListOf<FullTransaction>()
    val blockHash = header.hash
    val reversedHeaderHashHex = blockHash.toReversedHex()

    val complete: Boolean
        get() = associatedTransactionHexes.size == associatedTransactions.size

}
