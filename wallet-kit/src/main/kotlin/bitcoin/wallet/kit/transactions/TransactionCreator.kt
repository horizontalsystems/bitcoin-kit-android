package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.wallet.kit.transactions.builder.TransactionBuilder

class TransactionCreator(private val realmFactory: RealmFactory,
                         private val transactionBuilder: TransactionBuilder,
                         private val transactionProcessor: TransactionProcessor,
                         private val peerGroup: PeerGroup,
                         private val addressManager: AddressManager) {

    private val feeRate = 60

    fun create(address: String, value: Int) {
        val realm = realmFactory.realm
        val changePubKey = addressManager.changePublicKey()

        val transaction = transactionBuilder.buildTransaction(value, address, feeRate, true, changePubKey)

        check(realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst() == null) {
            throw TransactionAlreadyExists("hashHexReversed = ${transaction.hashHexReversed}")
        }

        realm.executeTransaction {
            it.insert(transaction)
        }

        transactionProcessor.enqueueRun()
        peerGroup.relay(transaction)
    }

    open class TransactionCreationException(msg: String) : Exception(msg)
    class TransactionAlreadyExists(msg: String) : TransactionCreationException(msg)

}
