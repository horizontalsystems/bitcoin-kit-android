package io.horizontalsystems.bitcoinkit.transactions.builder

import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.scripts.ScriptType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.HDWallet.Chain

class InputSigner(private val hdWallet: HDWallet) {

    fun sigScriptData(transaction: Transaction, index: Int): List<ByteArray> {

        val input = checkNotNull(transaction.inputs[index]) {
            throw NoInputAtIndexException(index)
        }

        val prevOutput = checkNotNull(input.previousOutput) {
            throw NoPreviousOutputException(index)
        }

        val pubKey = checkNotNull(prevOutput.publicKey) {
            throw NoPreviousOutputAddressException(index)
        }

        val chainIndex = if (pubKey.external) Chain.EXTERNAL.ordinal else Chain.INTERNAL.ordinal
        val privateKey = checkNotNull(hdWallet.privateKey(pubKey.index, chainIndex)) {
            throw NoPrivateKeyException(index)
        }

        val isWitness = prevOutput.scriptType in arrayOf(ScriptType.P2WPKH, ScriptType.P2WPKHSH)
        val txContent = transaction.toSignatureByteArray(index, isWitness) + byteArrayOf(SIGHASH_ALL, 0, 0, 0)
        val signature = privateKey.createSignature(txContent) + byteArrayOf(SIGHASH_ALL)

        if (prevOutput.scriptType == ScriptType.P2PK) {
            return listOf(signature)
        }

        return listOf(signature, pubKey.publicKey)
    }

    open class InputSignerException(index: Int) : Exception("index: $index")
    class NoInputAtIndexException(index: Int) : InputSignerException(index)
    class NoPreviousOutputException(index: Int) : InputSignerException(index)
    class NoPreviousOutputAddressException(index: Int) : InputSignerException(index)
    class NoPrivateKeyException(index: Int) : InputSignerException(index)

    companion object {
        const val SIGHASH_ALL: Byte = 1
    }

}
