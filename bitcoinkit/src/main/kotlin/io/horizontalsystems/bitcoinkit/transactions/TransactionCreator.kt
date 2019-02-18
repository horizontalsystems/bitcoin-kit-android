package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder

class TransactionCreator(
        private val realmFactory: RealmFactory,
        private val builder: TransactionBuilder,
        private val processor: TransactionProcessor,
        private val peerGroup: PeerGroup) {

    @Throws
    fun create(address: String, value: Long, feeRate: Int, senderPay: Boolean) {
        peerGroup.checkPeersSynced()

        realmFactory.realm.use { realm ->
            val transaction = builder.buildTransaction(value, address, feeRate, senderPay, realm)

            check(realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst() == null) {
                throw TransactionAlreadyExists("hashHexReversed = ${transaction.hashHexReversed}")
            }

            processor.processOutgoing(transaction, realm)
        }

        peerGroup.sendPendingTransactions()
    }

    open class TransactionCreationException(msg: String) : Exception(msg)
    class TransactionAlreadyExists(msg: String) : TransactionCreationException(msg)

}
