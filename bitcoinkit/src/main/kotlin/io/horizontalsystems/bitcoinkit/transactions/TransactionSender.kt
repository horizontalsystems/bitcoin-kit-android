package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.PeerGroup
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
