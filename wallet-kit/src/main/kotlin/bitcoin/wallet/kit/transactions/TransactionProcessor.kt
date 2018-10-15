package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.MerkleBlock
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
                    extractor.extract(transaction, realm)
                    linker.handle(transaction, it)
                    transaction.processed = true
                }
            }

            addressManager.fillGap()
        }

        realm.close()
    }

    fun getGapData(merkleBlocks: List<MerkleBlock>, realm: Realm): GapData {
        val gapData = GapData(null, null, null)

        merkleBlocks.forEach { merkleBlock ->
            for (transaction in merkleBlock.associatedTransactions) {
                extractor.extract(transaction, realm)

                if (!transaction.isMine) continue

                transaction.outputs.forEach { output ->

                    output.publicKey?.let { pubKey ->

                        if (pubKey.external) {
                            if (gapData.lastUsedExternalKey?.index ?: 0 < pubKey.index) {
                                gapData.lastUsedExternalKey = pubKey
                            }
                        }
                        else {
                            if (gapData.lastUsedInternalKey?.index ?: 0 < pubKey.index) {
                                gapData.lastUsedInternalKey = pubKey
                            }
                        }

                        if (gapData.firstGapShiftMerkleBlock == null && addressManager.gapShiftsOn(pubKey, realm)) {
                            gapData.firstGapShiftMerkleBlock = merkleBlock
                        }

                    }
                }
            }

        }

        return gapData

    }

    fun link(transaction: Transaction, realm: Realm) {
        linker.handle(transaction, realm)
    }
}

data class GapData(var firstGapShiftMerkleBlock: MerkleBlock?, var lastUsedExternalKey: PublicKey?, var lastUsedInternalKey: PublicKey?)
