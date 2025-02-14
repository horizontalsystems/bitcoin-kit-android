package io.horizontalsystems.dashkit.models

import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer

class CoinbaseTransactionSerializer {

    fun serialize(coinbaseTransaction: CoinbaseTransaction): ByteArray {
        val output = BitcoinOutput()

        output.write(TransactionSerializer.serialize(coinbaseTransaction.transaction))
        output.writeVarInt(coinbaseTransaction.coinbaseTransactionSize)
        output.writeUnsignedShort(coinbaseTransaction.version)
        output.writeUnsignedInt(coinbaseTransaction.height)
        output.write(coinbaseTransaction.merkleRootMNList)

        if (coinbaseTransaction.version >= 2) {
            output.write(coinbaseTransaction.merkleRootQuorums)
        }

        if (coinbaseTransaction.version >= 3) {
            coinbaseTransaction.bestCLHeightDiff?.let { output.writeVarInt(it) }
            coinbaseTransaction.bestCLSignature?.let { output.write(it) }
            coinbaseTransaction.creditPoolBalance?.let { output.writeLong(it) }
        }

        return output.toByteArray()
    }

}
