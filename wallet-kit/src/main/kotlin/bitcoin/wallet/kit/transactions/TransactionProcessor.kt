package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.Transaction

class TransactionProcessor(private val realmFactory: RealmFactory, private val extractor: TransactionExtractor = TransactionExtractor(), private val linker: TransactionLinker = TransactionLinker()) {

    fun enqueueRun() {
        // TODO implement with queue
        run()
    }

    private fun run() {
        val realm = realmFactory.realm
        val transactions = realm.where(Transaction::class.java)
                .equalTo("processed", false)
                .findAll()

        if (transactions.isNotEmpty()) {
            realm.executeTransaction {
                transactions.forEach { transaction ->
                    extractor.extract(transaction)
                    linker.handle(transaction)
                    transaction.processed = true
                }
            }
        }
    }
}
