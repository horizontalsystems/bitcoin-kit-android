package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.inTopologicalOrder
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import io.realm.Realm

class TransactionProcessor(private val extractor: TransactionExtractor, private val linker: TransactionLinker, private val addressManager: AddressManager) {

    fun process(transaction: Transaction, realm: Realm) {
        extractor.extractOutputs(transaction, realm)
        linker.handle(transaction, realm)

        if (transaction.isMine) {
            extractor.extractAddress(transaction)
            extractor.extractInputs(transaction)
        }
    }

    @Throws(BloomFilterManager.BloomFilterExpired::class)
    fun process(transactions: List<Transaction>, block: Block?, skipCheckBloomFilter: Boolean, realm: Realm) {
        var needToUpdateBloomFilter = false

        for (transaction in transactions.inTopologicalOrder()) {
            val transactionInDB = realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst()

            if (transactionInDB != null) {
                transactionInDB.status = Transaction.Status.RELAYED
                transactionInDB.block = block
                continue
            }

            process(transaction, realm)

            if (transaction.isMine) {
                transaction.block = block
                realm.insert(transaction)

                if (!skipCheckBloomFilter) {
                    needToUpdateBloomFilter = needToUpdateBloomFilter || addressManager.gapShifts(realm) || hasUnspentOutputs(transaction)
                }

            }
        }

        if (needToUpdateBloomFilter) {
            throw BloomFilterManager.BloomFilterExpired
        }
    }

    private fun hasUnspentOutputs(transaction: Transaction): Boolean {
        return transaction.outputs.any { output ->
            output.publicKey != null && (output.scriptType == ScriptType.P2PK || output.scriptType == ScriptType.P2WPKH)
        }
    }

}
