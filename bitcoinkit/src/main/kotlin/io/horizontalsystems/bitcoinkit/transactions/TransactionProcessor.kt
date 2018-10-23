package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.models.Transaction
import io.realm.Realm

class TransactionProcessor(private val extractor: TransactionExtractor, private val linker: TransactionLinker) {

    fun process(transaction: Transaction, realm: Realm) {
        extractor.extract(transaction, realm)
        linker.handle(transaction, realm)
    }

}
