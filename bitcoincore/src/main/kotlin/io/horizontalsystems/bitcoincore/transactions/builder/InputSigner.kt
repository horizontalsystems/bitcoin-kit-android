package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.transactions.scripts.Sighash
import io.horizontalsystems.hdwalletkit.HDWallet

class InputSigner(private val hdWallet: HDWallet, val network: Network) {

    fun sigScriptData(transaction: Transaction, inputsToSign: List<InputToSign>, outputs: List<TransactionOutput>, index: Int): List<ByteArray> {

        val input = inputsToSign[index]
        val prevOutput = input.previousOutput
        val publicKey = input.previousOutputPublicKey

        val privateKey = checkNotNull(hdWallet.privateKey(publicKey.account, publicKey.index, publicKey.external)) {
            throw Error.NoPrivateKey()
        }

        val isWitness = isFork() || prevOutput.scriptType in arrayOf(
                ScriptType.P2WPKH,
                ScriptType.P2WPKHSH
        )

        val txContent = TransactionSerializer.serializeForSignature(transaction, inputsToSign, outputs, index, isWitness) + sigHashType()
        val signature = privateKey.createSignature(txContent) + sigHashType(true)

        if (prevOutput.scriptType == ScriptType.P2PK) {
            return listOf(signature)
        }

        return listOf(signature, publicKey.publicKey)
    }

    private fun sigHashType(typeOnly: Boolean = false): ByteArray {
        val sigHash = if (isFork()) {
            Sighash.FORKID or Sighash.ALL
        } else {
            Sighash.ALL
        }

        if (typeOnly) {
            return byteArrayOf(sigHash.toByte())
        }

        return byteArrayOf(sigHash.toByte(), 0, 0, 0)
    }

    private fun isFork(): Boolean {
        return when (network) {
            // Fork
//            todo: fix it
//            is MainNetBitcoinCash,
//            is TestNetBitcoinCash -> true

            // Bitcoin
            else -> false
        }
    }

    open class Error : Exception() {
        class NoPrivateKey : Error()
        class NoPreviousOutput : Error()
        class NoPreviousOutputAddress : Error()
    }
}
