package io.horizontalsystems.bitcoinkit.blocks.validators

import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.NetworkParameters

class TestnetBitcoinCashValidator(network: NetworkParameters) : BitcoinCashValidator(network) {
    override fun validateDAA(candidate: Block, previousBlock: Block) {
        validateHeader(candidate, previousBlock)
    }
}
