package io.horizontalsystems.bitcoincore.blocks

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.Transaction

interface IBlockchainDataListener {
    fun onBlockInsert(block: Block)
    fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, block: Block?)
    fun onTransactionsDelete(hashes: List<String>)
}
