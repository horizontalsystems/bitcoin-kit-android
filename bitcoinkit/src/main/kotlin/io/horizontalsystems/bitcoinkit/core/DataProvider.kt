package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.*
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import io.realm.OrderedCollectionChangeSet
import io.realm.OrderedCollectionChangeSet.State
import io.realm.Realm
import io.realm.RealmResults

class DataProvider(private val realm: Realm, private val listener: Listener) {

    interface Listener {
        fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>)
        fun onBalanceUpdate(balance: Long)
        fun onBlockInfoUpdate(blockInfo: BlockInfo)
        fun onProgressUpdate(progress: Double)
    }

    private val unspentOutputsRealmResults = getUnspents()
    private val transactionRealmResults = getMyTransactions()
    private val blockRealmResults = getBlocks()

    //  Getters
    val balance get() = unspentOutputsRealmResults.map { it.value }.sum()
    val transactions get() = transactionRealmResults.mapNotNull { transactionInfo(it) }
    val lastBlockHeight get() = blockRealmResults.lastOrNull()?.height ?: 0

    init {
        transactionRealmResults.addChangeListener { transactions, changeSet ->
            handleTransactions(transactions, changeSet)
        }

        blockRealmResults.addChangeListener { blocks, changeSet ->
            handleBlocks(blocks, changeSet)
        }

        unspentOutputsRealmResults.addChangeListener { _, changeSet ->
            handleUnspentOutputs(changeSet)
        }
    }

    private fun handleTransactions(transactions: RealmResults<Transaction>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == State.UPDATE) {
            listener.let { listener ->
                val inserted = changeSet.insertions.asList().mapNotNull { transactionInfo(transactions[it]) }
                val updated = changeSet.changes.asList().mapNotNull { transactionInfo(transactions[it]) }
                val deleted = changeSet.deletions.asList()

                listener.onTransactionsUpdate(inserted, updated, deleted)
            }
        }
    }

    private fun handleUnspentOutputs(changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == State.UPDATE) {
            listener.onBalanceUpdate(balance)
        }
    }

    private fun handleBlocks(blocks: RealmResults<Block>, changeSet: OrderedCollectionChangeSet) {
        if (changeSet.state == State.UPDATE && (changeSet.deletions.isNotEmpty() || changeSet.insertions.isNotEmpty())) {
            blocks.lastOrNull()?.let { block ->
                listener.onBlockInfoUpdate(BlockInfo(
                        block.reversedHeaderHashHex,
                        block.height,
                        block.header?.timestamp
                ))
            }
        }
    }

    private fun transactionInfo(transaction: Transaction?): TransactionInfo? {
        if (transaction == null) return null

        var totalMineInput = 0L
        var totalMineOutput = 0L
        val fromAddresses = mutableListOf<TransactionAddress>()
        val toAddresses = mutableListOf<TransactionAddress>()

        transaction.inputs.forEach { input ->
            input.previousOutput?.let { previousOutput ->
                if (previousOutput.publicKey != null) {
                    totalMineInput += previousOutput.value
                }
            }

            input.address?.let { address ->
                fromAddresses.add(TransactionAddress(address, mine = input.previousOutput?.publicKey != null))
            }
        }

        transaction.outputs.forEach { output ->
            var mine = false

            if (output.publicKey != null) {
                totalMineOutput += output.value
                mine = true
            }

            output.address?.let { address ->
                toAddresses.add(TransactionAddress(address, mine))
            }
        }

        return TransactionInfo(
                transactionHash = transaction.hashHexReversed,
                from = fromAddresses,
                to = toAddresses,
                amount = totalMineOutput - totalMineInput,
                blockHeight = transaction.block?.height,
                timestamp = transaction.block?.header?.timestamp
        )
    }

    private fun getUnspents(): RealmResults<TransactionOutput> {
        return realm.where(TransactionOutput::class.java)
                .isNotNull("publicKey")
                .notEqualTo("scriptType", ScriptType.UNKNOWN)
                .isEmpty("inputs")
                .findAll()
    }

    private fun getBlocks(): RealmResults<Block> {
        return realm.where(Block::class.java)
                .sort("height")
                .findAll()
    }

    private fun getMyTransactions(): RealmResults<Transaction> {
        return realm.where(Transaction::class.java)
                .equalTo("isMine", true)
                .findAll()
    }
}
