package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.SentTransaction
import io.horizontalsystems.bitcoinkit.models.Transaction

class TransactionSyncer(
        private val storage: IStorage,
        private val transactionProcessor: TransactionProcessor,
        private val addressManager: AddressManager,
        private val bloomFilterManager: BloomFilterManager) {

    private val maxRetriesCount: Int = 3
    private val retriesPeriod: Long = 60 * 1000
    private val totalRetriesPeriod: Long = 60 * 60 * 24 * 1000

    fun handleTransactions(transactions: List<Transaction>) {
        if (transactions.isEmpty()) return

        storage.inTransaction { realm ->
            var needToUpdateBloomFilter = false

            try {
                transactionProcessor.process(transactions, null, true, realm)
            } catch (e: BloomFilterManager.BloomFilterExpired) {
                needToUpdateBloomFilter = true
            }

            if (needToUpdateBloomFilter) {
                addressManager.fillGap()
                bloomFilterManager.regenerateBloomFilter()
            }
        }
    }

    fun handleTransaction(sentTransaction: Transaction) {
        val newTransaction = storage.getNewTransaction(sentTransaction.hashHexReversed) ?: return
        val sntTransaction = storage.getSentTransaction(newTransaction.hashHexReversed) ?: run {
            storage.addSentTransaction(SentTransaction(newTransaction.hashHexReversed))

            return
        }

        sntTransaction.lastSendTime = System.currentTimeMillis()
        sntTransaction.retriesCount = sntTransaction.retriesCount + 1

        storage.updateSentTransaction(sntTransaction)
    }

    fun getPendingTransactions(): List<Transaction> {
        return storage.getNewTransactions().filter { transition ->
            val sentTransaction = storage.getSentTransaction(transition.hashHexReversed)
            if (sentTransaction == null) {
                true
            } else {
                sentTransaction.retriesCount < maxRetriesCount &&
                sentTransaction.lastSendTime < System.currentTimeMillis() - retriesPeriod &&
                sentTransaction.firstSendTime > System.currentTimeMillis() - totalRetriesPeriod
            }
        }
    }

    fun shouldRequestTransaction(hash: ByteArray): Boolean {
        return storage.getRelayedTransaction(hash) != null
    }
}
