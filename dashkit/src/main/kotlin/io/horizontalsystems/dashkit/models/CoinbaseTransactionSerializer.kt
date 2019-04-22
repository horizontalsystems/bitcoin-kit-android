package io.horizontalsystems.dashkit.models

import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer

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
