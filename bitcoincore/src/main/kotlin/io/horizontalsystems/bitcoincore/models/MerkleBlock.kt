package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.storage.FullTransaction

class MerkleBlock(val header: BlockHeader, val associatedTransactionHexes: List<String>) {

    var height: Int? = null
    var associatedTransactions = mutableListOf<FullTransaction>()
    val blockHash = header.hash
    val reversedHeaderHashHex = blockHash.toReversedHex()

    val complete: Boolean
        get() = associatedTransactionHexes.size == associatedTransactions.size

}
