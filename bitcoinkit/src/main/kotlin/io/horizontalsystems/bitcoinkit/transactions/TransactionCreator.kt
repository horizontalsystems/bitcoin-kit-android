package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.PeerGroup
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder

class TransactionCreator(private val realmFactory: RealmFactory,
                         private val builder: TransactionBuilder,
                         private val processor: TransactionProcessor,
                         private val peerGroup: PeerGroup,
                         private val addressManager: AddressManager) {

    val feeRate = 8

    fun create(address: String, value: Int) {
        val realm = realmFactory.realm
        val changePubKey = addressManager.changePublicKey()

        val transaction = builder.buildTransaction(value, address, feeRate, true, changePubKey)

        check(realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst() == null) {
            throw TransactionAlreadyExists("hashHexReversed = ${transaction.hashHexReversed}")
        }

        realm.executeTransaction {
            realm.insert(transaction)
            processor.process(transaction, realm)
        }

        peerGroup.relay(transaction)

        realm.close()
    }

    open class TransactionCreationException(msg: String) : Exception(msg)
    class TransactionAlreadyExists(msg: String) : TransactionCreationException(msg)

}
