package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.utils.AddressConverter

class TransactionProcessor(
        private val realmFactory: RealmFactory,
        private val addressManager: AddressManager,
        private val addressConverter: AddressConverter,
        private val extractor: TransactionExtractor = TransactionExtractor(addressConverter),
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

            addressManager.generateKeys()
        }

        realm.close()
    }
}
