package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.blocks.InitialBlockDownload
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.models.SentTransaction
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.network.peer.PeerManager
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.bitcoincore.network.peer.task.SendTransactionTask
import io.horizontalsystems.bitcoincore.storage.FullTransaction

class TransactionSender(
        private val transactionSyncer: TransactionSyncer,
        private val peerManager: PeerManager,
        private val initialBlockDownload: InitialBlockDownload,
        private val storage: IStorage,
        private val timer: TransactionSendTimer,
        private val maxRetriesCount: Int = 3,
        private val retriesPeriod: Int = 60)
    : IPeerTaskHandler, TransactionSendTimer.Listener {

    fun sendPendingTransactions() {
        try {
            val transactions = transactionSyncer.getNewTransactions()
            if (transactions.isEmpty()) {
                timer.stop()
                return
            }

            val transactionsToSend = getTransactionsToSend(transactions)
            if (transactionsToSend.isNotEmpty()) {
                send(transactionsToSend)
            }

        } catch (e: PeerGroup.Error) {
//            logger.warning("Handling pending transactions failed with: ${e.message}")
        }
    }

    fun canSendTransaction() {
        if (getPeersToSend().isEmpty()) {
            throw PeerGroup.Error("peers not synced")
        }
    }

    fun transactionsRelayed(transactions: List<FullTransaction>) {
        transactions.forEach { transaction ->
            storage.getSentTransaction(transaction.header.hash)?.let { sentTransaction ->
                storage.deleteSentTransaction(sentTransaction)
            }
        }
    }

    private fun getTransactionsToSend(transactions: List<FullTransaction>): List<FullTransaction> {
        return transactions.filter { transaction ->
            storage.getSentTransaction(transaction.header.hash)?.let { sentTransaction ->
                sentTransaction.retriesCount < maxRetriesCount && sentTransaction.lastSendTime < (System.currentTimeMillis() - retriesPeriod * 1000)
            } ?: true
        }
    }

    private fun getPeersToSend(): List<Peer> {
        if (peerManager.peersCount < minConnectedPeerSize) {
            return emptyList()
        }

        val freeSyncedPeer = initialBlockDownload.syncedPeers
                .sortedBy { it.ready } // not ready first
                .firstOrNull()
                ?: return emptyList()

        val readyPeers = peerManager.readyPears()
                .filter { it != freeSyncedPeer }
                .sortedBy { it.synced } // not synced first

        if (readyPeers.size == 1) {
            return readyPeers
        }

        return readyPeers.take(readyPeers.size / 2)
    }

    private fun send(transactions: List<FullTransaction>) {
        val peers = getPeersToSend()
        if (peers.isEmpty()) {
            return
        }

        timer.startIfNotRunning()

        transactions.forEach { transaction ->
            transactionSendStart(transaction)

            peers.forEach { peer ->
                peer.addTask(SendTransactionTask(transaction))
            }
        }
    }

    private fun transactionSendStart(transaction: FullTransaction) {
        val sentTransaction = storage.getSentTransaction(transaction.header.hash)

        if (sentTransaction == null) {
            storage.addSentTransaction(SentTransaction(transaction.header.hash))
        } else {
            sentTransaction.lastSendTime = System.currentTimeMillis()
            sentTransaction.sendSuccess = false
            storage.updateSentTransaction(sentTransaction)
        }
    }

    @Synchronized
    private fun transactionSendSuccess(transaction: FullTransaction) {
        val sentTransaction = storage.getSentTransaction(transaction.header.hash)

        if (sentTransaction == null || sentTransaction.sendSuccess) {
            return
        }

        sentTransaction.retriesCount++
        sentTransaction.sendSuccess = true

        if (sentTransaction.retriesCount >= maxRetriesCount) {
            transactionSyncer.handleInvalid(transaction)
            storage.deleteSentTransaction(sentTransaction)
        } else {
            storage.updateSentTransaction(sentTransaction)
        }
    }

    // IPeerTaskHandler

    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        return when (task) {
            is SendTransactionTask -> {
                transactionSendSuccess(task.transaction)
                true
            }
            else -> false
        }
    }

    // TransactionSendTimer.Listener

    override fun onTimePassed() {
        sendPendingTransactions()
    }

    companion object {
        const val minConnectedPeerSize = 2
    }
}
