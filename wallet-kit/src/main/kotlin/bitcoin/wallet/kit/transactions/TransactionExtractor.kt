package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.scripts.Script
import bitcoin.wallet.kit.scripts.ScriptType
import bitcoin.wallet.kit.hdwallet.Address

class TransactionExtractor(private val network: NetworkParameters) {

    fun extract(transaction: Transaction) {
        transaction.outputs.forEach { output ->
            val script = Script(output.lockingScript)
            val pkHash = script.getPubKeyHash()
            if (pkHash != null) {
                output.scriptType = script.getScriptType()
                output.keyHash = pkHash
                output.address = getAddress(pkHash, script.getScriptType())
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
        var addressType = Address.Type.P2PKH
        if (scriptType == ScriptType.P2SH)
            addressType = Address.Type.P2SH

        return Address(addressType, hash, network).toString()
    }
}
