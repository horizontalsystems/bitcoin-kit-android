package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder

class TransactionCreator(private val builder: TransactionBuilder, private val processor: TransactionProcessor, private val peerGroup: PeerGroup) {

    @Throws
    fun create(address: String, value: Long, feeRate: Int, senderPay: Boolean) {
        peerGroup.checkPeersSynced()

        val transaction = builder.buildTransaction(value, address, feeRate, senderPay)

        processor.processOutgoing(transaction)
        peerGroup.sendPendingTransactions()
    }

    open class TransactionCreationException(msg: String) : Exception(msg)
    class TransactionAlreadyExists(msg: String) : TransactionCreationException(msg)

}
