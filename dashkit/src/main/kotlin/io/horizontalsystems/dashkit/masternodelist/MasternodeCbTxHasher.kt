package io.horizontalsystems.dashkit.masternodelist

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.dashkit.models.CoinbaseTransaction
import io.horizontalsystems.dashkit.models.CoinbaseTransactionSerializer

class MasternodeCbTxHasher(private val coinbaseTransactionSerializer: CoinbaseTransactionSerializer, private val hasher: IHasher) {

    fun hash(coinbaseTransaction: CoinbaseTransaction): ByteArray {
        val serialized = coinbaseTransactionSerializer.serialize(coinbaseTransaction)

        return hasher.hash(serialized)
    }

}
