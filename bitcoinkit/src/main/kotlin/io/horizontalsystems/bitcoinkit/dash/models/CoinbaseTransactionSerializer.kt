package io.horizontalsystems.bitcoinkit.dash.models

import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.serializers.TransactionSerializer

class CoinbaseTransactionSerializer {

    fun serialize(coinbaseTransaction: CoinbaseTransaction): ByteArray {
        return BitcoinOutput()
                .write(TransactionSerializer.serialize(coinbaseTransaction.transaction))
                .writeVarInt(coinbaseTransaction.coinbaseTransactionSize)
                .writeUnsignedShort(coinbaseTransaction.version)
                .writeUnsignedInt(coinbaseTransaction.height)
                .write(coinbaseTransaction.merkleRootMNList)
                .toByteArray()
    }

}
