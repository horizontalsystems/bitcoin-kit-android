package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.transactions.scripts.Script
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType
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

                    getPubKey(address.hash, realm)?.let { pubKey ->
                        if (pubKey.scriptHashP2WPKH.contentEquals(address.hash)) {
                            output.scriptType = ScriptType.P2WPKHSH
                        }

                        output.publicKey = pubKey
                        transaction.isMine = true
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

    private fun getPubKey(hash: ByteArray, realm: Realm): PublicKey? {
        return realm.where(PublicKey::class.java)
                .beginGroup()
                    .equalTo("publicKeyHash", hash)
                    .or()
                    .equalTo("scriptHashP2WPKH", hash)
                .endGroup()
                .findFirst()
    }
}
