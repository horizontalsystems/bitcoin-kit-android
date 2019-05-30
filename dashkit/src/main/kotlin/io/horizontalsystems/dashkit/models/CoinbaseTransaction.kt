package io.horizontalsystems.dashkit.models

import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer

class CoinbaseTransaction(input: BitcoinInput) {
    val transaction = TransactionSerializer.deserialize(input)
    val coinbaseTransactionSize: Long
    val version: Int
    val height: Long
    val merkleRootMNList: ByteArray
    val merkleRootQuorums: ByteArray?

    init {
        coinbaseTransactionSize = input.readVarInt()

        val coinbaseTxPayload = ByteArray(coinbaseTransactionSize.toInt())

        input.readFully(coinbaseTxPayload)
        val coinbaseTransactionInput = BitcoinInput(coinbaseTxPayload)

        version = coinbaseTransactionInput.readUnsignedShort()
        height = coinbaseTransactionInput.readUnsignedInt()
        merkleRootMNList = coinbaseTransactionInput.readBytes(32)
        merkleRootQuorums = when {
            version >= 2 -> coinbaseTransactionInput.readBytes(32)
            else -> null
        }

        coinbaseTransactionInput.close()
    }
}
