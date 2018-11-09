package io.horizontalsystems.bitcoinkit.managers

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.scripts.ScriptType

class UnspentOutputProvider(private val realmFactory: RealmFactory) {
    fun allUnspentOutputs(): List<TransactionOutput> {
        realmFactory.realm.use {
            return it.where(TransactionOutput::class.java)
                    .isNotNull("publicKey")
                    .notEqualTo("scriptType", ScriptType.UNKNOWN)
                    .isEmpty("inputs")
                    .findAll()
        }
    }
}
