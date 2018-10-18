package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.models.PublicKey
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.scripts.Script
import bitcoin.wallet.kit.utils.AddressConverter
import io.realm.Realm

class TransactionExtractor(private val addressConverter: AddressConverter) {

    fun extract(transaction: Transaction, realm: Realm) {
        transaction.outputs.forEach { output ->
            val script = Script(output.lockingScript)
            val pkHash = script.getPubKeyHash()
            if (pkHash != null) {
                output.scriptType = script.getScriptType()
                output.keyHash = pkHash
                output.address = getAddress(pkHash, script.getScriptType())

                realm.where(PublicKey::class.java)
                        .equalTo("publicKeyHash", output.keyHash)
                        .findFirst()
                        ?.let { pubKey ->
                            transaction.isMine = true
                            output.publicKey = pubKey
                        }
            }
        }

        transaction.inputs.forEach { input ->
            val script = Script(input.sigScript)
            val pkHash = script.getPubKeyHashIn()
            if (pkHash != null) {
                input.keyHash = pkHash
                input.address = getAddress(pkHash, script.getScriptType())
            }
        }
    }

    private fun getAddress(hash: ByteArray, scriptType: Int): String {
        return addressConverter.convert(hash, scriptType).string
    }
}
