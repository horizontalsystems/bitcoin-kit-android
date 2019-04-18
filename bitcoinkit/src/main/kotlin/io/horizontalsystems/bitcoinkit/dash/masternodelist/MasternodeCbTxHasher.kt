package io.horizontalsystems.bitcoinkit.dash.masternodelist

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.bitcoinkit.dash.models.CoinbaseTransaction
import io.horizontalsystems.bitcoinkit.dash.models.CoinbaseTransactionSerializer

class MasternodeCbTxHasher(private val coinbaseTransactionSerializer: CoinbaseTransactionSerializer, private val hasher: IHasher) {

    fun hash(coinbaseTransaction: CoinbaseTransaction): ByteArray {
        val serialized = coinbaseTransactionSerializer.serialize(coinbaseTransaction)

        return hasher.hash(serialized)
    }

}
