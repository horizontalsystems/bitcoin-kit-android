package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.core.IPrivateWallet
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.hdwalletkit.Utils

class SchnorrInputSigner(
    private val hdWallet: IPrivateWallet
) {
    fun sigScriptData(
        transaction: Transaction,
        inputsToSign: List<InputToSign>,
        outputs: List<TransactionOutput>,
        index: Int
    ): List<ByteArray> {
        val input = inputsToSign[index]
        val publicKey = input.previousOutputPublicKey
        val tweakedPrivateKey = checkNotNull(hdWallet.privateKey(publicKey.account, publicKey.index, publicKey.external).tweakedOutputKey) {
            throw Error.NoPrivateKey()
        }
        val serializedTransaction = TransactionSerializer.serializeForTaprootSignature(transaction, inputsToSign, outputs, index)

        val signatureHash = Utils.taggedHash("TapSighash", serializedTransaction)
        val signature = tweakedPrivateKey.signSchnorr(signatureHash)

        return listOf(signature)
    }

    open class Error : Exception() {
        class NoPrivateKey : Error()
        class NoPreviousOutput : Error()
        class NoPreviousOutputAddress : Error()
    }
}
