package bitcoin.wallet.kit.transactions.builder

import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.scripts.ScriptType
import io.horizontalsystems.hdwalletkit.HDWallet

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

        val privateKey = checkNotNull(hdWallet.privateKey(pubKey.index, if (pubKey.external) HDWallet.Chain.EXTERNAL.ordinal else HDWallet.Chain.INTERNAL.ordinal)) {
            throw NoPrivateKeyException(index)
        }

        val serializedTransaction = transaction.toSignatureByteArray(index) + byteArrayOf(SIGHASH_ALL, 0, 0, 0)

        val signature = privateKey.createSignature(serializedTransaction) + byteArrayOf(SIGHASH_ALL)

        return when (prevOutput.scriptType) {
            ScriptType.P2PK -> listOf(signature)
            else -> listOf(signature, pubKey.publicKey)
        }
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
