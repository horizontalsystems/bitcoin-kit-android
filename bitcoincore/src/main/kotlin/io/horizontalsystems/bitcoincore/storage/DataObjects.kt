package io.horizontalsystems.bitcoincore.storage

import android.arch.persistence.room.Embedded
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
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

class FullTransaction(val header: Transaction, val inputs: List<TransactionInput>, val outputs: List<TransactionOutput>) {

    init {
        if (header.hashHexReversed.isEmpty()) {
            header.hash = HashUtils.doubleSha256(TransactionSerializer.serialize(this, withWitness = true))
            header.hashHexReversed = header.hash.toReversedHex()
        }

        inputs.forEach {
            it.transactionHashReversedHex = header.hashHexReversed
        }
        outputs.forEach {
            it.transactionHashReversedHex = header.hashHexReversed
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

class PreviousOutput(val publicKeyPath: String?, val value: Long)

class InputWithPreviousOutput(
        @Embedded val input: TransactionInput,
        @Embedded val previousOutput: PreviousOutput?)

class UnspentOutput(
        @Embedded val output: TransactionOutput,
        @Embedded val publicKey: PublicKey,
        @Embedded val transaction: Transaction,
        @Embedded val block: Block?)

class InputBlock(@Embedded val block: Block?)

class FullOutputInfo(
        @Embedded val output: TransactionOutput,
        @Embedded val input: InputBlock?)

class FullTransactionInfo(
        val block: Block?,
        val header: Transaction,
        val inputs: List<InputWithPreviousOutput>,
        val outputs: List<TransactionOutput>)
