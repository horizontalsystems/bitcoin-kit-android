package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.managers.PublicKeyManager
import io.horizontalsystems.bitcoincore.managers.BloomFilterManager
import io.horizontalsystems.bitcoincore.models.SentTransaction
import io.horizontalsystems.bitcoincore.storage.FullTransaction

class TransactionSyncer(
        private val storage: IStorage,
        private val transactionProcessor: TransactionProcessor,
        private val publicKeyManager: PublicKeyManager,
        private val bloomFilterManager: BloomFilterManager) {

    private val maxRetriesCount: Int = 3
    private val retriesPeriod: Long = 60 * 1000
    private val totalRetriesPeriod: Long = 60 * 60 * 24 * 1000

    fun handleTransactions(transactions: List<FullTransaction>) {
        if (transactions.isEmpty()) return

        var needToUpdateBloomFilter = false

        try {
            transactionProcessor.processIncoming(transactions, null, true)
        } catch (e: BloomFilterManager.BloomFilterExpired) {
            needToUpdateBloomFilter = true
        }

        if (needToUpdateBloomFilter) {
            publicKeyManager.fillGap()
            bloomFilterManager.regenerateBloomFilter()
        }
    }

    fun handleTransaction(sentTransaction: FullTransaction) {
        val newTransaction = storage.getNewTransaction(sentTransaction.header.hash) ?: return
        val sntTransaction = storage.getSentTransaction(newTransaction.hash) ?: run {
            storage.addSentTransaction(SentTransaction(newTransaction.hash))

            return
        }

        sntTransaction.lastSendTime = System.currentTimeMillis()
        sntTransaction.retriesCount = sntTransaction.retriesCount + 1

        storage.updateSentTransaction(sntTransaction)
    }

    fun getPendingTransactions(): List<FullTransaction> {
        return storage.getNewTransactions().filter { transition ->
            val sentTransaction = storage.getSentTransaction(transition.header.hash)
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
        return !storage.isRelayedTransactionExists(hash)
    }
}
