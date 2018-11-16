package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.SentTransaction
import io.horizontalsystems.bitcoinkit.models.Transaction

class TransactionSyncer(
        private val realmFactory: RealmFactory,
        private val transactionProcessor: TransactionProcessor,
        private val addressManager: AddressManager,
        private val bloomFilterManager: BloomFilterManager) {

    private val maxRetriesCount: Int = 3
    private val retriesPeriod: Long = 60 * 1000
    private val totalRetriesPeriod: Long = 60 * 60 * 24 * 1000

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

        realm.close()

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

    fun getPendingTransactions(): List<Transaction> {
        realmFactory.realm.use { realm ->
            val filterByTime: (Transaction) -> Boolean = { transition ->
                val sentTransaction = realm.where(SentTransaction::class.java).equalTo("hashHexReversed", transition.hashHexReversed).findFirst()
                if (sentTransaction == null) {
                    true
                } else {
                    sentTransaction.retriesCount < maxRetriesCount &&
                            sentTransaction.lastSendTime < System.currentTimeMillis() - retriesPeriod &&
                            sentTransaction.firstSendTime > System.currentTimeMillis() - totalRetriesPeriod
                }
            }

            return realm.where(Transaction::class.java)
                    .equalTo("status", Transaction.Status.NEW)
                    .findAll()
                    .filter(filterByTime)
        }
    }

    fun handleTransaction(transaction: Transaction) {
        realmFactory.realm.use { realm ->
            realm.where(Transaction::class.java)
                    .equalTo("hashHexReversed", transaction.hashHexReversed)
                    .equalTo("status", Transaction.Status.NEW)
                    .findFirst() ?: return

            realm.executeTransaction {
                val sentTransaction = realm.where(SentTransaction::class.java)
                        .equalTo("hashHexReversed", transaction.hashHexReversed)
                        .findFirst()

                if (sentTransaction == null) {
                    realm.insert(SentTransaction(transaction.hashHexReversed))
                } else {
                    sentTransaction.lastSendTime = System.currentTimeMillis()
                    sentTransaction.retriesCount = sentTransaction.retriesCount + 1
                }
            }
        }

    }
}
