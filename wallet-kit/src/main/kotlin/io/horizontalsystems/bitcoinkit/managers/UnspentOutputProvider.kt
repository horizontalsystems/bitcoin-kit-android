package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.scripts.ScriptType

class UnspentOutputProvider(private val realmFactory: RealmFactory) {

    fun allUnspentOutputs(): List<TransactionOutput> {

        var unspentOutputs: MutableList<TransactionOutput> = mutableListOf()

        realmFactory.realm.use {
            unspentOutputs = it.where(TransactionOutput::class.java)
                    .isNotNull("publicKey")
                    .`in`("scriptType", arrayOf(ScriptType.P2PKH, ScriptType.P2PK))
                    .isEmpty("inputs")
                    .findAll()
        }

        return unspentOutputs
    }

}
