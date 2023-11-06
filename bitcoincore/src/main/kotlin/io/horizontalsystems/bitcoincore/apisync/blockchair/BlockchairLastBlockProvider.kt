package io.horizontalsystems.bitcoincore.apisync.blockchair

import io.horizontalsystems.bitcoincore.apisync.model.BlockHeaderItem

class BlockchairLastBlockProvider(
    private val blockchairApi: BlockchairApi
) {
    fun lastBlockHeader(): BlockHeaderItem {
        return blockchairApi.lastBlockHeader()
    }
}
