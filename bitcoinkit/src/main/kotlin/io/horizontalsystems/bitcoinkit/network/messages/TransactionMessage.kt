package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.models.Transaction
import java.io.ByteArrayInputStream

/**
 * The 'tx' message contains a transaction which is not yet in a block. The transaction
 * will be held in the memory pool for a period of time to allow other peers to request
 * the transaction
 */
class TransactionMessage() : Message("tx") {

    lateinit var transaction: Transaction

    constructor(transaction: Transaction) : this() {
        this.transaction = transaction
    }

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            transaction = Transaction(input)
        }
    }

    override fun getPayload(): ByteArray {
        return transaction.toByteArray()
    }

    override fun toString(): String {
        return "TransactionMessage(${transaction.hashHexReversed})"
    }

}
