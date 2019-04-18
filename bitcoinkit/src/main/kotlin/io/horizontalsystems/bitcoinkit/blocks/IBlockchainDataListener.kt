package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Transaction

interface IBlockchainDataListener {
    fun onBlockInsert(block: Block)
    fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, block: Block?)
    fun onTransactionsDelete(ids: List<String>)
}
