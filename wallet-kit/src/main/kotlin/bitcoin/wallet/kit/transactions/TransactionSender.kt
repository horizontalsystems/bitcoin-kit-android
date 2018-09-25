package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.PeerGroup
import kotlin.concurrent.thread

class TransactionSender(private val realmFactory: RealmFactory, private val peerGroup: PeerGroup) {

    fun enqueueRun() {
        thread {
            run()
        }
    }

    fun run() {
        val realm = realmFactory.realm

        val nonSentTransactions = realm.where(Transaction::class.java)
                .equalTo("status", Transaction.Status.NEW)
                .findAll()

        nonSentTransactions.forEach {
            peerGroup.relay(it)
        }

        realm.close()
    }

}
