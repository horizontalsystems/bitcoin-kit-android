package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.scripts.Script
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.realm.Realm

class TransactionExtractor(private val addressConverter: AddressConverter) {

    fun extract(transaction: Transaction, realm: Realm) {
        transaction.outputs.forEach { output ->
            val script = Script(output.lockingScript)
            val pkHash = script.getPubKeyHash()
            if (pkHash != null) {
                output.scriptType = script.getScriptType()
                try {
                    val address = addressConverter.convert(pkHash, output.scriptType)
                    output.keyHash = address.hash
                    output.address = address.string

                    realm.where(PublicKey::class.java)
                            .equalTo("publicKeyHash", output.keyHash)
                            .findFirst()?.let { pubKey ->
                                transaction.isMine = true
                                output.publicKey = pubKey
                            }
                } catch (e: Exception) {
                }
            }
        }

        transaction.inputs.forEach { input ->
            val script = Script(input.sigScript)
            val pkHash = script.getPubKeyHashIn()
            if (pkHash != null) {
                try {
                    val address = addressConverter.convert(pkHash, script.getScriptType())
                    input.keyHash = address.hash
                    input.address = address.string
                } catch (e: Exception) {
                }
            }
        }
    }

}
