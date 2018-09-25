package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.NetworkParameters

class TransactionProcessor(
        private val realmFactory: RealmFactory,
        private val network: NetworkParameters,
        private val extractor: TransactionExtractor = TransactionExtractor(network),
        private val linker: TransactionLinker = TransactionLinker()) {

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
                    linker.handle(transaction, it)
                    transaction.processed = true
                }
            }
        }

        realm.close()
    }
}
