package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.scripts.ScriptType

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
