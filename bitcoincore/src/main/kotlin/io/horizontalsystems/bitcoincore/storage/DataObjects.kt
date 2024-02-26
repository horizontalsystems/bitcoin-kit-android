package io.horizontalsystems.bitcoincore.storage

import androidx.room.Embedded
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.utils.HashUtils

class BlockHeader(
        val version: Int,
        val previousBlockHeaderHash: ByteArray,
        val merkleRoot: ByteArray,
        val timestamp: Long,
        val bits: Long,
        val nonce: Long,
        val hash: ByteArray)

open class FullTransaction(
    val header: Transaction,
    val inputs: List<TransactionInput>,
    val outputs: List<TransactionOutput>,
    val forceHashUpdate: Boolean = true
) {

    lateinit var metadata: TransactionMetadata

    init {
        if (forceHashUpdate) {
            setHash(HashUtils.doubleSha256(TransactionSerializer.serialize(this, withWitness = false)))
        }
    }

    fun setHash(hash: ByteArray) {
        header.hash = hash

        metadata = TransactionMetadata(header.hash)

        inputs.forEach {
            it.transactionHash = header.hash
        }
        outputs.forEach {
            it.transactionHash = header.hash
        }
    }

}

class InputToSign(
        val input: TransactionInput,
        val previousOutput: TransactionOutput,
        val previousOutputPublicKey: PublicKey)

class TransactionWithBlock(
        @Embedded val transaction: Transaction,
        @Embedded val block: Block?)

class PublicKeyWithUsedState(
        @Embedded val publicKey: PublicKey,
        val usedCount: Int) {

    val used: Boolean
        get() = usedCount > 0
}

class InputWithPreviousOutput(
    val input: TransactionInput,
    val previousOutput: TransactionOutput?
)

class UnspentOutput(
    @Embedded val output: TransactionOutput,
    @Embedded val publicKey: PublicKey,
    @Embedded val transaction: Transaction,
    @Embedded val block: Block?
)

class UnspentOutputInfo(
    val outputIndex: Int,
    val transactionHash: ByteArray,
    val timestamp: Long,
    val address: String?,
    val value: Long
) {
    companion object {
        fun fromUnspentOutput(unspentOutput: UnspentOutput): UnspentOutputInfo {
            return UnspentOutputInfo(
                unspentOutput.output.index,
                unspentOutput.output.transactionHash,
                unspentOutput.transaction.timestamp,
                unspentOutput.output.address,
                unspentOutput.output.value,
            )
        }
    }
}

class FullTransactionInfo(
        val block: Block?,
        val header: Transaction,
        val inputs: List<InputWithPreviousOutput>,
        val outputs: List<TransactionOutput>,
        val metadata: TransactionMetadata
) {

    val rawTransaction: String
        get() {
            val fullTransaction = FullTransaction(header, inputs.map { it.input }, outputs)
            return TransactionSerializer.serialize(fullTransaction).toHexString()
        }
}

