package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.crypto.BloomFilter
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import io.horizontalsystems.bitcoinkit.utils.Utils
import io.realm.Sort

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

        var transactionOutputs: List<TransactionOutput> = realm.where(TransactionOutput::class.java)
                .isNull("publicKey")
                .`in`("scriptType", arrayOf(ScriptType.P2WPKH, ScriptType.P2PK))
                .findAll()

        realm.where(Block::class.java)
                .sort("height", Sort.DESCENDING)
                .findFirst()
                ?.height
                ?.let { bestBlockHeight ->
                    transactionOutputs = transactionOutputs.filter { needToSetToBloomFilter(it, bestBlockHeight) }
                }

        for (output in transactionOutputs) {
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

    /**
     * @return false if transaction output is spent more then 100 blocks before, otherwise true
     */
    private fun needToSetToBloomFilter(output: TransactionOutput, bestBlockHeight: Int): Boolean {
        if (output.inputs == null || output.inputs.size == 0) {
            return true
        }

        val outputSpentBlockHeight = output.inputs.firstOrNull()?.transaction?.block?.height

        return outputSpentBlockHeight == null || bestBlockHeight - outputSpentBlockHeight < 100
    }

}
