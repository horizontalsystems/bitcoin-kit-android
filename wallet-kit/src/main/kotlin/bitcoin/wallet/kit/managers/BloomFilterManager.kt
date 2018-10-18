package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.models.PublicKey
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.scripts.ScriptType
import bitcoin.walllet.kit.utils.Utils

class BloomFilterManager(elements: List<ByteArray>, private val realmFactory: RealmFactory) {

    interface Listener {
        fun onFilterUpdated(bloomFilter: BloomFilter)
    }

    var listener: Listener? = null
    var bloomFilter: BloomFilter? = null

    init {
        if (elements.isNotEmpty()) {
            bloomFilter = BloomFilter(elements)
        }
    }

    fun regenerateBloomFilter() {
        val realm = realmFactory.realm
        val elements = mutableListOf<ByteArray>()

        val publicKeys = realm.where(PublicKey::class.java).findAll()
        for (publicKey in publicKeys) {
            elements.add(publicKey.publicKeyHash)
            elements.add(publicKey.publicKey)
            elements.add(publicKey.scriptHashP2WPKH)
        }

        val unspentOutputs = realm.where(TransactionOutput::class.java)
                .isNull("publicKey")
                .`in`("scriptType", arrayOf(ScriptType.P2WPKH, ScriptType.P2PK))
                .findAll()
                .filter { it.inputs?.size ?: 0 == 0 }

        for (output in unspentOutputs) {
            output.transaction?.let { transaction ->
                val outpoint = transaction.hash + Utils.intToByteArray(output.index).reversedArray()
                elements.add(outpoint)

            }
        }

        if (elements.isNotEmpty()) {
            BloomFilter(elements).let {
                if (it != bloomFilter) {
                    bloomFilter = it
                    listener?.onFilterUpdated(it)
                }
            }
        }

        realm.close()
    }

}
