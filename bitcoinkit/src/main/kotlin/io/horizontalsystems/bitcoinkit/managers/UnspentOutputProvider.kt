package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
import io.realm.Sort

class UnspentOutputProvider(private val realmFactory: RealmFactory, private val confirmationsThreshold: Int = 6) {

    fun allUnspentOutputs(): List<TransactionOutput> {
        realmFactory.realm.use { realm ->

            val lastBlockHeight = realm.where(Block::class.java)
                    .sort("height", Sort.DESCENDING)
                    .findFirst()?.height ?: 0

            return realm.where(TransactionOutput::class.java)
                    .isNotNull("publicKey")
                    .notEqualTo("scriptType", ScriptType.UNKNOWN)
                    .isEmpty("inputs")
                    .beginGroup()
                    .equalTo("transactions.isOutgoing", true)
                    .or()
                    .equalTo("transactions.isOutgoing", false)
                    .lessThanOrEqualTo("transactions.block.height", lastBlockHeight - confirmationsThreshold + 1)
                    .endGroup()
                    .findAll()
                    .toList()
        }
    }

    fun getBalance(): Long = allUnspentOutputs().map { it.value }.sum()
}
