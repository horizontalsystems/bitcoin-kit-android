package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.utils.AddressConverter
import bitcoin.wallet.kit.network.NetworkParameters
import io.realm.Realm

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
                    process(transaction, realm)
                }
            }

            addressManager.fillGap()
        }

        realm.close()
    }

    fun process(transaction: Transaction, realm: Realm) {
        extractor.extract(transaction, realm)
        linker.handle(transaction, realm)
        transaction.processed = true
    }
}
