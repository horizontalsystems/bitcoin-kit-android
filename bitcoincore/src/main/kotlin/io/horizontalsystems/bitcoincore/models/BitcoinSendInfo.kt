package io.horizontalsystems.bitcoincore.models

import io.horizontalsystems.bitcoincore.storage.UnspentOutput

data class BitcoinSendInfo(
    val unspentOutputs: List<UnspentOutput>,
    val fee: Long,
    val changeValue: Long?,
    val changeAddress: Address?
)