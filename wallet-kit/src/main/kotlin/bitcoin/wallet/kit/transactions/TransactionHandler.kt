package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.core.toHexString
import bitcoin.wallet.kit.managers.ProgressSyncer
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.models.Transaction

class TransactionHandler(private val realmFactory: RealmFactory, private val processor: TransactionProcessor, private val progressSyncer: ProgressSyncer) {

    fun handle(transactions: Array<Transaction>, header: Header) {

        val realm = realmFactory.realm

        val reversedHashHex = header.hash.reversedArray().toHexString()

        var hasNewTransactions = false
        var hasNewSyncedBlocks = false

        val existingBlock = realm.where(Block::class.java).equalTo("reversedHeaderHashHex", reversedHashHex).findFirst()

        if (existingBlock != null) {
            if (existingBlock.synced) {
                return
            }

            realm.executeTransaction {

                if (existingBlock.header == null) {
                    existingBlock.header = header
                }

                transactions.forEach { transaction ->
                    val existingTransaction = realm.where(Transaction::class.java).equalTo("reversedHashHex", transaction.reversedHashHex).findFirst()

                    if (existingTransaction != null) {
                        existingTransaction.block = existingBlock
                        existingTransaction.status = Transaction.Status.RELAYED
                    } else {
                        val transactionManaged = it.copyToRealm(transaction)
                        transactionManaged.block = existingBlock

                        hasNewTransactions = true
                    }

                }

                existingBlock.synced = true
                hasNewSyncedBlocks = true
            }

        } else {

            val block = Block().apply {
                this.header = header
            }
            block.synced = true

            realm.executeTransaction {
                val blockManaged = it.copyFromRealm(block)

                transactions.forEach { transaction ->
                    val existingTransaction = realm.where(Transaction::class.java).equalTo("reversedHashHex", transaction.reversedHashHex).findFirst()

                    if (existingTransaction != null) {
                        existingTransaction.block = blockManaged
                        existingTransaction.status = Transaction.Status.RELAYED
                    } else {
                        val transactionManaged = it.copyToRealm(transaction)
                        transactionManaged.block = blockManaged

                        hasNewTransactions = true
                    }

                }

                hasNewSyncedBlocks = true
            }
        }

        if (hasNewTransactions) {
            processor.enqueueRun()
        }

        if (hasNewSyncedBlocks) {
            progressSyncer.enqueueRun()
        }
    }

}
