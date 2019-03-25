package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network

class TestnetBitcoinCashValidator(network: Network, storage: IStorage) : BitcoinCashValidator(network, storage) {
    override fun validateDAA(candidate: Block, previousBlock: Block) {
        validateHeader(candidate, previousBlock)
    }
}
