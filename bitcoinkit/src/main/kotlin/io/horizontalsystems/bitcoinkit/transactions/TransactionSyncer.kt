package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.Transaction

class TransactionSyncer(private val realmFactory: RealmFactory,
                        private val transactionProcessor: TransactionProcessor,
                        private val addressManager: AddressManager,
                        private val bloomFilterManager: BloomFilterManager) {

    fun handleTransactions(transactions: List<Transaction>) {
        if (transactions.isEmpty()) return

        val realm = realmFactory.realm
        var needToUpdateBloomFilter = false

        realm.executeTransaction {
            try {
                transactionProcessor.process(transactions, null, true, realm)
            } catch (e: BloomFilterManager.BloomFilterExpired) {
                needToUpdateBloomFilter = true
            }
        }

        if (needToUpdateBloomFilter) {
            addressManager.fillGap()
            bloomFilterManager.regenerateBloomFilter()
        }
    }

    fun shouldRequestTransaction(hash: ByteArray): Boolean {
        val realm = realmFactory.realm
        val exist = realm.where(Transaction::class.java).equalTo("hash", hash).count() > 0
        realm.close()
        return !exist
    }

    fun getNonSentTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()

        realmFactory.realm.use { realm ->
            realm.where(Transaction::class.java)
                    .equalTo("status", Transaction.Status.NEW)
                    .findAll().forEach {
                        transactions.add(realm.copyFromRealm(it))
                    }
        }

        return transactions
    }

}
